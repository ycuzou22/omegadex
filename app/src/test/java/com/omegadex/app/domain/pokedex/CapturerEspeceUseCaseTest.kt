package com.omegadex.app.domain.pokedex

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.model.TypeLieu
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class CapturerEspeceUseCaseTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var capturer: CapturerEspeceUseCase
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
        capturer = CapturerEspeceUseCase(progression)
    }

    @After
    fun fermer() = base.close()

    @Test
    fun `une espece non vue ou vue passe a capturee`() = runTest {
        progression.definirStatut(run, 263, StatutEspece.VUE)

        assertEquals(ResultatCapture.Capturee(StatutEspece.NON_VUE), capturer(run, 265))
        assertEquals(ResultatCapture.Capturee(StatutEspece.VUE), capturer(run, 263))

        val statuts = progression.statuts(run).first()
        assertEquals(StatutEspece.CAPTUREE, statuts[265])
        assertEquals(StatutEspece.CAPTUREE, statuts[263])
    }

    @Test
    fun `un statut deja capture ou plus avance n'est jamais abaisse`() = runTest {
        progression.definirStatut(run, 252, StatutEspece.ENTRAINEE)

        assertEquals(ResultatCapture.DejaCapturee, capturer(run, 252))
        assertEquals(StatutEspece.ENTRAINEE, progression.statuts(run).first()[252])
    }

    @Test
    fun `les echanges requis et les rencontres de Saphir Alpha ne s'attrapent pas dans Rubis Omega`() {
        val lieu = Lieu(1, "Route 101", TypeLieu.ROUTE, 0)
        fun rencontre(methode: MethodeRencontre, condition: String?) = Rencontre(lieu, 1, methode, 2, 3, 20, condition)

        assertTrue(rencontre(MethodeRencontre.HERBE, null).possibleDansRubisOmega())
        assertTrue(rencontre(MethodeRencontre.HERBE, "de nuit").possibleDansRubisOmega())
        assertFalse(rencontre(MethodeRencontre.ECHANGE_REQUIS, null).possibleDansRubisOmega())
        assertFalse(rencontre(MethodeRencontre.HERBE, "Saphir Alpha uniquement").possibleDansRubisOmega())
    }
}
