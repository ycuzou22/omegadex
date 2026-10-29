package com.omegadex.app.feature.sauvegarde

import com.omegadex.app.core.Horloge
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.repository.SauvegardeRepository
import com.omegadex.app.domain.sauvegarde.ExporterProgressionUseCase
import com.omegadex.app.domain.sauvegarde.ImporterSauvegardeUseCase
import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import com.omegadex.app.domain.sauvegarde.SauvegardeInvalide
import com.omegadex.app.testutil.FausseSession
import com.omegadex.app.testutil.FauxComptes
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SauvegardeViewModelTest {
    /** Runs des comptes, en mémoire. */
    private class FaussesSauvegardes : SauvegardeRepository {
        val runs = mutableMapOf<Long, MutableList<RunSauvegardee>>()

        override suspend fun runs(utilisateurId: Long) = runs[utilisateurId].orEmpty()

        override suspend fun restaurer(utilisateurId: Long, runs: List<RunSauvegardee>): List<Long> {
            this.runs.getOrPut(utilisateurId) { mutableListOf() }.addAll(runs)
            return runs.indices.map { it.toLong() + 1 }
        }
    }

    /** Fichiers en mémoire ; « illisible » et « protege » simulent les erreurs du stockage. */
    private class FauxFichiers : FichiersSauvegarde {
        val fichiers = mutableMapOf<String, Sauvegarde>()

        override suspend fun ecrire(destination: String, sauvegarde: Sauvegarde) {
            if (destination == "protege") throw IOException("lecture seule")
            fichiers[destination] = sauvegarde
        }

        override suspend fun lire(source: String): Sauvegarde = when (source) {
            "illisible" -> throw IOException("fichier supprimé")
            "photo.jpg" -> throw SauvegardeInvalide("Ce fichier n'est pas un fichier JSON")
            else -> fichiers[source] ?: throw IOException("introuvable")
        }
    }

    private val session = FausseSession()
    private val comptes = FauxComptes()
    private val sauvegardes = FaussesSauvegardes()
    private val fichiers = FauxFichiers()
    private val jour = LocalDate.of(2026, 10, 29)
    private val horloge = Horloge { jour.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }

    private fun run(nom: String) = RunSauvegardee(
        nom = nom,
        starterId = 252,
        difficulte = Difficulte.NORMAL,
        regles = emptyList(),
        dateDebut = 1_000L,
        dateFin = null,
        etapes = emptyList(),
        quetesCochees = emptyList(),
        especes = emptyList(),
        equipe = emptyList(),
        defis = emptyList(),
    )

    private fun viewModel() = SauvegardeViewModel(
        ExporterProgressionUseCase(session, comptes, sauvegardes, fichiers, horloge),
        ImporterSauvegardeUseCase(session, sauvegardes, fichiers),
        horloge,
    )

    private suspend fun connecter(identifiant: String): Long {
        val id = requireNotNull(comptes.creerUtilisateur(identifiant, "h", "s"))
        session.ouvrir(id)
        return id
    }

    @Before
    fun preparer() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun nettoyer() = Dispatchers.resetMain()

    @Test
    fun `le fichier propose porte la date du jour`() {
        assertEquals("omegadex-sauvegarde-2026-10-29.json", viewModel().nomFichierPropose())
    }

    @Test
    fun `exporter ecrit la progression du compte et annonce le nombre de runs`() = runTest {
        val yamao = connecter("Yamao")
        sauvegardes.runs[yamao] = mutableListOf(run("Rubis"), run("Nuzlocke"))
        val viewModel = viewModel()

        viewModel.exporter("content://sauvegarde.json")

        assertEquals(MessageSauvegarde.Exportee(2), viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.enCours)
        val fichier = fichiers.fichiers.getValue("content://sauvegarde.json")
        assertEquals("Yamao", fichier.identifiant)
        assertEquals(listOf("Rubis", "Nuzlocke"), fichier.runs.map { it.nom })
    }

    @Test
    fun `importer ajoute les runs nouvelles et signale celles deja presentes`() = runTest {
        val yamao = connecter("Yamao")
        sauvegardes.runs[yamao] = mutableListOf(run("Rubis"))
        fichiers.fichiers["content://ancienne.json"] =
            Sauvegarde(Sauvegarde.VERSION_FORMAT, 0, "Yamao", listOf(run("Rubis"), run("Hardcore")))
        val viewModel = viewModel()

        viewModel.importer("content://ancienne.json")

        assertEquals(MessageSauvegarde.Importee(runsImportees = 1, runsIgnorees = 1), viewModel.uiState.value.message)
        assertEquals(listOf("Rubis", "Hardcore"), sauvegardes.runs.getValue(yamao).map { it.nom })
    }

    @Test
    fun `un fichier invalide est refuse avec sa raison et rien n'est importe`() = runTest {
        val yamao = connecter("Yamao")
        val viewModel = viewModel()

        viewModel.importer("photo.jpg")
        assertEquals(
            MessageSauvegarde.FichierInvalide("Ce fichier n'est pas un fichier JSON"),
            viewModel.uiState.value.message,
        )

        fichiers.fichiers["content://future.json"] = Sauvegarde(Sauvegarde.VERSION_FORMAT + 1, 0, "Yamao", emptyList())
        viewModel.importer("content://future.json")
        val message = viewModel.uiState.value.message
        assertTrue(message is MessageSauvegarde.FichierInvalide)
        assertNull(sauvegardes.runs[yamao])
    }

    @Test
    fun `les erreurs de stockage et l'absence de joueur ont chacune leur message`() = runTest {
        val viewModel = viewModel()
        viewModel.exporter("content://x.json")
        assertEquals(MessageSauvegarde.NonConnecte, viewModel.uiState.value.message)
        viewModel.importer("content://x.json")
        assertEquals(MessageSauvegarde.NonConnecte, viewModel.uiState.value.message)

        connecter("Yamao")
        viewModel.exporter("protege")
        assertEquals(MessageSauvegarde.EchecEcriture, viewModel.uiState.value.message)
        viewModel.importer("illisible")
        assertEquals(MessageSauvegarde.EchecLecture, viewModel.uiState.value.message)

        viewModel.effacerMessage()
        assertNull(viewModel.uiState.value.message)
    }
}
