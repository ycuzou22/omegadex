// Instants et coordonnées recopiés de la maquette jouable « Animation — Évolution » : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import com.omegadex.app.ui.theme.TexteSurRoche
import kotlin.math.hypot

/** Chronologie (ms) depuis l'ouverture du voile, comme la maquette. */
private object ChronoEvolution {
    const val OUVERTURE_DUREE = 700f
    const val INTRO_DUREE = 500f
    const val EVOLUTION = 1_100f
    const val EVOLUTION_DUREE = 3_000f
    const val FLASH = EVOLUTION + EVOLUTION_DUREE
    const val FLASH_DUREE = 550f
    const val RAYONS = FLASH + 100f
    const val RAYONS_DUREE = 1_100f
    const val OMEGA = FLASH + 300f
    const val OMEGA_DUREE = 900f
    const val GEMME_DUREE = 700f
    const val TEXTES_DUREE = 500f
    const val FIN = FLASH + 1_300f
}

/** Intervalles (en part des 3 s d'évolution) où l'ancienne silhouette est visible : l'alternance accélère. */
private val ANCIENNE_VISIBLE = listOf(
    0f to 0.12f, 0.221f to 0.30f, 0.371f to 0.43f, 0.481f to 0.52f, 0.561f to 0.59f,
    0.621f to 0.64f, 0.661f to 0.675f, 0.691f to 0.70f, 0.711f to 0.72f,
)

/** Étincelles : abscisse dans la maquette (sur 390) et décalage de départ (ms). */
private val ETINCELLES = listOf(
    70f to 0f,
    120f to 500f,
    170f to 200f,
    215f to 800f,
    262f to 350f,
    305f to 1_000f,
)
private const val ETINCELLE_PERIODE = 1_800f

private val EASE_OUVERTURE = CubicBezierEasing(0.6f, 0f, 0.2f, 1f)
private val EASE_ATTERRISSAGE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

private fun avancement(t: Float, debut: Float, duree: Float, easing: Easing = LinearOutSlowInEasing): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Une espèce dans l'animation : son nom et ses types (clés de l'app : « eau », « sol »…). */
data class EspeceAnimee(val nom: String, val types: List<String>)

/** Textes de l'animation, déjà traduits. */
data class TextesEvolution(
    val intro: String,
    val surtitre: String,
    val titre: String,
    val detail: String?,
    val continuer: String,
    val nomsTypes: List<String>,
)

/**
 * Évolution plein écran qui prend le contrôle de l'app : ouverture circulaire, silhouettes qui alternent
 * de plus en plus vite au milieu d'anneaux et d'étincelles, flash, puis révélation de la nouvelle espèce.
 * Avec les animations réduites, la révélation s'affiche directement. [onFin] est appelé par « Continuer ».
 */
@Composable
fun AnimationEvolution(
    avant: EspeceAnimee,
    apres: EspeceAnimee,
    textes: TextesEvolution,
    onFin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduites = OmegadexTheme.animationsReduites
    val temps = remember { Animatable(if (reduites) ChronoEvolution.FIN else 0f) }
    LaunchedEffect(avant, apres) {
        if (!reduites) {
            temps.animateTo(ChronoEvolution.FIN, tween(ChronoEvolution.FIN.toInt(), easing = LinearEasing))
        }
    }
    ContenuEvolution(apres, textes, temps.value, onFin, modifier)
}

@Composable
private fun ContenuEvolution(
    apres: EspeceAnimee,
    textes: TextesEvolution,
    t: Float,
    onFin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ouverture = avancement(t, 0f, ChronoEvolution.OUVERTURE_DUREE, EASE_OUVERTURE)
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                // Ouverture circulaire depuis le haut de l'écran (clip-path: circle(… at 50% 24%)).
                val rayon = ouverture * 1.5f * hypot(size.width, size.height) / 1.4142f
                val cercle = Path().apply { addOval(Rect(Offset(size.width / 2f, size.height * 0.24f), rayon)) }
                clipPath(cercle) { this@drawWithContent.drawContent() }
            }
            .background(Roche)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        when {
            t < ChronoEvolution.EVOLUTION -> Intro(textes.intro, t)
            t < ChronoEvolution.FLASH -> PhaseEvolution(textes.intro, t)
            else -> Revelation(apres, textes, t, onFin)
        }
        val flash = avancement(t, ChronoEvolution.FLASH, ChronoEvolution.FLASH_DUREE, LinearEasing)
        if (flash in 0.001f..0.999f) {
            val opacite = if (flash < 0.35f) flash / 0.35f else 1f - (flash - 0.35f) / 0.65f
            Box(Modifier.fillMaxSize().background(Color(0xFFFFF1E4).copy(alpha = opacite)))
        }
    }
}

@Composable
private fun Intro(texte: String, t: Float) {
    Text(
        texte,
        style = MaterialTheme.typography.headlineSmall,
        color = Blanc,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp, start = 24.dp, end = 24.dp)
            .montee(t, 0f, ChronoEvolution.INTRO_DUREE),
    )
}

