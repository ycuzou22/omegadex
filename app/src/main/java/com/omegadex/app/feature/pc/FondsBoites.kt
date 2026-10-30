// Couleurs et positions des fonds de la maquette « 16 · Fonds en vignettes » du canevas : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.feature.pc

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.omegadex.app.R

/** Décor d'un fond : un ovale (centre et demi-axes en fractions de la boîte) ou une grille de points. */
internal sealed interface Decor {
    class Ovale(val x: Float, val y: Float, val rx: Float, val ry: Float, val couleur: Color) : Decor

    class Points(val pas: Float, val rayon: Float, val couleur: Color) : Decor

    class Bandes(val pas: Float, val couleur: Color) : Decor
}

/** Fond d'écran d'une boîte : son nom et son dessin. */
class FondBoite internal constructor(
    @param:StringRes val nom: Int,
    private val haut: Color,
    private val bas: Color,
    private val decors: List<Decor>,
) {
    fun dessiner(scope: DrawScope) = with(scope) {
        drawRect(Brush.verticalGradient(listOf(haut, bas)))
        decors.forEach { decor ->
            when (decor) {
                is Decor.Ovale -> drawOval(
                    decor.couleur,
                    topLeft = Offset((decor.x - decor.rx) * size.width, (decor.y - decor.ry) * size.height),
                    size = Size(decor.rx * 2f * size.width, decor.ry * 2f * size.height),
                )
                is Decor.Points -> dessinerPoints(decor)
                is Decor.Bandes -> dessinerBandes(decor)
            }
        }
    }

    private fun DrawScope.dessinerPoints(decor: Decor.Points) {
        val pas = decor.pas * size.width
        for (ligne in 0 until (size.height / pas).toInt() + 1) {
            for (colonne in 0 until (size.width / pas).toInt() + 1) {
                val centre = Offset(pas / 2f + colonne * pas, pas / 2f + ligne * pas)
                drawCircle(decor.couleur, decor.rayon * size.width, centre)
            }
        }
    }

    private fun DrawScope.dessinerBandes(decor: Decor.Bandes) {
        val pas = decor.pas * size.height
        for (bande in 0 until (size.height / pas).toInt() + 1) {
            drawRect(decor.couleur, Offset(0f, bande * pas), Size(size.width, pas / 2f))
        }
    }
}

private val BLANC_NUAGE = Color(0xFFFFFFFF)

/** Un fond par boîte, dans l'ordre des boîtes. */
val FONDS_BOITES: List<FondBoite> = listOf(
    FondBoite(
        R.string.pc_fond_foret,
        Color(0xFFCDEDBF),
        Color(0xFF8ACB82),
        listOf(
            Decor.Ovale(0.18f, 1.05f, 0.3f, 0.3f, Color(0xFF2F7A3E)),
            Decor.Ovale(0.78f, 1.08f, 0.36f, 0.34f, Color(0xFF3A8C48)),
            Decor.Ovale(0.5f, 1.15f, 0.3f, 0.26f, Color(0xFF2A6A36)),
        ),
    ),
    FondBoite(
        R.string.pc_fond_mer,
        Color(0xFF4AA8E8),
        Color(0xFF2E7FC0),
        listOf(Decor.Bandes(0.09f, Color(0x1FFFFFFF)), Decor.Ovale(0.3f, 0f, 0.4f, 0.2f, Color(0x33FFFFFF))),
    ),
    FondBoite(
        R.string.pc_fond_grotte,
        Color(0xFF9A8878),
        Color(0xFF4A3D35),
        listOf(
            Decor.Ovale(0.15f, 0.25f, 0.1f, 0.08f, Color(0xFF7A685B)),
            Decor.Ovale(0.82f, 0.68f, 0.15f, 0.11f, Color(0xFF6A584C)),
            Decor.Ovale(0.4f, 0.9f, 0.12f, 0.08f, Color(0xFF5A4A40)),
        ),
    ),
    FondBoite(
        R.string.pc_fond_volcan,
        Color(0xFF5A1A20),
        Color(0xFF2A0B10),
        listOf(
            Decor.Ovale(0.5f, 1.1f, 0.6f, 0.4f, Color(0x66E2465F)),
            Decor.Ovale(0.5f, 1.1f, 0.3f, 0.2f, Color(0x99FFB15C)),
        ),
    ),
    FondBoite(
        R.string.pc_fond_ciel,
        Color(0xFF8FD0FF),
        Color(0xFFE3F4FF),
        listOf(
            Decor.Ovale(0.25f, 0.28f, 0.14f, 0.06f, BLANC_NUAGE),
            Decor.Ovale(0.72f, 0.6f, 0.18f, 0.07f, BLANC_NUAGE),
            Decor.Ovale(0.4f, 0.86f, 0.12f, 0.05f, BLANC_NUAGE),
        ),
    ),
    FondBoite(
        R.string.pc_fond_desert,
        Color(0xFFF2D49B),
        Color(0xFFE8C482),
        listOf(Decor.Bandes(0.14f, Color(0x14000000)), Decor.Ovale(0.8f, 0.18f, 0.09f, 0.09f, Color(0xFFFFF3C4))),
    ),
    FondBoite(
        R.string.pc_fond_neige,
        Color(0xFFCDE6F4),
        Color(0xFF8FB7D6),
        listOf(Decor.Points(0.12f, 0.008f, Color(0xCCFFFFFF))),
    ),
    FondBoite(
        R.string.pc_fond_rubis,
        Color(0xFF7A0C22),
        Color(0xFF3A0610),
        listOf(Decor.Points(0.08f, 0.018f, Color(0xFFC21F3E))),
    ),
)

/** Le fond de la boîte [numero] (à partir de 1). */
fun fondBoite(numero: Int): FondBoite = FONDS_BOITES[(numero - 1).mod(FONDS_BOITES.size)]
