// Instants et coordonnées recopiés de la maquette jouable « Fusion 6 · Constellation Ω sur l'autel » : ce sont
// des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.rememberSprite
import com.omegadex.app.ui.components.spriteParDefaut
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import com.omegadex.app.ui.theme.Rubis
import com.omegadex.app.ui.theme.TexteSurRoche
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Chronologie (ms) depuis l'ouverture du voile, comme la maquette. */
private object ChronoEvolution {
    const val OUVERTURE_DUREE = 700f
    const val INTRO_DUREE = 500f
    const val ENROULE = 1_200f
    const val ENROULE_DUREE = 900f
    const val COCON = 2_100f
    const val COCON_APPARITION = 1_000f
    const val FISSURE = 4_100f
    const val FISSURE_DUREE = 500f
    const val ECLATE = 4_900f
    const val REVELE = 5_300f
    const val ECLORE_DUREE = 900f
    const val TEXTES_DUREE = 500f
    const val TRANSITION_SOL = 800f
    const val TRANSITION_OMBRE = 500f
    const val TRANSITION_GRAVURE = 600f
    const val TRANSITION_BRAISES = 800f
    const val ETOILE_DECALAGE = 55f
    const val ETOILE_ALLUMAGE = 600f
    const val ETOILE_DISPERSION = 900f
    const val ETOILE_RETOUR = 1_200f
    const val ONDE_PERIODE = 1_400f
    const val BRAISE_ATTISEE = 1_800f
    const val FIN = REVELE + 2_900f
}

/** Mesures de la maquette (écran de 390 × 844) : la scène de 240 dp porte l'autel, le sprite et le cocon. */
private object Scene {
    val COTE = 240.dp
    val HAUT = 188.dp
    val HAUT_MIN = 120.dp
    const val PART_HAUT = 0.223f
    val ECART_ENTETE = 100.dp
    val ECART_TEXTES = 64.dp

    /** Haut de la scène dans la maquette (dp sur l'écran de 390 × 844). */
    const val HAUT_MAQUETTE = 188f
}

private val EASE_OUVERTURE = CubicBezierEasing(0.6f, 0f, 0.2f, 1f)
private val EASE_ECLOSION = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val EASE_DISPERSION = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val EASE_ENTREE = CubicBezierEasing(0.42f, 0f, 1f, 1f)

private val RubisSombre = Color(0xFF2A0A12)
private val RubisNuit = Color(0xFF4A0A17)
private val RubisAutel = Color(0xFF6A0D20)
private val RubisCoeur = Color(0xFF5A0B1C)
private val RubisVoile = Color(0xFF2E0A13)
private val Creme = Color(0xFFFFF1E4)
private val CremeVive = Color(0xFFFFF8EF)
private val Abricot = Color(0xFFFFD29A)
private val Orange = Color(0xFFFF8A3C)
private val OmbreContact = Color(0xFF0A0405)

private fun avancement(t: Float, debut: Float, duree: Float, easing: Easing = LinearOutSlowInEasing): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Va-et-vient entre 0 et 1 sur [periode] ms, comme une animation CSS `ease-in-out infinite` aller-retour. */
private fun vaEtVient(t: Float, periode: Float) = (1f - cos(2f * PI.toFloat() * t / periode)) / 2f

/** Une espèce dans l'animation : son numéro national (pour l'image), son nom, ses types (« eau », « sol »…). */
data class EspeceAnimee(val id: Int, val nom: String, val types: List<String>)

/** Textes de l'animation, déjà traduits. */
data class TextesEvolution(
    val intro: String,
    val surtitre: String,
    val titre: String,
    val detail: String?,
    val continuer: String,
    val nomsTypes: List<String>,
)

/** Braise qui monte : abscisse, taille (dp), durée et décalage du cycle (ms), dérive horizontale (dp). */
private class Braise(val x: Float, val taille: Float, val duree: Float, val decalage: Float, val derive: Float)

/** Tirage pseudo-aléatoire stable de la maquette : les braises partent toujours des mêmes endroits. */
private class Graine(private var valeur: Long) {
    fun suivant(): Float {
        valeur = (valeur * 9_301 + 49_297) % 233_280
        return valeur / 233_280f
    }
}

/** Braises du bas de l'écran ; [Braise.x] est une part de la largeur. */
private val BRAISES_ECRAN: List<Braise> = Graine(23).let { g ->
    List(16) {
        val taille = 2f + Math.round(g.suivant() * 3f)
        Braise(g.suivant(), taille, 6_000f + g.suivant() * 5_000f, g.suivant() * 9_000f, (g.suivant() - 0.5f) * 100f)
    }
}

