package com.omegadex.app.feature.aventure

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.ui.components.dessinerGemme
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

private const val ECLOSION_MS = 520
private const val ROTATION_ECLOSION = -30f
private val EASE_REBOND = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)

/** Titre de la section des rencontres de l'étape. */
@Composable
internal fun EnTeteRencontres(rencontres: List<RencontreAffichee>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.aventure_rencontres),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            stringResource(R.string.aventure_compteur, rencontres.count { it.capturee }, rencontres.size),
            style = OmegadexTheme.chiffres.moyen,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Une espèce qu'on peut rencontrer : nom, lieu, niveaux et taux, puis le bouton « Attraper ».
 * Une fois capturée, un rubis éclot à la place du bouton ; [onRubis] reçoit le centre de ce rubis
 * (repère de la fenêtre), d'où part l'animation « Capture → Pokédex ».
 */
@Composable
internal fun LigneRencontre(
    rencontre: RencontreAffichee,
    onAttraper: () -> Unit,
    onRubis: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleurs = OmegadexTheme.couleurs
    val couleurType = rencontre.type?.let { CouleursTypes.de(it) } ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, couleurs.trait),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 18.dp, height = 16.dp).background(couleurType, FormeGemme))
            Column(Modifier.weight(1f)) {
                Text(rencontre.nom, style = MaterialTheme.typography.titleMedium)
                Text(
                    infoRencontre(rencontre),
                    style = OmegadexTheme.chiffres.petit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RubisCapture(rencontre.capturee, onRubis)
            if (rencontre.capturee) {
                Text(
                    stringResource(R.string.aventure_capturee),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = couleurs.gemme,
                )
            } else {
                val description = stringResource(R.string.aventure_attraper_description, rencontre.nom)
                Button(
                    onClick = onAttraper,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.semantics { contentDescription = description },
                ) {
                    Text(stringResource(R.string.aventure_attraper))
                }
            }
        }
    }
}

/** Emplacement du rubis : vide, puis le rubis éclot quand l'espèce devient capturée. */
@Composable
private fun RubisCapture(capturee: Boolean, onRubis: (Offset) -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val reduites = OmegadexTheme.animationsReduites
    val eclosion = remember { Animatable(if (capturee) 1f else 0f) }
    LaunchedEffect(capturee) {
        when {
            !capturee -> eclosion.snapTo(0f)
            reduites -> eclosion.snapTo(1f)
            eclosion.value < 1f -> eclosion.animateTo(1f, tween(ECLOSION_MS, easing = EASE_REBOND))
        }
    }
    Canvas(
        Modifier
            .size(width = 26.dp, height = 24.dp)
            .onGloballyPositioned { onRubis(it.boundsInWindow().center) }
            .graphicsLayer {
                val v = eclosion.value
                alpha = v.coerceIn(0f, 1f)
                scaleX = v
                scaleY = v
                rotationZ = ROTATION_ECLOSION * (1f - v)
            },
    ) {
        dessinerGemme(allumee = true, couleurs = couleurs)
    }
}

@Composable
private fun infoRencontre(rencontre: RencontreAffichee): String = buildList {
    rencontre.niveauMin?.let { min ->
        val max = rencontre.niveauMax ?: min
        add(
            if (min == max) {
                stringResource(R.string.lieu_niveau, min)
            } else {
                stringResource(R.string.lieu_niveaux, min, max)
            },
        )
    }
    rencontre.taux?.let { add(stringResource(R.string.lieu_taux, it)) }
    add(rencontre.lieu)
}.joinToString(" · ")
