package com.omegadex.app.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Rubis

/** Largeur et hauteur de référence de la maquette : les veines sont dessinées dans ce repère puis mises à l'échelle. */
private const val LARGEUR_REF = 350f
private const val HAUTEUR_REF = 250f
private const val DUREE_VEINE_MS = 3_600
private const val DECALAGE_VEINE_MS = 1_200
private const val OPACITE_MIN = 0.3f
private const val OPACITE_MAX = 0.95f
private const val OPACITE_OMEGA = 0.14f
private const val OMEGA_CENTRE_X = 251f
private const val OMEGA_CENTRE_Y = 191.5f
private const val OMEGA_RAYON = 34f
private const val OMEGA_EPAISSEUR = 10f
private const val OMEGA_ANGLE_DEPART = 116f
private const val OMEGA_BALAYAGE = 308f

private val VEINE_1 = listOf(-10f to 196f, 48f to 172f, 76f to 188f, 132f to 150f, 170f to 162f, 214f to 122f)
private val OMEGA_PIED_GAUCHE = listOf(210f to 236f, 236f to 236f, 236f to 222f)
private val OMEGA_PIED_DROIT = listOf(266f to 236f, 292f to 236f)
private val VEINE_2 = listOf(360f to 40f, 318f to 66f, 330f to 96f, 286f to 120f, 300f to 150f)

/**
 * Carte rubis de la DA « Épuré Oméga » : fond rubis, deux veines de magma qui pulsent lentement
 * et un Ω en filigrane. Les veines restent fixes si les animations système sont réduites.
 * La couleur reste rubis en thème sombre : c'est la carte de l'étape en cours, signature de l'app.
 */
@Composable
fun CarteRubis(modifier: Modifier = Modifier, contenu: @Composable ColumnScope.() -> Unit) {
    val reduites = OmegadexTheme.animationsReduites
    val transition = rememberInfiniteTransition(label = "veines")
    val pulsation = pulsationVeine()
    val opacite1 by transition.animateFloat(OPACITE_MIN, OPACITE_MAX, pulsation, label = "veine1")
    val opacite2 by transition.animateFloat(
        OPACITE_MIN,
        OPACITE_MAX,
        pulsationVeine(StartOffset(DECALAGE_VEINE_MS)),
        label = "veine2",
    )
    Box(
        modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Rubis),
    ) {
        Canvas(Modifier.matchParentSize()) {
            dessinerVeine(VEINE_1, LueurMagma, if (reduites) OPACITE_MAX else opacite1, largeur = 2.5f)
            dessinerVeine(VEINE_2, Magma, if (reduites) OPACITE_MAX else opacite2, largeur = 2f)
            dessinerOmegaFiligrane()
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = contenu)
    }
}

private fun pulsationVeine(decalage: StartOffset = StartOffset(0)): InfiniteRepeatableSpec<Float> = infiniteRepeatable(
    animation = keyframes {
        durationMillis = DUREE_VEINE_MS
        OPACITE_MIN at 0 using LinearEasing
        OPACITE_MAX at DUREE_VEINE_MS / 2 using LinearEasing
        OPACITE_MIN at DUREE_VEINE_MS
    },
    repeatMode = RepeatMode.Restart,
    initialStartOffset = decalage,
)

/** Échelle uniforme : la largeur de la carte correspond à la largeur de la maquette. */
private fun DrawScope.echelle() = size.width / LARGEUR_REF

/** Point de la maquette, repéré depuis le coin haut gauche. */
private fun DrawScope.depuisHaut(x: Float, y: Float) = Offset(x * echelle(), y * echelle())

/** Point de la maquette, repéré depuis le coin bas droit (la carte peut être plus haute que la maquette). */
private fun DrawScope.depuisBas(x: Float, y: Float) =
    Offset(size.width - (LARGEUR_REF - x) * echelle(), size.height - (HAUTEUR_REF - y) * echelle())

private fun DrawScope.dessinerVeine(points: List<Pair<Float, Float>>, couleur: Color, opacite: Float, largeur: Float) {
    val chemin = Path()
    points.forEachIndexed { index, (x, y) ->
        val p = depuisHaut(x, y)
        if (index == 0) chemin.moveTo(p.x, p.y) else chemin.lineTo(p.x, p.y)
    }
    drawPath(
        chemin,
        couleur.copy(alpha = opacite),
        style = Stroke(width = largeur.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Ω en filigrane dans le coin bas droit : « M210 236 h26 v-14 a34 34 0 1 1 30 0 v14 h26 » de la maquette. */
private fun DrawScope.dessinerOmegaFiligrane() {
    val centre = depuisBas(OMEGA_CENTRE_X, OMEGA_CENTRE_Y)
    val rayon = OMEGA_RAYON * echelle()
    val chemin = Path().apply {
        OMEGA_PIED_GAUCHE.forEachIndexed { index, (x, y) ->
            val p = depuisBas(x, y)
            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        arcTo(Rect(centre, rayon), OMEGA_ANGLE_DEPART, OMEGA_BALAYAGE, forceMoveTo = false)
        OMEGA_PIED_DROIT.forEach { (x, y) -> depuisBas(x, y).let { lineTo(it.x, it.y) } }
    }
    drawPath(
        chemin,
        Blanc.copy(alpha = OPACITE_OMEGA),
        style = Stroke(width = OMEGA_EPAISSEUR * echelle(), join = StrokeJoin.Round),
    )
}

@ApercusClairSombre
@Composable
private fun CarteRubisPreview() {
    OmegadexTheme {
        CarteRubis(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("ÉTAPE 3 SUR 13", color = Blanc)
            Text("Voltère", color = Blanc)
        }
    }
}
