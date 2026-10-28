package com.omegadex.app.data.repository

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgressionRepositoryTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private var maintenant = 1_000L
    private val horloge = Horloge { maintenant }

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, horloge)
        progression = ProgressionRepositoryImpl(base, horloge)
    }

    @After
    fun fermer() = base.close()

    private suspend fun nouvelleRun(identifiant: String): Pair<Long, Long> {
        val utilisateur = requireNotNull(comptes.creerUtilisateur(identifiant, "hash", "sel"))
        return utilisateur to
            comptes.creerRun(utilisateur, "Run de $identifiant", 252, Difficulte.HARDCORE, listOf("nuzlocke"))
    }

    @Test
    fun `un identifiant ne peut pas etre pris deux fois`() = runTest {
        nouvelleRun("Yamao")
        assertNull(comptes.creerUtilisateur("yamao", "autre", "sel"))
        assertEquals(1, comptes.utilisateurs().first().size)
    }

    @Test
    fun `deux comptes ne voient jamais la progression l'un de l'autre`() = runTest {
        val (yamao, runYamao) = nouvelleRun("yamao")
        val (invite, runInvite) = nouvelleRun("invite")
        progression.definirStatut(runYamao, 252, StatutEspece.CAPTUREE)

        assertEquals(listOf(runYamao), comptes.runs(yamao).first().map { it.id })
        assertNull(comptes.run(invite, runYamao).first())
        assertTrue(progression.statuts(runInvite).first().isEmpty())
        assertEquals(listOf("nuzlocke"), comptes.run(yamao, runYamao).first()?.regles)
    }

    @Test
    fun `quetes, etapes et statuts se mettent a jour`() = runTest {
        val (_, run) = nouvelleRun("yamao")
        progression.cocherQuete(run, 1, cochee = true)
        progression.cocherQuete(run, 2, cochee = true)
        progression.cocherQuete(run, 2, cochee = false)
        assertEquals(setOf(1), progression.quetesCochees(run).first())

        progression.deverrouillerEtape(run, 0)
        maintenant = 2_000L
        progression.validerEtape(run, 0)
        val etape = progression.etapes(run).first().single()
        assertEquals(1_000L to 2_000L, etape.dateDeverrouillage to etape.dateValidation)

        progression.definirStatut(run, 252, StatutEspece.VUE)
        progression.definirStatut(run, 252, StatutEspece.ENTRAINEE)
        assertEquals(mapOf(252 to StatutEspece.ENTRAINEE), progression.statuts(run).first())
    }

    @Test
    fun `equipe de 6 emplacements et defis`() = runTest {
        val (_, run) = nouvelleRun("yamao")
        progression.placerMembre(run, MembreEquipe(1, 252, "Gecko", 5))
        progression.placerMembre(run, MembreEquipe(1, 253, null, 16))
        assertEquals(listOf(253), progression.equipe(run).first().map { it.especeId })
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { progression.placerMembre(run, MembreEquipe(7, 252, null, 5)) }
        }

        val defi = progression.ajouterDefi(run, modeleId = 3, especeId = 253, etapeId = null)
        maintenant = 5_000L
        progression.changerEtatDefi(run, defi, EtatDefi.REUSSI)
        val resultat = progression.defis(run).first().single()
        assertEquals(EtatDefi.REUSSI to 5_000L, resultat.etat to resultat.dateFin)
    }
}
