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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.ui.components.dessinerGemme
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.LueurMagma
import com.omegadex.app.ui.theme.Magma
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Chronologie « Capture → Pokédex », proposition « Fissure + onde » de la maquette (ms, 0 = toucher « Attraper ») :
 * le rubis vient d'éclore à côté de la ligne (dans l'écran Aventure), il pulse et lance une onde qui efface
 * l'Aventure ; la grille du Pokédex apparaît, la caméra avance sur la case de l'espèce, des fissures de lave
 * courent sur la case vide qui se fend en deux, une onde révèle la case remplie et ses rubis de statut
 * s'allument ; le compteur passe, la caméra recule et la capture est annoncée.
 */
private object ChronoCapture {
    const val PULSE = 900f
    const val PULSE_DUREE = 500f
    const val ONDE = 1_150f
    const val ONDE_DUREE = 900f
    const val VOILE = 1_200f
    const val VOILE_DUREE = 600f
    const val GRILLE = 1_650f
    const val GRILLE_DUREE = 600f
    const val CAMERA = 2_300f
    const val CAMERA_DUREE = 810f
    const val FISSURES = 3_200f
    const val FISSURES_SECONDAIRES = 3_320f
    const val FISSURES_DUREE = 450f
    const val REFROIDIR = 3_700f
    const val REFROIDIR_DUREE = 300f
    const val FENTE = 3_650f
    const val FENTE_DUREE = 460f
    const val ONDE_CASE = 3_660f
    const val ONDE_CASE_DUREE = 750f
    const val RUBIS = 3_900f
    const val RUBIS_ECART = 150f
    const val RUBIS_DUREE = 260f
    const val COMPTEUR = 4_200f
    const val COMPTEUR_DUREE = 350f
    const val CAMERA_RETOUR = 4_700f
    const val CAMERA_RETOUR_DUREE = 600f
    const val ANNONCE = 5_100f
    const val ANNONCE_DUREE = 400f
    const val PASSER_DUREE = 300f
    const val FIN = 5_500f
    const val ATTENTE_FIN_MS = 1_400L
    const val ATTENTE_REDUITE_MS = 2_500L
}

private val EASE_OUT: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EASE_IN: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val EASE_CAMERA: Easing = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
private val EASE_ONDE: Easing = CubicBezierEasing(0.2f, 0.6f, 0.3f, 1f)
private val EASE_FENTE: Easing = CubicBezierEasing(0.5f, 0f, 0.8f, 0.5f)
private const val ZOOM_CAMERA = 2.2f
private const val COLONNES = 3
private val FORME_CASE = RoundedCornerShape(14.dp)
private val HAUTEUR_CASE = 128.dp

/** Repère des fissures, celui de la case de la maquette (116 × 128). */
private val REPERE_FISSURES = Size(116f, 128f)
private val FISSURE_PRINCIPALE = listOf(Offset(67f, 0f), Offset(51f, 51f), Offset(70f, 79f), Offset(49f, 128f))
private val FISSURES_SECONDAIRES = listOf(
    listOf(Offset(51f, 51f), Offset(18f, 44f)),
    listOf(Offset(70f, 79f), Offset(104f, 92f)),
    listOf(Offset(60f, 26f), Offset(88f, 18f)),
)

/** Les deux moitiés de la case vide, de part et d'autre de la fissure principale. */
private val MOITIE_GAUCHE = GenericShape { taille, _ ->
    moveTo(0f, 0f)
    lineTo(taille.width * 0.58f, 0f)
    lineTo(taille.width * 0.44f, taille.height * 0.40f)
    lineTo(taille.width * 0.60f, taille.height * 0.62f)
    lineTo(taille.width * 0.42f, taille.height)
    lineTo(0f, taille.height)
    close()
}
private val MOITIE_DROITE = GenericShape { taille, _ ->
    moveTo(taille.width * 0.58f, 0f)
    lineTo(taille.width, 0f)
    lineTo(taille.width, taille.height)
    lineTo(taille.width * 0.42f, taille.height)
    lineTo(taille.width * 0.60f, taille.height * 0.62f)
    lineTo(taille.width * 0.44f, taille.height * 0.40f)
    close()
}