/** Braises qui s'échappent du rebord de l'autel ; [Braise.x] est en dp dans la scène. */
private val BRAISES_AUTEL: List<Braise> = Graine(29).let { g ->
    List(14) {
        val taille = 2f + Math.round(g.suivant() * 3f)
        val x = -24f + g.suivant() * 288f
        Braise(x, taille, 3_500f + g.suivant() * 3_000f, g.suivant() * 6_000f, (g.suivant() - 0.5f) * 90f)
    }
}

/** Tracé Ω (repère 24 × 24), échantillonné pour pouvoir l'étirer et le pointiller. */
private val TRACE_OMEGA: List<Offset> = buildList {
    val cx = 12f
    val cy = 10.526f
    val r = 7.5f
    val debut = atan2(17.4f - cy, 9f - cx)
    val balayage = 2f * PI.toFloat() - (debut - atan2(17.4f - cy, 15f - cx))
    add(Offset(4f, 20f))
    add(Offset(9f, 20f))
    add(Offset(9f, 17.4f))
    for (i in 1..48) {
        val a = debut + balayage * i / 48f
        add(Offset(cx + r * cos(a), cy + r * sin(a)))
    }
    add(Offset(15f, 20f))
    add(Offset(20f, 20f))
}

/** Les 30 braises de la constellation, réparties sur le Ω (repère 24 × 24). */
private val ETOILES_OMEGA: List<Offset> = buildList {
    val cx = 12f
    val cy = 10.526f
    val r = 7.5f
    val debut = atan2(17.4f - cy, 9f - cx)
    val balayage = 2f * PI.toFloat() - (debut - atan2(17.4f - cy, 15f - cx))
    listOf(4f, 5.7f, 7.4f).forEach { add(Offset(it, 20f)) }
    add(Offset(9f, 18.7f))
    for (i in 0..21) {
        val a = debut + balayage * i / 21f
        add(Offset(cx + r * cos(a), cy + r * sin(a)))
    }
    add(Offset(15f, 18.7f))
    listOf(16.6f, 18.3f, 20f).forEach { add(Offset(it, 20f)) }
}

/** Le grand Ω du fond : 340 dp de côté, posé à (25, 100) dans la maquette, pieds sur le bord de l'autel. */
private const val OMEGA_COTE = 340f
private const val OMEGA_GAUCHE = 25f
private const val OMEGA_HAUT = 100f

/** Fissures du cocon, dans le repère de la scène (dp), avec leur décalage (ms). */
private val FISSURES = listOf(
    0f to listOf(120f to 30f, 112f to 70f, 128f to 96f, 114f to 130f, 126f to 168f),
    150f to listOf(128f to 96f, 160f to 110f, 168f to 140f),
    250f to listOf(114f to 130f, 84f to 146f, 78f to 180f),
)

