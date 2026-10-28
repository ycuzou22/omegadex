package com.omegadex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

/** Planche interactive de tous les composants de la DA, pour les vérifier d'un coup d'œil. */
@Composable
fun CatalogueComposants(modifier: Modifier = Modifier) {
    val quetes = remember { mutableStateListOf(true, false, false) }
    Column(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Boutons", style = MaterialTheme.typography.titleMedium)
        OmegaButton("Valider le défi", onClick = {}, avecOmega = true, modifier = Modifier.fillMaxWidth())
        OmegaButton("Ouvrir l'étape", onClick = {
        }, variante = VarianteBouton.SOMBRE, modifier = Modifier.fillMaxWidth())
        OmegaButton("Plus tard", onClick = {}, variante = VarianteBouton.CONTOUR, modifier = Modifier.fillMaxWidth())

        Text("Quêtes en joyaux", style = MaterialTheme.typography.titleMedium)
        listOf("Rejoindre Lavandia", "Battre le rival", "Vaincre Voltère").forEachIndexed { i, quete ->
            GemCheckbox(coche = quetes[i], onCocheChange = { quetes[i] = it }, libelle = quete)
        }

        Text("Types", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeChip(type = "eau", libelle = "Eau")
            TypeChip(type = "feu", libelle = "Feu")
            TypeChip(type = "plante", libelle = "Plante")
            TypeChip(type = "psy", libelle = "Psy")
        }

        Text("Jauges et statut", style = MaterialTheme.typography.titleMedium)
        ProgressGauge(progression = 0.6f, description = "Jauge à 60 %")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressGaugeRing(progression = 0.23f, description = "Aventure 23 %")
            Text("23 %", style = OmegadexTheme.chiffres.grand)
            ProgressGaugeRing(
                progression = 41f / 211f,
                description = "Pokédex 41 sur 211",
                couleur = OmegadexTheme.couleurs.magma,
            )
            Text("41/211", style = OmegadexTheme.chiffres.grand)
        }
        GemStatus(niveau = 2, description = "Statut 2 sur 5 : capturé")
    }
}

@ApercusClairSombre
@Composable
private fun CatalogueComposantsPreview() {
    OmegadexTheme { CatalogueComposants() }
}
