package com.omegadex.app.feature.succes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.succes.Succes
import com.omegadex.app.ui.components.Gemme
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.Roche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Annonce « Succès débloqué » posée en haut de l'écran, au-dessus de tous les onglets (T18.2) :
 * la gemme tombe et s'allume avec un éclat, puis l'annonce repart. Une annonce par succès, à la suite.
 */
@Composable
fun AnnonceSucces(modifier: Modifier = Modifier, viewModel: AnnonceSuccesViewModel = hiltViewModel()) {
    val annonces by viewModel.annonces.collectAsStateWithLifecycle()
    val courante = annonces.firstOrNull()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = courante != null,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            courante?.let { BandeauSucces(it, viewModel::suivante) }
        }
    }
}

@Composable
private fun BandeauSucces(succes: Succes, onFin: () -> Unit) {
    val reduites = OmegadexTheme.animationsReduites
    val sons = LocalSons.current
    val apparition = remember(succes) { Animatable(if (reduites) 1f else 0f) }
    LaunchedEffect(succes) {
        sons.jouer(Son.DEFI_REUSSI)
        if (!reduites) launch { apparition.animateTo(1f, tween(DUREE_GEMME_MS)) }
        delay(DUREE_ANNONCE_MS)
        onFin()
    }
    Surface(
        onClick = onFin,
        shape = MaterialTheme.shapes.large,
        color = Roche,
        contentColor = Blanc,
        shadowElevation = 8.dp,
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Gemme(
                allumee = true,
                modifier = Modifier
                    .size(width = 34.dp, height = 31.dp)
                    .graphicsLayer {
                        val p = apparition.value
                        // La gemme tombe en tournant, dépasse un peu sa taille puis se pose.
                        val echelle = if (p < REBOND) {
                            p / REBOND * DEPASSEMENT
                        } else {
                            DEPASSEMENT - (p - REBOND) / (1f - REBOND) * (DEPASSEMENT - 1f)
                        }
                        scaleX = echelle
                        scaleY = echelle
                        rotationZ = (1f - p) * ROTATION
                        alpha = p.coerceIn(0f, 1f)
                    },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.succes_annonce).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OmegadexTheme.couleurs.lueurMagma,
                )
                Text(stringResource(titreSucces(succes)), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(conditionSucces(succes), succes.seuil, succes.seuil),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val DUREE_GEMME_MS = 700
private const val DUREE_ANNONCE_MS = 3_500L
private const val REBOND = 0.7f
private const val DEPASSEMENT = 1.25f
private const val ROTATION = -90f
