package com.omegadex.app.domain.equipe

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
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
class PcUseCasesTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var deposer: DeposerAuPcUseCase
    private lateinit var retirer: RetirerDuPcUseCase
    private lateinit var definirDetails: DefinirDetailsUseCase
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
        deposer = DeposerAuPcUseCase(progression)
        retirer = RetirerDuPcUseCase(progression)
        definirDetails = DefinirDetailsUseCase(progression)
    }

    @After
    fun fermer() {
        base.close()
    }

    private suspend fun equipe() = progression.equipe(run).first()

    private suspend fun pc() = progression.pc(run).first()

    @Test
    fun `deposer un membre le range au PC avec sa nature et son talent`() = runTest {
        progression.placerMembre(run, MembreEquipe(1, 253, "Gecko", 18, Nature.RIGIDE, "Engrais"))
        progression.placerMembre(run, MembreEquipe(2, 258, null, 16))

        assertEquals(ResultatPc.Fait, deposer(run, emplacement = 1, boite = 2))

        assertEquals(listOf(2), equipe().map { it.emplacement })
        val range = pc().single()
        assertEquals(2, range.boite)
        assertEquals(253, range.especeId)
        assertEquals("Gecko", range.surnom)
        assertEquals(18, range.niveau)
        assertEquals(Nature.RIGIDE, range.nature)
        assertEquals("Engrais", range.talent)
    }

    @Test
    fun `l'equipe garde toujours au moins un pokemon`() = runTest {
        progression.placerMembre(run, MembreEquipe(3, 253, null, 18))
        assertEquals(ResultatPc.DernierMembre, deposer(run, emplacement = 3, boite = 1))
        assertEquals(ResultatPc.Introuvable, deposer(run, emplacement = 5, boite = 1))
        assertEquals(1, equipe().size)
        assertTrue(pc().isEmpty())
    }

    @Test
    fun `une boite pleine refuse un nouveau pokemon`() = runTest {
        repeat(TAILLE_BOITE + 1) { i ->
            progression.placerMembre(run, MembreEquipe(1, 300 + i, null, 5))
            progression.placerMembre(run, MembreEquipe(2, 252, null, 5))
            val attendu = if (i < TAILLE_BOITE) ResultatPc.Fait else ResultatPc.BoitePleine
            assertEquals(attendu, deposer(run, emplacement = 1, boite = 1))
        }
        assertEquals(TAILLE_BOITE, pc().count { it.boite == 1 })
        assertEquals(ResultatPc.Fait, deposer(run, emplacement = 1, boite = 2))
    }

    @Test
    fun `reprendre un pokemon le place dans le premier emplacement libre`() = runTest {
        progression.placerMembre(run, MembreEquipe(1, 253, null, 18))
        progression.placerMembre(run, MembreEquipe(2, 258, "Bulle", 16, Nature.MODESTE, null))
        deposer(run, emplacement = 2, boite = 1)
        progression.placerMembre(run, MembreEquipe(3, 263, null, 9))
        val id = pc().single().id

        assertEquals(ResultatPc.Fait, retirer(run, id))

        assertTrue(pc().isEmpty())
        val revenu = equipe().first { it.emplacement == 2 }
        assertEquals(MembreEquipe(2, 258, "Bulle", 16, Nature.MODESTE, null), revenu)
        assertEquals(ResultatPc.Introuvable, retirer(run, id))
    }

    @Test
    fun `une equipe complete refuse un pokemon du PC`() = runTest {
        (1..6).forEach { progression.placerMembre(run, MembreEquipe(it, 252 + it, null, 10)) }
        deposer(run, emplacement = 6, boite = 1)
        progression.placerMembre(run, MembreEquipe(6, 300, null, 10))

        assertEquals(ResultatPc.EquipePleine, retirer(run, pc().single().id))
        assertEquals(1, pc().size)
    }

    @Test
    fun `nature et talent se renseignent dans l'equipe comme au PC`() = runTest {
        progression.placerMembre(run, MembreEquipe(1, 253, null, 18))
        progression.placerMembre(run, MembreEquipe(2, 258, null, 16))
        deposer(run, emplacement = 2, boite = 1)
        val id = pc().single().id

        definirDetails(run, PlacePokemon.Equipe(1), Nature.JOVIAL, "  Engrais ")
        definirDetails(run, PlacePokemon.Pc(id), Nature.CALME, " ")

        assertEquals(Nature.JOVIAL, equipe().single().nature)
        assertEquals("Engrais", equipe().single().talent)
        assertEquals(Nature.CALME, pc().single().nature)
        assertNull(pc().single().talent)

        definirDetails(run, PlacePokemon.Equipe(1), null, null)
        assertNull(equipe().single().nature)
    }
}
