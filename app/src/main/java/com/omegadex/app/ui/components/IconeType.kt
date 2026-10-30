package com.omegadex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.OmegadexTheme
import java.text.Normalizer

/** Bas du rond, sous la couleur du type. */
private val GRIS_PALE = Color(0xFFECE6E7)
private val LISERE = Color(0x29141012)
private val CONTOUR = Color(0xFF141012)
private val DESSIN = Color.White

/** Part du rond occupée par le pictogramme. */
private const val PART_PICTOGRAMME = 0.68f
private const val LISERE_DP = 1f
private const val DEMI_TOUR = 180f

/** Épaisseurs dans le repère 24 × 24 des pictogrammes. */
private const val TRAIT = 2f
private const val TRAIT_LARGE = 2.6f
private const val TRAIT_DETAIL = 1.6f
private const val CONTOUR_PLEIN = 2.8f
private const val CONTOUR_TRAIT = 4.8f
private const val CONTOUR_TRAIT_LARGE = 5.4f

/**
 * Logo d'un type : rond coupé en deux, la couleur du type en haut et un gris pâle en bas, avec le pictogramme
 * du type par-dessus, blanc cerné de noir pour rester lisible sur les deux moitiés (maquette « Rond bicolore »).
 * [description] : le nom du type lu par TalkBack ; null quand le parent annonce déjà le type.
 */
@Composable
fun IconeType(type: String, modifier: Modifier = Modifier, taille: Dp = 22.dp, description: String? = null) {
    val cle = cleType(type)
    val semantique = if (description != null) {
        Modifier.semantics {
            contentDescription = description
            role = Role.Image
        }
    } else {
        Modifier
    }
    Canvas(modifier.size(taille).then(semantique)) { dessinerIconeType(cle, CouleursTypes.de(cle)) }
}

/** Plusieurs types côte à côte, sans texte. */
@Composable
fun IconesTypes(types: List<String>, modifier: Modifier = Modifier, taille: Dp = 22.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        types.forEach { IconeType(it, taille = taille) }
    }
}

/** Clé d'un type sans majuscule ni accent (« Ténèbres » → « tenebres »), celle des couleurs et des noms. */
internal fun cleType(type: String): String =
    Normalizer.normalize(type.trim().lowercase(), Normalizer.Form.NFD).replace(ACCENTS, "")

private val ACCENTS = Regex("\\p{Mn}+")

private fun DrawScope.dessinerIconeType(cle: String, couleur: Color) {
    val rayon = size.minDimension / 2f
    drawArc(couleur, startAngle = DEMI_TOUR, sweepAngle = DEMI_TOUR, useCenter = true)
    drawArc(GRIS_PALE, startAngle = 0f, sweepAngle = DEMI_TOUR, useCenter = true)
    val lisere = LISERE_DP.dp.toPx()
    drawCircle(LISERE, radius = rayon - lisere / 2f, style = Stroke(lisere))

    val elements = PictogrammesTypes.de(cle)
    if (elements.isEmpty()) return
    val cote = size.minDimension * PART_PICTOGRAMME
    val echelle = cote / PictogrammesTypes.REPERE
    translate(left = (size.width - cote) / 2f, top = (size.height - cote) / 2f) {
        scale(echelle, pivot = Offset.Zero) {
            elements.forEach { contour(it) }
            elements.forEach { remplissage(it, couleur) }
        }
    }
}

/** Contour noir de chaque tracé, posé sous le dessin. */
private fun DrawScope.contour(element: Element) {
    val largeur = when (element.couche) {
        Couche.PLEIN, Couche.DETAIL -> CONTOUR_PLEIN
        Couche.TRAIT, Couche.TRAIT_DETAIL -> CONTOUR_TRAIT
        Couche.TRAIT_LARGE -> CONTOUR_TRAIT_LARGE
    }
    if (element.couche == Couche.PLEIN || element.couche == Couche.DETAIL) {
        drawPath(element.chemin, CONTOUR, style = Fill)
    }
    drawPath(element.chemin, CONTOUR, style = Stroke(largeur, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.remplissage(element: Element, couleur: Color) {
    when (element.couche) {
        Couche.PLEIN -> drawPath(element.chemin, DESSIN)
        Couche.DETAIL -> drawPath(element.chemin, couleur)
        Couche.TRAIT -> drawPath(element.chemin, DESSIN, style = trait(TRAIT))
        Couche.TRAIT_LARGE -> drawPath(element.chemin, DESSIN, style = trait(TRAIT_LARGE))
        Couche.TRAIT_DETAIL -> drawPath(element.chemin, couleur, style = trait(TRAIT_DETAIL))
    }
}

private fun trait(largeur: Float) = Stroke(largeur, cap = StrokeCap.Round, join = StrokeJoin.Round)

@ApercusClairSombre
@Composable
private fun IconeTypePreview() {
    OmegadexTheme {
        Row {
            CouleursTypes.parType.keys.forEach { IconeType(it, taille = 28.dp) }
        }
    }
}
