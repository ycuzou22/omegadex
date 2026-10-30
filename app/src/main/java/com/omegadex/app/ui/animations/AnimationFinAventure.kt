// Coordonnées, couleurs et durées recopiées de la maquette « Lave 5 · Pluie de braises » du canevas :
// ce sont des données de dessin, pas des seuils.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omegadex.app.R
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Largeur et hauteur de la maquette : le décor est dessiné dans ce repère puis mis à l'échelle. */
private const val LARGEUR = 390f
private const val HAUTEUR = 844f

/** Instant (en secondes) montré quand les animations sont réduites : braises et vapeur déjà en l'air. */
private const val INSTANT_FIGE = 2.4f

private val FondNuit = Color(0xFF120A0C)
private val Or = Color(0xFFFFD36B)
private val OrPale = Color(0xFFFFF8E1)
private val Orange = Color(0xFFFF8A3C)
private val Magma = Color(0xFFFF6B2C)
private val LueurLave = Color(0xFFFFB15C)
private val RougeLave = Color(0xFFD83A14)
private val LaveProfonde = Color(0xFF7A1208)
private val Croute = Color(0xFF3A140C)
private val Vapeur = Color(0xFFE8DCD6)
private val OmbreTitre = Color(0xFF5A0D06)
private val TexteSousTitre = Color(0xFFF3D9C8)

/**
 * Écran de fin d'aventure : le sprite de Primo-Groudon au-dessus d'un lac de magma, entre des rochers fissurés
 * qui fument, sous une pluie de braises, avec « Félicitations ! ». Le décor tourne en boucle ;
 * avec les animations réduites, il s'affiche figé et le titre est déjà en place.
 */
@Composable
fun AnimationFinAventure(modifier: Modifier = Modifier) {
    val reduites = OmegadexTheme.animationsReduites
    var secondes by remember { mutableFloatStateOf(INSTANT_FIGE) }
    val entree = remember { Animatable(if (reduites) 1f else 0f) }
    LaunchedEffect(reduites) {
        if (reduites) {
            secondes = INSTANT_FIGE
            entree.snapTo(1f)
            return@LaunchedEffect
        }
        entree.animateTo(1f, tween(1_600))
    }
    LaunchedEffect(reduites) {
        if (reduites) return@LaunchedEffect
        val depart = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { secondes = INSTANT_FIGE + (it - depart) / 1_000f }
        }
    }
    ContenuFinAventure(secondes, entree.value, modifier)
}

