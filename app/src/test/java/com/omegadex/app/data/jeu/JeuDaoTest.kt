package com.omegadex.app.data.jeu

import com.omegadex.app.data.BasesDeTest
import com.omegadex.app.domain.model.MethodeApprentissage
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.TypeQuete
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JeuDaoTest {
    private lateinit var base: JeuDatabase

    @Before
    fun ouvrir() {
        base = BasesDeTest.jeu()
        BasesDeTest.remplirJeu(base)
    }

    @After
    fun fermer() = base.close()

    @Test
    fun `les etapes et leurs quetes sont ordonnees`() = runTest {
        assertEquals(listOf(0, 1), base.etapeDao().etapes().first().map { it.id })
        assertEquals(14, base.etapeDao().etape(1).first()?.plafond)
        val quetes = base.etapeDao().quetes(0).first()
        assertEquals(listOf(TypeQuete.PRINCIPALE, TypeQuete.ANNEXE), quetes.map { it.type })
        assertEquals(listOf("Route 104"), base.etapeDao().lieuxDebloques(1).first().map { it.nom })
    }

    @Test
    fun `le pokedex regional exclut les especes nationales`() = runTest {
        assertEquals(listOf("Arcko", "Massko", "Jungko"), base.especeDao().pokedexHoenn().first().map { it.nom })
        assertEquals(listOf(1, 252, 253, 254), base.especeDao().pokedexNational().first().map { it.id })
    }

    @Test
    fun `la famille d'evolution est complete depuis n'importe quel stade`() = runTest {
        val depuisMassko = base.especeDao().famille(253).first()
        assertEquals(listOf(252 to 253, 253 to 254), depuisMassko.map { it.especeSourceId to it.especeCibleId })
        assertEquals(depuisMassko, base.especeDao().famille(254).first())
        assertTrue(base.especeDao().famille(1).first().isEmpty())
    }

    @Test
    fun `talents, attaques et rencontres d'une espece`() = runTest {
        val talents = base.especeDao().talents(252).first()
        assertEquals(listOf("Engrais" to false, "Délestage" to true), talents.map { it.nom to it.cache })
        val attaque = base.attaqueDao().attaquesApprises(252).first().single()
        assertEquals("Écras'Face", attaque.attaque.nom)
        assertEquals(MethodeApprentissage.NIVEAU, attaque.methode)
        val rencontre = base.rencontreDao().lieuxDeLEspece(252).first().single()
        assertEquals("Route 101" to MethodeRencontre.CADEAU, rencontre.lieu.nom to rencontre.methode)
    }

    @Test
    fun `les especes accessibles suivent l'etape`() = runTest {
        assertEquals(listOf(252), base.rencontreDao().especesAccessibles(0).first())
        assertEquals(setOf(252, 253), base.rencontreDao().especesAccessibles(1).first().toSet())
    }
}
