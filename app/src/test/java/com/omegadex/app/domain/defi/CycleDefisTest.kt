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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CycleDefisTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var jeuBase: JeuDatabase
    private lateinit var progressionBase: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var proposer: ProposerDefisUseCase
    private lateinit var observer: ObserverDefisUseCase
    private var maintenant = 1_000L
    private var run = 0L

    @Before
    fun ouvrir() = runTest {
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        jeuBase = DatabaseModule.jeuDatabase(context)
        progressionBase = BasesDeTest.progression()
        val jeu = JeuRepositoryImpl(jeuBase)
        val session = FausseSession()
        val horloge = Horloge { maintenant++ }
        val comptes = CompteRepositoryImpl(progressionBase, horloge)
        progression = ProgressionRepositoryImpl(progressionBase, horloge, session)
        val runActive = FausseRunActive(comptes)
        session.ouvrir(requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")))
        val creerRun = CreerRunUseCase(comptes, progression, session, runActive)
        run = (creerRun("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()) as ResultatCreationRun.Creee).runId
        progression.placerMembre(run, MembreEquipe(1, 252, null, 5))
        val runEnCours = ObserverRunEnCoursUseCase(session, runActive, comptes, progression, jeu)
        proposer = ProposerDefisUseCase(runEnCours, progression, jeu)
        observer = ObserverDefisUseCase(runEnCours, progression, jeu)
    }

    @After
    fun fermer() {
        jeuBase.close()
        progressionBase.close()
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
    }

    private suspend fun accepterDeuxDefis(): Pair<Long, Long> {
        val proposes = proposer().first()
        val accepter = AccepterDefiUseCase(progression)
        val etape = accepter(run, proposes.first { it.modele.type == TypeDefi.ETAPE }).getOrThrow()
        val espece = accepter(run, proposes.first { it.especeId == 252 }).getOrThrow()
        return etape to espece
    }

    @Test
    fun `valider met a jour le bilan et l'historique`() = runTest {
        val (defiEtape, defiEspece) = accepterDeuxDefis()
        val valider = ValiderDefiUseCase(progression)
        val avant = requireNotNull(observer().first())
        assertEquals(BilanDefis(actifs = 2, reussis = 0, rates = 0), avant.bilan)
        assertTrue(avant.actifs.all { !it.enonce.contains('{') })
        assertEquals("Arcko", avant.actifs.single { it.defi.id == defiEspece }.especeNom)

        assertTrue(valider(run, defiEtape, reussi = true))
        assertTrue(valider(run, defiEspece, reussi = false))
        val apres = requireNotNull(observer().first { it?.bilan?.actifs == 0 })
        assertEquals(BilanDefis(actifs = 0, reussis = 1, rates = 1), apres.bilan)
        assertEquals(0.5f, apres.bilan.tauxReussite)
        assertEquals(listOf(defiEspece, defiEtape), apres.historique.map { it.defi.id })
        assertTrue(apres.historique.all { it.defi.dateFin != null })

        assertFalse("un défi terminé ne change plus", valider(run, defiEtape, reussi = false))
        assertEquals(EtatDefi.REUSSI, progression.defis(run).first().single { it.id == defiEtape }.etat)
    }

    @Test
    fun `retenter un defi rate en cree un nouveau et garde l'historique`() = runTest {
        val (defiEtape, defiEspece) = accepterDeuxDefis()
        val retenter = RetenterDefiUseCase(progression)
        assertNull("un défi actif ne se retente pas", retenter(run, defiEspece))

        ValiderDefiUseCase(progression)(run, defiEspece, reussi = false)
        val nouveau = requireNotNull(retenter(run, defiEspece))
        assertNull("déjà retenté", retenter(run, defiEspece))

        val defis = progression.defis(run).first()
        assertEquals(listOf(EtatDefi.ACTIF, EtatDefi.RATE, EtatDefi.ACTIF), defis.sortedBy { it.id }.map { it.etat })
        assertEquals(defis.single { it.id == defiEspece }.modeleId, defis.single { it.id == nouveau }.modeleId)
        val etat = requireNotNull(observer().first())
        assertEquals(BilanDefis(actifs = 2, reussis = 0, rates = 1), etat.bilan)
        assertEquals(listOf(defiEspece), etat.historique.map { it.defi.id })
        assertTrue(defiEtape in etat.actifs.map { it.defi.id })
    }
}
