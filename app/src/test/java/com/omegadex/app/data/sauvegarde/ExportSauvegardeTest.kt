package com.omegadex.app.data.sauvegarde

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.data.repository.SauvegardeRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.sauvegarde.ExporterProgressionUseCase
import com.omegadex.app.domain.sauvegarde.ResultatExport
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import com.omegadex.app.testutil.FausseSession
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportSauvegardeTest {
    /** Garde en mémoire ce qui aurait été écrit dans le fichier choisi. */
    private class FauxFichiers : FichiersSauvegarde {
        val ecrits = mutableMapOf<String, String>()
        var enPanne = false

        override suspend fun ecrire(destination: String, sauvegarde: Sauvegarde) {
            if (enPanne) throw IOException("stockage plein")
            ecrits[destination] = SauvegardeJson.encoder(sauvegarde)
        }

        override suspend fun lire(source: String) =
            SauvegardeJson.decoder(ecrits[source] ?: throw IOException("fichier absent"))
    }

    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var exporter: ExporterProgressionUseCase
    private val fichiers = FauxFichiers()
    private val session = FausseSession()
    private var maintenant = 1_000L
    private val horloge = Horloge { maintenant }

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, horloge)
        progression = ProgressionRepositoryImpl(base, horloge, session)
        exporter = ExporterProgressionUseCase(session, comptes, SauvegardeRepositoryImpl(base), fichiers, horloge)
    }

    @After
    fun fermer() = base.close()

    /** Crée un compte, le connecte et remplit une run avec un peu de tout. */
    private suspend fun compteAvecProgression(identifiant: String): Long {
        val utilisateur = requireNotNull(comptes.creerUtilisateur(identifiant, EMPREINTE, SEL))
        session.ouvrir(utilisateur)
        val run = comptes.creerRun(utilisateur, "Run de $identifiant", 252, Difficulte.HARDCORE, listOf("nuzlocke"))
        progression.deverrouillerEtape(run, 0)
        progression.validerEtape(run, 0)
        progression.cocherQuete(run, 1, cochee = true)
        progression.definirStatut(run, 252, StatutEspece.ENTRAINEE)
        progression.placerMembre(run, MembreEquipe(1, 252, "Gecko", 12))
        val defi = progression.ajouterDefi(run, modeleId = 3, especeId = 252, etapeId = null)
        progression.changerEtatDefi(run, defi, EtatDefi.REUSSI)
        return utilisateur
    }

    @Test
    fun `l'export contient toute la progression du compte connecte et seulement elle`() = runTest {
        compteAvecProgression("invite")
        compteAvecProgression("yamao")
        maintenant = 9_000L

        val sauvegarde = requireNotNull(exporter.construire())

        assertEquals(Sauvegarde.VERSION_FORMAT, sauvegarde.version)
        assertEquals(9_000L, sauvegarde.dateExport)
        assertEquals("yamao", sauvegarde.identifiant)
        val run = sauvegarde.runs.single()
        assertEquals("Run de yamao", run.nom)
        assertEquals(Difficulte.HARDCORE to listOf("nuzlocke"), run.difficulte to run.regles)
        assertEquals(listOf(0), run.etapes.filter { it.dateValidation != null }.map { it.etapeId })
        assertEquals(listOf(1), run.quetesCochees.map { it.queteId })
        assertEquals(StatutEspece.ENTRAINEE, run.especes.single().statut)
        assertEquals(MembreEquipe(1, 252, "Gecko", 12), run.equipe.single())
        assertEquals(EtatDefi.REUSSI, run.defis.single().etat)
    }

    @Test
    fun `le fichier porte le format et sa version, sans aucun mot de passe`() = runTest {
        compteAvecProgression("yamao")

        assertEquals(ResultatExport.Reussi(nombreRuns = 1), exporter("content://sauvegarde.json"))

        val texte = requireNotNull(fichiers.ecrits["content://sauvegarde.json"])
        val json = JSONObject(texte)
        assertEquals(SauvegardeJson.FORMAT, json.getString(SauvegardeJson.Cles.FORMAT))
        assertEquals(Sauvegarde.VERSION_FORMAT, json.getInt(SauvegardeJson.Cles.VERSION))
        assertFalse(texte.contains(EMPREINTE))
        assertFalse(texte.contains(SEL))
        assertFalse(texte.contains("hash", ignoreCase = true))
        assertFalse(texte.contains("motDePasse", ignoreCase = true))
        val defi = json.getJSONArray(SauvegardeJson.Cles.RUNS).getJSONObject(0)
            .getJSONArray(SauvegardeJson.Cles.DEFIS).getJSONObject(0)
        assertTrue(defi.isNull(SauvegardeJson.Cles.ETAPE_ID))
    }

    @Test
    fun `sans joueur connecte rien n'est exporte`() = runTest {
        compteAvecProgression("yamao")
        session.fermer()

        assertNull(exporter.construire())
        assertEquals(ResultatExport.NonConnecte, exporter("content://sauvegarde.json"))
        assertTrue(fichiers.ecrits.isEmpty())
    }

    @Test
    fun `un fichier impossible a ecrire donne un echec propre`() = runTest {
        compteAvecProgression("yamao")
        fichiers.enPanne = true

        assertEquals(ResultatExport.EchecEcriture, exporter("content://sauvegarde.json"))
    }

    private companion object {
        const val EMPREINTE = "empreinte-argon2-secrete"
        const val SEL = "sel-tres-secret"
    }
}