@Composable
private fun PhaseEvolution(legende: String, t: Float) {
    val local = t - ChronoEvolution.EVOLUTION
    val part = (local / ChronoEvolution.EVOLUTION_DUREE).coerceIn(0f, 1f)
    val ancienneVisible = ANCIENNE_VISIBLE.any { (debut, fin) -> part in debut..fin }
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { dessinerEtincelles(local) }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 262.dp)
                .size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { dessinerHaloEtAnneaux(local) }
            // Silhouettes blanches qui alternent ; la nouvelle forme est plus grande.
            Box(
                Modifier
                    .size(width = 84.dp, height = 78.dp)
                    .graphicsLayer { alpha = if (ancienneVisible) 1f else 0f }
                    .clip(FormeGemme)
                    .background(Blanc),
            )
            Box(
                Modifier
                    .size(width = 124.dp, height = 112.dp)
                    .graphicsLayer { alpha = if (ancienneVisible) 0f else 1f }
                    .clip(FormeGemmeEvoluee)
                    .background(Blanc),
            )
        }
        Text(
            legende,
            style = MaterialTheme.typography.bodyLarge,
            color = TexteSurRoche,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 120.dp),
        )
    }
}

@Composable
private fun Revelation(apres: EspeceAnimee, textes: TextesEvolution, t: Float, onFin: () -> Unit) {
    val debut = ChronoEvolution.FLASH
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 96.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 70.dp).size(240.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                dessinerRayons(avancement(t, ChronoEvolution.RAYONS, ChronoEvolution.RAYONS_DUREE))
                dessinerOmegaFiligrane(avancement(t, ChronoEvolution.OMEGA, ChronoEvolution.OMEGA_DUREE))
            }
            val p = avancement(t, debut, ChronoEvolution.GEMME_DUREE, EASE_ATTERRISSAGE)
            Box(
                Modifier
                    .offset(58.dp, 54.dp)
                    .size(width = 124.dp, height = 112.dp)
                    .graphicsLayer {
                        val montee = (p / 0.6f).coerceAtMost(1f)
                        val pose = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        alpha = montee
                        val echelle = if (p < 0.6f) lerp(0.3f, 1.15f, montee) else lerp(1.15f, 1f, pose)
                        scaleX = echelle
                        scaleY = echelle
                        rotationZ = if (p < 0.6f) lerp(-25f, 6f, montee) else lerp(6f, 0f, pose)
                    }
                    .clip(FormeGemmeEvoluee)
                    .background(brosseEspece(apres.types)),
            )
        }
        Text(
            textes.surtitre.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = LueurMagma,
            modifier = Modifier.padding(top = 10.dp).montee(t, debut, ChronoEvolution.TEXTES_DUREE),
        )
        Text(
            textes.titre,
            style = MaterialTheme.typography.headlineMedium,
            color = Blanc,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).montee(t, debut + 200f, ChronoEvolution.TEXTES_DUREE),
        )
        Column(
            Modifier.padding(top = 16.dp).montee(t, debut + 400f, ChronoEvolution.TEXTES_DUREE),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apres.types.zip(textes.nomsTypes).forEach { (cle, nom) -> TypeChip(cle, nom) }
            }
            textes.detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TexteSurRoche,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        OmegaButton(
            textes.continuer,
            onClick = onFin,
            avecOmega = true,
            modifier = Modifier.fillMaxWidth().montee(t, debut + 400f, ChronoEvolution.TEXTES_DUREE),
        )
    }
}

private fun Modifier.montee(t: Float, debut: Float, duree: Float) = graphicsLayer {
    val p = avancement(t, debut, duree)
    alpha = p
    translationY = (1f - p) * 14.dp.toPx()
}

/** Dégradé de gemme aux couleurs de l'espèce : reflet clair, type principal, second type (ou ombre). */
private fun brosseEspece(types: List<String>): Brush {
    val principal = CouleursTypes.de(types.firstOrNull().orEmpty())
    val second = types.getOrNull(1)?.let { CouleursTypes.de(it) } ?: lerp(principal, Color.Black, 0.35f)
    val reflet = lerp(principal, Color.White, 0.4f)
    return Brush.linearGradient(
        0f to reflet,
        0.38f to reflet,
        0.38f to principal,
        0.7f to principal,
        0.7f to second,
        1f to second,
    )
}

/** Gemme plus large de l'espèce évoluée (`polygon(22% 0, 78% 0, 100% 30%, 50% 100%, 0 30%)`). */
private object FormeGemmeEvoluee : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(
        Path().apply {
            moveTo(size.width * 0.22f, 0f)
            lineTo(size.width * 0.78f, 0f)
            lineTo(size.width, size.height * 0.3f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height * 0.3f)
            close()
        },
    )
}

