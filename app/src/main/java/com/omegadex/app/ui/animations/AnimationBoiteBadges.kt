// Instants recopiés de la maquette « 9 · Écrin qui s'ouvre + caméra » du canevas : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omegadex.app.R
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.ui.components.ArriveeBadge
import com.omegadex.app.ui.components.BoiteBadges
import com.omegadex.app.ui.components.ChronoEnvolBadge
import com.omegadex.app.ui.components.dessinerGemme
import com.omegadex.app.ui.components.nomBadge
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import com.omegadex.app.ui.theme.TexteEteint
import kotlinx.coroutines.delay

/**
 * Chronologie de la boîte à badges (ms) : l'écrin arrive fermé, le couvercle frappé du Ω s'ouvre, la caméra
 * avance vers la niche du nouveau badge, le badge y est forgé (voir [ChronoEnvolBadge]), puis la caméra recule
 * pour montrer toute la collection et le badge est annoncé.
 */
private object ChronoBoite {
    const val ARRIVEE_DUREE = 520f
    const val COUVERCLE = 600f
    const val COUVERCLE_DUREE = 900f
    const val LUEUR = 1_000f
    const val CAMERA_AVANT = 1_100f
    const val CAMERA_DUREE = 1_000f
    const val ENVOL = 2_100f
    const val COMPTEUR = 3_000f
    const val CAMERA_RETOUR = 3_400f
    const val CAMERA_RETOUR_DUREE = 700f
    const val ANNONCE = 4_000f
    const val ANNONCE_DUREE = 400f
    const val FIN = 4_400f
    const val ATTENTE_FIN_MS = 1_600L
    const val ATTENTE_REDUITE_MS = 2_500L
}

private val EASE_OUT: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EASE_CAMERA: Easing = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
private val EASE_REBOND: Easing = CubicBezierEasing(0.3f, 1.2f, 0.5f, 1f)
private val FORME_ECRIN = RoundedCornerShape(22.dp)
private val COUVERCLE_HAUT = Color(0xFF2E2428)
private val COUVERCLE_BAS = Color(0xFF1F181B)
private val COUVERCLE_TRAIT = Color(0xFF4E4247)
private const val ZOOM_CAMERA = 2f

private fun phase(t: Float, debut: Float, duree: Float, easing: Easing = EASE_OUT): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

/**
 * Boîte à badges plein écran, jouée après l'animation « Étape validée » d'une arène : [nouveau] rejoint les
 * badges [obtenus] (qui le contiennent déjà). Toucher l'écran passe à l'image finale, un second toucher ferme.
 * Avec les animations réduites, l'image finale s'affiche sans mouvement. Appelle [onFin] une seule fois.
 */
@Composable
fun AnimationBoiteBadges(
    obtenus: Set<BadgeArene>,
    nouveau: BadgeArene,
    onFin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduites = OmegadexTheme.animationsReduites
    val temps = remember { Animatable(0f) }
    var passe by remember { mutableStateOf(reduites) }
    var fini by remember { mutableStateOf(false) }
    val terminer = {
        if (!fini) {
            fini = true
            onFin()
        }
    }
    LaunchedEffect(nouveau) {
        if (!reduites) temps.animateTo(ChronoBoite.FIN, tween(ChronoBoite.FIN.toInt(), easing = LinearEasing))
        delay(if (reduites) ChronoBoite.ATTENTE_REDUITE_MS else ChronoBoite.ATTENTE_FIN_MS)
        terminer()
    }
    LaunchedEffect(passe) {
        if (passe && !reduites) {
            delay(ChronoBoite.ATTENTE_FIN_MS)
            terminer()
        }
    }
    val t = if (passe) ChronoBoite.FIN else temps.value
    ContenuBoiteBadges(
        obtenus = obtenus,
        nouveau = nouveau,
        t = t,
        onToucher = { if (t < ChronoBoite.FIN) passe = true else terminer() },
        modifier = modifier,
    )
}

@Composable
private fun ContenuBoiteBadges(
    obtenus: Set<BadgeArene>,
    nouveau: BadgeArene,
    t: Float,
    onToucher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visee = remember { ViseeCamera() }
    val nom = stringResource(nomBadge(nouveau))
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2A1F22), Roche, Color(0xFF0B0809))))
            .clickable(onClickLabel = stringResource(R.string.badges_passer), onClick = onToucher)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .onGloballyPositioned { visee.cadre = it },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val zoom = zoomCamera(t)
                    // Cible relevée caméra immobile (écrin posé), puis figée pendant tout le zoom.
                    if (zoom == 0f) visee.relever()
                    val origine = visee.cible
                    if (origine != null && zoom > 0f && size.width > 0f) {
                        transformOrigin = TransformOrigin(origine.x / size.width, origine.y / size.height)
                        translationX = (size.width / 2f - origine.x) * zoom
                        translationY = (size.height / 2f - origine.y) * zoom
                        scaleX = 1f + (ZOOM_CAMERA - 1f) * zoom
                        scaleY = scaleX
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Ecrin(obtenus, nouveau, t) { visee.niche = it }
        }
        TextesSequence(nom, t)
    }
}

