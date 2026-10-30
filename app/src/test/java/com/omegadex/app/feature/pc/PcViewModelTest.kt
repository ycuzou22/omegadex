package com.omegadex.app.feature.pc

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.equipe.DefinirDetailsUseCase
import com.omegadex.app.domain.equipe.DeposerAuPcUseCase
import com.omegadex.app.domain.equipe.NOMBRE_BOITES
import com.omegadex.app.domain.equipe.ResultatPc
import com.omegadex.app.domain.equipe.RetirerDuPcUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PcViewModelTest {
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

    private fun pc() = PcViewModel(
        ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu),
        progression,
        jeu,
        DeposerAuPcUseCase(progression),
        RetirerDuPcUseCase(progression),
        DefinirDetailsUseCase(progression),
    )

    private suspend fun nouvelleRun(): Long {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        return requireNotNull(runActive.runActive(1).first())
    }

    private suspend fun PcViewModel.etat(condition: (PcUiState.Pc) -> Boolean = { true }) =
        uiState.first { it is PcUiState.Pc && condition(it) } as PcUiState.Pc

    @Test
    fun `sans run, pas de PC`() = runTest(timeout = DELAI_CI) {
        assertEquals(PcUiState.AucuneRun, pc().uiState.first { it != PcUiState.Chargement })
    }

    @Test
    fun `deposer puis reprendre un membre, avec sa fiche`() = runTest(timeout = DELAI_CI) {
        val run = nouvelleRun()
        progression.equipe(run).first().forEach { progression.retirerMembre(run, it.emplacement) }
        progression.placerMembre(run, MembreEquipe(1, 252, null, 10))
        progression.placerMembre(run, MembreEquipe(2, 253, "Gecko", 18))
        val vm = pc()
        assertEquals(30, vm.etat().cases.size)

        vm.selectionner(PlacePokemon.Equipe(2))
        val fiche = requireNotNull(vm.etat { it.fiche != null }.fiche)
        assertEquals("Gecko", fiche.pokemon.surnom)
        assertEquals(18, fiche.pokemon.niveau)

        vm.deplacer()
        val apres = vm.etat { it.occupees == 1 && it.equipe[1] == null && it.fiche == null }
        assertNull(apres.fiche)
        assertNull(apres.equipe[1])
        val range = requireNotNull(apres.cases.first())

        vm.selectionner(range.place)
        vm.etat { it.fiche?.pokemon?.place == range.place }
        vm.definirNature(Nature.JOVIAL)
        assertEquals(Nature.JOVIAL, vm.etat { it.fiche?.pokemon?.nature != null }.fiche?.pokemon?.nature)

        vm.deplacer()
        assertEquals(253, vm.etat { it.occupees == 0 && it.equipe[1] != null }.equipe[1]?.especeId)
    }

    @Test
    fun `le dernier membre ne peut pas partir et les boites tournent en boucle`() = runTest(timeout = DELAI_CI) {
        val run = nouvelleRun()
        progression.equipe(run).first().forEach { progression.retirerMembre(run, it.emplacement) }
        progression.placerMembre(run, MembreEquipe(1, 252, null, 10))
        val vm = pc()
        vm.etat()

        vm.selectionner(PlacePokemon.Equipe(1))
        vm.etat { it.fiche != null }
        vm.deplacer()
        assertEquals(ResultatPc.DernierMembre, vm.etat { it.refus != null }.refus)

        vm.boitePrecedente()
        assertEquals(NOMBRE_BOITES, vm.etat { it.boite == NOMBRE_BOITES }.boite)
        vm.boiteSuivante()
        assertEquals(1, vm.etat { it.boite == 1 }.boite)
    }

    private companion object {
        /** Room et Robolectric sont lents sur la CI : les 10 s par défaut de runTest ne suffisent pas toujours. */
        val DELAI_CI = 60.seconds
    }
}
