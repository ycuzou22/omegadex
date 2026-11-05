package com.omegadex.app.domain.succes

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.data.repository.SuccesRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SuccesUseCasesTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var succes: SuccesRepositoryImpl
    private lateinit var surveiller: SurveillerSuccesUseCase
    private lateinit var vitrine: ObserverSuccesUseCase
    private val session = FausseSession()
    private var run = 0L

    @Before
    fun ouvrir() = runBlocking {
        val horloge = Horloge { 1_000L }
        base = BasesDeTest.progression()
        val comptes = CompteRepositoryImpl(base, horloge)
        val utilisateur = requireNotNull(comptes.creerUtilisateur("yamao", "hash", "sel"))
        session.ouvrir(utilisateur)
        run = comptes.creerRun(utilisateur, "Run", 252, Difficulte.NORMAL, emptyList())
        progression = ProgressionRepositoryImpl(base, horloge, session)
        succes = SuccesRepositoryImpl(base, horloge, session)
        val mesures = ObserverMesuresSuccesUseCase(progression)
        surveiller = SurveillerSuccesUseCase(mesures, succes)
        vitrine = ObserverSuccesUseCase(mesures, succes)
    }

    @After
    fun fermer() {
        base.close()
    }

    private suspend fun capturer(ids: IntRange) =
        ids.forEach { progression.definirStatut(run, it, StatutEspece.CAPTUREE) }

    @Test
    fun `un succes atteint est debloque une seule fois`() = runTest {
        capturer(1..10)
        assertEquals(listOf(Succes.CAPTURES_10), succes.debloquer(run, setOf(Succes.CAPTURES_10)))
        assertEquals(emptyList<Succes>(), succes.debloquer(run, setOf(Succes.CAPTURES_10)))
        assertEquals(mapOf(Succes.CAPTURES_10 to 1_000L), succes.succes(run).first())
    }

    @Test
    fun `la surveillance emet chaque nouveau succes apres l'action qui l'atteint`() = runTest {
        // Cumul des succès émis : chacun doit apparaître exactement une fois.
        val attente = async {
            surveiller(run)
                .runningFold(emptyList<Succes>()) { cumul, nouveaux -> cumul + nouveaux }
                .first { it.size >= 2 }
        }
        capturer(1..10)
        progression.validerEtape(run, 1)
        val emis = attente.await()

        assertEquals(listOf(Succes.CAPTURES_10, Succes.BADGES_1).sorted(), emis.sorted())
    }

    @Test
    fun `la vitrine montre les succes debloques en premier et l'avancement des autres`() = runTest {
        capturer(1..12)
        succes.debloquer(run, setOf(Succes.CAPTURES_10))

        val etats = vitrine(run).first { it.first().debloque }
        assertEquals(Succes.CAPTURES_10, etats.first().succes)
        assertEquals(Succes.entries.size, etats.size)
        val cinquante = etats.first { it.succes == Succes.CAPTURES_50 }
        assertEquals(12 to false, cinquante.avancement to cinquante.debloque)
        assertTrue(etats.drop(1).none { it.debloque })
    }
}
