package com.omegadex.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

private const val DUREE_JAUGE_MS = 600
private const val ANGLE_DEPART = -90f
private const val TOUR_COMPLET = 360f

@Composable
private fun progressionAnimee(progression: Float): Float {
    val cible = progression.coerceIn(0f, 1f)
    val animee by animateFloatAsState(
        targetValue = cible,
        animationSpec = if (OmegadexTheme.animationsReduites) snap() else tween(DUREE_JAUGE_MS),
        label = "progression",
    )
    return animee
}

/** Losange de facette centré sur [centre]. */
private fun losange(centre: Offset, largeur: Float, hauteur: Float) = Path().apply {
    moveTo(centre.x, centre.y - hauteur / 2f)
    lineTo(centre.x + largeur / 2f, centre.y)
    lineTo(centre.x, centre.y + hauteur / 2f)
    lineTo(centre.x - largeur / 2f, centre.y)
    close()
}

/**
 * Jauge en barre : piste, remplissage rubis et facette de magma au bout du remplissage.
 * [description] est lue par TalkBack (par exemple « Aventure 23 % »).
 */
@Composable
fun ProgressGauge(
    progression: Float,
    description: String,
    modifier: Modifier = Modifier,
    couleur: Color = MaterialTheme.colorScheme.primary,
) {
    val couleurs = OmegadexTheme.couleurs
    val valeur = progressionAnimee(progression)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progression.coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        val epaisseur = 8.dp.toPx()
        val haut = (size.height - epaisseur) / 2f
        val rayon = CornerRadius(epaisseur / 2f)
        drawRoundRect(couleurs.piste, Offset(0f, haut), Size(size.width, epaisseur), rayon)
        val fin = size.width * valeur
        if (fin > 0f) {
            drawRoundRect(couleur, Offset(0f, haut), Size(fin, epaisseur), rayon)
            val centre = Offset(fin.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), size.height / 2f)
            drawPath(losange(centre, 12.dp.toPx(), size.height), couleurs.magma)
        }
    }
}

/**
 * Jauge en anneau des maquettes de l'accueil, avec un contenu au centre (facette, Ω…).
 * Par défaut, une facette de la couleur de la jauge.
 */
@Composable
fun ProgressGaugeRing(
    progression: Float,
    description: String,
    modifier: Modifier = Modifier,
    couleur: Color = MaterialTheme.colorScheme.primary,
    diametre: Dp = 44.dp,
    centre: @Composable () -> Unit = { FacetteCentrale(couleur) },
) {
    val couleurs = OmegadexTheme.couleurs
    val valeur = progressionAnimee(progression)
    Box(
        modifier
            .size(diametre)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progression.coerceIn(0f, 1f), 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diametre)) {
            val trait = 5.dp.toPx()
            val inset = trait / 2f
            val zone = Size(size.width - trait, size.height - trait)
            drawArc(couleurs.piste, 0f, TOUR_COMPLET, false, Offset(inset, inset), zone, style = Stroke(trait))
            if (valeur > 0f) {
                drawArc(
                    couleur,
                    ANGLE_DEPART,
                    TOUR_COMPLET * valeur,
                    false,
                    Offset(inset, inset),
                    zone,
                    style = Stroke(trait, cap = StrokeCap.Round),
                )
            }
        }
        centre()
    }
}

@Composable
private fun FacetteCentrale(couleur: Color) {
    Canvas(Modifier.size(width = 9.dp, height = 13.dp)) {
        drawPath(losange(Offset(size.width / 2f, size.height / 2f), size.width, size.height), couleur)
    }
}

@ApercusClairSombre
@Composable
private fun ProgressGaugePreview() {
    OmegadexTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressGauge(progression = 0.6f, description = "Jauge à 60 %")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressGaugeRing(progression = 0.23f, description = "Aventure 23 %")
                Text("23 %", style = OmegadexTheme.chiffres.grand)
                ProgressGaugeRing(
                    progression = 41f / 211f,
                    description = "Pokédex 41 sur 211",
                    couleur = OmegadexTheme.couleurs.magma,
                )
            }
        }
    }
}
