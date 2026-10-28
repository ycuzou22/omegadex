package com.omegadex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

private const val ECHELLE_APPUI = 0.96f
private const val DUREE_ECLAT_MS = 900
private const val DEPART_ECLAT = -0.4f
private const val ARRIVEE_ECLAT = 1.4f
private const val LARGEUR_ECLAT = 0.3f
private const val OPACITE_ECLAT = 0.65f
private const val MILIEU_ECLAT = 0.5f
private const val OPACITE_INACTIF = 0.38f

/**
 * Bouton de la DA « Épuré Oméga ».
 * À l'appui (ou au survol avec une souris), un éclat de magma traverse le bouton en diagonale
 * et le bouton s'enfonce légèrement. Les deux effets sont coupés si les animations système sont réduites.
 */
@Composable
fun OmegaButton(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBouton = VarianteBouton.PRINCIPAL,
    avecOmega: Boolean = false,
    actif: Boolean = true,
) {
    val couleursDa = OmegadexTheme.couleurs
    val (fond, contenu) = couleursBouton(variante)
    val reduites = OmegadexTheme.animationsReduites
    val interactions = remember { MutableInteractionSource() }
    val appuye by interactions.collectIsPressedAsState()
    val survole by interactions.collectIsHoveredAsState()
    val focus by interactions.collectIsFocusedAsState()

    val echelle by animateFloatAsState(
        targetValue = if (appuye && !reduites) ECHELLE_APPUI else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "echelle",
    )
    val eclat = remember { Animatable(DEPART_ECLAT) }
    LaunchedEffect(appuye, survole, reduites) {
        if ((appuye || survole) && !reduites) {
            eclat.snapTo(DEPART_ECLAT)
            eclat.animateTo(ARRIVEE_ECLAT, tween(DUREE_ECLAT_MS, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = echelle
                scaleY = echelle
            },
        enabled = actif,
        shape = MaterialTheme.shapes.small,
        color = if (actif) fond else fond.copy(alpha = OPACITE_INACTIF),
        contentColor = contenu,
        border = bordureBouton(variante, focus),
        interactionSource = interactions,
    ) {
        Row(
            modifier = Modifier
                .eclatDeMagma(progression = { eclat.value }, couleur = couleursDa.lueurMagma)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 12.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avecOmega) {
                Icon(
                    painter = painterResource(R.drawable.ic_omega),
                    contentDescription = null,
                    tint = if (variante == VarianteBouton.PRINCIPAL) couleursDa.omegaSurRubis else contenu,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
            }
            Text(text = texte, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Fond et contenu du bouton selon sa variante. */
@Composable
private fun couleursBouton(variante: VarianteBouton): Pair<Color, Color> {
    val schema = MaterialTheme.colorScheme
    return when (variante) {
        VarianteBouton.PRINCIPAL -> schema.primary to schema.onPrimary
        VarianteBouton.SOMBRE -> schema.secondary to schema.onSecondary
        VarianteBouton.CONTOUR -> schema.surface to schema.onSurface
    }
}

/** Contour magma quand le bouton a le focus clavier, trait fin pour la variante contour. */
@Composable
private fun bordureBouton(variante: VarianteBouton, focus: Boolean): BorderStroke? {
    val couleurs = OmegadexTheme.couleurs
    return when {
        focus -> BorderStroke(2.dp, couleurs.magma)
        variante == VarianteBouton.CONTOUR -> BorderStroke(1.5.dp, couleurs.trait)
        else -> null
    }
}

/** Bande de lumière diagonale qui traverse le contenu quand [progression] va de -0,4 à 1,4. */
private fun Modifier.eclatDeMagma(progression: () -> Float, couleur: Color) = drawWithContent {
    drawContent()
    val x = progression() * size.width
    if (x > DEPART_ECLAT * size.width && x < ARRIVEE_ECLAT * size.width) {
        val demi = LARGEUR_ECLAT * size.width / 2f
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                MILIEU_ECLAT to couleur.copy(alpha = OPACITE_ECLAT),
                1f to Color.Transparent,
                start = Offset(x - demi, 0f),
                end = Offset(x + demi, size.height / 2f),
            ),
        )
    }
}

@ApercusClairSombre
@Composable
private fun OmegaButtonPreview() {
    OmegadexTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OmegaButton("Valider le défi", onClick = {}, avecOmega = true, modifier = Modifier.fillMaxWidth())
            OmegaButton("Ouvrir l'étape", onClick = {
            }, variante = VarianteBouton.SOMBRE, modifier = Modifier.fillMaxWidth())
            OmegaButton("Plus tard", onClick = {
            }, variante = VarianteBouton.CONTOUR, modifier = Modifier.fillMaxWidth())
            OmegaButton("Indisponible", onClick = {}, actif = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