/**
 * Évolution plein écran qui prend le contrôle de l'app : ouverture circulaire, puis le Pokémon devient une
 * silhouette de lumière, s'enferme dans un cocon qui flotte au-dessus d'un autel en rubis pendant qu'une
 * constellation de braises dessine le Ω, le cocon se fissure et éclate, et la nouvelle espèce apparaît sur
 * l'autel. Avec les animations réduites, la révélation s'affiche directement.
 * [onFin] est appelé par « Continuer ».
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
    var ecoule by remember { mutableFloatStateOf(if (reduites) ChronoEvolution.FIN else 0f) }
    LaunchedEffect(avant, apres, reduites) {
        if (reduites) return@LaunchedEffect
        // Horloge continue : la chronologie s'arrête à FIN, les braises et les anneaux continuent de vivre.
        val debut = withFrameMillis { it }
        while (true) {
            withFrameMillis { ecoule = (it - debut).toFloat() }
        }
    }
    ContenuEvolution(avant, apres, textes, ecoule, !reduites, onFin, modifier)
}

@Composable
private fun ContenuEvolution(
    avant: EspeceAnimee,
    apres: EspeceAnimee,
    textes: TextesEvolution,
    ecoule: Float,
    anime: Boolean,
    onFin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = min(ecoule, ChronoEvolution.FIN)
    val ouverture = avancement(t, 0f, ChronoEvolution.OUVERTURE_DUREE, EASE_OUVERTURE)
    BoxWithConstraints(
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
        val hautScene = (maxHeight * Scene.PART_HAUT).coerceIn(Scene.HAUT_MIN, Scene.HAUT)
        Canvas(Modifier.fillMaxSize()) {
            val origine = Offset(size.width / 2f - 195.dp.toPx(), hautScene.toPx() - Scene.HAUT_MAQUETTE.dp.toPx())
            dessinerFond(origine)
            dessinerConstellation(t, origine)
            dessinerSol(origine)
            if (anime) dessinerBraisesEcran(t, ecoule)
        }
        if (t < ChronoEvolution.ECLATE) {
            Text(
                textes.intro,
                style = MaterialTheme.typography.headlineSmall,
                color = Blanc,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = (hautScene - Scene.ECART_ENTETE).coerceAtLeast(24.dp), start = 24.dp, end = 24.dp)
                    .montee(t, 0f, ChronoEvolution.INTRO_DUREE),
            )
        }
        SceneEvolution(avant, apres, t, ecoule, anime, Modifier.align(Alignment.TopCenter).padding(top = hautScene))
        if (t >= ChronoEvolution.REVELE) {
            Revelation(apres, textes, t, onFin, hautScene + Scene.COTE + Scene.ECART_TEXTES)
        }
    }
}

/** L'autel, l'ombre éclairée, le sprite et le cocon, dans le carré de 240 dp de la maquette. */
@Composable
private fun SceneEvolution(
    avant: EspeceAnimee,
    apres: EspeceAnimee,
    t: Float,
    ecoule: Float,
    anime: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(Scene.COTE)) {
        Canvas(Modifier.fillMaxSize()) {
            dessinerAutel(t)
            dessinerOmbreEclairee(t, ecoule, anime)
            if (anime) dessinerBraisesAutel(t, ecoule)
        }
        if (t < ChronoEvolution.COCON) SpriteAvant(avant, t)
        if (t >= ChronoEvolution.REVELE) SpriteApres(apres, t)
        Canvas(Modifier.fillMaxSize()) {
            dessinerCocon(t, ecoule, anime)
            dessinerEclair(t)
        }
    }
}

/** L'ancienne espèce, qui devient une silhouette de lumière et se resserre avant le cocon. */
@Composable
private fun BoxScope.SpriteAvant(avant: EspeceAnimee, t: Float) {
    val e = avancement(t, ChronoEvolution.ENROULE, ChronoEvolution.ENROULE_DUREE, EASE_ENTREE)
    SpriteScene(rememberSprite(avant.id) ?: spriteParDefaut, avant.nom, lerp(1f, 0.82f, e), 1f, e)
}

/** La nouvelle espèce, qui éclot d'un éclat de lumière puis se pose sur l'autel. */
@Composable
private fun BoxScope.SpriteApres(apres: EspeceAnimee, t: Float) {
    val q = ((t - ChronoEvolution.REVELE) / ChronoEvolution.ECLORE_DUREE).coerceIn(0f, 1f)
    val echelle: Float
    val opacite: Float
    val lumiere: Float
    if (q < 0.6f) {
        val f = EASE_ECLOSION.transform(q / 0.6f)
        echelle = lerp(0.6f, 1.12f, f)
        opacite = f
        lumiere = lerp(0.8f, 0.3f, f)
    } else {
        val f = EASE_ECLOSION.transform((q - 0.6f) / 0.4f)
        echelle = lerp(1.12f, 1f, f)
        opacite = 1f
        lumiere = lerp(0.3f, 0f, f)
    }
    SpriteScene(rememberSprite(apres.id) ?: spriteParDefaut, apres.nom, echelle, opacite, lumiere)
}

/** Sprite de 200 dp au centre de la scène, en pixels nets ; [lumiere] le recouvre de blanc (0 à 1). */
@Composable
private fun BoxScope.SpriteScene(image: ImageBitmap, nom: String, echelle: Float, opacite: Float, lumiere: Float) {
    val couche = Modifier
        .align(Alignment.TopStart)
        .offset(20.dp, 20.dp)
        .size(200.dp)
        .graphicsLayer {
            scaleX = echelle
            scaleY = echelle
            alpha = opacite
        }
    Image(
        bitmap = image,
        contentDescription = nom,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = couche,
    )
    if (lumiere > 0f) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            colorFilter = ColorFilter.tint(Blanc, BlendMode.SrcIn),
            modifier = couche.graphicsLayer { alpha = lumiere },
        )
    }
}