private fun phase(t: Float, debut: Float, duree: Float, easing: Easing = EASE_OUT): Float =
    easing.transform(((t - debut) / duree).coerceIn(0f, 1f))

/**
 * Animation de capture par-dessus l'écran Aventure. [origine] est le centre du rubis apparu à côté de la ligne
 * de rencontre (repère de la racine), d'où part l'onde. Toucher passe à l'image finale, un second toucher ferme.
 * Avec les animations réduites, l'image finale s'affiche sans mouvement. Appelle [onFin] une seule fois.
 */
@Composable
fun AnimationCapture(capture: CaptureAnimee, origine: Offset?, onFin: () -> Unit, modifier: Modifier = Modifier) {
    val reduites = OmegadexTheme.animationsReduites
    val temps = remember(capture) { Animatable(0f) }
    var passe by remember(capture) { mutableStateOf(reduites) }
    var fini by remember(capture) { mutableStateOf(false) }
    val terminer = {
        if (!fini) {
            fini = true
            onFin()
        }
    }
    LaunchedEffect(capture) {
        if (!reduites) temps.animateTo(ChronoCapture.FIN, tween(ChronoCapture.FIN.toInt(), easing = LinearEasing))
        delay(if (reduites) ChronoCapture.ATTENTE_REDUITE_MS else ChronoCapture.ATTENTE_FIN_MS)
        terminer()
    }
    LaunchedEffect(passe) {
        if (passe && !reduites) {
            delay(ChronoCapture.ATTENTE_FIN_MS)
            terminer()
        }
    }
    val t = if (passe) ChronoCapture.FIN else temps.value
    ContenuCapture(
        capture = capture,
        origine = origine,
        t = t,
        onToucher = { if (t < ChronoCapture.FIN) passe = true else terminer() },
        modifier = modifier,
    )
}

/**
 * Point visé par la caméra : le centre de la case de l'espèce, dans le repère de la scène. Recalculé à chaque
 * image tant que la caméra est immobile, puis figé : relu pendant le zoom, il suivrait la caméra elle-même.
 */
private class ViseeCase {
    var scene: LayoutCoordinates? = null
    var case: LayoutCoordinates? = null
    var cible: Offset? = null
        private set

    fun relever() {
        val repere = scene ?: return
        val visee = case ?: return
        if (repere.isAttached && visee.isAttached) cible = repere.localBoundingBoxOf(visee, clipBounds = false).center
    }

    /** [origine], donnée dans le repère de la racine, ramenée dans celui de la scène. */
    fun versScene(origine: Offset?): Offset? {
        val repere = scene ?: return null
        return if (origine != null && repere.isAttached) repere.windowToLocal(origine) else null
    }
}

@Composable
private fun ContenuCapture(
    capture: CaptureAnimee,
    origine: Offset?,
    t: Float,
    onToucher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visee = remember(capture) { ViseeCase() }
    val fond = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxSize()
            .clickable(onClickLabel = stringResource(R.string.badges_passer), onClick = onToucher)
            .onGloballyPositioned { visee.scene = it },
    ) {
        // L'Aventure s'efface sous le fond de l'écran, puis la grille du Pokédex émerge.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = phase(t, ChronoCapture.VOILE, ChronoCapture.VOILE_DUREE, EASE_IN) }
                .background(fond),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = phase(t, ChronoCapture.GRILLE, ChronoCapture.GRILLE_DUREE)
                    alpha = p
                    scaleX = 0.96f + 0.04f * p
                    scaleY = scaleX
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val zoom = zoomCamera(t)
                        if (zoom == 0f) visee.relever()
                        val cible = visee.cible
                        if (cible != null && zoom > 0f && size.width > 0f) {
                            transformOrigin = TransformOrigin(cible.x / size.width, cible.y / size.height)
                            translationX = (size.width / 2f - cible.x) * zoom
                            translationY = (size.height / 2f - cible.y) * zoom
                            scaleX = 1f + (ZOOM_CAMERA - 1f) * zoom
                            scaleY = scaleX
                        }
                    },
            ) {
                GrilleCapture(capture, t) { visee.case = it }
            }
        }
        OndeEtRubis(t) { visee.versScene(origine) }
        TextesCapture(capture.nom, t)
    }
}

