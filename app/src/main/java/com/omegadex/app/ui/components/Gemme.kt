package com.omegadex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursOmegadex
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

private const val FIN_FACETTE_CLAIRE = 0.38f
private const val FIN_CORPS = 0.70f

/** Dégradé à arêtes franches du rubis taillé : facette claire, corps, facette sombre (diagonale à 135°). */
fun brosseGemme(couleurs: CouleursOmegadex, taille: Size): Brush = Brush.linearGradient(
    0f to couleurs.gemmeClaire,
    FIN_FACETTE_CLAIRE to couleurs.gemmeClaire,
    FIN_FACETTE_CLAIRE to couleurs.gemme,
    FIN_CORPS to couleurs.gemme,
    FIN_CORPS to couleurs.gemmeSombre,
    1f to couleurs.gemmeSombre,
    start = Offset.Zero,
    end = Offset(taille.width, taille.height),
)

/** Dessine une gemme allumée (dégradé rubis) ou éteinte (gris rosé) dans toute la zone. */
fun DrawScope.dessinerGemme(allumee: Boolean, couleurs: CouleursOmegadex) {
    val contour = FormeGemme.createOutline(size, layoutDirection, this) as Outline.Generic
    if (allumee) {
        drawPath(contour.path, brosseGemme(couleurs, size))
    } else {
        drawPath(contour.path, couleurs.gemmeEteinte)
    }
}

/** Gemme seule, décorative : la description pour l'accessibilité est portée par le composant parent. */
@Composable
fun Gemme(allumee: Boolean, modifier: Modifier = Modifier) {
    val couleurs = OmegadexTheme.couleurs
    Canvas(modifier.size(width = 24.dp, height = 22.dp)) { dessinerGemme(allumee, couleurs) }
}

@ApercusClairSombre
@Composable
private fun GemmePreview() {
    OmegadexTheme {
        Row {
            Gemme(allumee = true)
            Gemme(allumee = false)
        }
    }
}