@Composable
private fun Revelation(apres: EspeceAnimee, textes: TextesEvolution, t: Float, onFin: () -> Unit, haut: Dp) {
    val debut = ChronoEvolution.REVELE
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = haut, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            textes.surtitre.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = LueurMagma,
            modifier = Modifier.montee(t, debut, ChronoEvolution.TEXTES_DUREE),
        )
        Text(
            textes.titre,
            style = MaterialTheme.typography.headlineMedium,
            color = Blanc,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).montee(t, debut + 200f, ChronoEvolution.TEXTES_DUREE),
        )
        Column(
            Modifier.padding(top = 14.dp).montee(t, debut + 400f, ChronoEvolution.TEXTES_DUREE),
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

/* ---------- Décor plein écran ---------- */

/** Point de la maquette (x, y en dp sur l'écran de 390) ramené à l'écran réel. */
private fun DrawScope.maquette(origine: Offset, x: Float, y: Float) =
    Offset(origine.x + x.dp.toPx(), origine.y + y.dp.toPx())

private fun DrawScope.dessinerFond(origine: Offset) {
    drawRect(
        Brush.radialGradient(
            0f to RubisCoeur,
            0.38f to RubisVoile,
            0.72f to Roche,
            center = maquette(origine, 195f, 270f),
            radius = 606.dp.toPx(),
        ),
    )
    // Ω en pointillés, tracé de la constellation.
    val k = (OMEGA_COTE / 24f).dp.toPx()
    val coin = maquette(origine, OMEGA_GAUCHE, OMEGA_HAUT)
    drawPath(
        cheminOmega { Offset(coin.x + it.x * k, coin.y + it.y * k) },
        Rubis.copy(alpha = 0.35f),
        style = Stroke(
            0.3f * k,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.4f * k, 0.8f * k)),
        ),
    )
}

/** Sol sombre au pied de l'autel. */
private fun DrawScope.dessinerSol(origine: Offset) {
    val haut = maquette(origine, 0f, 470f).y
    drawRect(
        Brush.verticalGradient(0f to RubisSombre, 0.4f to Roche, startY = haut, endY = size.height),
        topLeft = Offset(0f, haut),
        size = Size(size.width, size.height - haut),
    )
}

