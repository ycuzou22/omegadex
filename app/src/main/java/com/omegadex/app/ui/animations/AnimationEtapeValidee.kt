// Coordonnées et instants recopiés de la maquette jouable du canevas : ce sont des données, pas des seuils.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import com.omegadex.app.ui.theme.Rubis
import com.omegadex.app.ui.theme.RubisEclat
import com.omegadex.app.ui.theme.RubisProfond
import com.omegadex.app.ui.theme.TexteSurRoche
import kotlinx.coroutines.delay

/**
 * Chronologie de la maquette « Animation — Étape validée » (millisecondes depuis l'apparition du voile).
 * Le voile roche apparaît, les veines de magma se tracent, le Ω se dessine, le badge tombe avec un éclat,
 * puis les textes montent. Avec les animations réduites, l'image finale s'affiche sans mouvement.
 */
private object Chrono {
    const val VOILE = 0f
    const val VOILE_DUREE = 350f
    const val VEINE_1 = 150f
    const val VEINE_2 = 400f
    const val VEINE_DUREE = 1_000f
    const val OMEGA = 550f
    const val OMEGA_DUREE = 800f
    const val BADGE = 1_150f
    const val BADGE_DUREE = 700f
    const val ECLAT = 1_650f
    const val ECLAT_DUREE = 1_000f
    const val TITRE = 1_750f
    const val SOUS_TITRE = 1_950f
    const val TEXTE_DUREE = 500f
    const val FIN = 3_550f
    const val ATTENTE_REDUITE_MS = 2_500L
}

private val EASE_ATTERRISSAGE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val EASE_OUT = LinearOutSlowInEasing

/** Avancement (0 à 1) d'une phase qui commence à [debut] et dure [duree], à l'instant [t]. */
private fun avancement(t: Float, debut: Float, duree: Float, easing: Easing = EASE_OUT): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Dimensions de la maquette (390 × 844) : les veines y sont dessinées puis mises à l'échelle de l'écran. */
private const val LARGEUR_MAQUETTE = 390f
private const val HAUTEUR_MAQUETTE = 844f
private val VEINE_1 = listOf(
    -10f to 560f,
    60f to 520f,
    96f to 548f,
    160f to 480f,
    200f to 500f,
    260f to 430f,
    300f to 452f,
    400f to 380f,
)
private val VEINE_2 = listOf(
    400f to 250f,
    340f to 290f,
    356f to 330f,
    290f to 372f,
    310f to 410f,
    240f to 470f,
)

/**
 * Voile plein écran « Étape validée ». Annoncé par TalkBack ; appelle [onFin] une fois l'animation jouée
 * (ou après une courte pause, sans mouvement, si les animations sont réduites).
 */
@Composable
fun AnimationEtapeValidee(textes: TextesEtapeValidee, onFin: () -> Unit, modifier: Modifier = Modifier) {
    val reduites = OmegadexTheme.animationsReduites
    val temps = remember { Animatable(if (reduites) Chrono.FIN else 0f) }
    LaunchedEffect(textes) {
        if (reduites) {
            delay(Chrono.ATTENTE_REDUITE_MS)
        } else {
            temps.animateTo(Chrono.FIN, tween(Chrono.FIN.toInt(), easing = LinearEasing))
        }
        onFin()
    }
    ContenuEtapeValidee(textes, temps.value, modifier)
}

@Composable
private fun ContenuEtapeValidee(textes: TextesEtapeValidee, t: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = avancement(t, Chrono.VOILE, Chrono.VOILE_DUREE) }
            .background(Roche)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            dessinerVeine(VEINE_1, Magma, 3.dp.toPx(), avancement(t, Chrono.VEINE_1, Chrono.VEINE_DUREE))
            dessinerVeine(VEINE_2, LueurMagma, 2.4.dp.toPx(), avancement(t, Chrono.VEINE_2, Chrono.VEINE_DUREE))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            EmblemeBadge(t)
            Text(
                textes.surtitre.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LueurMagma,
                modifier = Modifier.montee(t, Chrono.TITRE),
            )
            Column(
                Modifier.montee(t, Chrono.SOUS_TITRE).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    textes.titre,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Blanc,
                    textAlign = TextAlign.Center,
                )
                textes.sousTitre?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TexteSurRoche,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Texte qui monte de 12 dp en apparaissant (animation « rise » de la maquette). */
private fun Modifier.montee(t: Float, debut: Float) = graphicsLayer {
    val p = avancement(t, debut, Chrono.TEXTE_DUREE)
    alpha = p
    translationY = (1f - p) * 12.dp.toPx()
}

