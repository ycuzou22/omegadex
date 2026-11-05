package com.omegadex.app.feature.defis

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.di.DatabaseModule
import com.omegadex.app.domain.defi.AccepterDefiUseCase
import com.omegadex.app.domain.defi.ActionsRoulette
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.ObserverRouletteUseCase
import com.omegadex.app.domain.defi.ReleverTirageUseCase
import com.omegadex.app.domain.defi.RouletteDefis
import com.omegadex.app.domain.defi.TournerRouletteUseCase
import com.omegadex.app.domain.defi.ActionsSurDefis
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.defi.RetenterDefiUseCase
import com.omegadex.app.domain.defi.ValiderDefiUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import com.omegadex.app.testutil.FauxTirages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefisViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var fabrique: (SavedStateHandle) -> DefisViewModel
    private var maintenant = 1_000L

    @Before
    fun ouvrir() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        jeuBase = DatabaseModule.jeuDatabase(context)
        progressionBase = BasesDeTest.progression()
        val jeu = JeuRepositoryImpl(jeuBase)
        val session = FausseSession()
        val horloge = Horloge { maintenant++ }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        val progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val runActive = FausseRunActive(comptes)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val run = requireNotNull(runActive.runActive(1).first())
        progression.placerMembre(run, MembreEquipe(1, 252, null, 5))
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        val observerDefis = ObserverDefisUseCase(runEnCours, progression, jeu)
        val proposer = ProposerDefisUseCase(runEnCours, progression, jeu)
        val accepter = AccepterDefiUseCase(progression, jeu)
        val tirages = FauxTirages()
        fabrique = { etat ->
            DefisViewModel(
                etat,
                observerDefis,
                runEnCours,
                ActionsSurDefis(accepter, ValiderDefiUseCase(progression), RetenterDefiUseCase(progression)),
                ActionsRoulette(
                    ObserverRouletteUseCase(runEnCours, proposer, observerDefis, tirages, progression, jeu),
                    TournerRouletteUseCase(tirages),
                    ReleverTirageUseCase(accepter, tirages),
                ),
                jeu,
            )
        }
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        Dispatchers.resetMain()
    }

    private suspend fun DefisViewModel.espace(condition: (DefisUiState.Espace) -> Boolean = { true }) =
        uiState.first { it is DefisUiState.Espace && condition(it) } as DefisUiState.Espace

    private fun DefisUiState.Espace.case(categorie: CategorieDefi) =
        requireNotNull(roulette).cases.first { it.categorie == categorie }

    @Test
    fun `tourner la roulette, relever le defi tire puis le valider`() = runTest {
        val vm = fabrique(SavedStateHandle())
        val depart = vm.espace { it.roulette != null }
        assertEquals(OngletDefis.ROULETTE, depart.onglet)
        assertEquals(CategorieDefi.entries, requireNotNull(depart.roulette).cases.map { it.categorie })
        val champion = depart.case(CategorieDefi.CHAMPION)
        assertEquals("l'étape de départ n'a pas de badge : le combat est ouvert", EtatCase.LIBRE, champion.etat)

        vm.tourner(CategorieDefi.CHAMPION)
        val tour = vm.toursDeRoulette.first()
        assertEquals(tour.gagnant, tour.rouleau[RouletteDefis.ARRET_ROULEAU])
        val tire = vm.espace { it.case(CategorieDefi.CHAMPION).etat == EtatCase.TIRE }.case(CategorieDefi.CHAMPION)
        assertEquals(tour.gagnant.titre, tire.defi?.titre)
        assertNull("relances illimitées en Normal", tire.relancesRestantes)

        vm.releverTirage(CategorieDefi.CHAMPION)
        val actif = vm.espace { it.actifs.size == 1 && it.case(CategorieDefi.CHAMPION).etat == EtatCase.EN_COURS }
        assertFalse("une catégorie occupée ne tourne plus", actif.case(CategorieDefi.CHAMPION).peutTourner)
        assertEquals(1, requireNotNull(actif.roulette).occupees)

        vm.choisirOnglet(OngletDefis.ACTIFS)
        vm.valider(requireNotNull(actif.actifs.single().defiId), reussi = true)
        val termine = vm.espace { it.termines.size == 1 }
        assertEquals(EtatDefi.REUSSI, termine.termines.single().etat)
        assertEquals(1f, termine.bilan.tauxReussite)

        vm.filtrer(Difficulte.DIFFICILE)
        assertTrue(vm.espace { it.filtre == Difficulte.DIFFICILE }.termines.isEmpty())
    }

    @Test
    fun `la page d'un pokemon n'a pas de roulette`() = runTest {
        val vm = fabrique(SavedStateHandle(mapOf(DefisViewModel.ARGUMENT_ESPECE to 252)))
        val page = vm.espace { it.especeNom != null }
        assertEquals("Arcko", page.especeNom)
        assertNull(page.roulette)
        assertEquals(listOf(OngletDefis.ACTIFS, OngletDefis.TERMINES), page.onglets)
        assertEquals(OngletDefis.ACTIFS, page.onglet)
    }
}
