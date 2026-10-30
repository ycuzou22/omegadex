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
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.sauvegarde.ExporterProgressionUseCase
import com.omegadex.app.domain.sauvegarde.ImporterSauvegardeUseCase
import com.omegadex.app.domain.sauvegarde.ResultatExport
import com.omegadex.app.domain.sauvegarde.ResultatImport
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import com.omegadex.app.testutil.FausseSession
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportSauvegardeTest {
    /** Fichiers en mémoire : le texte écrit est relu tel quel, comme depuis le stockage. */
    private class FauxFichiers : FichiersSauvegarde {
        val contenus = mutableMapOf<String, String>()

        override suspend fun ecrire(destination: String, sauvegarde: Sauvegarde) {
            contenus[destination] = SauvegardeJson.encoder(sauvegarde)
        }

        override suspend fun lire(source: String) =
            SauvegardeJson.decoder(contenus[source] ?: throw IOException("fichier supprimé"))
    }

    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var sauvegardes: SauvegardeRepositoryImpl
    private lateinit var exporter: ExporterProgressionUseCase
    private lateinit var importer: ImporterSauvegardeUseCase
    private val fichiers = FauxFichiers()
    private val session = FausseSession()
    private var maintenant = 1_000L
    private val horloge = Horloge { maintenant }

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, horloge)
        progression = ProgressionRepositoryImpl(base, horloge, session)
        sauvegardes = SauvegardeRepositoryImpl(base)
        exporter = ExporterProgressionUseCase(session, comptes, sauvegardes, fichiers, horloge)
        importer = ImporterSauvegardeUseCase(session, sauvegardes, fichiers)
    }

    @After
    fun fermer() = base.close()

    private suspend fun nouveauCompte(identifiant: String): Long {
        val utilisateur = requireNotNull(comptes.creerUtilisateur(identifiant, "hash", "sel"))
        session.ouvrir(utilisateur)
        return utilisateur
    }

    /** Deux runs avec de la progression dans tous les domaines, dont des valeurs nulles. */
    private suspend fun remplir(utilisateur: Long) {
        val run = comptes.creerRun(utilisateur, "Nuzlocke", 252, Difficulte.HARDCORE, listOf("nuzlocke", "niveau"))
        progression.deverrouillerEtape(run, 0)
        maintenant = 2_000L
        progression.validerEtape(run, 0)
        progression.deverrouillerEtape(run, 1)
        progression.cocherQuete(run, 1, cochee = true)
        progression.cocherQuete(run, 3, cochee = true)
        progression.definirStatut(run, 252, StatutEspece.EVOLUEE)
        progression.definirStatut(run, 263, StatutEspece.VUE)
        progression.placerMembre(run, MembreEquipe(1, 253, "Gecko", 18, Nature.RIGIDE, "Engrais"))
        progression.placerMembre(run, MembreEquipe(4, 263, null, 3))
        progression.placerMembre(run, MembreEquipe(5, 270, "Nénu", 9, Nature.CALME, null))
        progression.deposerAuPc(run, emplacement = 5, boite = 2)
        val reussi = progression.ajouterDefi(run, modeleId = 3, especeId = 252, etapeId = null)
        progression.changerEtatDefi(run, reussi, EtatDefi.REUSSI)
        progression.ajouterDefi(run, modeleId = 7, especeId = null, etapeId = 1)
        maintenant = 3_000L
        comptes.creerRun(utilisateur, "Tranquille", 255, Difficulte.NORMAL, emptyList())
    }

    @Test
    fun `un export puis un import restituent la progression a l'identique`() = runTest {
        remplir(nouveauCompte("yamao"))
        assertTrue(exporter("content://yamao.json") is ResultatExport.Reussi)
        val avant = requireNotNull(exporter.construire()).runs

        val neuf = nouveauCompte("nouveau-telephone")
        assertEquals(ResultatImport.Reussi(runsImportees = 2, runsIgnorees = 0), importer("content://yamao.json"))

        assertEquals(avant, sauvegardes.runs(neuf))
        assertEquals(2, comptes.runs(neuf).first().size)
    }

    @Test
    fun `un fichier version 1 s'importe sans PC ni nature`() = runTest {
        remplir(nouveauCompte("yamao"))
        exporter("content://yamao.json")
        val json = JSONObject(requireNotNull(fichiers.contenus["content://yamao.json"]))
        json.put(SauvegardeJson.Cles.VERSION, 1)
        val runs = json.getJSONArray(SauvegardeJson.Cles.RUNS)
        for (i in 0 until runs.length()) {
            val run = runs.getJSONObject(i)
            run.remove(SauvegardeJson.Cles.PC)
            val equipe = run.getJSONArray(SauvegardeJson.Cles.EQUIPE)
            for (j in 0 until equipe.length()) {
                equipe.getJSONObject(j).remove(SauvegardeJson.Cles.NATURE)
                equipe.getJSONObject(j).remove(SauvegardeJson.Cles.TALENT)
            }
        }
        fichiers.contenus["content://v1.json"] = json.toString()
        val neuf = nouveauCompte("ancien-fichier")

        assertEquals(ResultatImport.Reussi(runsImportees = 2, runsIgnorees = 0), importer("content://v1.json"))

        val run = sauvegardes.runs(neuf).first { it.nom == "Nuzlocke" }
        assertTrue(run.pc.isEmpty())
        assertEquals(MembreEquipe(1, 253, "Gecko", 18), run.equipe.first { it.emplacement == 1 })
    }

    @Test
    fun `le PC et les natures font partie de la sauvegarde`() = runTest {
        remplir(nouveauCompte("yamao"))
        val run = requireNotNull(exporter.construire()).runs.first { it.nom == "Nuzlocke" }

        val gecko = run.equipe.first { it.emplacement == 1 }
        assertEquals(Nature.RIGIDE to "Engrais", gecko.nature to gecko.talent)
        val range = run.pc.single()
        assertEquals(2 to 270, range.boite to range.especeId)
        assertEquals("Nénu" to Nature.CALME, range.surnom to range.nature)
    }

    @Test
    fun `importer deux fois la meme sauvegarde ne cree pas de doublon`() = runTest {
        val yamao = nouveauCompte("yamao")
        remplir(yamao)
        exporter("content://yamao.json")

        assertEquals(ResultatImport.Reussi(runsImportees = 0, runsIgnorees = 2), importer("content://yamao.json"))
        assertEquals(2, sauvegardes.runs(yamao).size)
    }

    @Test
    fun `l'import va dans le compte connecte, jamais dans le compte d'origine`() = runTest {
        val yamao = nouveauCompte("yamao")
        remplir(yamao)
        exporter("content://yamao.json")
        val invite = nouveauCompte("invite")

        importer("content://yamao.json")

        assertEquals(2, sauvegardes.runs(invite).size)
        assertEquals(2, sauvegardes.runs(yamao).size)
    }

    @Test
    fun `un fichier invalide est refuse proprement et rien n'est importe`() = runTest {
        remplir(nouveauCompte("yamao"))
        exporter("content://yamao.json")
        val texte = requireNotNull(fichiers.contenus["content://yamao.json"])
        val versionFuture = Sauvegarde.VERSION_FORMAT + 1
        val invalides = mapOf(
            "pas-du-json" to "{ ceci n'est pas du JSON",
            "autre-format" to JSONObject(texte).put("format", "autre-appli").toString(),
            "version-future" to JSONObject(texte).put("version", versionFuture).toString(),
            "champ-manquant" to JSONObject(texte).apply { remove("runs") }.toString(),
            "statut-inconnu" to texte.replace("\"EVOLUEE\"", "\"SHINY\""),
            "date-fin-manquante" to texte.replace("\"dateFin\"", "\"dateFinX\""),
            "emplacement-7" to texte.replace("\"emplacement\": 4", "\"emplacement\": 7"),
        )
        // Chaque altération doit réellement modifier le fichier, sinon le test ne prouverait rien.
        invalides.values.forEach { assertTrue(it != texte) }
        val neuf = nouveauCompte("nouveau-telephone")

        invalides.forEach { (nom, contenu) ->
            fichiers.contenus["content://$nom.json"] = contenu
            val resultat = importer("content://$nom.json")
            assertTrue("$nom devrait être refusé, obtenu $resultat", resultat is ResultatImport.FichierInvalide)
            assertTrue("$nom : raison vide", (resultat as ResultatImport.FichierInvalide).raison.isNotBlank())
        }
        assertTrue(sauvegardes.runs(neuf).isEmpty())
    }

    @Test
    fun `fichier illisible ou joueur deconnecte`() = runTest {
        nouveauCompte("yamao")
        assertEquals(ResultatImport.EchecLecture, importer("content://disparu.json"))

        session.fermer()
        assertEquals(ResultatImport.NonConnecte, importer("content://disparu.json"))
    }
}
