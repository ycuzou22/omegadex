package com.omegadex.app.securite

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.AccesRunRefuse
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.data.securite.HacheurArgon2
import com.omegadex.app.domain.compte.InscrireUseCase
import com.omegadex.app.domain.compte.ResultatInscription
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.testutil.FausseSession
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Garanties de sécurité des comptes locaux (T06.4) :
 * empreinte non réversible, sel propre à chaque compte, données strictement isolées entre deux comptes.
 */
@RunWith(RobolectricTestRunner::class)
class SecuriteComptesTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private val session = FausseSession()
    private val hacheur = HacheurArgon2()
    private val horloge = Horloge { 1_000L }

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, horloge)
        progression = ProgressionRepositoryImpl(base, horloge, session)
    }

    @After
    fun fermer() = base.close()

    private suspend fun inscrire(identifiant: String, motDePasse: String): Long {
        val resultat =
            InscrireUseCase(comptes, hacheur, session)(identifiant, motDePasse.toCharArray(), motDePasse.toCharArray())
        return (resultat as ResultatInscription.Reussie).utilisateurId
    }

    @Test
    fun `l'empreinte stockee ne contient pas le mot de passe et ne se dechiffre pas`() = runTest {
        inscrire("Yamao", MOT_DE_PASSE)
        val stocke = requireNotNull(comptes.identifiants("Yamao"))

        assertFalse(stocke.hashMotDePasse.contains(MOT_DE_PASSE))
        val octets = Base64.getDecoder().decode(stocke.hashMotDePasse.substringAfterLast('$'))
        assertEquals(TAILLE_HASH, octets.size)
        assertFalse(String(octets, Charsets.ISO_8859_1).contains(MOT_DE_PASSE))
        // Fonction à sens unique : seule une nouvelle dérivation avec le même sel retrouve l'empreinte.
        assertTrue(hacheur.verifier(MOT_DE_PASSE.toCharArray(), stocke.sel, stocke.hashMotDePasse))
        assertFalse(hacheur.verifier("rubis-omegA".toCharArray(), stocke.sel, stocke.hashMotDePasse))
        assertFalse(hacheur.verifier(MOT_DE_PASSE.toCharArray(), hacheur.nouveauSel(), stocke.hashMotDePasse))
    }

    @Test
    fun `deux comptes avec le meme mot de passe ont un sel et une empreinte differents`() = runTest {
        inscrire("Yamao", MOT_DE_PASSE)
        inscrire("Invite", MOT_DE_PASSE)
        val yamao = requireNotNull(comptes.identifiants("Yamao"))
        val invite = requireNotNull(comptes.identifiants("Invite"))

        assertNotEquals(yamao.sel, invite.sel)
        assertNotEquals(yamao.hashMotDePasse, invite.hashMotDePasse)
        assertEquals(TAILLE_SEL, Base64.getDecoder().decode(yamao.sel).size)
    }

    @Test
    fun `les sels sont aleatoires`() {
        val sels = List(NOMBRE_SELS) { hacheur.nouveauSel() }
        assertEquals(NOMBRE_SELS, sels.toSet().size)
    }

    @Test
    fun `deux comptes ne voient ni ne modifient jamais les donnees l'un de l'autre`() = runTest {
        val yamao = inscrire("Yamao", MOT_DE_PASSE)
        val runYamao = comptes.creerRun(yamao, "Run Yamao", 252, Difficulte.NORMAL, emptyList())
        remplir(runYamao)

        val invite = inscrire("Invite", "saphir-alpha")
        val runInvite = comptes.creerRun(invite, "Run Invite", 255, Difficulte.NORMAL, emptyList())

        // Le compte invité ne lit rien de la run de Yamao, ni par la liste des runs ni par son identifiant.
        assertEquals(listOf(runInvite), comptes.runs(invite).first().map { it.id })
        assertEquals(null, comptes.run(invite, runYamao).first())
        assertTrue(progression.etapes(runYamao).first().isEmpty())
        assertTrue(progression.quetesCochees(runYamao).first().isEmpty())
        assertTrue(progression.statuts(runYamao).first().isEmpty())
        assertTrue(progression.equipe(runYamao).first().isEmpty())
        assertTrue(progression.defis(runYamao).first().isEmpty())

        // Et ne peut rien y écrire.
        val ecritures: List<suspend () -> Unit> = listOf(
            { progression.deverrouillerEtape(runYamao, 1) },
            { progression.validerEtape(runYamao, 0) },
            { progression.cocherQuete(runYamao, 1, cochee = false) },
            { progression.definirStatut(runYamao, 252, StatutEspece.VUE) },
            { progression.placerMembre(runYamao, MembreEquipe(2, 25, null, 5)) },
            { progression.retirerMembre(runYamao, 1) },
            { progression.ajouterDefi(runYamao, modeleId = 1, especeId = null, etapeId = null) },
            { progression.changerEtatDefi(runYamao, 1, EtatDefi.RATE) },
        )
        ecritures.forEach { ecriture -> assertThrows(AccesRunRefuse::class.java) { runBlocking { ecriture() } } }

        // Les données de Yamao sont intactes quand il se reconnecte.
        session.ouvrir(yamao)
        assertEquals(1, progression.etapes(runYamao).first().size)
        assertEquals(setOf(1), progression.quetesCochees(runYamao).first())
        assertEquals(mapOf(252 to StatutEspece.CAPTUREE), progression.statuts(runYamao).first())
        assertEquals(listOf(252), progression.equipe(runYamao).first().map { it.especeId })
        assertEquals(EtatDefi.ACTIF, progression.defis(runYamao).first().single().etat)
    }

    @Test
    fun `sans joueur connecte aucune progression n'est lisible ni modifiable`() = runTest {
        val yamao = inscrire("Yamao", MOT_DE_PASSE)
        val run = comptes.creerRun(yamao, "Run Yamao", 252, Difficulte.NORMAL, emptyList())
        remplir(run)
        session.fermer()

        assertTrue(progression.statuts(run).first().isEmpty())
        assertTrue(progression.equipe(run).first().isEmpty())
        assertThrows(AccesRunRefuse::class.java) { runBlocking { progression.cocherQuete(run, 2, cochee = true) } }
    }

    private suspend fun remplir(run: Long) {
        progression.validerEtape(run, 0)
        progression.cocherQuete(run, 1, cochee = true)
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.placerMembre(run, MembreEquipe(1, 252, "Gecko", 5))
        progression.ajouterDefi(run, modeleId = 1, especeId = 252, etapeId = null)
    }

    private companion object {
        const val MOT_DE_PASSE = "rubis-omega"
        const val TAILLE_HASH = 32
        const val TAILLE_SEL = 16
        const val NOMBRE_SELS = 200
    }
}