/** 0 → 1 quand la caméra avance vers la case, 1 pendant que la case se complète, puis retour à 0. */
private fun zoomCamera(t: Float): Float = phase(t, ChronoCapture.CAMERA, ChronoCapture.CAMERA_DUREE, EASE_CAMERA) -
    phase(t, ChronoCapture.CAMERA_RETOUR, ChronoCapture.CAMERA_RETOUR_DUREE, EASE_CAMERA)

/** Le rubis de la ligne pulse, puis son onde s'élargit jusqu'à couvrir l'écran. */
@Composable
private fun BoxScope.OndeEtRubis(t: Float, origine: () -> Offset?) {
    val couleurs = OmegadexTheme.couleurs
    Canvas(Modifier.matchParentSize()) {
        val centre = origine() ?: Offset(size.width / 2f, size.height / 3f)
        val onde = ((t - ChronoCapture.ONDE) / ChronoCapture.ONDE_DUREE).coerceIn(0f, 1f)
        if (onde > 0f && onde < 1f) {
            drawCircle(
                color = couleurs.gemmeClaire,
                radius = 40.dp.toPx() * (0.3f + 13.7f * EASE_ONDE.transform(onde)) / 2f,
                center = centre,
                alpha = 1f - onde,
                style = Stroke(3.dp.toPx()),
            )
        }
        val pulse = ((t - ChronoCapture.PULSE) / ChronoCapture.PULSE_DUREE).coerceIn(0f, 1f)
        if (pulse > 0f && pulse < 1f) {
            val echelle = 1f + 0.45f * sin(PI.toFloat() * pulse)
            val taille = Size(26.dp.toPx() * echelle, 24.dp.toPx() * echelle)
            val halo = 0.35f * sin(PI.toFloat() * pulse)
            drawCircle(couleurs.gemmeClaire, radius = taille.width, center = centre, alpha = halo)
            translate(centre.x - taille.width / 2f, centre.y - taille.height / 2f) {
                val cadre = drawContext.size
                drawContext.size = taille
                dessinerGemme(allumee = true, couleurs = couleurs)
                drawContext.size = cadre
            }
        }
    }
}

