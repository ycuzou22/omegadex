package com.omegadex.app.domain.run

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.testutil.FausseRunActive
import com.omegadex.app.testutil.FausseSession
import kotlinx.coroutines.flow.first
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
class CreerRunUseCaseTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var comptes: CompteRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var runActive: FausseRunActive
    private lateinit var creerRun: CreerRunUseCase
    private val session = FausseSession()
    private val horloge = Horloge { 42_000L }

    @Before
    fun ouvrir() {
        base = BasesDeTest.progression()
        comptes = CompteRepositoryImpl(base, horloge)
        progression = ProgressionRepositoryImpl(base, horloge, session)
        runActive = FausseRunActive(comptes)
        creerRun = CreerRunUseCase(comptes, progression, session, runActive)
    }

    @After
    fun fermer() = base.close()

    private suspend fun connecter(): Long = requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")).also {
        session.ouvrir(it)
    }

    @Test
    fun `une run complete s'ouvre sur l'etape 0 et devient la run active`() = runTest {
        val joueur = connecter()
        val resultat = creerRun(
            "  Nuzlocke Gobou ",
            Starter.GOBOU,
            Difficulte.HARDCORE,
            setOf(RegleOptionnelle.SURNOMS, RegleOptionnelle.NUZLOCKE),
        )

        val runId = (resultat as ResultatCreationRun.Creee).runId
        val run = requireNotNull(comptes.run(joueur, runId).first())
        assertEquals("Nuzlocke Gobou", run.nom)
        assertEquals(258, run.starterId)
        assertEquals(Difficulte.HARDCORE, run.difficulte)
        assertEquals(listOf("nuzlocke", "surnoms"), run.regles)
        assertEquals(42_000L, run.dateDebut)
        assertEquals(runId, runActive.runActive(joueur).first())
        val etapes = progression.etapes(runId).first()
        assertEquals(listOf(ETAPE_DE_DEPART), etapes.filter { it.deverrouillee }.map { it.etapeId })
        assertEquals(ETAPE_DE_DEPART, ObserverRunEnCoursUseCase.etapeEnCours(etapes))
    }

    @Test
    fun `nom vide, nom trop long et starter manquant sont refuses`() = runTest {
        connecter()
        assertEquals(
            ResultatCreationRun.Refusee(setOf(ErreurCreationRun.NOM_VIDE, ErreurCreationRun.STARTER_MANQUANT)),
            creerRun("   ", null, Difficulte.NORMAL, emptySet()),
        )
        assertEquals(
            ResultatCreationRun.Refusee(setOf(ErreurCreationRun.NOM_TROP_LONG)),
            creerRun("x".repeat(LONGUEUR_MAX_NOM_RUN + 1), Starter.ARCKO, Difficulte.NORMAL, emptySet()),
        )
    }

    @Test
    fun `sans joueur connecte aucune run n'est creee`() = runTest {
        assertEquals(
            ResultatCreationRun.Refusee(setOf(ErreurCreationRun.NON_CONNECTE)),
            creerRun("Run", Starter.ARCKO, Difficulte.NORMAL, emptySet()),
        )
    }

    @Test
    fun `plusieurs runs par compte, la derniere creee est active`() = runTest {
        val joueur = connecter()
        creerRun("Première", Starter.ARCKO, Difficulte.NORMAL, emptySet())
        val seconde = (
            creerRun(
                "Seconde",
                Starter.POUSSIFEU,
                Difficulte.DIFFICILE,
                emptySet(),
            ) as ResultatCreationRun.Creee
            )
        assertEquals(2, comptes.runs(joueur).first().size)
        assertEquals(seconde.runId, runActive.runActive(joueur).first())
    }

    @Test
    fun `l'etape en cours suit la progression`() {
        fun etape(id: Int, validee: Boolean) = ProgressionEtape(id, true, 1, if (validee) 2 else null)
        assertEquals(0, ObserverRunEnCoursUseCase.etapeEnCours(emptyList()))
        assertEquals(1, ObserverRunEnCoursUseCase.etapeEnCours(listOf(etape(0, true), etape(1, false))))
        assertEquals(3, ObserverRunEnCoursUseCase.etapeEnCours(listOf(etape(0, true), etape(1, true), etape(2, true))))
        assertTrue(Starter.entries.map { it.especeId } == listOf(252, 255, 258))
        assertNull(Starter.de(1))
    }
}
