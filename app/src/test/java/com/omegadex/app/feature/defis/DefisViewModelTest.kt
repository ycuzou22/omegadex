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
import com.omegadex.app.domain.defi.ActionsSurDefis
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.defi.RetenterDefiUseCase
import com.omegadex.app.domain.defi.ValiderDefiUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.TypeDefi
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
        fabrique = { etat ->
            DefisViewModel(
                etat,
                ObserverDefisUseCase(runEnCours, progression, jeu),
                ProposerDefisUseCase(runEnCours, progression, jeu),
                runEnCours,
                ActionsSurDefis(
                    AccepterDefiUseCase(progression),
                    ValiderDefiUseCase(progression),
                    RetenterDefiUseCase(progression),
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

    @Test
    fun `relever puis valider un defi met a jour les listes et la jauge`() = runTest {
        val vm = fabrique(SavedStateHandle())
        val depart = vm.espace()
        assertTrue(depart.actifs.isEmpty())
        val defiEtape = depart.disponibles.first { it.type == TypeDefi.ETAPE }

        vm.choisirOnglet(OngletDefis.DISPONIBLES)
        vm.relever(defiEtape.cle)
        // Le défi relevé quitte les disponibles dès que les propositions sont recalculées.
        val actif = vm.espace { espace ->
            espace.actifs.size == 1 && espace.disponibles.none { it.titre == defiEtape.titre }
        }
        assertEquals(OngletDefis.ACTIFS, actif.onglet)

        vm.valider(requireNotNull(actif.actifs.single().defiId), reussi = true)
        val termine = vm.espace { it.termines.size == 1 }
        assertEquals(EtatDefi.REUSSI, termine.termines.single().etat)
        assertEquals(1f, termine.bilan.tauxReussite)

        vm.filtrer(Difficulte.DIFFICILE)
        assertTrue(vm.espace { it.filtre == Difficulte.DIFFICILE }.disponibles.isEmpty())
    }

    @Test
    fun `la page d'un pokemon montre son defi tire au sort et refuse un second defi actif`() = runTest {
        val vm = fabrique(SavedStateHandle(mapOf(DefisViewModel.ARGUMENT_ESPECE to 252)))
        val page = vm.espace { it.especeNom != null && it.disponibles.isNotEmpty() }
        assertEquals("Arcko", page.especeNom)
        val tire = page.disponibles.single()
        assertEquals(252, tire.especeId)

        vm.relever(tire.cle)
        vm.espace { it.actifs.size == 1 && it.disponibles.isEmpty() }
        vm.relever(tire.cle)
        vm.refusEspece.first()
        assertEquals(1, vm.espace().actifs.size)
    }
}
