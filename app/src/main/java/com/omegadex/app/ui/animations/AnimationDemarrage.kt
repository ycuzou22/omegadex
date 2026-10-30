// Coordonnées et instants recopiés de la maquette « Goutte de magma forgée » du canevas : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.omegadex.app.R
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.sons.SonsAuxReperes
import com.omegadex.app.ui.theme.OmegadexTheme
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Chronologie du démarrage (ms) : une goutte de magma tombe et s'écrase, refroidit en rubis taillé dans un
 * nuage de vapeur, le Ω s'y trace, un reflet traverse la pierre et « OMÉGADEX » se resserre dessous.
 */
private object ChronoDemarrage {
    const val CHUTE_DUREE = 550f
    const val ECRASEMENT = 470f
    const val FORGE = 450f
    const val FORGE_DUREE = 1_300f
    const val REFROIDISSEMENT_DUREE = 850f
    const val VAPEUR_DUREE = 1_100f
    const val OMEGA = 1_300f
    const val OMEGA_DUREE = 450f
    const val REFLET = 1_650f
    const val REFLET_DUREE = 600f
    const val MARQUE = 1_200f
    const val MARQUE_DUREE = 1_000f
    const val FIN = 2_400f
    const val SORTIE_DUREE = 350f
    const val ATTENTE_REDUITE_MS = 900L
}

private val VAPEURS = listOf(-0.12f to 600f, 0.04f to 750f, -0.04f to 900f)
private val EASE_OUT: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EASE_CHUTE: Easing = CubicBezierEasing(0.5f, 0f, 1f, 0.6f)
private val EASE_REBOND: Easing = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
private val FOND = Color(0xFF140F11)
private val FOND_CENTRE = Color(0xFF2A1A1E)
private val MAGMA_CLAIR = Color(0xFFFFF1B8)
private val MAGMA = Color(0xFFFFB15C)
private val MAGMA_SOMBRE = Color(0xFFFF6B2C)
private val CONTOUR = Color(0xFFFFD6DC)

/** Facettes du rubis dans un carré de 120 unités : sommets et couleur une fois refroidi. */
private val FACETTES = listOf(
    listOf(36f to 10f, 84f to 10f, 92f to 42f, 28f to 42f) to Color(0xFFF27A8E),
    listOf(36f to 10f, 28f to 42f, 8f to 42f) to Color(0xFFFFB3BF),
    listOf(84f to 10f, 112f to 42f, 92f to 42f) to Color(0xFFC21F3E),
    listOf(8f to 42f, 28f to 42f, 60f to 112f) to Color(0xFFE2465F),
    listOf(28f to 42f, 60f to 42f, 60f to 112f) to Color(0xFFC9334F),
    listOf(60f to 42f, 92f to 42f, 60f to 112f) to Color(0xFFA80F2C),
    listOf(92f to 42f, 112f to 42f, 60f to 112f) to Color(0xFF7A0C22),
)
private val CONTOUR_GEMME = listOf(36f to 10f, 84f to 10f, 112f to 42f, 60f to 112f, 8f to 42f)
private const val UNITES = 120f
private const val TAILLE_GEMME_DP = 150
/** Le rubis est un peu au-dessus du centre, la marque dessous. */
private const val DECALAGE_GEMME_DP = -40
private const val DECALAGE_MARQUE_DP = 72

private fun phase(t: Float, debut: Float, duree: Float, easing: Easing = EASE_OUT): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

/**
 * Animation de démarrage de l'application (≈2,4 s), jouée une fois par lancement par-dessus le premier écran.
 * Toucher l'écran la passe. Avec les animations réduites, le logo final s'affiche brièvement sans mouvement.
 * Appelle [onFin] une seule fois, une fois le fondu de sortie terminé.
 */
