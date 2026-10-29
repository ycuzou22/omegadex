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
import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ModeleDefi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Règles du moteur de défis (T11.4) vérifiées sur les 13 étapes et les modèles réels de la base embarquée,
 * puis de bout en bout sur une run : plafond, Hardcore, déblocage par étape, défi Évolution atteignable.
 */
@RunWith(RobolectricTestRunner::class)
class MoteurDefisEmbarqueTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var jeu: JeuRepositoryImpl
    private lateinit var modeles: List<ModeleDefi>
    private lateinit var etapes: List<Etape>

    /** Massko évolue au niveau 36 : son défi Évolution n'apparaît qu'aux étapes dont le plafond l'autorise. */
    private val massko = EspeceCandidate(
        id = 253,
        nom = "Massko",
        dansEquipe = true,
        capturee = true,
        aUneMega = false,
        talentCache = false,
        evolutions = listOf(EvolutionPossible("Jungko", ConditionEvolution.NIVEAU, "36")),
    )

    @Before
    fun ouvrir() = runTest {
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        jeuBase = DatabaseModule.jeuDatabase(context)
        progressionBase = BasesDeTest.progression()
        jeu = JeuRepositoryImpl(jeuBase)
        modeles = jeu.modelesDefis().first()
        etapes = jeu.etapes().first()
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
    }

    private fun generer(etape: Etape, difficulte: Difficulte, national: Boolean, sonde: Boolean = false) =
        GenerateurDefis.generer(
            ContexteDefis(difficulte, etape, "Arcko", listOf(massko), emptyList(), national),
            if (sonde) modeles.map { it.copy(gabarit = "{niveau}") } else modeles,
        )

    @Test
    fun `aucun defi embarque ne fixe un niveau au-dessus du plafond de son etape`() {
        assertTrue(modeles.none { (it.parametres["ecart"]?.toIntOrNull() ?: 0) < 0 })
        etapes.forEach { etape ->
            Difficulte.entries.forEach { difficulte ->
                generer(etape, difficulte, national = true, sonde = true).forEach { defi ->
                    val niveau = defi.enonce.toInt()
                    assertTrue("étape ${etape.id}, modèle ${defi.modele.id} : $niveau", niveau <= etape.plafond)
                }
            }
        }
    }

    @Test
    fun `chaque difficulte contient tous les defis de la difficulte inferieure, a chaque etape`() {
        etapes.forEach { etape ->
            val ids = Difficulte.entries.map { difficulte ->
                generer(etape, difficulte, national = true).map { it.modele.id to it.especeId }.toSet()
            }
            ids.zipWithNext().forEach { (inferieure, superieure) ->
                assertTrue("étape ${etape.id}", superieure.containsAll(inferieure))
            }
            val hardcoreSeulement = ids.last() - ids[1]
            val parId = modeles.associateBy { it.id }
            assertTrue(hardcoreSeulement.all { parId.getValue(it.first).difficulteMin == Difficulte.HARDCORE })
        }
    }

    @Test
    fun `un modele n'est propose qu'a partir de son etape et un defi d'etape seulement a la sienne`() {
        etapes.forEach { etape ->
            val proposes = generer(etape, Difficulte.HARDCORE, national = false)
            proposes.forEach { defi ->
                assertTrue("${defi.modele.id} avant l'étape ${defi.modele.etapeMin}", defi.modele.etapeMin <= etape.id)
                if (defi.modele.type == TypeDefi.ETAPE) assertEquals(etape.id, defi.etapeId)
                assertFalse(ConditionDeblocage.POKEDEX_NATIONAL in defi.modele.conditions)
            }
            val attendus = modeles.filter { it.type == TypeDefi.ETAPE && it.etapeId == etape.id }.map { it.id }
            val defisEtape = proposes.filter { it.modele.type == TypeDefi.ETAPE }.map { it.modele.id }
            assertEquals(attendus.toSet(), defisEtape.toSet())
        }
    }

    @Test
    fun `le defi evolution de massko n'apparait qu'aux etapes dont le plafond atteint 36`() {
        etapes.forEach { etape ->
            val evolutions = generer(etape, Difficulte.DIFFICILE, national = false)
                .filter { it.modele.type == TypeDefi.EVOLUTION }
            assertEquals("étape ${etape.id} (plafond ${etape.plafond})", etape.plafond >= 36, evolutions.isNotEmpty())
            evolutions.forEach { assertTrue(it.enonce, it.enonce.contains("Jungko")) }
        }
    }

    @Test
    fun `de bout en bout, le defi evolution d'arcko attend une etape au plafond suffisant`() = runTest {
        val session = FausseSession()
        val horloge = Horloge { 1_000L }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        val progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val runActive = FausseRunActive(comptes)
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        val proposer = ProposerDefisUseCase(runEnCours, progression, jeu)
        assertTrue("sans joueur connecté, aucun défi", proposer().first().isEmpty())

        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creerRun = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creerRun("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        progression.placerMembre(run, MembreEquipe(1, 252, null, 5))

        fun evolutionArcko(defis: List<DefiPropose>) =
            defis.filter { it.modele.type == TypeDefi.EVOLUTION && it.especeId == 252 }

        val depart = etapes.first()
        assertEquals(depart.plafond >= 16, evolutionArcko(proposer().first()).isNotEmpty())

        val cible = etapes.first { it.plafond >= 16 }
        etapes.filter { it.id < cible.id }.forEach {
            progression.deverrouillerEtape(run, it.id)
            progression.validerEtape(run, it.id)
        }
        val proposes = proposer().first { defis -> defis.any { it.etapeId == cible.id } }
        val evolution = evolutionArcko(proposes).single()
        assertTrue(evolution.enonce, evolution.enonce.contains("Massko"))
    }

    @Test
    fun `retenter est refuse tant qu'un autre defi espece est actif sur le meme pokemon`() = runTest {
        val session = FausseSession()
        var maintenant = 1_000L
        val horloge = Horloge { maintenant++ }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        val progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val runActive = FausseRunActive(comptes)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creerRun = CreerRunUseCase(comptes, progression, session, runActive)
        val run = (creerRun("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        progression.placerMembre(run, MembreEquipe(1, 252, null, 5))
        val proposer = ProposerDefisUseCase(
            ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu),
            progression,
            jeu,
        )
        val accepter = AccepterDefiUseCase(progression)
        val valider = ValiderDefiUseCase(progression)
        val retenter = RetenterDefiUseCase(progression)

        val pourArcko = proposer().first().filter { it.especeId == 252 }
        val premier = accepter(run, pourArcko[0]).getOrThrow()
        assertTrue(valider(run, premier, reussi = false))

        val suivant = proposer().first().first { it.especeId == 252 && it.modele.id != pourArcko[0].modele.id }
        val second = accepter(run, suivant).getOrThrow()
        assertNull("Arcko porte déjà un défi espèce actif", retenter(run, premier))

        assertTrue(valider(run, second, reussi = true))
        assertNotNull(retenter(run, premier))
        assertFalse("un défi inconnu ne se valide pas", valider(run, defiId = 999, reussi = true))
        val etats = progression.defis(run).first().sortedBy { it.id }.map { it.etat }
        assertEquals(listOf(EtatDefi.RATE, EtatDefi.REUSSI, EtatDefi.ACTIF), etats)
    }
}
