package com.omegadex.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vérifie le contraste WCAG AA (4,5:1) des couples texte / fond des deux thèmes. */
class ContrasteTest {
    private fun contraste(a: Color, b: Color): Double {
        val clair = max(a.luminance(), b.luminance()) + 0.05
        val sombre = min(a.luminance(), b.luminance()) + 0.05
        return clair / sombre
    }

    private fun couples(s: ColorScheme) = mapOf(
        "onBackground / background" to (s.onBackground to s.background),
        "onSurface / surface" to (s.onSurface to s.surface),
        "onSurfaceVariant / surface" to (s.onSurfaceVariant to s.surface),
        "onSurfaceVariant / background" to (s.onSurfaceVariant to s.background),
        "onSurfaceVariant / surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
        "primary / background" to (s.primary to s.background),
        "primary / surface" to (s.primary to s.surface),
        "onPrimary / primary" to (s.onPrimary to s.primary),
        "onPrimaryContainer / primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
        "onSecondary / secondary" to (s.onSecondary to s.secondary),
        "onTertiary / tertiary" to (s.onTertiary to s.tertiary),
        "onError / error" to (s.onError to s.error),
        "inverseOnSurface / inverseSurface" to (s.inverseOnSurface to s.inverseSurface),
    )

    private fun verifier(nom: String, s: ColorScheme) {
        couples(s).forEach { (couple, paire) ->
            val ratio = contraste(paire.first, paire.second)
            assertTrue("$nom · $couple : contraste %.2f < 4,5".format(ratio), ratio >= SEUIL_AA)
        }
    }

    @Test
    fun `le theme clair respecte le contraste AA`() = verifier("clair", SchemaClair)

    @Test
    fun `le theme sombre respecte le contraste AA`() = verifier("sombre", SchemaSombre)

    @Test
    fun `le texte sur roche respecte le contraste AA`() {
        listOf(CouleursOmegadexClaires, CouleursOmegadexSombres).forEach {
            assertTrue(contraste(it.texteSurRoche, it.roche) >= SEUIL_AA)
            assertTrue(contraste(Color.White, it.roche) >= SEUIL_AA)
        }
    }

    private companion object {
        const val SEUIL_AA = 4.5
    }
}
