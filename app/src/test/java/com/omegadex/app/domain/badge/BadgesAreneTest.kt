package com.omegadex.app.domain.badge

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class BadgesAreneTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private val session = FausseSession()
    private var run = 0L

    @Before
    fun ouvrir() = runBlocking {
        base = BasesDeTest.progression()
        val horloge = Horloge { 1_000L }
        val comptes = CompteRepositoryImpl(base, horloge)
        val utilisateur = requireNotNull(comptes.creerUtilisateur("yamao", "hash", "sel"))
        session.ouvrir(utilisateur)
        run = comptes.creerRun(utilisateur, "Run", 252, Difficulte.NORMAL, emptyList())
        progression = ProgressionRepositoryImpl(base, horloge, session)
    }

    @After
    fun fermer() = base.close()

    @Test
    fun `chaque arene donne son badge, les autres etapes aucun`() {
        assertEquals(8, BadgeArene.entries.size)
        assertEquals(BadgeArene.ROCHE, BadgeArene.pourEtape(1))
        assertEquals(BadgeArene.PLUIE, BadgeArene.pourEtape(9))
        listOf(0, 8, 10, 11, 12).forEach { assertNull(BadgeArene.pourEtape(it)) }
    }

    @Test
    fun `les badges obtenus suivent les etapes validees`() {
        val etapes = listOf(
            ProgressionEtape(1, true, 1L, 2L),
            ProgressionEtape(2, true, 1L, null),
            ProgressionEtape(3, true, 1L, 3L),
        )
        assertEquals(setOf(1, 3), etapes.idsValidees())
        assertEquals(setOf(BadgeArene.ROCHE, BadgeArene.DYNAMO), BadgeArene.obtenus(etapes.idsValidees()))
    }

    @Test
    fun `la boite se remplit a chaque arene validee`() = runTest {
        val badges = ObserverBadgesUseCase(progression)
        assertTrue(badges(run).first().isEmpty())

        progression.validerEtape(run, 0)
        progression.validerEtape(run, 1)
        progression.validerEtape(run, 2)

        assertEquals(setOf(BadgeArene.ROCHE, BadgeArene.POING), badges(run).first())
    }
}