@Composable
private fun GrilleCapture(capture: CaptureAnimee, t: Float, onCase: (LayoutCoordinates) -> Unit) {
    val titre = if (capture.national) R.string.pokedex_titre_national else R.string.pokedex_titre_hoenn
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(titre),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Compteur(capture, t)
        }
        capture.cases.chunked(COLONNES).forEach { ligne ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ligne.forEach { case ->
                    val modifier = Modifier.weight(1f).height(HAUTEUR_CASE)
                    if (case.especeId == capture.especeId) {
                        CaseCible(case, t, modifier.onGloballyPositioned(onCase))
                    } else {
                        FaceCase(case, case.apres, t = null, modifier = modifier)
                    }
                }
                repeat(COLONNES - ligne.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** « 13 / 211 » glisse vers le haut, « 14 / 211 » arrive par le bas. */
@Composable
private fun Compteur(capture: CaptureAnimee, t: Float) {
    val couleurs = OmegadexTheme.couleurs
    val p = phase(t, ChronoCapture.COMPTEUR, ChronoCapture.COMPTEUR_DUREE)
    Box(contentAlignment = Alignment.CenterEnd) {
        Text(
            stringResource(R.string.aventure_compteur, capture.capturees, capture.total),
            style = OmegadexTheme.chiffres.petit,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - p
                translationY = -6.dp.toPx() * p
            },
        )
        Text(
            stringResource(R.string.aventure_compteur, capture.capturees + 1, capture.total),
            style = OmegadexTheme.chiffres.petit,
            color = couleurs.gemme,
            modifier = Modifier.graphicsLayer {
                alpha = p
                translationY = 6.dp.toPx() * (1f - p)
            },
        )
    }
}

/** La case de l'espèce attrapée : fissures, fente en deux, puis la case remplie et son onde. */
@Composable
private fun CaseCible(case: CaseCapture, t: Float, modifier: Modifier) {
    val couleurs = OmegadexTheme.couleurs
    val ouverte = t >= ChronoCapture.FENTE
    val fondAvant = if (case.avant == StatutEspece.NON_VUE) couleurs.piste else MaterialTheme.colorScheme.surface
    Box(modifier) {
        if (ouverte) {
            FaceCase(case, case.apres, t = t, modifier = Modifier.matchParentSize())
        } else {
            FaceCase(case, case.avant, t = null, modifier = Modifier.matchParentSize())
        }
        val fente = phase(t, ChronoCapture.FENTE, ChronoCapture.FENTE_DUREE, EASE_FENTE)
        if (ouverte && fente < 1f) {
            Moitie(fondAvant, MOITIE_GAUCHE, fente, versX = -26f, versY = 30f, rotation = -14f)
            Moitie(fondAvant, MOITIE_DROITE, fente, versX = 26f, versY = 34f, rotation = 12f)
        }
        Canvas(Modifier.matchParentSize()) {
            val refroidi = 1f - phase(t, ChronoCapture.REFROIDIR, ChronoCapture.REFROIDIR_DUREE)
            if (refroidi > 0f) {
                val principale = phase(t, ChronoCapture.FISSURES, ChronoCapture.FISSURES_DUREE)
                val secondaires = phase(t, ChronoCapture.FISSURES_SECONDAIRES, ChronoCapture.FISSURES_DUREE)
                tracerFissure(FISSURE_PRINCIPALE, principale, refroidi)
                FISSURES_SECONDAIRES.forEach { tracerFissure(it, secondaires, refroidi) }
            }
            val onde = ((t - ChronoCapture.ONDE_CASE) / ChronoCapture.ONDE_CASE_DUREE).coerceIn(0f, 1f)
            if (onde > 0f && onde < 1f) {
                drawCircle(
                    color = Magma,
                    radius = size.width / 2f * (0.3f + 1.3f * EASE_OUT.transform(onde)),
                    alpha = 0.9f * (1f - onde),
                    style = Stroke(3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Moitie(fond: Color, forme: GenericShape, p: Float, versX: Float, versY: Float, rotation: Float) {
    val trait = OmegadexTheme.couleurs.trait
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                translationX = versX.dp.toPx() * p
                translationY = versY.dp.toPx() * p
                rotationZ = rotation * p
                alpha = 1f - p
                shape = forme
                clip = true
            }
            .background(fond)
            .border(1.dp, trait, FORME_CASE),
    )
}

/** Trace la fissure [points] (repère de la maquette) jusqu'à [p] de sa longueur, avec une lueur de lave. */
private fun DrawScope.tracerFissure(points: List<Offset>, p: Float, alpha: Float) {
    if (p <= 0f) return
    val echelle = Offset(size.width / REPERE_FISSURES.width, size.height / REPERE_FISSURES.height)
    val sommets = points.map { Offset(it.x * echelle.x, it.y * echelle.y) }
    val longueurs = sommets.zipWithNext { a, b -> (b - a).getDistance() }
    var reste = longueurs.sum() * p
    sommets.zipWithNext().forEachIndexed { i, (a, b) ->
        if (reste <= 0f) return
        val fin = if (reste >= longueurs[i]) b else a + (b - a) * (reste / longueurs[i])
        drawLine(LueurMagma, a, fin, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round, alpha = 0.35f * alpha)
        drawLine(Magma, a, fin, strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round, alpha = alpha)
        reste -= longueurs[i]
    }
}

/**
 * Une case de la grille, dessinée comme dans l'écran Pokédex. [t] non nul : les rubis gagnés par la capture
 * s'allument l'un après l'autre.
 */
@Composable
private fun FaceCase(case: CaseCapture, statut: StatutEspece, t: Float?, modifier: Modifier = Modifier) {
    val couleurs = OmegadexTheme.couleurs
    val silhouette = statut == StatutEspece.NON_VUE
    Column(
        modifier
            .background(if (silhouette) couleurs.piste else MaterialTheme.colorScheme.surface, FORME_CASE)
            .border(1.dp, couleurs.trait, FORME_CASE)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp)
            .clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.pokedex_numero, case.numero),
            style = OmegadexTheme.chiffres.petit,
            color = couleurs.texteEteint,
        )
        Text(
            case.nom,
            style = MaterialTheme.typography.titleSmall,
            color = if (silhouette) couleurs.texteEteint else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!silhouette) {
            case.types.firstOrNull()?.let { type ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).background(CouleursTypes.de(type), CircleShape))
                    Text(
                        nomDuType(type)?.let { stringResource(it) } ?: type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (silhouette) {
                Text(
                    stringResource(R.string.pokedex_marquer_vu),
                    style = MaterialTheme.typography.labelSmall,
                    color = couleurs.texteEteint,
                )
            } else {
                RubisCase(case, statut, t)
            }
            if (statut != StatutEspece.MAITRISEE) {
                Text("+", style = MaterialTheme.typography.labelLarge, color = couleurs.gemme)
            }
        }
    }
}

/** Les 5 rubis du statut ; ceux gagnés par la capture éclatent puis s'allument. */
@Composable
private fun RubisCase(case: CaseCapture, statut: StatutEspece, t: Float?) {
    val couleurs = OmegadexTheme.couleurs
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(StatutEspece.MAITRISEE.ordinal) { index ->
            val gagne = t != null && index >= case.avant.ordinal && index < statut.ordinal
            val p = if (gagne) {
                val rang = index - case.avant.ordinal
                val debut = ChronoCapture.RUBIS + rang * ChronoCapture.RUBIS_ECART
                phase(t, debut, ChronoCapture.RUBIS_DUREE, LinearEasing)
            } else {
                1f
            }
            val couleur = when {
                index >= statut.ordinal -> couleurs.gemmeEteinte
                p < 0.5f -> lerp(couleurs.gemmeEteinte, couleurs.gemmeClaire, p * 2f)
                else -> lerp(couleurs.gemmeClaire, couleurs.gemme, (p - 0.5f) * 2f)
            }
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        val eclat = if (gagne) 0.8f * sin(PI.toFloat() * p) else 0f
                        scaleX = 1f + eclat
                        scaleY = scaleX
                    }
                    .background(couleur, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun BoxScope.TextesCapture(nom: String, t: Float) {
    Text(
        stringResource(R.string.badges_passer),
        color = OmegadexTheme.couleurs.texteEteint,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .graphicsLayer {
                alpha = phase(t, ChronoCapture.VOILE, ChronoCapture.VOILE_DUREE) *
                    (1f - phase(t, ChronoCapture.ANNONCE, ChronoCapture.PASSER_DUREE))
            },
    )
    Text(
        stringResource(R.string.capture_annonce, nom),
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .graphicsLayer {
                val p = phase(t, ChronoCapture.ANNONCE, ChronoCapture.ANNONCE_DUREE)
                alpha = p
                translationY = 6.dp.toPx() * (1f - p)
            }
            .background(Roche, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@ApercusClairSombre
@Composable
private fun AnimationCaptureFinPreview() {
    val cases = listOf(
        CaseCapture(261, 10, "Medhyèna", listOf("tenebres"), StatutEspece.VUE, StatutEspece.VUE),
        CaseCapture(262, 11, "Grahyèna", listOf("tenebres"), StatutEspece.NON_VUE, StatutEspece.NON_VUE),
        CaseCapture(263, 12, "Zigzaton", listOf("normal"), StatutEspece.CAPTUREE, StatutEspece.CAPTUREE),
        CaseCapture(264, 13, "Linéon", listOf("normal"), StatutEspece.NON_VUE, StatutEspece.NON_VUE),
        CaseCapture(265, 14, "Chenipotte", listOf("insecte"), StatutEspece.NON_VUE, StatutEspece.CAPTUREE),
        CaseCapture(266, 15, "Armulys", listOf("insecte"), StatutEspece.NON_VUE, StatutEspece.NON_VUE),
    )
    OmegadexTheme {
        ContenuCapture(CaptureAnimee(265, "Chenipotte", false, cases, 13, 211), null, ChronoCapture.FIN, onToucher = {})
    }
}
