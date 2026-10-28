package com.omegadex.app.feature.lieu

import androidx.lifecycle.SavedStateHandle
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.TypeLieu
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
class FicheLieuViewModelTest {
    private lateinit var base: JeuDatabase

    @Before
    fun ouvrir() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        base = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
    }

    @After
    fun fermer() {
        base.close()
        Dispatchers.resetMain()
    }

    private fun fiche(lieuId: Int) =
        FicheLieuViewModel(SavedStateHandle(mapOf(FicheLieuViewModel.ARGUMENT_LIEU to lieuId)), JeuRepositoryImpl(base))

    @Test
    fun `la fiche liste les rencontres par methode avec niveaux et les objets importants`() = runTest {
        val etat = fiche(2).uiState.first { it is FicheLieuUiState.Fiche } as FicheLieuUiState.Fiche
        assertEquals("Route 104" to TypeLieu.ROUTE, etat.nom to etat.type)
        val groupe = etat.rencontres.single()
        assertEquals(MethodeRencontre.HERBE, groupe.methode)
        assertEquals(LigneRencontre(253, "Massko", "Plante", 12, 14, 20, null), groupe.lignes.single())
        assertEquals(listOf("Badge Roche"), etat.objets)
    }

    @Test
    fun `un lieu inconnu est signale`() = runTest {
        assertEquals(FicheLieuUiState.Introuvable, fiche(99).uiState.first { it != FicheLieuUiState.Chargement })
    }
}
