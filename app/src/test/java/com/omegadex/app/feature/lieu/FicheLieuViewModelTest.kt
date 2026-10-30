package com.omegadex.app.feature.lieu

import androidx.lifecycle.SavedStateHandle
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.TypeLieu
import com.omegadex.app.domain.rencontre.Debloqueur
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
class FicheLieuViewModelTest {
    private lateinit var base: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var runActive: FausseRunActive
    private val session = FausseSession()

    @Before
    fun ouvrir() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        base = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        progressionBase = BasesDeTest.progression()
        val horloge = Horloge { 1_000L }
        comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        runActive = FausseRunActive(comptes)
    }

    @After
    fun fermer() {
        base.close()
        progressionBase.close()
        Dispatchers.resetMain()
    }

    private fun fiche(lieuId: Int) = FicheLieuViewModel(
        SavedStateHandle(mapOf(FicheLieuViewModel.ARGUMENT_LIEU to lieuId)),
        JeuRepositoryImpl(base),
        ObserverRunEnCoursUseCase(session, runActive, comptes, progression, JeuRepositoryImpl(base)),
    )

    @Test
    fun `la fiche liste les rencontres par methode avec niveaux et les objets importants`() = runTest {
        val etat = fiche(2).uiState.first { it is FicheLieuUiState.Fiche } as FicheLieuUiState.Fiche
        assertEquals("Route 104" to TypeLieu.ROUTE, etat.nom to etat.type)
        val groupe = etat.rencontres.single()
        assertEquals(MethodeRencontre.HERBE, groupe.methode)
        assertEquals(
            LigneRencontre(253, "Massko", "Plante", 12, 14, 20, null, types = listOf("Plante")),
            groupe.lignes.single(),
        )
        assertEquals(listOf("Badge Roche"), etat.objets)
    }

    @Test
    fun `un lieu inconnu est signale`() = runTest {
        assertEquals(FicheLieuUiState.Introuvable, fiche(99).uiState.first { it != FicheLieuUiState.Chargement })
    }

    @Test
    fun `dans une run, une rencontre pas encore possible est verrouillee avec son etape`() = runTest {
        base.openHelper.writableDatabase.execSQL("INSERT INTO rencontre VALUES (3, 2, 254, 'SURF', 20, 25, 5, NULL)")
        val sansRun = fiche(2).uiState.first { it is FicheLieuUiState.Fiche } as FicheLieuUiState.Fiche
        assertTrue(sansRun.rencontres.flatMap { it.lignes }.all { it.verrou == null })

        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val etat = fiche(2).uiState.first {
            it is FicheLieuUiState.Fiche && it.rencontres.any { g -> g.lignes.any { l -> l.verrou != null } }
        } as FicheLieuUiState.Fiche
        val parMethode = etat.rencontres.associate { it.methode to it.lignes.single().verrou }
        assertEquals(
            mapOf(MethodeRencontre.HERBE to Verrou(1, null), MethodeRencontre.SURF to Verrou(6, Debloqueur.SURF)),
            parMethode,
        )
    }
}
