package com.omegadex.app.domain.equipe

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
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

/** Base jeu de test : Arcko (252) évolue en Massko (253) au niveau 16, puis en Jungko (254) au niveau 36. */
@RunWith(RobolectricTestRunner::class)
class EvolutionUseCasesTest {
    private lateinit var jeu: JeuDatabase
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var changerNiveau: ChangerNiveauUseCase
    private lateinit var faireEvoluer: FaireEvoluerUseCase
    private val horloge = Horloge { 1_000L }
    private val session = FausseSession()
    private var run = 0L

    @Before
    fun ouvrir() = runBlocking {
        jeu = BasesDeTest.jeu()
        BasesDeTest.remplirJeu(jeu)
        base = BasesDeTest.progression()
        val comptes = CompteRepositoryImpl(base, horloge)
        val utilisateur = requireNotNull(comptes.creerUtilisateur("yamao", "hash", "sel"))
        session.ouvrir(utilisateur)
        run = comptes.creerRun(utilisateur, "Run", 252, Difficulte.NORMAL, emptyList())
        progression = ProgressionRepositoryImpl(base, horloge, session)
        val repository = JeuRepositoryImpl(jeu)
        changerNiveau = ChangerNiveauUseCase(progression, repository)
        faireEvoluer = FaireEvoluerUseCase(progression, repository)
        progression.placerMembre(run, MembreEquipe(1, 252, "Gecko", 15))
    }

    @After
    fun fermer() {
        base.close()
        jeu.close()
    }

    private suspend fun membre() = progression.equipe(run).first().single()

    @Test
    fun `saisir un niveau l'enregistre et detecte l'evolution atteinte`() = runTest {
        assertEquals(ResultatNiveau.Enregistre(emptyList()), changerNiveau(run, 1, 15))

        val resultat = changerNiveau(run, 1, 16) as ResultatNiveau.Enregistre

        assertEquals(16, membre().niveau)
        assertEquals(listOf(253), resultat.evolutions.map { it.evolution.especeCibleId })
    }

    @Test
    fun `faire evoluer garde surnom et niveau et met a jour les statuts`() = runTest {
        progression.definirStatut(run, 252, StatutEspece.ENTRAINEE)
        changerNiveau(run, 1, 17)

        assertEquals(ResultatEvolution.Evolue(252, 253), faireEvoluer(run, 1, 253))

        assertEquals(MembreEquipe(1, 253, "Gecko", 17), membre())
        val statuts = progression.statuts(run).first()
        assertEquals(StatutEspece.EVOLUEE, statuts[252])
        assertEquals(StatutEspece.CAPTUREE, statuts[253])
    }

    @Test
    fun `un statut deja plus avance n'est jamais abaisse`() = runTest {
        progression.definirStatut(run, 252, StatutEspece.MAITRISEE)
        progression.definirStatut(run, 253, StatutEspece.ENTRAINEE)

        faireEvoluer(run, 1, 253)

        val statuts = progression.statuts(run).first()
        assertEquals(StatutEspece.MAITRISEE, statuts[252])
        assertEquals(StatutEspece.ENTRAINEE, statuts[253])
    }

    @Test
    fun `evolution impossible, membre absent ou niveau invalide sont refuses`() = runTest {
        assertEquals(ResultatEvolution.EvolutionImpossible, faireEvoluer(run, 1, 254))
        assertEquals(ResultatEvolution.EvolutionImpossible, faireEvoluer(run, 1, 1))
        assertEquals(ResultatEvolution.MembreAbsent, faireEvoluer(run, 2, 253))
        assertEquals(ResultatNiveau.MembreAbsent, changerNiveau(run, 2, 16))
        assertEquals(ResultatNiveau.NiveauInvalide, changerNiveau(run, 1, 0))
        assertEquals(ResultatNiveau.NiveauInvalide, changerNiveau(run, 1, 101))

        assertEquals(MembreEquipe(1, 252, "Gecko", 15), membre())
        assertTrue(progression.statuts(run).first().isEmpty())
    }
}
