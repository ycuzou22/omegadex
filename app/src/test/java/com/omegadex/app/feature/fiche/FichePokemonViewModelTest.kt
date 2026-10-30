package com.omegadex.app.feature.fiche

import androidx.lifecycle.SavedStateHandle
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.MethodeApprentissage
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FichePokemonViewModelTest {
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

    private fun fiche(especeId: Int): FichePokemonViewModel {
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        return FichePokemonViewModel(
            SavedStateHandle(mapOf(FichePokemonViewModel.ARGUMENT_ESPECE to especeId)),
            jeu,
            runEnCours,
            progression,
            ObserverDefisUseCase(runEnCours, progression, jeu),
            ProposerDefisUseCase(runEnCours, progression, jeu),
        )
    }

    private suspend fun nouvelleRun() {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        CreerRunUseCase(comptes, progression, session, runActive)("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet())
    }

    private suspend fun FichePokemonViewModel.fiche() =
        uiState.first { it is FichePokemonUiState.Fiche } as FichePokemonUiState.Fiche

    @Test
    fun `la fiche vient de la base jeu, sans statut hors d'une run`() = runTest {
        val etat = fiche(252).fiche()
        assertEquals("Arcko", etat.espece.nom)
        assertEquals(listOf("Plante"), etat.espece.types)
        assertNull(etat.statut)
        assertEquals(FichePokemonUiState.Introuvable, fiche(999).uiState.first { it != FichePokemonUiState.Chargement })
    }

    @Test
    fun `le statut d'entrainement avance d'une touche et recule d'un appui long`() = runTest {
        nouvelleRun()
        val vm = fiche(252)
        assertEquals(StatutEspece.NON_VUE, vm.fiche().statut)
        vm.avancerStatut()
        vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.statut == StatutEspece.VUE }
        vm.avancerStatut()
        vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.statut == StatutEspece.CAPTUREE }
        vm.reculerStatut()
        vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.statut == StatutEspece.VUE }
    }

    @Test
    fun `les infos viennent de la base jeu, dont le talent cache et le stade Mega de la famille`() = runTest {
        val arcko = fiche(252).fiche()
        assertEquals(listOf("Engrais" to false, "Délestage" to true), arcko.talents.map { it.nom to it.cache })
        assertEquals("Jungko", arcko.stadeMega)
        assertEquals(45, arcko.espece.tauxCapture)
        assertEquals(0.875, arcko.espece.ratioMale ?: 0.0, 0.0)
        assertEquals("Jungko", fiche(254).fiche().stadeMega)
        assertNull(fiche(1).fiche().stadeMega)
    }

    @Test
    fun `les attaques viennent de la base jeu et le niveau actuel de l'equipe`() = runTest {
        val sansRun = fiche(252).fiche()
        assertEquals(
            listOf("Écras'Face" to MethodeApprentissage.NIVEAU),
            sansRun.attaques.map { it.attaque.nom to it.methode },
        )
        assertEquals(1, sansRun.attaques.single().niveau)
        assertNull(sansRun.niveauActuel)

        nouvelleRun()
        val run = requireNotNull(runActive.runActive(1).first())
        progression.placerMembre(run, MembreEquipe(2, 252, null, 14))
        val vm = fiche(252)
        vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.niveauActuel == 14 }
        assertNull(fiche(253).fiche().niveauActuel)
    }

    @Test
    fun `les lieux portent leur etape de deblocage et s'eteignent tant qu'elle n'est pas atteinte`() = runTest {
        val massko = fiche(253).fiche().lieux.single()
        assertEquals("Route 104", massko.rencontre.lieu.nom)
        assertEquals(12 to 14, massko.rencontre.niveauMin to massko.rencontre.niveauMax)
        assertEquals("Roxanne", massko.etape?.titre)
        assertNull(massko.accessible)

        nouvelleRun()
        fiche(253).uiState.first { (it as? FichePokemonUiState.Fiche)?.lieux?.single()?.accessible == false }
        fiche(252).uiState.first { (it as? FichePokemonUiState.Fiche)?.lieux?.single()?.accessible == true }
    }

    @Test
    fun `une espece a echanger est annoncee a part, sans lieu`() = runTest {
        jeuBase.openHelper.writableDatabase.execSQL(
            "INSERT INTO rencontre VALUES (3, 1, 1, 'ECHANGE_REQUIS', NULL, NULL, NULL, NULL)",
        )
        val bulbizarre = fiche(1).fiche()
        assertTrue(bulbizarre.echangeRequis)
        assertTrue(bulbizarre.lieux.isEmpty())
        assertFalse(fiche(252).fiche().echangeRequis)
    }

    @Test
    fun `defis et notes suivent la run active`() = runTest {
        val sansRun = fiche(252).fiche()
        assertNull(sansRun.defis)
        assertEquals("", sansRun.notes)

        nouvelleRun()
        val vm = fiche(252)
        val etat = vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.defis != null } as FichePokemonUiState.Fiche
        assertEquals(Difficulte.NORMAL, etat.defis?.difficulteRun)
        vm.modifierNotes("Garder Engrais")
        vm.uiState.first { (it as? FichePokemonUiState.Fiche)?.notes == "Garder Engrais" }
    }
}