@Composable
private fun ContenuFinAventure(t: Float, entree: Float, modifier: Modifier = Modifier) {
    val decor = remember { DecorLave.nouveau() }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(FondNuit)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Canvas(Modifier.fillMaxSize()) { decor.dessiner(this, t) }
        val echelle = maxWidth.value / LARGEUR
        val taille = min(250f * echelle, maxHeight.value * 0.32f).dp
        val flottement = if (entree < 1f) 0f else sin(t * 2f * PI.toFloat() / 4f) * 4f
        Image(
            painter = BitmapPainter(
                ImageBitmap.imageResource(R.drawable.sprite_primo_groudon),
                filterQuality = FilterQuality.None,
            ),
            contentDescription = stringResource(R.string.fin_sprite_primo_groudon),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (maxHeight.value * 0.04f + flottement).dp)
                .size(taille),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = (maxHeight.value * 0.17f).dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitreFelicitations(t, entree)
            Text(
                stringResource(R.string.fin_sous_titre),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .graphicsLayer {
                        val p = ((entree - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        alpha = p
                        translationY = (1f - p) * 10.dp.toPx()
                    },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TexteSousTitre,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val RebondTitre = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

@Composable
private fun TitreFelicitations(t: Float, entree: Float) {
    val p = RebondTitre.transform((entree / 0.7f).coerceIn(0f, 1f))
    // Lueur qui respire (2,6 s) et reflet qui balaie les lettres (3,5 s), comme la maquette.
    val respiration = (1f - cos(t * 2f * PI.toFloat() / 2.6f)) / 2f
    val cycleReflet = (t % 3.5f) / 3.5f
    val reflet = ((cycleReflet - 0.4f) / 0.3f).coerceIn(0f, 1f)
    val texte = stringResource(R.string.fin_felicitations)
    val base = MaterialTheme.typography.displaySmall.copy(
        fontSize = 46.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
    )
    Box(
        Modifier
            .semantics { heading() }
            .graphicsLayer {
                alpha = p.coerceIn(0f, 1f)
                val echelle = 0.7f + 0.3f * p
                scaleX = echelle
                scaleY = echelle
                translationY = (1f - p) * 18.dp.toPx()
            },
    ) {
        Text(texte, Modifier.offset(y = 3.dp), style = base.copy(color = OmbreTitre))
        val debut = -600f + reflet * 1_200f
        Text(
            texte,
            style = base.copy(
                brush = Brush.linearGradient(
                    0f to Or,
                    0.4f to Or,
                    0.5f to OrPale,
                    0.6f to Or,
                    1f to Orange,
                    start = Offset(debut, 0f),
                    end = Offset(debut + 900f, 120f),
                ),
                shadow = Shadow(
                    color = Magma.copy(alpha = 0.75f + 0.2f * respiration),
                    blurRadius = 16f + 20f * respiration,
                ),
            ),
        )
    }
}

/** Particules tirées une fois pour toutes : le décor est le même à chaque ouverture. */
private class DecorLave private constructor(
    private val braises: List<Braise>,
    private val bulles: List<Bulle>,
    private val vapeurs: List<Bouffee>,
) {
    private class Braise(
        val origine: Offset,
        val rayon: Float,
        val couleur: Color,
        val duree: Float,
        val phase: Float,
        val dx: Float,
        val floue: Boolean,
    )

    private class PlaqueCroute(val x: Float, val y: Float, val rx: Float, val ry: Float)

    private class Bulle(val x: Float, val y: Float, val rayon: Float, val duree: Float, val phase: Float)

    private class Bouffee(val x: Float, val y: Float, val rayon: Float, val duree: Float, val phase: Float)

    fun dessiner(scope: DrawScope, t: Float) = with(scope) {
        val s = size.width / LARGEUR
        // Le bas de la maquette colle au bas de l'écran : rochers et lac restent en bas quel que soit le format.
        translate(top = size.height - HAUTEUR * s) {
            scale(s, s, pivot = Offset.Zero) {
                halo(t)
                braises.forEach { braise(it, t) }
                rocher(78f, 790f, 1.15f, false, t)
                rocher(318f, 796f, 1.35f, true, t)
                rocher(200f, 812f, 0.55f, false, t)
                lueurBase(78f, 792f, 95f, 14f)
                lueurBase(318f, 798f, 110f, 16f)
                lueurBase(200f, 812f, 50f, 9f)
                vague(792f, 5f, 130f, decalage(t, 11f), Brush.verticalGradient(listOf(RougeLave, RougeLave)))
                vague(
                    800f,
                    4f,
                    97.5f,
                    -decalage(t, 7f),
                    Brush.verticalGradient(
                        0f to Or,
                        0.08f to Orange,
                        0.25f to RougeLave,
                        1f to LaveProfonde,
                        startY = 796f,
                        endY = HAUTEUR,
                    ),
                )
                croutes(t)
                bulles.forEach { bulle(it, t) }
                vapeurs.forEach { bouffee(it, t) }
            }
        }
    }

    private fun decalage(t: Float, duree: Float) = (t % duree) / duree * LARGEUR

    private fun DrawScope.halo(t: Float) {
        val respiration = (1f - cos(t * 2f * PI.toFloat() / 4f)) / 2f
        scale(1f, 260f / 300f, pivot = Offset(195f, HAUTEUR)) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Orange.copy(alpha = 0.85f), Orange.copy(alpha = 0f)),
                    center = Offset(195f, HAUTEUR),
                    radius = 300f,
                ),
                radius = 300f * (1f + 0.06f * respiration),
                center = Offset(195f, HAUTEUR),
                alpha = 0.55f + 0.35f * respiration,
            )
        }
    }

    private fun DrawScope.braise(b: Braise, t: Float) {
        val p = ((t / b.duree) + b.phase) % 1f
        val opacite = when {
            p < 0.1f -> p / 0.1f
            p > 0.9f -> (1f - p) / 0.1f * 0.8f
            else -> 1f - 0.2f * ((p - 0.1f) / 0.8f)
        }
        val centre = Offset(b.origine.x + b.dx * p, b.origine.y - 420f * p)
        if (b.floue) {
            // Halo doux en trois disques pleins : bien moins coûteux qu'un dégradé par particule et par image.
            drawCircle(b.couleur, b.rayon * 2.5f, centre, alpha = opacite * 0.15f)
            drawCircle(b.couleur, b.rayon * 1.7f, centre, alpha = opacite * 0.25f)
            drawCircle(b.couleur, b.rayon, centre, alpha = opacite * 0.4f)
        } else {
            drawCircle(b.couleur, b.rayon, centre, alpha = opacite)
        }
    }

    private fun DrawScope.lueurBase(x: Float, y: Float, rx: Float, ry: Float) {
        scale(1f, ry * 2.2f / rx, pivot = Offset(x, y)) {
            drawCircle(
                Brush.radialGradient(listOf(Orange.copy(alpha = 0.85f), Orange.copy(alpha = 0f)), Offset(x, y), rx),
                radius = rx,
                center = Offset(x, y),
            )
        }
    }

    private fun DrawScope.vague(y: Float, amplitude: Float, periode: Float, decalage: Float, remplissage: Brush) {
        val chemin = Path().apply {
            moveTo(0f, HAUTEUR)
            var x = 0f
            while (x <= LARGEUR) {
                lineTo(x, y + amplitude * sin((x + decalage) / periode * 2f * PI.toFloat()))
                x += 4f
            }
            lineTo(LARGEUR, HAUTEUR)
            close()
        }
        drawPath(chemin, remplissage)
    }

    private fun DrawScope.croutes(t: Float) {
        val decalage = decalage(t, 7f)
        CROUTES.forEach { plaque ->
            val x = ((plaque.x + decalage) % 1_170f) - 390f
            if (x in -40f..430f) {
                drawOval(
                    Croute,
                    Offset(x - plaque.rx, plaque.y - plaque.ry),
                    Size(plaque.rx * 2, plaque.ry * 2),
                    alpha = 0.7f,
                )
            }
        }
    }

    private fun DrawScope.bulle(b: Bulle, t: Float) {
        val p = ((t / b.duree) + b.phase) % 1f
        val echelle = when {
            p < 0.72f -> p / 0.72f
            p < 0.84f -> 1f + 0.2f * (p - 0.72f) / 0.12f
            else -> 1.2f
        }
        val opacite = when {
            p < 0.1f -> p / 0.1f
            p < 0.8f -> 1f
            p < 0.84f -> (0.84f - p) / 0.04f
            else -> 0f
        }
        val centre = Offset(b.x, b.y - b.rayon)
        if (opacite > 0f) {
            drawCircle(Or, b.rayon * echelle, centre, alpha = opacite)
            drawCircle(Orange, b.rayon * echelle, centre, alpha = opacite, style = Stroke(2f))
            drawCircle(
                Color(0xFFFFF6C8),
                b.rayon * echelle * 0.25f,
                Offset(b.x - b.rayon * 0.35f * echelle, b.y - b.rayon * 1.35f * echelle),
                alpha = opacite * 0.7f,
            )
        }
        if (p >= 0.8f) {
            val q = (p - 0.8f) / 0.2f
            drawCircle(Or, b.rayon * (0.6f + 1.6f * q), centre, alpha = 0.9f * (1f - q), style = Stroke(2f))
        }
    }

    private fun DrawScope.bouffee(b: Bouffee, t: Float) {
        val p = ((t / b.duree) + b.phase) % 1f
        val opacite = if (p < 0.2f) p / 0.2f * 0.75f else 0.75f * (1f - (p - 0.2f) / 0.8f)
        val rayon = b.rayon * (0.5f + 1.9f * p)
        val centre = Offset(b.x, b.y - 150f * p)
        drawCircle(Vapeur, rayon * 1.6f, centre, alpha = opacite * 0.12f)
        drawCircle(Vapeur, rayon * 1.1f, centre, alpha = opacite * 0.22f)
        drawCircle(Vapeur, rayon * 0.6f, centre, alpha = opacite * 0.3f)
    }

    private fun DrawScope.rocher(x: Float, y: Float, s: Float, miroir: Boolean, t: Float) {
        val pulsation = 0.55f + 0.45f * (1f - cos(t * 2f * PI.toFloat() / 2.4f)) / 2f
        translate(x, y) {
            scale(if (miroir) -s else s, s, pivot = Offset.Zero) {
                val forme = polygone(FORME_ROCHER)
                drawPath(
                    forme,
                    Brush.verticalGradient(
                        0f to Color(0xFF4A3A3D),
                        0.6f to Color(0xFF2A1F22),
                        1f to Color(0xFF3A1A12),
                        startY = -88f,
                        endY = 40f,
                    ),
                )
                clipPath(forme) {
                    FACETTES.forEach { (points, couleur, opacite) ->
                        drawPath(polygone(points), couleur, alpha = opacite)
                    }
                    drawRect(
                        Brush.verticalGradient(
                            0f to Magma.copy(alpha = 0f),
                            0.7f to Magma.copy(alpha = 0.45f),
                            1f to LueurLave.copy(alpha = 0.9f),
                            startY = -20f,
                            endY = 40f,
                        ),
                        topLeft = Offset(-80f, -20f),
                        size = Size(160f, 60f),
                    )
                    drawCircle(Color(0xFF6B5557), 3f, Offset(-40f, -30f))
                    drawCircle(Color(0xFF120C0E), 2.5f, Offset(30f, -20f))
                    drawCircle(Color(0xFF7D6668), 2f, Offset(-10f, -66f))
                }
                drawPath(
                    ligne(FORME_ROCHER),
                    Color(0xFF0D090A),
                    style = Stroke(3f, join = StrokeJoin.Round),
                )
                val fissure = ligne(FISSURE)
                val rond = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(fissure, Magma, alpha = 0.35f * pulsation, style = Stroke(11f, cap = StrokeCap.Round))
                drawPath(fissure, Magma, alpha = 0.6f * pulsation, style = rond)
                drawPath(
                    fissure,
                    LueurLave,
                    alpha = pulsation,
                    style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawLine(LueurLave, Offset(-12f, -4f), Offset(8f, -8f), 2f, StrokeCap.Round, alpha = pulsation)
                drawPath(
                    ligne(FISSURE_2),
                    Orange,
                    alpha = pulsation,
                    style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }

    companion object {
        private val FORME_ROCHER = listOf(
            -74f to 40f, -70f to -18f, -56f to -56f, -26f to -82f, 6f to -88f,
            36f to -74f, 60f to -44f, 72f to -8f, 76f to 40f,
        )
        private val FISSURE = listOf(-30f to 40f, -26f to 14f, -12f to -4f, -18f to -26f, -4f to -46f, 2f to -60f)
        private val FISSURE_2 = listOf(44f to 40f, 40f to 18f, 50f to -2f, 46f to -18f)
        private val FACETTES = listOf(
            Triple(
                listOf(-56f to -56f, -26f to -82f, 6f to -88f, 36f to -74f, 12f to -58f, -22f to -52f),
                Color(0xFF5A4648),
                1f,
            ),
            Triple(listOf(-26f to -82f, 6f to -88f, 12f to -58f, -22f to -52f), Color(0xFF6B5557), 1f),
            Triple(
                listOf(36f to -74f, 60f to -44f, 72f to -8f, 76f to 40f, 28f to 40f, 22f to -14f, 12f to -58f),
                Color(0xFF1A1214),
                0.75f,
            ),
            Triple(
                listOf(-70f to -18f, -56f to -56f, -22f to -52f, -34f to -10f, -40f to 40f, -74f to 40f),
                Color(0xFF2E2326),
                0.8f,
            ),
            Triple(listOf(-34f to -10f, -22f to -52f, 12f to -58f, 22f to -14f, -4f to 2f), Color(0xFF3A2D30), 0.6f),
        )
        private val CROUTES: List<PlaqueCroute> = Random(5).let { r ->
            (10 until 1_170 step 70).map { x ->
                PlaqueCroute(
                    x.toFloat(),
                    r.nextInt(800, 837).toFloat(),
                    r.nextInt(14, 31).toFloat(),
                    r.nextInt(3, 7).toFloat(),
                )
            }
        }

        private fun polygone(points: List<Pair<Float, Float>>) = ligne(points).apply { close() }

        private fun ligne(points: List<Pair<Float, Float>>) = Path().apply {
            moveTo(points.first().first, points.first().second)
            points.drop(1).forEach { (x, y) -> lineTo(x, y) }
        }

        fun nouveau(): DecorLave {
            val r = Random(5)
            fun Random.entre(a: Float, b: Float) = a + nextFloat() * (b - a)
            fun braises(n: Int, y0: Float, y1: Float, couleur: Color, r0: Float, r1: Float, floue: Boolean = false) =
                List(n) {
                    Braise(
                        origine = Offset(r.entre(0f, LARGEUR), r.entre(y0, y1)),
                        rayon = r.entre(r0, r1),
                        couleur = couleur,
                        duree = r.entre(4f, 9f),
                        phase = r.nextFloat(),
                        dx = r.entre(-40f, 40f),
                        floue = floue,
                    )
                }
            val toutesLesBraises = braises(90, 300f, 900f, LueurLave, 1f, 3f) +
                braises(30, 400f, 900f, Color(0xFFFFF1A8), 1.5f, 4f) +
                braises(20, 400f, 900f, Magma, 4f, 8f, floue = true)
            fun bulles(n: Int, x0: Float, x1: Float, r0: Float, r1: Float) = List(n) {
                Bulle(r.entre(x0, x1), r.entre(822f, 840f), r.entre(r0, r1), r.entre(1.8f, 3.6f), r.nextFloat())
            }
            val toutesLesBulles =
                bulles(6, 150f, 250f, 4f, 8f) + bulles(3, 0f, 30f, 4f, 7f) + bulles(3, 360f, 390f, 4f, 7f)
            val sources = listOf(80f to 692f, 330f to 678f, 201f to 775f)
            val bouffees = sources.flatMap { (x, y) ->
                List(4) { k ->
                    val duree = r.entre(3.2f, 5f)
                    Bouffee(x + r.entre(-6f, 6f), y, r.entre(10f, 16f), duree, k / 4f + r.entre(0f, 0.1f))
                }
            }
            return DecorLave(toutesLesBraises, toutesLesBulles, bouffees)
        }
    }
}

@ApercusClairSombre
@Composable
private fun AnimationFinAventurePreview() {
    OmegadexTheme {
        ContenuFinAventure(t = INSTANT_FIGE, entree = 1f)
    }
}
