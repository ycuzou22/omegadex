// Coordonnées et instants recopiés de la maquette « 7 · Fissure + onde » du canevas : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.CouleursOmegadex
import com.omegadex.app.ui.theme.FormeGemme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Chronologie de l'allumage de la gemme Oméga (millisecondes depuis le cochage) : la pierre se fend de veines
 * de magma, l'énergie éclate (flash et deux ondes), le Ω est frappé en rougeoyant, puis les veines refroidissent
 * et restent en filigrane.
 */
object ChronoGemmeOmega {
    const val VEINE_DECALAGE = 70f
    const val VEINE_DUREE = 280f
    const val FLASH = 280f
    const val FLASH_DUREE = 520f
    const val ONDE_RUBIS = 280f
    const val ONDE_MAGMA = 420f
    const val ONDE_DUREE = 720f
    const val TABLE = 480f
    const val TABLE_DUREE = 200f
    const val REFROIDISSEMENT = 560f
    const val REFROIDISSEMENT_DUREE = 500f
    const val OMEGA = 420f
    const val FRAPPE_DUREE = 380f
    const val BRAISE_DUREE = 700f

    /** Fin de l'animation : au-delà, la gemme est dans son état final. */
    const val FIN = ONDE_MAGMA + ONDE_DUREE
}

/** Veine de magma : départ (fractions de la gemme), longueur (fraction de la largeur) et angle en degrés. */
private class Veine(val x: Float, val y: Float, val longueur: Float, val angle: Float)

private val VEINES = listOf(
    Veine(0.14f, 0.40f, 0.40f, 16f),
    Veine(0.56f, 0.34f, 0.34f, -22f),
    Veine(0.46f, 0.60f, 0.26f, 64f),
)

private val EASE_OUT: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private const val ALPHA_TABLE = 0.22f
private const val ALPHA_VEINES_FINALE = 0.45f
private const val ALPHA_GRAVURE = 0.35f

/** Avancement de 0 à 1 d'une phase qui commence à [debut] et dure [duree] (en ms). */
private fun avancement(t: Float, debut: Float, duree: Float, easing: Easing = EASE_OUT): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/**
 * Dessine la gemme Oméga dans toute la zone. Éteinte, la pierre porte un Ω gravé ;
 * allumée, elle prend le dégradé rubis et le Ω luit. [t] est le temps écoulé depuis le cochage (ms) :
 * [ChronoGemmeOmega.FIN] ou plus donne l'état final, sans animation.
 * Les ondes débordent de la zone : le parent ne doit pas la rogner.
 */
fun DrawScope.dessinerGemmeOmega(allumee: Boolean, couleurs: CouleursOmegadex, t: Float = ChronoGemmeOmega.FIN) {
    if (allumee) dessinerOndes(t, couleurs)
    val contour = FormeGemme.createOutline(size, layoutDirection, this) as Outline.Generic
    clipPath(contour.path) {
        drawRect(couleurs.gemmeEteinte)
        if (allumee) {
            dessinerRubis(t, couleurs)
            dessinerVeines(t, couleurs)
            dessinerOmegaAllume(t, couleurs)
        } else {
            dessinerOmega(couleurs.texteEteint.copy(alpha = ALPHA_GRAVURE))
        }
    }
}

/** Deux ondes circulaires (rubis puis magma) qui s'élargissent en s'effaçant. */
private fun DrawScope.dessinerOndes(t: Float, couleurs: CouleursOmegadex) {
    val centre = Offset(size.width / 2f, size.height * 0.45f)
    val epaisseur = size.width * 0.025f + 1.dp.toPx()
    val ondes = listOf(
        ChronoGemmeOmega.ONDE_RUBIS to couleurs.gemmeClaire,
        ChronoGemmeOmega.ONDE_MAGMA to couleurs.magma,
    )
    ondes.forEach { (debut, couleur) ->
        val p = avancement(t, debut, ChronoGemmeOmega.ONDE_DUREE)
        if (p > 0f && p < 1f) {
            drawCircle(
                color = couleur.copy(alpha = 0.9f * (1f - p)),
                radius = size.width / 2f * lerp(0.55f, 1.7f, p),
                center = centre,
                style = Stroke(epaisseur),
            )
        }
    }
}

