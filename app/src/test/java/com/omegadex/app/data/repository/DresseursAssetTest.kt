package com.omegadex.app.data.repository

import com.omegadex.app.domain.dresseur.Genre
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** org.json n'existe qu'avec Android : Robolectric le fournit. */
@RunWith(RobolectricTestRunner::class)
class DresseursAssetTest {
    private val dresseurs = DresseursAsset.lireDresseurs(File("src/main/assets/${DresseursAsset.FICHIER}").readText())

    @Test
    fun `les huit champions sont decrits, chacun ferme son etape`() {
        val champions = dresseurs.filter { it.titre in setOf("Champion", "Championne", "Champions") && it.queteId != null }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 9), champions.map { it.etapeId })
        assertTrue(champions.all { it.equipe.isNotEmpty() && it.attention != null })
        assertEquals(listOf(12, 14), champions.first().equipe.map { it.niveau })
        assertTrue(champions.single { it.cle == "levy-et-tatia" }.duo)
        assertEquals(Genre.PLURIEL, champions.single { it.cle == "levy-et-tatia" }.genre)
        assertEquals(Genre.FEMININ, champions.single { it.cle == "roxanne" }.genre)
        champions.forEach { champion -> assertEquals(champion, dresseurs.last { it.etapeId == champion.etapeId }) }
    }

    @Test
    fun `chaque combat de Flora existe pour les trois starters, avec le starter qui a l'avantage`() {
        val flora = dresseurs.filter { it.nom == "Flora" }
        assertEquals(12, flora.size)
        flora.groupBy { it.cle }.values.forEach { combats ->
            assertEquals(setOf(ARCKO, POUSSIFEU, GOBOU), combats.map { it.starter }.toSet())
        }
        val premier = flora.filter { it.lieu == "Route 103" }.associate { it.starter to it.equipe.single().especeId }
        assertEquals(mapOf(ARCKO to POUSSIFEU, POUSSIFEU to GOBOU, GOBOU to ARCKO), premier)
        assertTrue(flora.all { it.genre == Genre.FEMININ && it.image == "flora" })
    }

    @Test
    fun `les autres dresseurs ont une equipe, une image de repli et des cles uniques`() {
        assertTrue(dresseurs.size > 300)
        assertTrue(dresseurs.all { it.equipe.isNotEmpty() && it.image.isNotBlank() && it.etapeId in 0..11 })
        assertEquals(dresseurs.size, dresseurs.map { it.cle to it.starter }.toSet().size)
        assertTrue(dresseurs.filter { it.nom != "Flora" }.all { it.starter == null })
        val gamin = dresseurs.first { it.titre == "Gamin" }
        assertEquals("gamin", gamin.image)
        assertNull(gamin.queteId)
        assertEquals(37, dresseurs.single { it.nom == "Pierre Rochard" }.queteId)
    }

    private companion object {
        const val ARCKO = 252
        const val POUSSIFEU = 255
        const val GOBOU = 258
    }
}