private fun cheminOmega(placer: (Offset) -> Offset) = Path().apply {
    TRACE_OMEGA.forEachIndexed { i, point ->
        val p = placer(point)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
}

/** État d'une braise de la constellation : décalage, échelle, opacité, couleur et halo (0 à 1). */
private class EtatEtoile(val depl: Offset, val echelle: Float, val opacite: Float, val couleur: Color, val halo: Float)

private fun etatEtoile(t: Float, i: Int, direction: Offset): EtatEtoile {
    val d = i * ChronoEvolution.ETOILE_DECALAGE
    return when {
        t < ChronoEvolution.COCON -> EtatEtoile(Offset.Zero, 1f, 0.18f, Magma, 0f)
        t < ChronoEvolution.ECLATE -> {
            val q = ((t - ChronoEvolution.COCON - d) / ChronoEvolution.ETOILE_ALLUMAGE).coerceIn(0f, 1f)
            if (q < 0.5f) {
                val f = LinearOutSlowInEasing.transform(q / 0.5f)
                EtatEtoile(Offset.Zero, lerp(0.6f, 1.8f, f), lerp(0.18f, 1f, f), lerp(Magma, Creme, f), f)
            } else {
                val f = LinearOutSlowInEasing.transform((q - 0.5f) / 0.5f)
                EtatEtoile(Offset.Zero, lerp(1.8f, 1.1f, f), 1f, lerp(Creme, LueurMagma, f), lerp(1f, 0.6f, f))
            }
        }
        t < ChronoEvolution.REVELE -> {
            val p = avancement(t, ChronoEvolution.ECLATE, ChronoEvolution.ETOILE_DISPERSION, EASE_DISPERSION)
            EtatEtoile(direction * p, lerp(1.2f, 0.4f, p), 1f - p, lerp(Creme, Magma, p), 0f)
        }
        else -> {
            val p = avancement(t, ChronoEvolution.REVELE + d, ChronoEvolution.ETOILE_RETOUR, FastOutSlowInEasing)
            EtatEtoile(Offset.Zero, 1f, 0.55f * p, LueurMagma, 0.6f)
        }
    }
}

/** Les braises du Ω : éteintes, puis allumées une à une pendant le cocon, dispersées par l'éclat, revenues. */
private fun DrawScope.dessinerConstellation(t: Float, origine: Offset) {
    val k = OMEGA_COTE / 24f
    val centre = maquette(origine, 195f, 270f)
    ETOILES_OMEGA.forEachIndexed { i, point ->
        val base = maquette(origine, OMEGA_GAUCHE + point.x * k, OMEGA_HAUT + point.y * k)
        val vers = base - centre
        val longueur = max(1f, vers.getDistance())
        val etat = etatEtoile(t, i, vers / longueur * 160.dp.toPx())
        if (etat.opacite <= 0f) return@forEachIndexed
        val c = base + etat.depl
        val rayon = 3.5.dp.toPx() * etat.echelle
        if (etat.halo > 0f) {
            val r = rayon + 10.dp.toPx()
            drawCircle(
                Brush.radialGradient(
                    0f to etat.couleur.copy(alpha = 0.9f * etat.halo * etat.opacite),
                    1f to etat.couleur.copy(alpha = 0f),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
        drawCircle(etat.couleur.copy(alpha = etat.opacite), radius = rayon, center = c)
    }
}

/** Opacité d'une braise du bas de l'écran sur son cycle : apparition, montée, extinction en haut. */
private fun opaciteBraiseEcran(q: Float) = when {
    q < 0.1f -> q / 0.1f
    q < 0.8f -> lerp(1f, 0.8f, (q - 0.1f) / 0.7f)
    else -> lerp(0.8f, 0f, (q - 0.8f) / 0.2f)
}

/** Opacité d'une braise de l'autel : apparition, puis extinction régulière en montant. */
private fun opaciteBraiseAutel(q: Float) = if (q < 0.1f) q / 0.1f else lerp(1f, 0f, (q - 0.1f) / 0.9f)

private fun DrawScope.dessinerBraise(centre: Offset, taille: Float, opacite: Float, vive: Float) {
    val r = taille.dp.toPx() / 2f
    val halo = r + 5.dp.toPx()
    drawCircle(
        Brush.radialGradient(
            0f to Orange.copy(alpha = 0.7f * opacite),
            1f to Orange.copy(alpha = 0f),
            center = centre,
            radius = halo,
        ),
        radius = halo,
        center = centre,
    )
    drawCircle(lerp(LueurMagma, Abricot, vive).copy(alpha = opacite), radius = r, center = centre)
}

/** Braises qui montent du bas de l'écran ; plus discrètes pendant le cocon. */
private fun DrawScope.dessinerBraisesEcran(t: Float, ecoule: Float) {
    val discret = when {
        t < ChronoEvolution.COCON -> 0f
        t < ChronoEvolution.REVELE -> avancement(t, ChronoEvolution.COCON, ChronoEvolution.TRANSITION_BRAISES)
        else -> 1f - avancement(t, ChronoEvolution.REVELE, ChronoEvolution.TRANSITION_BRAISES)
    }
    val attenuation = lerp(1f, 0.45f, discret)
    BRAISES_ECRAN.forEach { b ->
        val q = ((ecoule + b.decalage) % b.duree) / b.duree
        val centre = Offset(
            size.width * b.x + (b.derive * q).dp.toPx(),
            size.height + (12f - b.taille / 2f - 900f * q).dp.toPx(),
        )
        dessinerBraise(centre, b.taille, opaciteBraiseEcran(q) * attenuation, 0f)
    }
}

/* ---------- Scène : autel, ombre éclairée, cocon ---------- */

private fun DrawScope.rect(x: Float, y: Float, l: Float, h: Float) =
    Rect(Offset(x.dp.toPx(), y.dp.toPx()), Size(l.dp.toPx(), h.dp.toPx()))

/**
 * Ovale rempli d'un dégradé radial elliptique (CSS `radial-gradient(ellipse at …)`, rayon au coin le plus
 * éloigné) : un cercle écrasé verticalement. [centreY] est la position verticale du centre, en part de l'ovale.
 */
private fun DrawScope.ovaleDegrade(zone: Rect, vararg arrets: Pair<Float, Color>, centreY: Float = 0.5f) {
    val ratio = zone.height / zone.width
    val centre = zone.center
    scale(1f, ratio, pivot = centre) {
        val foyer = Offset(centre.x, centre.y + (centreY - 0.5f) * zone.width)
        drawCircle(
            Brush.radialGradient(*arrets, center = foyer, radius = zone.width / 2f * 1.4142f),
            radius = zone.width / 2f,
            center = centre,
        )
    }
}

/** Ovale mis à l'échelle autour de son centre. */
private fun Rect.agrandi(sx: Float, sy: Float): Rect {
    val l = width * sx
    val h = height * sy
    return Rect(Offset(center.x - l / 2f, center.y - h / 2f), Size(l, h))
}

/** Autel en rubis avec son Ω gravé, qui s'illumine pendant le cocon. */
private fun DrawScope.dessinerAutel(t: Float) {
    val dessus = rect(-30f, 198f, 300f, 64f)
    ovaleDegrade(
        rect(-60f, 178f, 360f, 124f).translate(0f, 10.dp.toPx()),
        0f to Color.Black.copy(alpha = 0.5f),
        0.7f to Color.Black.copy(alpha = 0f),
    )
    drawOval(
        Brush.verticalGradient(listOf(RubisCoeur, RubisSombre), startY = 222.dp.toPx(), endY = 286.dp.toPx()),
        topLeft = rect(-30f, 222f, 300f, 64f).topLeft,
        size = dessus.size,
    )
    ovaleDegrade(dessus, 0f to Rubis, 0.6f to RubisAutel, 1f to RubisNuit, centreY = 0.4f)
    val trait = 2.dp.toPx()
    drawOval(
        LueurMagma.copy(alpha = 0.55f),
        topLeft = dessus.topLeft + Offset(trait / 2f, trait / 2f),
        size = Size(dessus.width - trait, dessus.height - trait),
        style = Stroke(trait),
    )
    dessinerGravure(t)
}

private fun DrawScope.dessinerGravure(t: Float) {
    // Ω gravé : translate(150 32) scale(4.4 1.25) translate(-12 -12) dans le dessus de l'autel.
    val gravure = cheminOmega {
        Offset((120f + (it.x - 12f) * 4.4f).dp.toPx(), (230f + (it.y - 12f) * 1.25f).dp.toPx())
    }
    val allumage = when {
        t < ChronoEvolution.COCON -> 0f
        else -> avancement(t, ChronoEvolution.COCON, ChronoEvolution.TRANSITION_GRAVURE, FastOutSlowInEasing)
    }
    val final = avancement(t, ChronoEvolution.REVELE, ChronoEvolution.TRANSITION_GRAVURE, FastOutSlowInEasing)
    val couleur = lerp(lerp(RubisSombre.copy(alpha = 0.9f), Abricot, allumage), LueurMagma.copy(alpha = 0.8f), final)
    val lueur = allumage * (1f - final)
    if (lueur > 0f) {
        drawPath(
            gravure,
            Magma.copy(alpha = 0.55f * lueur),
            style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    drawPath(gravure, couleur, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Taille et intensité de la flaque de lumière : faible au repos, battante sous le cocon, éclatante à l'éclat. */
private class EtatSol(val opacite: Float, val sx: Float, val sy: Float) {
    fun vers(autre: EtatSol, f: Float) =
        EtatSol(lerp(opacite, autre.opacite, f), lerp(sx, autre.sx, f), lerp(sy, autre.sy, f))
}

private val SOL_REPOS = EtatSol(0.35f, 0.6f, 0.7f)
private val SOL_EVEIL = EtatSol(0.7f, 0.85f, 0.85f)
private val SOL_COCON = EtatSol(1f, 1.08f, 1.08f)
private val SOL_ECLAT = EtatSol(1f, 1.9f, 1.4f)
private val ETAPES_SOL = listOf(
    ChronoEvolution.ENROULE to SOL_EVEIL,
    ChronoEvolution.COCON to SOL_COCON,
    ChronoEvolution.ECLATE to SOL_ECLAT,
    ChronoEvolution.REVELE to SOL_EVEIL,
)

/** L'ombre éclairée au pied du sprite, les ondes de chaleur et l'ombre de contact. */
private fun DrawScope.dessinerOmbreEclairee(t: Float, ecoule: Float, anime: Boolean) {
    var sol = SOL_REPOS
    ETAPES_SOL.forEach { (debut, cible) ->
        if (t >= debut) {
            sol = sol.vers(cible, avancement(t, debut, ChronoEvolution.TRANSITION_SOL, FastOutSlowInEasing))
        }
    }
    val flaque = rect(-10f, 196f, 260f, 70f).agrandi(sol.sx, sol.sy)
    val arrets = arrayOf(
        0f to Creme.copy(alpha = 0.95f * sol.opacite),
        0.2f to LueurMagma.copy(alpha = 0.7f * sol.opacite),
        0.46f to Magma.copy(alpha = 0.35f * sol.opacite),
        0.72f to Magma.copy(alpha = 0f),
    )
    ovaleDegrade(flaque, *arrets)
    // Sous le cocon la flaque bat (brightness 1 → 1,4) ; à l'éclat elle flambe (brightness 1,5).
    val surcroit = when {
        t >= ChronoEvolution.ECLATE && t < ChronoEvolution.REVELE -> 0.5f
        anime && t >= ChronoEvolution.COCON && t < ChronoEvolution.ECLATE -> 0.4f * vaEtVient(ecoule, 500f)
        else -> 0f
    }
    if (surcroit > 0f) {
        ovaleDegrade(flaque, *arrets.map { (p, c) -> p to c.copy(alpha = c.alpha * surcroit) }.toTypedArray())
    }
    if (anime && t >= ChronoEvolution.COCON && t < ChronoEvolution.ECLATE) dessinerOndes(t)
    val ombre = when {
        t < ChronoEvolution.COCON -> 1f
        t < ChronoEvolution.REVELE -> 1f - avancement(t, ChronoEvolution.COCON, ChronoEvolution.TRANSITION_OMBRE)
        else -> avancement(t, ChronoEvolution.REVELE, ChronoEvolution.TRANSITION_OMBRE)
    }
    if (ombre > 0f) {
        ovaleDegrade(
            rect(66f, 212f, 108f, 18f),
            0f to OmbreContact.copy(alpha = 0.8f * ombre),
            0.7f to OmbreContact.copy(alpha = 0f),
        )
    }
}

/** Trois ondes de chaleur qui partent de l'autel pendant le cocon. */
private fun DrawScope.dessinerOndes(t: Float) {
    val base = rect(-20f, 200f, 280f, 60f)
    listOf(0f, 450f, 900f).forEach { decalage ->
        val local = t - ChronoEvolution.COCON - decalage
        if (local < 0f) return@forEach
        val q = LinearOutSlowInEasing.transform((local % ChronoEvolution.ONDE_PERIODE) / ChronoEvolution.ONDE_PERIODE)
        val e = lerp(0.3f, 1.25f, q)
        val zone = base.agrandi(e, e)
        drawOval(LueurMagma.copy(alpha = lerp(0.9f, 0f, q)), zone.topLeft, zone.size, style = Stroke(2.dp.toPx()))
    }
}

/** Braises qui s'échappent du rebord de l'autel ; attisées (plus rapides et plus vives) pendant le cocon. */
private fun DrawScope.dessinerBraisesAutel(t: Float, ecoule: Float) {
    val attise = t >= ChronoEvolution.COCON && t < ChronoEvolution.REVELE
    val dansCocon = (min(ecoule, ChronoEvolution.REVELE) - ChronoEvolution.COCON).coerceAtLeast(0f)
    BRAISES_AUTEL.forEach { b ->
        // Pendant le cocon, chaque braise accomplit son cycle en 1,8 s ; le temps propre reste continu.
        val propre = ecoule + (b.duree / ChronoEvolution.BRAISE_ATTISEE - 1f) * dansCocon
        val q = ((propre + b.decalage) % b.duree) / b.duree
        val centre = Offset((b.x + b.derive * q + b.taille / 2f).dp.toPx(), (236f + b.taille / 2f - 560f * q).dp.toPx())
        dessinerBraise(centre, b.taille, opaciteBraiseAutel(q), if (attise) 1f else 0f)
    }
}

/** Le cocon de lumière : apparaît, flotte au-dessus de l'autel, bat, puis se fissure. */
private fun DrawScope.dessinerCocon(t: Float, ecoule: Float, anime: Boolean) {
    if (t < ChronoEvolution.COCON || t >= ChronoEvolution.ECLATE) return
    val local = t - ChronoEvolution.COCON
    val apparition = EASE_ECLOSION.transform((local / ChronoEvolution.COCON_APPARITION).coerceIn(0f, 1f))
    val opacite = (local / ChronoEvolution.COCON_APPARITION / 0.4f).coerceIn(0f, 1f)
    val flotte = if (anime) -10f - 8f * vaEtVient(ecoule - ChronoEvolution.COCON, 1_600f) else -10f
    val bat = if (anime) 1f + 0.06f * vaEtVient(ecoule, 500f) else 1f
    val centre = Offset(120.dp.toPx(), 120.dp.toPx())
    translate(top = flotte.dp.toPx()) {
        scale(lerp(0.2f, 1f, apparition), pivot = centre) {
            scale(bat, pivot = centre) {
                ovaleDegrade(
                    rect(14f, -16f, 212f, 272f),
                    0f to Orange.copy(alpha = 0.45f * opacite),
                    0.5f to Orange.copy(alpha = 0.3f * opacite),
                    0.7f to Orange.copy(alpha = 0f),
                )
                ovaleDegrade(
                    rect(44f, 14f, 152f, 212f),
                    0f to CremeVive.copy(alpha = opacite),
                    0.45f to Abricot.copy(alpha = opacite),
                    0.78f to Orange.copy(alpha = opacite),
                    1f to Magma.copy(alpha = 0f),
                    centreY = 0.42f,
                )
            }
            dessinerAnneaux(ecoule, anime, centre, opacite)
            if (t >= ChronoEvolution.FISSURE) dessinerFissures(t - ChronoEvolution.FISSURE)
        }
    }
}

private fun DrawScope.dessinerAnneaux(ecoule: Float, anime: Boolean, centre: Offset, opacite: Float) {
    rotate(if (anime) 360f * ecoule / 3_500f else 0f, centre) {
        val r = rect(16f, 48f, 208f, 144f)
        drawOval(
            LueurMagma.copy(alpha = opacite),
            r.topLeft,
            r.size,
            style = Stroke(
                2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 12.dp.toPx())),
            ),
        )
    }
    rotate(if (anime) -360f * ecoule / 5_000f else 0f, centre) {
        val r = rect(48f, 10f, 144f, 220f)
        drawOval(
            Magma.copy(alpha = 0.7f * opacite),
            r.topLeft,
            r.size,
            style = Stroke(
                1.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 9.dp.toPx())),
            ),
        )
    }
}

/** Les fissures se tracent l'une après l'autre sur le cocon. */
private fun DrawScope.dessinerFissures(local: Float) {
    FISSURES.forEach { (decalage, points) ->
        val p = avancement(local, decalage, ChronoEvolution.FISSURE_DUREE)
        if (p <= 0f) return@forEach
        val chemin = Path().apply {
            points.forEachIndexed { i, (x, y) ->
                if (i == 0) moveTo(x.dp.toPx(), y.dp.toPx()) else lineTo(x.dp.toPx(), y.dp.toPx())
            }
        }
        val mesure = PathMeasure().apply { setPath(chemin, false) }
        val trace = Path().also { mesure.getSegment(0f, mesure.length * p, it, true) }
        drawPath(trace, RubisCoeur, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** L'éclat : un disque de lumière qui grandit quand le cocon cède. */
private fun DrawScope.dessinerEclair(t: Float) {
    if (t < ChronoEvolution.ECLATE || t >= ChronoEvolution.REVELE) return
    val local = t - ChronoEvolution.ECLATE
    val e = lerp(0.2f, 1f, EASE_ECLOSION.transform((local / ChronoEvolution.COCON_APPARITION).coerceIn(0f, 1f)))
    val opacite = (local / ChronoEvolution.COCON_APPARITION / 0.4f).coerceIn(0f, 1f)
    val centre = Offset(120.dp.toPx(), 120.dp.toPx())
    val rayon = 254.dp.toPx() * e
    drawCircle(
        Brush.radialGradient(
            0f to Creme.copy(alpha = 0.95f * opacite),
            0.35f to LueurMagma.copy(alpha = 0.5f * opacite),
            0.7f to Magma.copy(alpha = 0f),
            center = centre,
            radius = rayon,
        ),
        radius = 180.dp.toPx() * e,
        center = centre,
    )
}

@ApercusClairSombre
@Composable
private fun AnimationEvolutionPreview() {
    OmegadexTheme {
        ContenuEvolution(
            EspeceAnimee(258, "Gobou", listOf("eau")),
            EspeceAnimee(259, "Flobio", listOf("eau", "sol")),
            TextesEvolution(
                "Hein ? Gobou évolue !",
                "Évolution réussie",
                "Gobou a évolué\nen Flobio !",
                "Défi réussi : « Évoluer avant Voltère »",
                "Continuer",
                listOf("Eau", "Sol"),
            ),
            ecoule = ChronoEvolution.FIN,
            anime = false,
            onFin = {},
        )
    }
}
