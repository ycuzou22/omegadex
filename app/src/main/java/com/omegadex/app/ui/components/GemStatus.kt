package com.omegadex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

const val NIVEAUX_STATUT = 5

/**
 * Statut d'entraînement d'un Pokémon en 5 rubis : les [niveau] premiers sont allumés.
 * [description] est lue d'un bloc par TalkBack (par exemple « Statut 2 sur 5 : capturé »).
 */
@Composable
fun GemStatus(niveau: Int, description: String, modifier: Modifier = Modifier, total: Int = NIVEAUX_STATUT) {
    val couleurs = OmegadexTheme.couleurs
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Canvas(Modifier.size(width = 18.dp, height = 16.dp)) {
                dessinerGemme(allumee = index < niveau, couleurs = couleurs)
            }
        }
    }
}

@ApercusClairSombre
@Composable
private fun GemStatusPreview() {
    OmegadexTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GemStatus(niveau = 0, description = "Statut 0 sur 5 : non vu")
            GemStatus(niveau = 2, description = "Statut 2 sur 5 : capturé")
            GemStatus(niveau = 5, description = "Statut 5 sur 5 : défi réussi")
        }
    }
}
