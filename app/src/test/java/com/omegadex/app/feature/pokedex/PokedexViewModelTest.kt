package com.omegadex.app.feature.pokedex

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.accueil.Jauge
import com.omegadex.app.domain.accueil.ObserverJaugesUseCase
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
import org.junit.Assert.assertNull
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
        progression,
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

    @Test
    fun `une touche fait avancer le statut d'un cran et l'enregistre`() = runTest {
        val run = nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }

        viewModel.avancerStatut(252)

        assertEquals(StatutModifie(252, "Arcko", StatutEspece.NON_VUE, StatutEspece.VUE), viewModel.evenements.first())
        val etat = viewModel.uiState.first {
            it is PokedexUiState.Pret && it.especes.first().statut == StatutEspece.VUE
        } as PokedexUiState.Pret
        assertEquals(1 to 0, etat.vues to etat.capturees)
        assertEquals(StatutEspece.VUE, progression.statuts(run).first()[252])
    }

    @Test
    fun `les touches successives parcourent tous les statuts puis s'arretent a Maitrisee`() = runTest {
        val run = nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }

        val parcours = StatutEspece.entries.drop(1).map { attendu ->
            viewModel.avancerStatut(253)
            viewModel.evenements.first().apres.also { assertEquals(attendu, it) }
        }
        assertEquals(StatutEspece.MAITRISEE, parcours.last())
        assertEquals(StatutEspece.MAITRISEE, progression.statuts(run).first()[253])

        // Déjà maîtrisée : une touche de plus ne change rien.
        viewModel.avancerStatut(253)
        assertEquals(StatutEspece.MAITRISEE, progression.statuts(run).first()[253])
    }

    @Test
    fun `seule l'entree dans les capturees declenche l'animation de capture`() = runTest {
        val run = nouvelleRun()
        progression.definirStatut(run, 252, StatutEspece.VUE)
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret && it.vues == 1 }

        viewModel.avancerStatut(252)
        val capture = viewModel.evenements.first()
        assertTrue(capture.capture)
        assertEquals(StatutEspece.CAPTUREE, capture.apres)

        viewModel.uiState.first { it is PokedexUiState.Pret && it.capturees == 1 }
        viewModel.avancerStatut(252)
        assertFalse(viewModel.evenements.first().capture)
    }

    @Test
    fun `un appui long fait reculer le statut sans descendre sous Non vue`() = runTest {
        val run = nouvelleRun()
        progression.definirStatut(run, 254, StatutEspece.CAPTUREE)
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret && it.capturees == 1 }

        viewModel.reculerStatut(254)
        assertEquals(StatutEspece.VUE, viewModel.evenements.first().apres)
        assertEquals(StatutEspece.VUE, progression.statuts(run).first { it[254] == StatutEspece.VUE }[254])

        viewModel.reculerStatut(253)
        assertNull(progression.statuts(run).first()[253])
    }

    @Test
    fun `le compteur suit aussitot les statuts enregistres ailleurs`() = runTest {
        val run = nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }

        // Même flux Room que celui des jauges de l'accueil (T13.1).
        progression.definirStatut(run, 253, StatutEspece.EVOLUEE)

        val etat = viewModel.uiState.first { it is PokedexUiState.Pret && it.capturees == 1 } as PokedexUiState.Pret
        assertEquals(StatutEspece.EVOLUEE, etat.especes.single { it.id == 253 }.statut)
    }

    @Test
    fun `les jauges de l'accueil suivent aussitot un changement de statut`() = runTest {
        nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }
        val jauges = ObserverJaugesUseCase(
            ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu),
            progression,
            jeu,
        )
        assertEquals(Jauge(0, 3), requireNotNull(jauges().first()).pokedex)

        viewModel.avancerStatut(252)
        assertEquals(StatutEspece.VUE, viewModel.evenements.first().apres)
        val apresVue = requireNotNull(jauges().first()).pokedex
        assertEquals("une espèce vue ne compte pas comme capturée", Jauge(0, 3), apresVue)

        viewModel.avancerStatut(252)
        assertTrue(viewModel.evenements.first().capture)
        assertEquals(Jauge(1, 3), requireNotNull(jauges().first { it?.pokedex?.valeur == 1 }).pokedex)
    }

    @Test
    fun `l'ordre des statuts est celui de l'entrainement`() {
        assertEquals(StatutEspece.VUE, StatutEspece.NON_VUE.suivant())
        assertEquals(StatutEspece.MAITRISEE, StatutEspece.MAITRISEE.suivant())
        assertEquals(StatutEspece.EVOLUEE, StatutEspece.MAITRISEE.precedent())
        assertEquals(StatutEspece.NON_VUE, StatutEspece.NON_VUE.precedent())
    }

    @Test
    fun `la recherche et les filtres mettent la grille a jour sans toucher au compteur`() = runTest {
        val run = nouvelleRun()
        progression.definirStatut(run, 253, StatutEspece.CAPTUREE)
        val viewModel = viewModel()
        val depart = viewModel.uiState.first { it is PokedexUiState.Pret && it.capturees == 1 } as PokedexUiState.Pret
        assertEquals(listOf("plante"), depart.typesDisponibles)
        assertEquals(depart.especes, depart.resultats)

        viewModel.rechercher("jung")
        assertEquals(listOf(254), viewModel.resultats(FiltresPokedex(recherche = "jung")))
        viewModel.rechercher("2")
        assertEquals(listOf(253), viewModel.resultats(FiltresPokedex(recherche = "2")))

        viewModel.rechercher("")
        viewModel.basculerStatut(StatutEspece.NON_VUE)
        assertEquals(listOf(252, 254), viewModel.resultats(FiltresPokedex(statuts = setOf(StatutEspece.NON_VUE))))
        viewModel.basculerStatut(StatutEspece.CAPTUREE)
        val deux = setOf(StatutEspece.NON_VUE, StatutEspece.CAPTUREE)
        assertEquals(listOf(252, 253, 254), viewModel.resultats(FiltresPokedex(statuts = deux)))
        viewModel.basculerStatut(StatutEspece.NON_VUE)
        assertEquals(listOf(253), viewModel.resultats(FiltresPokedex(statuts = setOf(StatutEspece.CAPTUREE))))

        val etat = viewModel.uiState.first { it is PokedexUiState.Pret } as PokedexUiState.Pret
        assertEquals(3, etat.especes.size)
        assertEquals(1 to 1, etat.vues to etat.capturees)
    }

    @Test
    fun `choisir deux fois le meme type retire le filtre et effacer remet tout`() = runTest {
        nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }

        viewModel.choisirType("eau")
        assertEquals(emptyList<Int>(), viewModel.resultats(FiltresPokedex(type = "eau")))
        viewModel.choisirType("plante")
        assertEquals(listOf(252, 253, 254), viewModel.resultats(FiltresPokedex(type = "plante")))
        viewModel.choisirType("plante")
        assertEquals(listOf(252, 253, 254), viewModel.resultats(FiltresPokedex()))

        viewModel.rechercher("zzz")
        viewModel.basculerStatut(StatutEspece.VUE)
        val vide = FiltresPokedex(recherche = "zzz", statuts = setOf(StatutEspece.VUE))
        assertEquals(emptyList<Int>(), viewModel.resultats(vide))
        viewModel.effacerFiltres()
        assertEquals(listOf(252, 253, 254), viewModel.resultats(FiltresPokedex()))
    }

    @Test
    fun `un changement de statut garde les filtres et sort l'espece des resultats filtres`() = runTest {
        nouvelleRun()
        val viewModel = viewModel()
        viewModel.uiState.first { it is PokedexUiState.Pret }
        viewModel.basculerStatut(StatutEspece.NON_VUE)
        val nonVues = FiltresPokedex(statuts = setOf(StatutEspece.NON_VUE))
        assertEquals(listOf(252, 253, 254), viewModel.resultats(nonVues))

        viewModel.avancerStatut(252)

        val etat = viewModel.uiState.first {
            it is PokedexUiState.Pret && it.vues == 1
        } as PokedexUiState.Pret
        assertEquals(nonVues, etat.filtres)
        assertEquals(listOf(253, 254), etat.resultats.map { it.id })
    }

    /** Attend l'état qui applique [filtres] et rend les espèces retenues. */
    private suspend fun PokedexViewModel.resultats(filtres: FiltresPokedex): List<Int> {
        val etat = uiState.first { it is PokedexUiState.Pret && it.filtres == filtres } as PokedexUiState.Pret
        return etat.resultats.map { it.id }
    }
}
