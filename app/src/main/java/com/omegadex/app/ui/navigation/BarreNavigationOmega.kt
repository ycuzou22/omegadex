package com.omegadex.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

private const val DUREE_OMEGA_MS = 220

/**
 * Barre de navigation de la DA : 5 onglets sur une surface arrondie en haut.
 * L'onglet actif est signalé à la fois par la couleur rubis, un libellé plus gras
 * et un petit Ω au-dessus de l'icône, pour ne pas reposer sur la couleur seule.
 */
@Composable
fun BarreNavigationOmega(
    destinationCourante: Destination?,
    onDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Destination.entries.forEach { destination ->
                OngletOmega(
                    destination = destination,
                    actif = destination == destinationCourante,
                    onClick = { onDestination(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OngletOmega(destination: Destination, actif: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val couleur = if (actif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .selectable(selected = actif, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        IndicateurOmega(visible = actif)
        Icon(
            painter = painterResource(destination.icone),
            contentDescription = null,
            tint = couleur,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(destination.libelle),
            color = couleur,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (actif) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}

/** Petit Ω rubis qui apparaît au-dessus de l'onglet actif. */
@Composable
private fun IndicateurOmega(visible: Boolean) {
    val reduites = OmegadexTheme.animationsReduites
    Box(Modifier.height(12.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduites) fadeIn(snap()) else scaleIn(tween(DUREE_OMEGA_MS)) + fadeIn(tween(DUREE_OMEGA_MS)),
            exit = if (reduites) fadeOut(snap()) else scaleOut(tween(DUREE_OMEGA_MS)) + fadeOut(tween(DUREE_OMEGA_MS)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_omega_onglet),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(width = 16.dp, height = 12.dp),
            )
        }
    }
}

@ApercusClairSombre
@Composable
private fun BarreNavigationOmegaPreview() {
    var courante by remember { mutableStateOf(Destination.ACCUEIL) }
    OmegadexTheme {
        Column {
            Spacer(Modifier.height(24.dp))
            BarreNavigationOmega(destinationCourante = courante, onDestination = { courante = it })
        }
    }
}
