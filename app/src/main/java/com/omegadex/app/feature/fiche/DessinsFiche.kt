// Tracés recopiés de la maquette E · Fiche Pokémon : les coordonnées sont des données, pas des seuils.
@file:Suppress("MagicNumber")

package com.omegadex.app.feature.fiche

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private const val OPACITE_FILIGRANE = 0.08f

/** Étincelle à quatre branches de l'écrin (« M12 2l2 8 8 2-8 2-2 8-2-8-8-2 8-2z »). */
internal fun DrawScope.dessinerEtincelle() {
    val c = center
    val r = size.minDimension / 2f
    val etoile = Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + r * 0.17f, c.y - r * 0.17f)
        lineTo(c.x + r, c.y)
        lineTo(c.x + r * 0.17f, c.y + r * 0.17f)
        lineTo(c.x, c.y + r)
        lineTo(c.x - r * 0.17f, c.y + r * 0.17f)
        lineTo(c.x - r, c.y)
        lineTo(c.x - r * 0.17f, c.y - r * 0.17f)
        close()
    }
    drawPath(etoile, Color.White)
}

/** Ω géant en filigrane dans l'en-tête (maquette : « M-20 240 h60 v-26 a70 70 0 1 1 60 0 v26 h60 »). */
internal fun DrawScope.dessinerOmegaFiligrane(couleur: Color) {
    val e = size.width / 390f
    val chemin = Path().apply {
        moveTo(-20f * e, 240f * e)
        lineTo(40f * e, 240f * e)
        lineTo(40f * e, 214f * e)
        arcTo(
            Rect(Offset(70f * e, 151.4f * e), 70f * e),
            115.4f,
            309.2f,
            forceMoveTo = false,
        )
        lineTo(100f * e, 240f * e)
        lineTo(160f * e, 240f * e)
    }
    drawPath(chemin, couleur.copy(alpha = OPACITE_FILIGRANE), style = Stroke(16f * e, join = StrokeJoin.Round))
}
