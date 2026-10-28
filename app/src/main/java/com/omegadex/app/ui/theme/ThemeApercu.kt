package com.omegadex.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Planche du thème : palette, gemme, échelle typographique et chiffres. */
@Composable
fun PlancheTheme(modifier: Modifier = Modifier) {
    val couleurs = MaterialTheme.colorScheme
    val da = OmegadexTheme.couleurs
    Column(
        modifier = modifier
            .background(couleurs.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Direction artistique", style = MaterialTheme.typography.labelMedium, color = couleurs.primary)
        Text("Groudon, rubis et Oméga", style = MaterialTheme.typography.headlineLarge, color = couleurs.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(couleurs.primary, da.gemmeSombre, da.magma, da.lueurMagma, da.roche, couleurs.surface)
                .forEach { Pastille(it) }
            Box(
                Modifier
                    .size(36.dp)
                    .background(da.gemme, FormeGemme),
            )
        }
        Text("Étape 3 · Lavandia", style = MaterialTheme.typography.titleLarge, color = couleurs.onBackground)
        Text(
            "Texte courant en Manrope, lisible en clair comme en sombre.",
            style = MaterialTheme.typography.bodyMedium,
            color = couleurs.onSurfaceVariant,
        )
        Text("Hoenn #007 · Niv. 16 · 23 %", style = OmegadexTheme.chiffres.grand, color = couleurs.onBackground)
    }
}

@Composable
private fun Pastille(couleur: Color) {
    Box(
        Modifier
            .size(36.dp)
            .background(couleur, MaterialTheme.shapes.extraSmall),
    )
}

@Preview(name = "Clair", showBackground = true)
@Composable
private fun PlancheThemeClairPreview() {
    OmegadexTheme(darkTheme = false) { PlancheTheme() }
}

@Preview(name = "Sombre", showBackground = true, backgroundColor = 0xFF1A1416)
@Composable
private fun PlancheThemeSombrePreview() {
    OmegadexTheme(darkTheme = true) { PlancheTheme() }
}
