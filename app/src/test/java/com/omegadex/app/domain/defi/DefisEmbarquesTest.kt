package com.omegadex.app.domain.defi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.di.DatabaseModule
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.run.ResultatCreationRun
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.testutil.FausseRunActive
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

/** Le moteur appliqué aux 60 modèles réels de la base embarquée. */
@RunWith(RobolectricTestRunner::class)
class DefisEmbarquesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var jeu: JeuRepositoryImpl

    @Before
    fun ouvrir() {
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        jeuBase = DatabaseModule.jeuDatabase(context)
        progressionBase = BasesDeTest.progression()
        jeu = JeuRepositoryImpl(jeuBase)
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
    }

    private val candidat = EspeceCandidate(
        id = 252,
        nom = "Arcko",
        dansEquipe = true,
        capturee = true,
        aUneMega = true,
        talentCache = true,
        evolutions = listOf(EvolutionPossible("Massko", ConditionEvolution.NIVEAU, "16")),
    )

    @Test
    fun `chaque etape a ses defis par difficulte et tous les marqueurs sont remplis`() = runTest {
        val modeles = jeu.modelesDefis().first()
        assertEquals(60, modeles.size)
        jeu.etapes().first().forEach { etape ->
            val parDifficulte = Difficulte.entries.associateWith { difficulte ->
                val contexte = ContexteDefis(
                    difficulte,
                    etape,
                    "Arcko",
                    listOf(candidat),
                    emptyList(),
                    nationalObtenu = true,
                )
                GenerateurDefis.generer(contexte, modeles)
            }
            fun defisEtape(difficulte: Difficulte) =
                parDifficulte.getValue(difficulte).filter { it.modele.type == TypeDefi.ETAPE }
            val etapeNormal = defisEtape(Difficulte.NORMAL)
            val etapeDifficile = defisEtape(Difficulte.DIFFICILE)
            assertEquals("étape ${etape.id}", 1, etapeNormal.size)
            assertEquals("étape ${etape.id}", 2, etapeDifficile.size)
            parDifficulte.values.flatten().forEach { defi ->
                assertFalse("${defi.modele.id} : ${defi.enonce}", defi.enonce.contains('{'))
                Regex("niveau (\\d+)").findAll(defi.enonce).forEach {
                    assertTrue(defi.enonce, it.groupValues[1].toInt() <= etape.plafond)
                }
            }
            assertTrue(
                parDifficulte.getValue(Difficulte.NORMAL).size <= parDifficulte.getValue(Difficulte.DIFFICILE).size,
            )
        }
    }

    @Test
    fun `une run propose ses defis, les accepte une fois et refuse un second defi pour le meme pokemon`() = runTest {
        val session = FausseSession()
        val horloge = Horloge { 1_000L }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        val progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val runActive = FausseRunActive(comptes)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creerRun = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creerRun("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        progression.placerMembre(run, MembreEquipe(1, 252, null, 5))
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        val proposer = ProposerDefisUseCase(runEnCours, progression, jeu)
        val accepter = AccepterDefiUseCase(progression)

        val proposes = proposer().first()
        assertTrue(proposes.any { it.modele.type == TypeDefi.ETAPE && it.etapeId == 0 })
        val pourArcko = proposes.filter { it.especeId == 252 }
        assertTrue(pourArcko.size >= 2)

        assertTrue(accepter(run, pourArcko[0]).isSuccess)
        assertTrue(accepter(run, pourArcko[1]).isFailure)
        assertEquals(listOf(EtatDefi.ACTIF), progression.defis(run).first().map { it.etat })
        assertTrue(proposer().first().none { it.especeId == 252 })
    }
}
