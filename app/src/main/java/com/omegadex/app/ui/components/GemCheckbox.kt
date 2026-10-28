package com.omegadex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

private const val ECHELLE_DEPART = 0.5f
private const val ECHELLE_REBOND = 1.25f
private const val DUREE_REBOND_MS = 400
private const val MOMENT_REBOND_MS = 240

/**
 * Case à cocher en rubis taillé. Éteinte, elle est gris rosé ; cochée, elle s'allume
 * et rebondit (0,5 → 1,25 → 1). Avec un [libelle], toute la ligne est cliquable
 * et le texte est barré une fois la quête faite.
 */
@Composable
fun GemCheckbox(
    coche: Boolean,
    onCocheChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    libelle: String? = null,
    actif: Boolean = true,
) {
    val couleurs = OmegadexTheme.couleurs
    val reduites = OmegadexTheme.animationsReduites
    val echelle = remember { Animatable(1f) }
    var etatPrecedent by remember { mutableStateOf(coche) }
    LaunchedEffect(coche) {
        if (coche && !etatPrecedent && !reduites) {
            echelle.snapTo(ECHELLE_DEPART)
            echelle.animateTo(
                1f,
                keyframes {
                    durationMillis = DUREE_REBOND_MS
                    ECHELLE_REBOND at MOMENT_REBOND_MS
                },
            )
        }
        etatPrecedent = coche
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(value = coche, enabled = actif, role = Role.Checkbox, onValueChange = onCocheChange),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = if (libelle == null) 12.dp else 0.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(width = 24.dp, height = 22.dp)
                    .graphicsLayer {
                        scaleX = echelle.value
                        scaleY = echelle.value
                    },
            ) { dessinerGemme(allumee = coche, couleurs = couleurs) }
        }
        if (libelle != null) {
            Text(
                text = libelle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (coche) couleurs.texteEteint else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (coche) TextDecoration.LineThrough else null,
            )
        }
    }
}

@ApercusClairSombre
@Composable
private fun GemCheckboxPreview() {
    OmegadexTheme {
        Column(Modifier.padding(16.dp)) {
            GemCheckbox(coche = true, onCocheChange = {}, libelle = "Rejoindre Lavandia")
            GemCheckbox(coche = false, onCocheChange = {}, libelle = "Battre le rival")
            GemCheckbox(coche = false, onCocheChange = {}, libelle = "Vaincre Voltère")
        }
    }
}
