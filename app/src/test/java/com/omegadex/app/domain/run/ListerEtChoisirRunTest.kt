package com.omegadex.app.domain.run

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListerEtChoisirRunTest {
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var jeuBase: JeuDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var creerRun: CreerRunUseCase
    private lateinit var listerRuns: ListerRunsUseCase
    private lateinit var choisirRun: ChoisirRunUseCase
    private lateinit var supprimerRun: SupprimerRunUseCase
    private lateinit var runEnCours: ObserverRunEnCoursUseCase
    private val session = FausseSession()
    private var maintenant = 1_000L

    @Before
    fun ouvrir() {
        progressionBase = BasesDeTest.progression()
        jeuBase = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        val horloge = Horloge { maintenant++ }
        comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val jeu = JeuRepositoryImpl(jeuBase)
        val runActive = FausseRunActive(comptes)
        creerRun = CreerRunUseCase(comptes, progression, session, runActive)
        listerRuns = ListerRunsUseCase(session, comptes, runActive, progression, jeu)
        choisirRun = ChoisirRunUseCase(session, runActive)
        supprimerRun = SupprimerRunUseCase(session, comptes)
        runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
    }

    @After
    fun fermer() {
        progressionBase.close()
        jeuBase.close()
    }

    private suspend fun nouvelleRun(nom: String, starter: Starter) =
        (creerRun(nom, starter, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId

    @Test
    fun `la liste montre chaque run avec sa progression et la run active`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val premiere = nouvelleRun("Première", Starter.ARCKO)
        progression.validerEtape(premiere, 0)
        progression.deverrouillerEtape(premiere, 1)
        val seconde = nouvelleRun("Seconde", Starter.GOBOU)

        val liste = listerRuns().first()
        assertEquals(listOf(seconde, premiere), liste.map { it.run.id })
        assertEquals(listOf(true, false), liste.map { it.active })
        assertEquals(listOf(0, 1), liste.map { it.etapesValidees })
        assertEquals(listOf("Départ", "Roxanne"), liste.map { it.etapeEnCours?.titre })
        assertEquals(2, liste.first().totalEtapes)
    }

    @Test
    fun `changer de run met a jour la run en cours`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val premiere = nouvelleRun("Première", Starter.ARCKO)
        progression.validerEtape(premiere, 0)
        progression.deverrouillerEtape(premiere, 1)
        val seconde = nouvelleRun("Seconde", Starter.GOBOU)
        assertEquals(seconde, runEnCours().first()?.run?.id)
        assertEquals(0, runEnCours().first()?.etape?.id)

        choisirRun(premiere)

        val enCours = requireNotNull(runEnCours().first())
        assertEquals("Première", enCours.run.nom)
        assertEquals(1, enCours.etape?.id)
        assertTrue(listerRuns().first().single { it.run.id == premiere }.active)
    }

    @Test
    fun `un compte ne liste que ses propres runs`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        nouvelleRun("Run de Yamao", Starter.ARCKO)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Invite", "h", "s")))
        assertTrue(listerRuns().first().isEmpty())
        assertNull(runEnCours().first())
        session.fermer()
        assertTrue(listerRuns().first().isEmpty())
    }

    @Test
    fun `supprimer une run efface sa progression et une autre run devient active`() = runTest {
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val premiere = nouvelleRun("Première", Starter.ARCKO)
        val seconde = nouvelleRun("Seconde", Starter.GOBOU)
        progression.cocherQuete(seconde, 1, cochee = true)
        assertEquals(seconde, runEnCours().first()?.run?.id)

        supprimerRun(seconde)

        assertEquals(listOf(premiere), listerRuns().first().map { it.run.id })
        assertEquals(premiere, runEnCours().first()?.run?.id)
        assertTrue(progression.quetesCochees(seconde).first().isEmpty())

        supprimerRun(premiere)
        assertTrue(listerRuns().first().isEmpty())
        assertNull(runEnCours().first())
    }

    @Test
    fun `on ne peut pas supprimer la run d'un autre compte`() = runTest {
        val yamao = requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s"))
        session.ouvrir(yamao)
        val runDeYamao = nouvelleRun("Run de Yamao", Starter.ARCKO)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Invite", "h", "s")))

        supprimerRun(runDeYamao)

        session.ouvrir(yamao)
        assertEquals(listOf(runDeYamao), listerRuns().first().map { it.run.id })
    }
}
