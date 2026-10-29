// Instants et distances recopiés de la maquette « Envol forgé · badges illustrés » : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omegadex.app.R
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.PlexMono
import com.omegadex.app.ui.theme.Roche
import com.omegadex.app.ui.theme.RocheClaire
import com.omegadex.app.ui.theme.RocheSurface

/**
 * Arrivée d'un badge dans sa niche (millisecondes depuis le début de son envol) : il tombe d'en haut chauffé
 * comme du métal en fusion, la niche s'embrase, des étincelles jaillissent et il refroidit jusqu'à sa couleur.
 */
object ChronoEnvolBadge {
    const val ENVOL_DUREE = 900f
    const val REFROIDISSEMENT_DUREE = 1_600f
    const val BRAISE = 760f
    const val BRAISE_DUREE = 800f
    const val ETINCELLES = 820f
    const val ETINCELLES_DUREE = 700f
    const val NOM = 1_200f
    const val NOM_DUREE = 400f
    const val FIN = REFROIDISSEMENT_DUREE
}

/** Badge en train d'arriver dans sa niche, [t] ms après le début de son envol. */
data class ArriveeBadge(val badge: BadgeArene, val t: Float)

@StringRes
fun nomBadge(badge: BadgeArene): Int = when (badge) {
    BadgeArene.ROCHE -> R.string.badge_roche
    BadgeArene.POING -> R.string.badge_poing
    BadgeArene.DYNAMO -> R.string.badge_dynamo
    BadgeArene.CHALEUR -> R.string.badge_chaleur
    BadgeArene.BALANCE -> R.string.badge_balance
    BadgeArene.PLUME -> R.string.badge_plume
    BadgeArene.ESPRIT -> R.string.badge_esprit
    BadgeArene.PLUIE -> R.string.badge_pluie
}

private val FORME_ECRIN = RoundedCornerShape(22.dp)
private val NUMERO_ETEINT = Color(0xFF6A5E63)
private val NOM_ETEINT = Color(0xFFA0959A)
private val EASE_OUT: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EASE_ENVOL: Easing = CubicBezierEasing(0.4f, 0f, 0.3f, 1f)
private val ETINCELLES = listOf(
    Offset(-38f, -30f),
    Offset(36f, -34f),
    Offset(42f, 10f),
    Offset(-40f, 16f),
    Offset(4f, -44f),
)

/**
 * Boîte à badges : écrin de roche avec une niche par arène. Les badges [obtenus] y sont posés, les autres niches
 * montrent leur numéro. Pendant l'animation de gain, [arrivee] fait arriver un badge, [compteur] et [lueur]
 * (0 à 1, braise montant du fond) sont pilotés par la séquence et [onNicheArrivee] reçoit les coordonnées de la
 * niche visée, pour la caméra. Le badge en vol déborde de l'écrin : le parent ne doit pas le rogner.
 */
