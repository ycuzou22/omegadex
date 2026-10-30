package com.omegadex.app.feature.equipe

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.equipe.AjouterMembreUseCase
import com.omegadex.app.domain.equipe.ChangerNiveauUseCase
import com.omegadex.app.domain.equipe.FaireEvoluerUseCase
import com.omegadex.app.domain.equipe.MethodeEvolution
import com.omegadex.app.domain.equipe.ObserverSuggestionsRotationUseCase
import com.omegadex.app.domain.equipe.RetirerMembreUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EquipeViewModelTest {
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

    private fun equipe(): EquipeViewModel {
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        return EquipeViewModel(
            runEnCours,
            progression,
            jeu,
            ObserverDefisUseCase(runEnCours, progression, jeu),
            ChangerNiveauUseCase(progression, jeu),
            FaireEvoluerUseCase(progression, jeu),
            CompositionEquipe(
                AjouterMembreUseCase(progression),
                RetirerMembreUseCase(progression),
                ObserverSuggestionsRotationUseCase(ObserverPokedexUseCase(jeu, progression), progression),
            ),
        )
    }

    private suspend fun nouvelleRun(): Long {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        return requireNotNull(runActive.runActive(1).first())
    }

    private suspend fun EquipeViewModel.equipe(condition: (EquipeUiState.Equipe) -> Boolean = { true }) =
        uiState.first { it is EquipeUiState.Equipe && condition(it) } as EquipeUiState.Equipe

    @Test
    fun `sans run, pas d'equipe`() = runTest {
        assertEquals(EquipeUiState.AucuneRun, equipe().uiState.first { it != EquipeUiState.Chargement })
    }

    @Test
    fun `six emplacements, chaque niveau compare au plafond de l'etape`() = runTest {
        val run = nouvelleRun()
        val vm = equipe()
        val plafond = requireNotNull(vm.equipe().plafond)
        progression.placerMembre(run, MembreEquipe(1, 252, "Gecko", plafond + 2))
        progression.placerMembre(run, MembreEquipe(3, 253, null, plafond))

        val etat = vm.equipe { it.membres.size == 2 }
        assertEquals((1..TAILLE_EQUIPE).toList(), etat.emplacements.map { it.numero })
        assertEquals(listOf(1, 3), etat.emplacements.filter { it.membre != null }.map { it.numero })
        val arcko = requireNotNull(etat.emplacements[0].membre)
        assertEquals("Arcko" to "Gecko", arcko.nom to arcko.surnom)
        assertEquals(2, arcko.ecartPlafond)
        assertEquals(0, etat.emplacements[2].membre?.ecartPlafond)
        assertEquals(1, etat.auDessusDuPlafond)
    }

    @Test
    fun `une capture jamais utilisee est suggeree et entre dans l'equipe au plafond`() = runTest {
        val run = nouvelleRun()
        progression.definirStatut(run, 253, StatutEspece.CAPTUREE)
        val vm = equipe()

        val suggestion = vm.equipe { e -> e.suggestions.any { it.especeId == 253 } }
            .suggestions.first { it.especeId == 253 }
        val plafond = requireNotNull(vm.equipe().plafond)
        assertEquals("Massko" to plafond, suggestion.nom to suggestion.niveau)

        vm.ajouterSuggestion(suggestion, emplacement = null)
        // L'équipe et les suggestions se mettent à jour l'une après l'autre : attendre les deux.
        val etat = vm.equipe { e -> e.membres.any { it.especeId == 253 } && e.suggestions.none { it.especeId == 253 } }
        assertEquals(plafond, etat.membres.first { it.especeId == 253 }.niveau)
    }

    @Test
    fun `une evolution par bonheur a son bouton et monte le membre d'un niveau en evoluant`() = runTest {
        // Évolution fictive par bonheur, sans précision d'heure : Massko → Bulbizarre.
        jeuBase.openHelper.writableDatabase.execSQL("INSERT INTO evolution VALUES (90, 253, 1, 'BONHEUR', NULL)")
        val run = nouvelleRun()
        progression.placerMembre(run, MembreEquipe(2, 253, null, 20))
        val vm = equipe()

        val massko = vm.equipe { e -> e.membres.any { it.especeId == 253 } }.membres.first { it.especeId == 253 }
        val bonheur = massko.evolutions.single()
        assertEquals(MethodeEvolution.BONHEUR to "Bulbizarre", bonheur.methode to bonheur.cible.nom)

        vm.evoluerManuellement(2, bonheur)
        val apres = vm.equipe { e -> e.membres.any { it.especeId == 1 } }.membres.first { it.especeId == 1 }
        assertEquals(21, apres.niveau)
        val animation = vm.evolutionEnCours.first { it != null }!!
        assertEquals("Massko" to "Bulbizarre", animation.avant.nom to animation.apres.nom)
    }

    @Test
    fun `atteindre le niveau d'evolution la propose, la confirmer fait evoluer le membre`() = runTest {
        val run = nouvelleRun()
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.placerMembre(run, MembreEquipe(1, 252, null, 15))
        val vm = equipe()
        vm.equipe { it.membres.isNotEmpty() }

        vm.changerNiveau(1, -1)
        vm.equipe { it.membres.single().niveau == 14 }
        assertNull(vm.evolutionProposee.value)

        vm.changerNiveau(1, 1)
        vm.changerNiveau(1, 1)
        val proposee = vm.evolutionProposee.first { it != null }!!
        assertEquals("Arcko", proposee.avant.nom)
        assertEquals(listOf("Massko" to 16), proposee.cibles.map { it.espece.nom to it.niveauRequis })

        vm.faireEvoluer(253)
        assertNull(vm.evolutionProposee.value)
        val enCours = vm.evolutionEnCours.first { it != null }!!
        assertEquals("Arcko" to "Massko", enCours.avant.nom to enCours.apres.nom)
        assertEquals(253, vm.equipe { it.membres.single().especeId == 253 }.membres.single().especeId)
        assertEquals(StatutEspece.EVOLUEE, progression.statuts(run).first()[252])

        vm.finirAnimation()
        assertNull(vm.evolutionEnCours.value)
    }

    @Test
    fun `retirer un membre libere son emplacement`() = runTest {
        val run = nouvelleRun()
        progression.placerMembre(run, MembreEquipe(2, 252, null, 5))
        val vm = equipe()
        vm.equipe { it.membres.size == 1 }
        vm.retirer(2)
        assertNull(vm.equipe { it.membres.isEmpty() }.emplacements[1].membre)
    }
}