/**
 * Point visé par la caméra : le centre de la niche du nouveau badge, dans le repère de la scène.
 * Il est recalculé à chaque image tant que la caméra est immobile (l'écrin bouge encore à son arrivée,
 * ce qui ne relance pas de mise en page), puis figé : relu pendant le zoom, il suivrait la caméra elle-même.
 */
private class ViseeCamera {
    var cadre: LayoutCoordinates? = null
    var niche: LayoutCoordinates? = null
    var cible: Offset? = null
        private set

    fun relever() {
        val scene = cadre ?: return
        val visee = niche ?: return
        if (scene.isAttached && visee.isAttached) cible = scene.localBoundingBoxOf(visee, clipBounds = false).center
    }
}

/** 0 → 1 quand la caméra avance vers la niche, 1 pendant la forge, puis retour à 0. */
private fun zoomCamera(t: Float): Float = phase(t, ChronoBoite.CAMERA_AVANT, ChronoBoite.CAMERA_DUREE, EASE_CAMERA) -
    phase(t, ChronoBoite.CAMERA_RETOUR, ChronoBoite.CAMERA_RETOUR_DUREE, EASE_CAMERA)

@Composable
private fun Ecrin(obtenus: Set<BadgeArene>, nouveau: BadgeArene, t: Float, onNiche: (LayoutCoordinates) -> Unit) {
    val arrivee = phase(t, 0f, ChronoBoite.ARRIVEE_DUREE, EASE_REBOND)
    Box(
        Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = phase(t, 0f, ChronoBoite.ARRIVEE_DUREE)
                translationY = 90.dp.toPx() * (1f - arrivee)
                scaleX = 0.85f + 0.15f * arrivee
                scaleY = scaleX
            },
    ) {
        BoiteBadges(
            obtenus = obtenus,
            arrivee = ArriveeBadge(nouveau, t - ChronoBoite.ENVOL),
            compteur = if (t < ChronoBoite.COMPTEUR) obtenus.size - 1 else obtenus.size,
            lueur = lueurInterieure(t),
            onNicheArrivee = onNiche,
        )
        Couvercle(phase(t, ChronoBoite.COUVERCLE, ChronoBoite.COUVERCLE_DUREE, LinearEasing))
    }
}

/** La braise monte du fond de l'écrin à son ouverture, puis reste en veilleuse. */
private fun lueurInterieure(t: Float): Float {
    val p = ((t - ChronoBoite.LUEUR) / 2_600f).coerceIn(0f, 1f)
    return when {
        p <= 0f -> 0f
        p < 0.3f -> p / 0.3f
        else -> 1f - 0.65f * EASE_OUT.transform((p - 0.3f) / 0.7f)
    }
}

/** Couvercle frappé du Ω et fermé d'un rubis : il se soulève puis bascule vers l'arrière ([p] de 0 à 1). */
@Composable
private fun BoxScope.Couvercle(p: Float) {
    if (p >= 1f) return
    val couleurs = OmegadexTheme.couleurs
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                rotationX = when {
                    p < 0.15f -> 6f * EASE_OUT.transform(p / 0.15f)
                    p < 0.85f -> 6f - 118f * EASE_CAMERA.transform((p - 0.15f) / 0.7f)
                    else -> -112f
                }
                alpha = if (p < 0.85f) 1f else 1f - (p - 0.85f) / 0.15f
                transformOrigin = TransformOrigin(0.5f, 0f)
                cameraDistance = 12f * density
            }
            .background(Brush.verticalGradient(listOf(COUVERCLE_HAUT, COUVERCLE_BAS)), FORME_ECRIN)
            .border(1.dp, COUVERCLE_TRAIT, FORME_ECRIN),
        contentAlignment = Alignment.Center,
    ) {
        Text("Ω", color = COUVERCLE_TRAIT, fontSize = 84.sp, fontWeight = FontWeight.ExtraBold)
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 11.dp)
                .size(width = 26.dp, height = 24.dp),
        ) { dessinerGemme(allumee = true, couleurs = couleurs) }
    }
}

@Composable
private fun BoxScope.TextesSequence(nom: String, t: Float) {
    Text(
        stringResource(R.string.badges_passer),
        color = TexteEteint,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .graphicsLayer { alpha = 1f - phase(t, ChronoBoite.ANNONCE - 200f, 300f) },
    )
    Text(
        stringResource(R.string.badges_annonce, nom),
        color = LueurMagma,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 96.dp)
            .graphicsLayer {
                val p = phase(t, ChronoBoite.ANNONCE, ChronoBoite.ANNONCE_DUREE)
                alpha = p
                translationY = 6.dp.toPx() * (1f - p)
            },
    )
}

@ApercusClairSombre
@Composable
private fun AnimationBoiteBadgesPreview() {
    OmegadexTheme {
        ContenuBoiteBadges(
            obtenus = setOf(BadgeArene.ROCHE, BadgeArene.POING, BadgeArene.DYNAMO),
            nouveau = BadgeArene.DYNAMO,
            t = ChronoBoite.FIN,
            onToucher = {},
        )
    }
}
