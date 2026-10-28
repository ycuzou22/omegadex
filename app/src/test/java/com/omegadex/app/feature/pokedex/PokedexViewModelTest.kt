package com.omegadex.app.feature.pokedex

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
import com.omegadex.app.domain.pokedex.RegleNational
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Base jeu de test : Arcko (252), Massko (253) et Jungko (254) dans le Pokédex de Hoenn (n° 1 à 3),
 * Bulbizarre (1) seulement au national. À l'étape 0, seul Arcko est trouvable (Route 101).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PokedexViewModelTest {
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

    private fun viewModel() = PokedexViewModel(
        ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu),
        ObserverPokedexUseCase(jeu, progression),
        jeu,
    )

    private suspend fun nouvelleRun(): Long {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creer = CreerRunUseCase(comptes, progression, session, runActive)
        return (creer("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
    }

    private suspend fun pret() = viewModel().uiState.first { it is PokedexUiState.Pret } as PokedexUiState.Pret

    @Test
    fun `sans run le Pokedex le dit`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        assertEquals(PokedexUiState.AucuneRun, viewModel().uiState.first { it != PokedexUiState.Chargement })
    }

    @Test
    fun `la grille montre le Pokedex de Hoenn dans l'ordre regional, en silhouettes au depart`() = runTest {
        nouvelleRun()

        val etat = pret()

        assertFalse(etat.national)
        assertEquals(listOf(252, 253, 254), etat.especes.map { it.id })
        assertEquals(listOf(1, 2, 3), etat.especes.map { it.numero })
        assertTrue(etat.especes.all { it.statut == StatutEspece.NON_VUE })
        assertEquals(0 to 0, etat.vues to etat.capturees)
    }

    @Test
    fun `les especes des zones debloquees sont mises en avant tant qu'elles ne sont pas capturees`() = runTest {
        val run = nouvelleRun()
        assertEquals(listOf(252), pret().especes.filter { it.aPortee }.map { it.id })

        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.definirStatut(run, 253, StatutEspece.VUE)

        val etat = pret()
        assertTrue(etat.especes.none { it.aPortee })
        assertEquals(2 to 1, etat.vues to etat.capturees)
    }

    @Test
    fun `apres la Ligue le Pokedex passe au national avec les numeros nationaux`() = runTest {
        val run = nouvelleRun()
        progression.validerEtape(run, RegleNational.ETAPE_LIGUE)

        val etat = pret()

        assertTrue(etat.national)
        assertEquals(listOf(1, 252, 253, 254), etat.especes.map { it.numero })
    }
}
