package com.omegadex.app.domain.equipe

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
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
class CompositionEquipeTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var ajouter: AjouterMembreUseCase
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
        ajouter = AjouterMembreUseCase(progression)
    }

    @After
    fun fermer() {
        base.close()
    }

    private suspend fun equipe() = progression.equipe(run).first().associate { it.emplacement to it.especeId }

    @Test
    fun `impossible d'ajouter une espece non capturee`() = runTest {
        assertEquals(ResultatAjout.NonCapturee, ajouter(run, 252, 5))
        progression.definirStatut(run, 252, StatutEspece.VUE)
        assertEquals(ResultatAjout.NonCapturee, ajouter(run, 252, 5))
        assertTrue(equipe().isEmpty())
    }

    @Test
    fun `une capture entre dans le premier emplacement libre, au niveau donne`() = runTest {
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        progression.definirStatut(run, 263, StatutEspece.EVOLUEE)
        progression.placerMembre(run, MembreEquipe(1, 280, null, 5))

        assertEquals(ResultatAjout.Ajoute(2), ajouter(run, 252, 7, surnom = "Gecko"))
        assertEquals(ResultatAjout.Ajoute(3), ajouter(run, 263, 4, surnom = " "))
        val membres = progression.equipe(run).first().associateBy { it.emplacement }
        assertEquals(MembreEquipe(2, 252, "Gecko", 7), membres[2])
        assertEquals(MembreEquipe(3, 263, null, 4), membres[3])
        assertEquals(ResultatAjout.NiveauInvalide, ajouter(run, 252, 0))
    }

    @Test
    fun `echanger remplace le membre de l'emplacement, une equipe pleine refuse l'ajout`() = runTest {
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)
        (1..6).forEach { progression.placerMembre(run, MembreEquipe(it, 260 + it, null, 5)) }

        assertEquals(ResultatAjout.EquipePleine, ajouter(run, 252, 5))
        assertEquals(ResultatAjout.Ajoute(4), ajouter(run, 252, 5, emplacement = 4))
        assertEquals(252, equipe()[4])
        assertEquals(6, equipe().size)

        RetirerMembreUseCase(progression)(run, 4)
        assertEquals((1..6).toSet() - 4, equipe().keys)
    }
}