@Composable
fun BoiteBadges(
    obtenus: Set<BadgeArene>,
    modifier: Modifier = Modifier,
    arrivee: ArriveeBadge? = null,
    compteur: Int = obtenus.size,
    lueur: Float = 0f,
    onNicheArrivee: (LayoutCoordinates) -> Unit = {},
) {
    val total = BadgeArene.entries.size
    val noms = BadgeArene.entries.filter { it in obtenus }.map { stringResource(nomBadge(it)) }
    val detail = if (noms.isEmpty()) {
        stringResource(R.string.badges_aucun)
    } else {
        stringResource(R.string.badges_obtenus, noms.joinToString(", "))
    }
    val description = stringResource(R.string.badges_description, compteur, total, detail)
    Column(
        modifier
            .drawBehind { dessinerFondEcrin(lueur) }
            .border(1.dp, RocheClaire, FORME_ECRIN)
            .padding(20.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.badges_titre),
                color = Blanc,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.badges_compteur, compteur, total),
                color = LueurMagma,
                fontFamily = PlexMono,
                fontSize = 15.sp,
            )
        }
        BadgeArene.entries.chunked(COLONNES).forEach { rangee ->
            Row(Modifier.fillMaxWidth()) {
                rangee.forEach { badge ->
                    NicheBadge(
                        badge = badge,
                        obtenu = badge in obtenus,
                        arrivee = arrivee?.takeIf { it.badge == badge },
                        onPosition = onNicheArrivee,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val COLONNES = 4

private fun DrawScope.dessinerFondEcrin(lueur: Float) {
    val rayon = CornerRadius(22.dp.toPx())
    drawRoundRect(Roche, cornerRadius = rayon)
    if (lueur > 0f) {
        val braise = Brush.radialGradient(
            listOf(Magma.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(size.width / 2f, size.height),
            radius = size.width * 0.65f,
        )
        drawRoundRect(braise, cornerRadius = rayon, alpha = lueur)
    }
}

@Composable
private fun NicheBadge(
    badge: BadgeArene,
    obtenu: Boolean,
    arrivee: ArriveeBadge?,
    onPosition: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = arrivee?.t
    val pose = obtenu && (t == null || t >= ChronoEnvolBadge.ENVOL_DUREE * 0.85f)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .then(
                    if (arrivee != null) Modifier.onGloballyPositioned(onPosition) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { dessinerNiche() }
            if (!pose) {
                Text("${badge.ordinal + 1}", color = NUMERO_ETEINT, fontFamily = PlexMono, fontSize = 14.sp)
            }
            if (t != null) Canvas(Modifier.fillMaxSize()) { dessinerBraise(t) }
            if (obtenu) BadgeEnVol(badge, t)
            if (t != null) Canvas(Modifier.fillMaxSize()) { dessinerEtincelles(t) }
        }
        Text(
            stringResource(nomBadge(badge)),
            color = if (obtenu) Blanc else NOM_ETEINT,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                alpha = if (t == null) 1f else avancementBadge(t, ChronoEnvolBadge.NOM, ChronoEnvolBadge.NOM_DUREE)
            },
        )
    }
}

/** Le badge, posé ([t] null) ou en vol : il tombe en tournant avec une traînée de lave, puis refroidit. */
@Composable
private fun BadgeEnVol(badge: BadgeArene, t: Float?) {
    if (t != null) {
        Canvas(
            Modifier
                .size(48.dp)
                .graphicsLayer { appliquerEnvol((t - 70f) / ChronoEnvolBadge.ENVOL_DUREE, trainee = true) },
        ) {
            drawCircle(Brush.radialGradient(listOf(Magma, Color.Transparent)), radius = size.minDimension / 2f)
        }
    }
    Canvas(
        Modifier
            .size(56.dp)
            .graphicsLayer { if (t != null) appliquerEnvol(t / ChronoEnvolBadge.ENVOL_DUREE, trainee = false) },
    ) {
        val chaleur = when {
            t == null -> 0f
            t < DEBUT_REFROIDISSEMENT -> 1f
            else -> 1f - avancementBadge(t, DEBUT_REFROIDISSEMENT, DUREE_REFROIDISSEMENT)
        }
        dessinerBadge(badge, chaleur)
    }
}

private const val DEBUT_REFROIDISSEMENT = ChronoEnvolBadge.REFROIDISSEMENT_DUREE * 0.55f
private const val DUREE_REFROIDISSEMENT = ChronoEnvolBadge.REFROIDISSEMENT_DUREE - DEBUT_REFROIDISSEMENT

/** Trajectoire de l'envol (keyframes `envol` et `trainee` de la maquette), [p] de 0 à 1. */
private fun GraphicsLayerScope.appliquerEnvol(p: Float, trainee: Boolean) {
    val q = p.coerceIn(0f, 1f)
    translationX = cles(q, 0f to 0f, 0.6f to 14f, 0.85f to 0f, 1f to 0f).dp.toPx()
    translationY = cles(q, 0f to -170f, 0.6f to -50f, 0.85f to 3f, 1f to 0f).dp.toPx()
    val echelle = if (trainee) {
        cles(q, 0f to 1.5f, 0.6f to 1.2f, 1f to 1f)
    } else {
        cles(q, 0f to 1.6f, 0.6f to 1.25f, 0.85f to 0.96f, 1f to 1f)
    }
    scaleX = echelle
    scaleY = echelle
    rotationZ = if (trainee) 0f else cles(q, 0f to -25f, 0.6f to 10f, 0.85f to 0f, 1f to 0f)
    alpha = when {
        p <= 0f -> 0f
        trainee -> cles(q, 0f to 0f, 0.15f to 0.7f, 0.6f to 0.5f, 1f to 0f)
        else -> cles(q, 0f to 0f, 0.1f to 1f, 1f to 1f)
    }
}

private fun DrawScope.dessinerNiche() {
    val rayon = size.minDimension / 2f
    drawCircle(RocheSurface, rayon)
    drawCircle(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.4f to Color.Transparent), rayon)
}

/** La niche s'embrase à l'atterrissage du badge. */
private fun DrawScope.dessinerBraise(t: Float) {
    val p = ((t - ChronoEnvolBadge.BRAISE) / ChronoEnvolBadge.BRAISE_DUREE).coerceIn(0f, 1f)
    if (p <= 0f || p >= 1f) return
    val opacite = if (p < 0.25f) p / 0.25f else 1f - EASE_OUT.transform((p - 0.25f) / 0.75f)
    val echelle = cles(p, 0f to 0.4f, 0.25f to 1.15f, 1f to 1.35f)
    val rayon = (size.minDimension / 2f + 6.dp.toPx()) * echelle
    val braise = Brush.radialGradient(
        0f to LueurMagma,
        0.45f to Magma,
        0.7f to Magma.copy(alpha = 0f),
        center = center,
        radius = rayon,
    )
    drawCircle(braise, rayon, alpha = opacite)
}

private fun DrawScope.dessinerEtincelles(t: Float) {
    val p = ((t - ChronoEnvolBadge.ETINCELLES) / ChronoEnvolBadge.ETINCELLES_DUREE).coerceIn(0f, 1f)
    if (p <= 0f || p >= 1f) return
    val f = EASE_OUT.transform(p)
    ETINCELLES.forEach { direction ->
        val position = center + Offset(direction.x.dp.toPx(), direction.y.dp.toPx()) * f
        drawCircle(LueurMagma, 2.5.dp.toPx(), position, alpha = 1f - p)
    }
}

/** Avancement de 0 à 1 d'une phase qui commence à [debut] et dure [duree] (ms). */
fun avancementBadge(t: Float, debut: Float, duree: Float): Float =
    EASE_OUT.transform(((t - debut) / duree).coerceIn(0f, 1f))

/** Interpolation entre des clés (instant de 0 à 1, valeur), adoucie sur chaque segment. */
private fun cles(p: Float, vararg points: Pair<Float, Float>): Float {
    val suivant = points.indexOfFirst { it.first >= p }
    if (suivant == -1) return points.last().second
    if (suivant == 0) return points.first().second
    val (t0, v0) = points[suivant - 1]
    val (t1, v1) = points[suivant]
    val f = EASE_ENVOL.transform(((p - t0) / (t1 - t0)).coerceIn(0f, 1f))
    return v0 + (v1 - v0) * f
}

@ApercusClairSombre
@Composable
private fun BoiteBadgesPreview() {
    OmegadexTheme {
        BoiteBadges(
            obtenus = setOf(BadgeArene.ROCHE, BadgeArene.POING, BadgeArene.DYNAMO),
            modifier = Modifier.padding(16.dp),
        )
    }
}
