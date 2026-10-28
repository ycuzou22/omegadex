package com.omegadex.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.domain.model.Difficulte
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunActiveDataStoreTest {
    @get:Rule
    val dossier = TemporaryFolder()

    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private var maintenant = 1_000L

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, Horloge { maintenant++ })
    }

    @After
    fun fermer() = base.close()

    /** Ouvre le magasin sur [fichier] le temps de [bloc], puis le libère comme à l'arrêt de l'app. */
    private fun <T> avecMagasin(fichier: File, bloc: suspend (RunActiveDataStore) -> T): T {
        val portee = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val magasin = PreferenceDataStoreFactory.create(scope = portee) { fichier }
        return try {
            runBlocking { bloc(RunActiveDataStore(magasin, comptes)) }
        } finally {
            runBlocking { portee.coroutineContext.job.cancelAndJoin() }
        }
    }

    @Test
    fun `la run choisie est retrouvee apres un redemarrage, sinon la plus recente`() {
        val fichier = File(dossier.root, "runs.preferences_pb")
        val (joueur, premiere, seconde) = runBlocking {
            val id = requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s"))
            Triple(
                id,
                comptes.creerRun(id, "Première", 252, Difficulte.NORMAL, emptyList()),
                comptes.creerRun(id, "Seconde", 255, Difficulte.NORMAL, emptyList()),
            )
        }
        avecMagasin(fichier) { runs ->
            assertEquals(seconde, runs.runActive(joueur).first())
            runs.choisir(joueur, premiere)
            assertEquals(premiere, runs.runActive(joueur).first())
        }
        avecMagasin(fichier) { runs -> assertEquals(premiere, runs.runActive(joueur).first()) }
    }

    @Test
    fun `un compte sans run n'a pas de run active et chaque compte garde la sienne`() {
        val fichier = File(dossier.root, "runs.preferences_pb")
        avecMagasin(fichier) { runs ->
            val yamao = requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s"))
            val invite = requireNotNull(comptes.creerUtilisateur("Invite", "h", "s"))
            assertNull(runs.runActive(yamao).first())
            val runYamao = comptes.creerRun(yamao, "Run", 252, Difficulte.NORMAL, emptyList())
            runs.choisir(invite, runYamao)
            assertEquals(runYamao, runs.runActive(yamao).first())
            assertNull(runs.runActive(invite).first())
        }
    }
}
