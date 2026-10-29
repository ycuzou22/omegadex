package com.omegadex.app.ui.components

import com.omegadex.app.ui.theme.CouleursTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconeTypeTest {
    @Test
    fun `chaque type a son pictogramme et son nom`() {
        CouleursTypes.parType.keys.forEach { cle ->
            assertTrue("pictogramme manquant : $cle", PictogrammesTypes.de(cle).isNotEmpty())
            assertTrue("nom manquant : $cle", nomDuType(cle) != null)
        }
    }

    @Test
    fun `la cle d'un type ignore majuscules et accents`() {
        assertEquals("tenebres", cleType("Ténèbres"))
        assertEquals("electrik", cleType(" Électrik "))
        assertEquals("fee", cleType("Fée"))
        assertEquals("plante", cleType("plante"))
    }
}
