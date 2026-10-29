package com.omegadex.app.feature.aventure

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.aventure.CocherQueteUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.run.ResultatCreationRun
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AventureViewModelTest {
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var jeuBase: JeuDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var runActive: FausseRunActive
    private lateinit var jeu: JeuRepositoryImpl
    private val session = FausseSession()

    @Before
    fun ouvrir() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        progressionBase = BasesDeTest.progression()
        jeuBase = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        val horloge = Horloge { 1_000L }
        comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        runActive = FausseRunActive(comptes)
        jeu = JeuRepositoryImpl(jeuBase)
    }

    @After
    fun fermer() {
        progressionBase.close()
        jeuBase.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = AventureViewModel(
        ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu),
        jeu,
        progression,
        CocherQueteUseCase(jeu, progression),
    )

    @Test
    fun `sans run l'aventure le dit`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        assertEquals(AventureUiState.AucuneRun, viewModel().uiState.first { it != AventureUiState.Chargement })
    }

    @Test
    fun `seule l'etape en cours est affichee avec ses quetes triees par type`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creer = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creer("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        progression.cocherQuete(run, 2, cochee = true)

        val etat = viewModel().uiState.first { it is AventureUiState.EtapeEnCours } as AventureUiState.EtapeEnCours
        assertEquals(0 to 2, etat.numero to etat.total)
        assertEquals("Départ", etat.titre)
        assertEquals(5, etat.plafond)
        assertEquals(listOf(1), etat.principales.map { it.id })
        assertEquals(listOf(2 to true), etat.annexes.map { it.id to it.cochee })
    }

    @Test
    fun `cocher une quete l'enregistre et met a jour l'ecran`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creer = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creer("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        val viewModel = viewModel()
        viewModel.uiState.first { it is AventureUiState.EtapeEnCours }

        viewModel.cocher(2, cochee = true)
        val apres = viewModel.uiState.first {
            it is AventureUiState.EtapeEnCours && it.annexes.single().cochee
        } as AventureUiState.EtapeEnCours
        assertEquals(listOf(false), apres.principales.map { it.cochee })
        assertEquals(setOf(2), progression.quetesCochees(run).first())

        // Un nouvel écran (redémarrage) relit l'état enregistré.
        val relu = viewModel().uiState.first { it is AventureUiState.EtapeEnCours } as AventureUiState.EtapeEnCours
        assertEquals(true, relu.annexes.single().cochee)
        viewModel.cocher(2, cochee = false)
        assertEquals(emptySet<Int>(), progression.quetesCochees(run).first { it.isEmpty() })
    }

    @Test
    fun `la derniere quete principale valide l'etape et affiche la suivante`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creer = CreerRunUseCase(comptes, progression, session, runActive)
        creer("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val viewModel = viewModel()
        val depart = viewModel.uiState.first { it is AventureUiState.EtapeEnCours } as AventureUiState.EtapeEnCours
        assertEquals(listOf("Route 101"), depart.lieux.map { it.nom })

        viewModel.cocher(1, cochee = true)

        assertEquals(EtapeValidee(0, "Départ", null, "Roxanne", setOf(0)), viewModel.evenements.first())
        val suivante = viewModel.uiState.first {
            it is AventureUiState.EtapeEnCours && it.numero == 1
        } as AventureUiState.EtapeEnCours
        assertEquals("Roxanne", suivante.titre)
        assertEquals(listOf(3), suivante.principales.map { it.id })

        viewModel.cocher(3, cochee = true)
        assertEquals(EtapeValidee(1, "Roxanne", "Badge Roche", null, setOf(0, 1)), viewModel.evenements.first())
        assertEquals(AventureUiState.Terminee, viewModel.uiState.first { it == AventureUiState.Terminee })
    }
}
