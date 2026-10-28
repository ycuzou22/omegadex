package com.omegadex.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Rayons de la DA : 14 dp (boutons), 18 dp (champs, puces larges), 22 dp (cartes), 26 dp (feuilles). */
val FormesOmegadex = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/**
 * Rubis taillé vu de face : table plate en haut, couronne, pointe en bas.
 * Même découpe que le `clip-path: polygon(28% 0, 72% 0, 100% 34%, 50% 100%, 0 34%)` des maquettes.
 */
object FormeGemme : Shape {
    private const val TABLE_GAUCHE = 0.28f
    private const val TABLE_DROITE = 0.72f
    private const val COURONNE = 0.34f

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val chemin = Path().apply {
            moveTo(size.width * TABLE_GAUCHE, 0f)
            lineTo(size.width * TABLE_DROITE, 0f)
            lineTo(size.width, size.height * COURONNE)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height * COURONNE)
            close()
        }
        return Outline.Generic(chemin)
    }
}