private fun DrawScope.dessinerEtincelles(local: Float) {
    val echelle = size.width / 390f
    val depart = size.height - 250.dp.toPx()
    ETINCELLES.forEach { (x, decalage) ->
        if (local < decalage) return@forEach
        val q = FastOutLinearInEasing.transform(((local - decalage) % ETINCELLE_PERIODE) / ETINCELLE_PERIODE)
        val opacite = if (q < 0.2f) q / 0.2f else 1f - (q - 0.2f) / 0.8f
        val e = lerp(0.6f, 1f, q)
        val centre = Offset(x * echelle, depart - 260.dp.toPx() * q)
        val l = 10.dp.toPx() * e
        val h = 9.dp.toPx() * e
        val gemme = Path().apply {
            moveTo(centre.x - l * 0.22f, centre.y - h / 2f)
            lineTo(centre.x + l * 0.22f, centre.y - h / 2f)
            lineTo(centre.x + l / 2f, centre.y - h / 2f + h * 0.34f)
            lineTo(centre.x, centre.y + h / 2f)
            lineTo(centre.x - l / 2f, centre.y - h / 2f + h * 0.34f)
            close()
        }
        drawPath(gemme, LueurMagma.copy(alpha = opacite.coerceIn(0f, 1f)))
    }
}

private fun DrawScope.dessinerHaloEtAnneaux(local: Float) {
    val e = size.width / 240f
    val c = center
    val pulsation = (kotlin.math.sin(local / 1_000f * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f) + 1f) / 2f
    val halo = lerp(0.85f, 1.15f, pulsation)
    drawCircle(
        Brush.radialGradient(
            0f to Magma.copy(alpha = 0.55f * lerp(0.6f, 1f, pulsation)),
            0.65f to Magma.copy(alpha = 0f),
            center = c,
            radius = 100f * e * halo,
        ),
        radius = 100f * e * halo,
        center = c,
    )
    rotate(360f * local / 6_000f, c) {
        drawCircle(
            Magma,
            radius = 110f * e,
            center = c,
            style = Stroke(
                2f * e,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * e, 14f * e)),
            ),
        )
    }
    rotate(-360f * local / 9_000f, c) {
        drawCircle(
            LueurMagma.copy(alpha = 0.6f),
            radius = 92f * e,
            center = c,
            style = Stroke(
                1.5f * e,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * e, 10f * e)),
            ),
        )
    }
}

private val RAYONS_EVOLUTION = listOf(
    listOf(55f, 4f, 55f, 20f),
    listOf(55f, 90f, 55f, 106f),
    listOf(4f, 55f, 20f, 55f),
    listOf(90f, 55f, 106f, 55f),
    listOf(18f, 18f, 29f, 29f),
    listOf(81f, 81f, 92f, 92f),
    listOf(92f, 18f, 81f, 29f),
    listOf(29f, 81f, 18f, 92f),
)

private fun DrawScope.dessinerRayons(p: Float) {
    if (p <= 0f || p >= 1f) return
    val opacite = if (p < 0.3f) p / 0.3f else 1f - (p - 0.3f) / 0.7f
    val e = size.width / 110f * lerp(0.4f, 1.7f, p)
    val c = center
    rotate(30f * p, c) {
        RAYONS_EVOLUTION.forEach { r ->
            drawLine(
                LueurMagma.copy(alpha = opacite),
                Offset(c.x + (r[0] - 55f) * e, c.y + (r[1] - 55f) * e),
                Offset(c.x + (r[2] - 55f) * e, c.y + (r[3] - 55f) * e),
                strokeWidth = 2.5f * e,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Ω en filigrane derrière la nouvelle espèce, tracé trait par trait. */
private fun DrawScope.dessinerOmegaFiligrane(p: Float) {
    if (p <= 0f) return
    val e = size.width / 24f
    val chemin = Path().apply {
        moveTo(4f * e, 20f * e)
        lineTo(9f * e, 20f * e)
        lineTo(9f * e, 17.4f * e)
        arcTo(Rect(Offset(12f * e, 10.53f * e), 7.5f * e), 113.6f, 312.8f, forceMoveTo = false)
        lineTo(15f * e, 20f * e)
        lineTo(20f * e, 20f * e)
    }
    val mesure = PathMeasure().apply { setPath(chemin, false) }
    val trace = Path().also { mesure.getSegment(0f, mesure.length * p, it, true) }
    drawPath(
        trace,
        Magma.copy(alpha = 0.5f),
        style = Stroke(0.8f * e, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@ApercusClairSombre
@Composable
private fun AnimationEvolutionPreview() {
    OmegadexTheme {
        ContenuEvolution(
            EspeceAnimee("Flobio", listOf("eau", "sol")),
            TextesEvolution(
                "Hein ? Gobou évolue !",
                "Évolution réussie",
                "Gobou a évolué\nen Flobio !",
                "Défi réussi : « Évoluer avant Voltère »",
                "Continuer",
                listOf("Eau", "Sol"),
            ),
            t = ChronoEvolution.FIN,
            onFin = {},
        )
    }
}
