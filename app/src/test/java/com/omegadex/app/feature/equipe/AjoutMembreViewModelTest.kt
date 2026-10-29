package com.omegadex.app.feature.equipe

import androidx.lifecycle.SavedStateHandle
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.defi.AccepterDefiUseCase
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.equipe.AjouterMembreUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AjoutMembreViewModelTest {
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var runActive: FausseRunActive
    private lateinit var jeu: JeuRepositoryImpl
    private val session = FausseSession()

    @Before
    fun ouvrir() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        jeuBase = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        progressionBase = BasesDeTest.progression()
        jeu = JeuRepositoryImpl(jeuBase)
        val horloge = Horloge { 1_000L }
        comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        runActive = FausseRunActive(comptes)
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
        Dispatchers.resetMain()
    }

    private fun ajout(emplacement: Int): AjoutMembreViewModel {
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        return AjoutMembreViewModel(
            SavedStateHandle(mapOf(AjoutMembreViewModel.ARGUMENT_EMPLACEMENT to emplacement)),
            runEnCours,
            progression,
            jeu,
            AjouterMembreUseCase(progression),
            ProposerDefisUseCase(runEnCours, progression, jeu),
            AccepterDefiUseCase(progression),
        )
    }

    private suspend fun AjoutMembreViewModel.choix(condition: (AjoutMembreUiState.Choix) -> Boolean = { true }) =
        uiState.first { it is AjoutMembreUiState.Choix && condition(it) } as AjoutMembreUiState.Choix

    @Test
    fun `seules les captures hors equipe sont proposees, et la recherche les filtre`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val run = requireNotNull(runActive.runActive(1).first())
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.definirStatut(run, 253, StatutEspece.ENTRAINEE)
        progression.definirStatut(run, 254, StatutEspece.VUE)
        progression.definirStatut(run, 1, StatutEspece.CAPTUREE)
        progression.placerMembre(run, MembreEquipe(1, 1, null, 5))

        val vm = ajout(2)
        assertEquals(listOf("Arcko", "Massko"), vm.choix().especes.map { it.nom }.sorted())
        vm.rechercher("mass")
        assertEquals(listOf("Massko"), vm.choix { it.recherche == "mass" }.especes.map { it.nom })
    }

    @Test
    fun `choisir puis ajouter place la capture dans l'emplacement et ferme l'ecran`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val run = requireNotNull(runActive.runActive(1).first())
        progression.definirStatut(run, 253, StatutEspece.CAPTUREE)

        val vm = ajout(3)
        val massko = vm.choix().especes.single { it.id == 253 }
        vm.choisir(massko)
        assertEquals(massko, vm.choix { it.choisie != null }.choisie)
        vm.ajouter(12)
        vm.choix { it.termine || it.defiPropose != null }
        assertEquals(MembreEquipe(3, 253, null, 12), progression.equipe(run).first().single())
        assertTrue(vm.choix().choisie == null)
    }
}
