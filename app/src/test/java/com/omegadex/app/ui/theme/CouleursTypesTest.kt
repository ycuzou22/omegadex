package com.omegadex.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CouleursTypesTest {
    private fun contraste(a: Color, b: Color): Float {
        val clair = max(a.luminance(), b.luminance()) + 0.05f
        val sombre = min(a.luminance(), b.luminance()) + 0.05f
        return clair / sombre
    }

    @Test
    fun `les 18 types ont une couleur`() {
        assertEquals(18, CouleursTypes.parType.size)
    }

    @Test
    fun `le texte de chaque puce de type respecte le contraste AA`() {
        CouleursTypes.parType.forEach { (type, fond) ->
            val ratio = contraste(texteLisibleSur(fond), fond)
            assertTrue("$type : contraste %.2f < 4,5".format(ratio), ratio >= 4.5f)
        }
    }

    @Test
    fun `un type inconnu ne plante pas`() {
        assertEquals(Trait, CouleursTypes.de("inconnu"))
    }
}
