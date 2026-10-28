package com.omegadex.app.domain.aventure

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CocherQueteUseCaseTest {
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var cocher: CocherQueteUseCase
    private val session = FausseSession()
    private var run = 0L

    @Before
    fun ouvrir() = runTest {
        progressionBase = BasesDeTest.progression()
        jeuBase = BasesDeTest.jeu().also(BasesDeTest::remplirJeu)
        val horloge = Horloge { 1_000L }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        cocher = CocherQueteUseCase(JeuRepositoryImpl(jeuBase), progression)
        val joueur = requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s"))
        session.ouvrir(joueur)
        run = comptes.creerRun(joueur, "Run", 252, Difficulte.NORMAL, emptyList())
        progression.deverrouillerEtape(run, 0)
    }

    @After
    fun fermer() {
        progressionBase.close()
        jeuBase.close()
    }

    private suspend fun deverrouillees() = progression.etapes(run).first().filter {
        it.deverrouillee
    }.map { it.etapeId }

    private suspend fun etapeEnCours() = ObserverRunEnCoursUseCase.etapeEnCours(progression.etapes(run).first())

    @Test
    fun `une quete annexe ne debloque ni ne bloque rien`() = runTest {
        assertEquals(ResultatCochage.Enregistre, cocher(run, 0, queteId = 2, cochee = true))
        assertEquals(listOf(0), deverrouillees())
        assertEquals(0, etapeEnCours())
    }

    @Test
    fun `la derniere quete principale valide l'etape et deverrouille la suivante`() = runTest {
        assertEquals(ResultatCochage.EtapeValidee(0, 1), cocher(run, 0, queteId = 1, cochee = true))
        assertEquals(listOf(0, 1), deverrouillees())
        assertEquals(1, etapeEnCours())
        assertEquals(setOf(1), progression.quetesCochees(run).first())
    }

    @Test
    fun `l'etape suivante reste cachee tant qu'une principale manque, meme avec toutes les annexes`() = runTest {
        cocher(run, 0, queteId = 2, cochee = true)
        cocher(run, 0, queteId = 1, cochee = true)
        assertEquals(1, etapeEnCours())
        // Décocher puis recocher une principale ne valide pas deux fois l'étape.
        cocher(run, 0, queteId = 1, cochee = false)
        assertEquals(ResultatCochage.Enregistre, cocher(run, 0, queteId = 1, cochee = true))
    }

    @Test
    fun `la derniere etape se valide sans etape suivante`() = runTest {
        cocher(run, 0, queteId = 1, cochee = true)
        assertEquals(ResultatCochage.EtapeValidee(1, null), cocher(run, 1, queteId = 3, cochee = true))
        assertEquals(2, etapeEnCours())
    }

    @Test
    fun `regle de deblocage`() {
        fun q(id: Int, type: TypeQuete) = Quete(id, 0, null, type, id, "q$id", null)
        val quetes = listOf(q(1, TypeQuete.PRINCIPALE), q(2, TypeQuete.PRINCIPALE), q(3, TypeQuete.ANNEXE))
        assertFalse(ReglesDeblocage.principalesTerminees(quetes, setOf(1, 3)))
        assertTrue(ReglesDeblocage.principalesTerminees(quetes, setOf(1, 2)))
        assertFalse(ReglesDeblocage.principalesTerminees(listOf(q(3, TypeQuete.ANNEXE)), setOf(3)))
    }
}