/** Ω tracé, éclat qui s'ouvre, et badge rubis qui tombe en tournant. */
@Composable
private fun EmblemeBadge(t: Float) {
    Box(Modifier.size(180.dp)) {
        Canvas(Modifier.fillMaxSize()) { dessinerOmega(avancement(t, Chrono.OMEGA, Chrono.OMEGA_DUREE)) }
        Canvas(Modifier.offset(15.dp, 10.dp).size(150.dp)) {
            dessinerEclat(avancement(t, Chrono.ECLAT, Chrono.ECLAT_DUREE))
        }
        val p = avancement(t, Chrono.BADGE, Chrono.BADGE_DUREE, EASE_ATTERRISSAGE)
        Box(
            Modifier
                .offset(58.dp, 56.dp)
                .size(width = 64.dp, height = 58.dp)
                .graphicsLayer {
                    val chute = (p / ATTERRISSAGE).coerceAtMost(1f)
                    val rebond = ((p - ATTERRISSAGE) / (1f - ATTERRISSAGE)).coerceIn(0f, 1f)
                    alpha = chute
                    translationY = lerp(-120f, 0f, chute) * density
                    val echelle = if (p < ATTERRISSAGE) lerp(0.4f, 1.2f, chute) else lerp(1.2f, 1f, rebond)
                    scaleX = echelle
                    scaleY = echelle
                    rotationZ = if (p < ATTERRISSAGE) lerp(-30f, 8f, chute) else lerp(8f, 0f, rebond)
                }
                .clip(FormeGemme)
                .background(
                    Brush.linearGradient(
                        0f to RubisEclat,
                        0.38f to RubisEclat,
                        0.38f to Rubis,
                        0.7f to Rubis,
                        0.7f to RubisProfond,
                        1f to RubisProfond,
                    ),
                ),
        )
    }
}

/** Le badge atteint sa position à 60 % de sa chute, puis se pose (maquette : keyframe `land`). */
private const val ATTERRISSAGE = 0.6f

private fun DrawScope.dessinerVeine(points: List<Pair<Float, Float>>, couleur: Color, largeur: Float, p: Float) {
    if (p <= 0f) return
    val sx = size.width / LARGEUR_MAQUETTE
    val sy = size.height / HAUTEUR_MAQUETTE
    val chemin = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * sx, y * sy) else lineTo(x * sx, y * sy) }
    }
    drawPath(portion(chemin, p), couleur, style = Stroke(largeur, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Ω de l'app (« M4 20h5v-2.6a7.5 7.5 0 1 1 6 0V20h5 » dans un repère 24 × 24), tracé jusqu'à [p]. */
private fun DrawScope.dessinerOmega(p: Float) {
    if (p <= 0f) return
    val e = size.width / REPERE_ICONE
    val chemin = Path().apply {
        moveTo(4f * e, 20f * e)
        lineTo(9f * e, 20f * e)
        lineTo(9f * e, 17.4f * e)
        arcTo(Rect(Offset(12f * e, 10.53f * e), 7.5f * e), 113.6f, 312.8f, forceMoveTo = false)
        lineTo(15f * e, 20f * e)
        lineTo(20f * e, 20f * e)
    }
    drawPath(
        portion(chemin, p),
        LueurMagma,
        style = Stroke(1.4f * e, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private const val REPERE_ICONE = 24f

/** Huit rayons qui s'ouvrent en tournant puis s'éteignent (keyframe `burst`). */
private fun DrawScope.dessinerEclat(p: Float) {
    if (p <= 0f || p >= 1f) return
    val opacite = if (p < 0.3f) p / 0.3f else 1f - (p - 0.3f) / 0.7f
    val echelle = lerp(0.3f, 1.6f, p)
    val c = Offset(size.width / 2f, size.height / 2f)
    val e = size.width / REPERE_ECLAT * echelle
    rotate(40f * p, c) {
        RAYONS.forEach { rayon ->
            drawLine(
                LueurMagma.copy(alpha = opacite),
                Offset(c.x + (rayon[0] - 55f) * e, c.y + (rayon[1] - 55f) * e),
                Offset(c.x + (rayon[2] - 55f) * e, c.y + (rayon[3] - 55f) * e),
                strokeWidth = 3f * e,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val REPERE_ECLAT = 110f
private val RAYONS = listOf(
    listOf(55f, 6f, 55f, 20f),
    listOf(55f, 90f, 55f, 104f),
    listOf(6f, 55f, 20f, 55f),
    listOf(90f, 55f, 104f, 55f),
    listOf(20f, 20f, 30f, 30f),
    listOf(80f, 80f, 90f, 90f),
    listOf(90f, 20f, 80f, 30f),
    listOf(30f, 80f, 20f, 90f),
)

/** Début d'un chemin, sur la fraction [p] de sa longueur. */
private fun portion(chemin: Path, p: Float): Path {
    val mesure = PathMeasure().apply { setPath(chemin, false) }
    return Path().also { mesure.getSegment(0f, mesure.length * p, it, true) }
}

@ApercusClairSombre
@Composable
private fun AnimationEtapeValideePreview() {
    OmegadexTheme {
        ContenuEtapeValidee(
            TextesEtapeValidee("Étape 3 validée", "Badge Dynamo", "Prochaine étape débloquée : Adriane"),
            t = Chrono.FIN,
        )
    }
}