@Composable
fun AnimationDemarrage(onFin: () -> Unit, modifier: Modifier = Modifier) {
    val reduites = OmegadexTheme.animationsReduites
    val temps = remember { Animatable(if (reduites) ChronoDemarrage.FIN else 0f) }
    val sortie = remember { Animatable(0f) }
    var passe by remember { mutableStateOf(false) }
    LaunchedEffect(passe) {
        if (!passe) {
            if (reduites) {
                delay(ChronoDemarrage.ATTENTE_REDUITE_MS)
            } else {
                temps.animateTo(ChronoDemarrage.FIN, tween(ChronoDemarrage.FIN.toInt(), easing = LinearEasing))
            }
        }
        sortie.animateTo(1f, tween(if (reduites) 0 else ChronoDemarrage.SORTIE_DUREE.toInt()))
        onFin()
    }
    val t = temps.value
    SonsAuxReperes(t, REPERES_SONS_DEMARRAGE)
    val description = stringResource(R.string.demarrage_description)
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - sortie.value }
            .background(Brush.radialGradient(listOf(FOND_CENTRE, FOND)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                passe = true
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { dessinerDemarrage(t) }
        Marque(t)
    }
}

/** Sons du démarrage : la goutte s'écrase, puis la vapeur s'échappe du rubis. */
private val REPERES_SONS_DEMARRAGE = listOf(
    ChronoDemarrage.ECRASEMENT to Son.BADGE_IMPACT,
    VAPEURS.first().second to Son.BADGE_VAPEUR,
)

@Composable
private fun Marque(t: Float) {
    val p = phase(t, ChronoDemarrage.MARQUE, ChronoDemarrage.MARQUE_DUREE)
    Text(
        text = stringResource(R.string.demarrage_marque),
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (0.9f - 0.58f * p).em,
        modifier = Modifier
            .offset(y = DECALAGE_MARQUE_DP.dp)
            .graphicsLayer { alpha = p },
    )
}

private fun DrawScope.dessinerDemarrage(t: Float) {
    val taille = TAILLE_GEMME_DP.dp.toPx()
    val centre = Offset(size.width / 2f, size.height / 2f + DECALAGE_GEMME_DP.dp.toPx())
    dessinerGoutte(t, centre)
    VAPEURS.forEach { (decalage, debut) -> dessinerVapeur(t, centre, decalage * taille, debut) }
    dessinerRubis(t, centre, taille)
}

private fun DrawScope.dessinerGoutte(t: Float, centre: Offset) {
    if (t >= ChronoDemarrage.CHUTE_DUREE) return
    val chute = phase(t, 0f, ChronoDemarrage.ECRASEMENT, EASE_CHUTE)
    val ecrase = phase(t, ChronoDemarrage.ECRASEMENT, ChronoDemarrage.CHUTE_DUREE - ChronoDemarrage.ECRASEMENT)
    val largeur = 34.dp.toPx() * (1f + 0.6f * ecrase)
    val hauteur = 42.dp.toPx() * (1f - 0.6f * ecrase)
    val y = -hauteur + (centre.y + hauteur) * chute
    val haut = Offset(centre.x - largeur / 2f, y - hauteur / 2f)
    drawCircle(
        Brush.radialGradient(
            listOf(MAGMA_SOMBRE.copy(alpha = 0.5f), Color.Transparent),
            centre.copy(y = y),
            largeur * 1.6f,
        ),
        radius = largeur * 1.6f,
        center = centre.copy(y = y),
    )
    drawOval(
        Brush.radialGradient(
            listOf(MAGMA_CLAIR, MAGMA, MAGMA_SOMBRE),
            Offset(haut.x + largeur * 0.4f, haut.y + hauteur * 0.35f),
            largeur,
        ),
        topLeft = haut,
        size = Size(largeur, hauteur),
        alpha = 1f - ecrase,
    )
}

private fun DrawScope.dessinerVapeur(t: Float, centre: Offset, decalage: Float, debut: Float) {
    val p = phase(t, debut, ChronoDemarrage.VAPEUR_DUREE)
    if (p <= 0f || p >= 1f) return
    val opacite = if (p < 0.3f) p / 0.3f * 0.8f else 0.8f * (1f - (p - 0.3f) / 0.7f)
    val rayon = 13.dp.toPx() * (0.6f + p)
    val c = Offset(centre.x + decalage, centre.y + 40.dp.toPx() - 120.dp.toPx() * p)
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f * opacite), Color.Transparent), c, rayon * 1.6f),
        radius = rayon * 1.6f,
        center = c,
    )
}

