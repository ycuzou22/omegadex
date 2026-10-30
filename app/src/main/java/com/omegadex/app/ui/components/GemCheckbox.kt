package com.omegadex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Case à cocher en gemme Oméga. Éteinte, la pierre porte un Ω gravé ; cochée, elle se fend de veines de magma,
 * s'allume dans un flash avec deux ondes et le Ω est frappé en rougeoyant (~1,1 s, voir [ChronoGemmeOmega]).
 * Avec les animations réduites, l'état final s'affiche aussitôt. Avec un [libelle], toute la ligne est
 * cliquable et le texte est barré une fois la quête faite (sauf si [barrerSiCoche] vaut false, pour une
 * option choisie).
 */
@Composable
fun GemCheckbox(
    coche: Boolean,
    onCocheChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    libelle: String? = null,
    actif: Boolean = true,
    barrerSiCoche: Boolean = true,
) {
    val couleurs = OmegadexTheme.couleurs
    val reduites = OmegadexTheme.animationsReduites
    val sons = LocalSons.current
    val temps = remember { Animatable(ChronoGemmeOmega.FIN) }
    var etatPrecedent by remember { mutableStateOf(coche) }
    LaunchedEffect(coche) {
        if (coche && !etatPrecedent && !reduites) {
            temps.snapTo(0f)
            temps.animateTo(
                ChronoGemmeOmega.FIN,
                tween(durationMillis = ChronoGemmeOmega.FIN.toInt(), easing = LinearEasing),
            )
        } else {
            temps.snapTo(ChronoGemmeOmega.FIN)
        }
        etatPrecedent = coche
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(value = coche, enabled = actif, role = Role.Checkbox, onValueChange = {
                sons.jouer(if (it) Son.COCHER else Son.DECOCHER)
                onCocheChange(it)
            }),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = if (libelle == null) 10.dp else 0.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(width = 28.dp, height = 26.dp)) {
                dessinerGemmeOmega(allumee = coche, couleurs = couleurs, t = temps.value)
            }
        }
        if (libelle != null) {
            Text(
                text = libelle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (coche && barrerSiCoche) couleurs.texteEteint else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (coche && barrerSiCoche) TextDecoration.LineThrough else null,
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
