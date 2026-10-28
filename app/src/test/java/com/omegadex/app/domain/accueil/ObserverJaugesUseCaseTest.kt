package com.omegadex.app.domain.accueil

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.run.ResultatCreationRun
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ObserverJaugesUseCaseTest {
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var runActive: FausseRunActive
    private lateinit var jauges: ObserverJaugesUseCase
    private val session = FausseSession()

    @Before
    fun ouvrir() {
        jeuBase = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        progressionBase = BasesDeTest.progression()
        val jeu = JeuRepositoryImpl(jeuBase)
        val horloge = Horloge { 1_000L }
        comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        runActive = FausseRunActive(comptes)
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        jauges = ObserverJaugesUseCase(runEnCours, progression, jeu)
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
    }

    @Test
    fun `les trois jauges suivent l'aventure, les captures et les defis`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        assertNull(jauges().first())
        val creer = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creer("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        assertEquals(JaugesAccueil(Jauge(0, 2), Jauge(0, 3), Jauge(0, 0)), jauges().first())

        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.definirStatut(run, 253, StatutEspece.VUE)
        progression.definirStatut(run, 1, StatutEspece.CAPTUREE)
        progression.validerEtape(run, 0)
        val defi = progression.ajouterDefi(run, modeleId = 1, especeId = null, etapeId = 0)
        progression.ajouterDefi(run, modeleId = 2, especeId = null, etapeId = 0)
        progression.changerEtatDefi(run, defi, EtatDefi.REUSSI)

        val apres = jauges().first()
        assertEquals(Jauge(1, 2), apres?.aventure)
        // Bulbizarre (hors Hoenn) ne compte pas tant que le national n'est pas débloqué.
        assertEquals(Jauge(1, 3), apres?.pokedex)
        assertEquals(Jauge(1, 2), apres?.defis)
        assertEquals(0.5f, apres?.aventure?.progression)
        assertEquals(0f, Jauge(0, 0).progression)
    }
}