/** Le rubis s'allume dans un flash (luminosité 2,4 → 1), puis la table de la gemme s'éclaire. */
private fun DrawScope.dessinerRubis(t: Float, couleurs: CouleursOmegadex) {
    val p = ((t - ChronoGemmeOmega.FLASH) / ChronoGemmeOmega.FLASH_DUREE).coerceIn(0f, 1f)
    if (p <= 0f) return
    val opacite = (p / 0.3f).coerceAtMost(1f)
    val luminosite = if (p < 0.3f) {
        lerp(2.4f, 1.7f, p / 0.3f)
    } else {
        lerp(1.7f, 1f, EASE_OUT.transform((p - 0.3f) / 0.7f))
    }
    drawRect(brosseGemme(couleurs, size), alpha = opacite)
    drawRect(Color.White, alpha = ((luminosite - 1f) / 1.4f * 0.6f * opacite).coerceIn(0f, 1f))
    val table = avancement(t, ChronoGemmeOmega.TABLE, ChronoGemmeOmega.TABLE_DUREE)
    if (table > 0f) {
        val facette = Path().apply {
            moveTo(size.width * 0.28f, 0f)
            lineTo(size.width * 0.72f, 0f)
            lineTo(size.width * 0.62f, size.height * 0.34f)
            lineTo(size.width * 0.38f, size.height * 0.34f)
            close()
        }
        drawPath(facette, Color.White, alpha = ALPHA_TABLE * table)
    }
}

/** Les veines se tracent l'une après l'autre, puis refroidissent jusqu'à rester en filigrane. */
private fun DrawScope.dessinerVeines(t: Float, couleurs: CouleursOmegadex) {
    val refroidi = avancement(t, ChronoGemmeOmega.REFROIDISSEMENT, ChronoGemmeOmega.REFROIDISSEMENT_DUREE)
    val opacite = lerp(1f, ALPHA_VEINES_FINALE, refroidi)
    val epaisseur = max(1.5.dp.toPx(), size.height * 0.03f)
    VEINES.forEachIndexed { i, veine ->
        val p = avancement(t, i * ChronoGemmeOmega.VEINE_DECALAGE, ChronoGemmeOmega.VEINE_DUREE)
        if (p <= 0f) return@forEachIndexed
        val debut = Offset(size.width * veine.x, size.height * veine.y)
        val angle = Math.toRadians(veine.angle.toDouble())
        val longueur = size.width * veine.longueur * p
        val fin = debut + Offset((cos(angle) * longueur).toFloat(), (sin(angle) * longueur).toFloat())
        drawLine(couleurs.lueurMagma, debut, fin, epaisseur * 3f, StrokeCap.Round, alpha = 0.35f * opacite)
        drawLine(couleurs.magma, debut, fin, epaisseur, StrokeCap.Round, alpha = opacite)
    }
}

/** Le Ω est frappé comme un sceau (échelle 1,9 → 0,92 → 1) et rougeoie comme une braise. */
private fun DrawScope.dessinerOmegaAllume(t: Float, couleurs: CouleursOmegadex) {
    val p = ((t - ChronoGemmeOmega.OMEGA) / ChronoGemmeOmega.FRAPPE_DUREE).coerceIn(0f, 1f)
    if (p <= 0f) return
    val opacite = (p / 0.65f).coerceAtMost(1f)
    val echelle = if (p < 0.65f) {
        lerp(1.9f, 0.92f, EASE_OUT.transform(p / 0.65f))
    } else {
        lerp(0.92f, 1f, EASE_OUT.transform((p - 0.65f) / 0.35f))
    }
    val braise = ((t - ChronoGemmeOmega.OMEGA) / ChronoGemmeOmega.BRAISE_DUREE).coerceIn(0f, 1f)
    val lueur = if (braise < 0.35f) braise / 0.35f else 1f - EASE_OUT.transform((braise - 0.35f) / 0.65f)
    scale(echelle, pivot = Offset(size.width / 2f, size.height * 0.4f)) {
        if (lueur > 0f) dessinerOmega(couleurs.magma.copy(alpha = 0.55f * lueur * opacite), epaisseurRelative = 2.4f)
        dessinerOmega(couleurs.omegaSurRubis.copy(alpha = opacite))
    }
}

/**
 * Signe Ω de la DA (même tracé que `ic_omega`), centré dans la moitié haute de la gemme.
 * [epaisseurRelative] élargit le trait, pour la lueur de braise.
 */
private fun DrawScope.dessinerOmega(couleur: Color, epaisseurRelative: Float = 1f) {
    val e = size.width * 0.5f / 16f
    val dx = size.width / 2f - 12f * e
    val dy = size.height * 0.1f - 3f * e
    val chemin = Path().apply {
        moveTo(dx + 4f * e, dy + 20f * e)
        lineTo(dx + 9f * e, dy + 20f * e)
        lineTo(dx + 9f * e, dy + 17.4f * e)
        arcTo(Rect(Offset(dx + 12f * e, dy + 10.53f * e), 7.5f * e), 113.6f, 312.8f, forceMoveTo = false)
        lineTo(dx + 15f * e, dy + 20f * e)
        lineTo(dx + 20f * e, dy + 20f * e)
    }
    val trait = Stroke(2.6f * e * epaisseurRelative, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(chemin, couleur, style = trait)
}
