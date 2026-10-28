package com.omegadex.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.omegadex.app.ui.theme.OmegadexTheme

/** Écran temporaire affiché par les onglets qui ne sont pas encore développés. */
@Composable
fun EcranEnConstruction(titre: String, modifier: Modifier = Modifier, detail: String? = null) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = titre, style = MaterialTheme.typography.headlineMedium)
        if (detail != null) {
            Text(text = detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EcranEnConstructionPreview() {
    OmegadexTheme {
        EcranEnConstruction(titre = "Pokédex", detail = "Bientôt disponible")
    }
}