private fun DrawScope.dessinerRubis(t: Float, centre: Offset, taille: Float) {
    if (t < ChronoDemarrage.FORGE) return
    val forge = phase(t, ChronoDemarrage.FORGE, ChronoDemarrage.FORGE_DUREE, EASE_REBOND)
    val chaleur = 1f - phase(t, ChronoDemarrage.FORGE, ChronoDemarrage.REFROIDISSEMENT_DUREE)
    val apparition = phase(t, ChronoDemarrage.FORGE, ChronoDemarrage.FORGE_DUREE * 0.2f)
    val echelle = 0.2f + 0.8f * forge
    val u = taille * echelle / UNITES
    val origine = Offset(centre.x - UNITES / 2f * u, centre.y - UNITES / 2f * u)
    fun point(p: Pair<Float, Float>) = Offset(origine.x + p.first * u, origine.y + p.second * u)
    fun chemin(points: List<Pair<Float, Float>>) = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(point(p).x, point(p).y) else lineTo(point(p).x, point(p).y) }
        close()
    }
    val halo = taille * 0.75f
    drawCircle(
        Brush.radialGradient(listOf(lerp(Color(0x66E2465F), MAGMA_SOMBRE, chaleur), Color.Transparent), centre, halo),
        radius = halo,
        center = centre,
        alpha = apparition,
    )
    FACETTES.forEach { (points, couleur) ->
        drawPath(chemin(points), lerp(couleur, lerp(MAGMA, MAGMA_CLAIR, chaleur * 0.5f), chaleur), alpha = apparition)
    }
    val gemme = chemin(CONTOUR_GEMME)
    drawPath(gemme, CONTOUR, alpha = apparition, style = Stroke(width = 2f * u, join = StrokeJoin.Round))
    dessinerOmega(t, u, ::point)
    val reflet = phase(t, ChronoDemarrage.REFLET, ChronoDemarrage.REFLET_DUREE)
    if (reflet > 0f && reflet < 1f) {
        clipPath(gemme) {
            val x = origine.x - UNITES * u + reflet * UNITES * u * 3f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                    start = Offset(x - 20f * u, origine.y),
                    end = Offset(x + 20f * u, origine.y + 30f * u),
                ),
            )
        }
    }
}

private fun DrawScope.dessinerOmega(t: Float, u: Float, point: (Pair<Float, Float>) -> Offset) {
    val p = phase(t, ChronoDemarrage.OMEGA, ChronoDemarrage.OMEGA_DUREE)
    if (p <= 0f) return
    fun o(x: Float, y: Float) = point(x to y)
    val omega = Path().apply {
        o(40f, 74f).let { moveTo(it.x, it.y) }
        o(51f, 74f).let { lineTo(it.x, it.y) }
        cubicTo(o(38f, 66f).x, o(38f, 66f).y, o(36f, 50f).x, o(36f, 50f).y, o(44f, 40f).x, o(44f, 40f).y)
        cubicTo(o(52f, 30f).x, o(52f, 30f).y, o(68f, 30f).x, o(68f, 30f).y, o(76f, 40f).x, o(76f, 40f).y)
        cubicTo(o(84f, 50f).x, o(84f, 50f).y, o(82f, 66f).x, o(82f, 66f).y, o(69f, 74f).x, o(69f, 74f).y)
        o(80f, 74f).let { lineTo(it.x, it.y) }
    }
    val mesure = PathMeasure().apply { setPath(omega, false) }
    val trace = Path()
    mesure.getSegment(0f, mesure.length * min(p, 1f), trace, true)
    drawPath(trace, Color.White, style = Stroke(width = 7f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Preview
@Composable
private fun AnimationDemarragePreview() {
    OmegadexTheme { AnimationDemarrage(onFin = {}) }
}
