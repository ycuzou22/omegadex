package com.omegadex.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** org.json n'existe qu'avec Android : Robolectric le fournit. */
@RunWith(RobolectricTestRunner::class)
class DresseursAssetTest {
    @Test
    fun `les huit champions sont decrits, un par arene`() {
        val dresseurs = DresseursAsset.lireDresseurs(File("src/main/assets/${DresseursAsset.FICHIER}").readText())
        assertEquals(8, dresseurs.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 9), dresseurs.map { it.etapeId })
        assertTrue(dresseurs.all { it.equipe.isNotEmpty() && it.attention != null })
        assertEquals(listOf(12, 14), dresseurs.first().equipe.map { it.niveau })
        assertTrue(dresseurs.single { it.cle == "levy-et-tatia" }.duo)
    }
}
