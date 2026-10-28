package com.omegadex.app.domain.pokedex

import com.omegadex.app.core.Horloge
import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece
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
class PokedexNationalTest {
    private lateinit var jeu: JeuDatabase
    private lateinit var base: ProgressionDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var pokedex: ObserverPokedexUseCase
    private val horloge = Horloge { 1_000L }
    private val session = FausseSession()
    private var run = 0L

    @Before
    fun ouvrir() = runBlocking {
        jeu = BasesDeTest.jeu()
        BasesDeTest.remplirJeu(jeu)
        // Bulbizarre (hors du Pokédex de Hoenn) ne s'obtient que par échange.
        jeu.openHelper.writableDatabase.apply {
            execSQL("INSERT INTO lieu VALUES (124, 'Hors de Hoenn', 'HORS_HOENN', 1)")
            execSQL("INSERT INTO rencontre VALUES (3, 124, 1, 'ECHANGE_REQUIS', NULL, NULL, NULL, NULL)")
        }
        base = BasesDeTest.progression()
        val comptes = CompteRepositoryImpl(base, horloge)
        val utilisateur = requireNotNull(comptes.creerUtilisateur("yamao", "hash", "sel"))
        session.ouvrir(utilisateur)
        run = comptes.creerRun(utilisateur, "Run", 252, Difficulte.NORMAL, emptyList())
        progression = ProgressionRepositoryImpl(base, horloge, session)
        pokedex = ObserverPokedexUseCase(JeuRepositoryImpl(jeu), progression)
    }

    @After
    fun fermer() {
        base.close()
        jeu.close()
    }

    @Test
    fun `le national est invisible avant la Ligue`() = runTest {
        (0 until RegleNational.ETAPE_LIGUE).forEach { progression.validerEtape(run, it) }
        progression.deverrouillerEtape(run, RegleNational.ETAPE_LIGUE)

        val resultat = pokedex(run).first()

        assertFalse(resultat.national)
        assertEquals(listOf(252, 253, 254), resultat.entrees.map { it.espece.id })
    }

    @Test
    fun `valider la Ligue debloque le national, especes hors Hoenn comprises`() = runTest {
        progression.validerEtape(run, RegleNational.ETAPE_LIGUE)

        val resultat = pokedex(run).first()

        assertTrue(resultat.national)
        assertEquals(listOf(1, 252, 253, 254), resultat.entrees.map { it.espece.id })
    }

    @Test
    fun `mention echange requis et statut des especes`() = runTest {
        progression.validerEtape(run, RegleNational.ETAPE_LIGUE)
        progression.definirStatut(run, 252, StatutEspece.CAPTUREE)

        val entrees = pokedex(run).first().entrees.associateBy { it.espece.id }

        assertTrue(entrees.getValue(1).echangeRequis)
        assertFalse(entrees.getValue(252).echangeRequis)
        assertEquals(StatutEspece.CAPTUREE, entrees.getValue(252).statut)
        assertEquals(StatutEspece.NON_VUE, entrees.getValue(253).statut)
    }

    @Test
    fun `la regle ne retient que la validation de la Ligue`() {
        val ligueDeverrouillee = listOf(ProgressionEtape(RegleNational.ETAPE_LIGUE, true, 1L, null))
        val ligueValidee = listOf(ProgressionEtape(RegleNational.ETAPE_LIGUE, true, 1L, 2L))
        assertFalse(RegleNational.debloque(emptyList()))
        assertFalse(RegleNational.debloque(ligueDeverrouillee))
        assertTrue(RegleNational.debloque(ligueValidee))
    }
}
