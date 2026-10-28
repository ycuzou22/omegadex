package com.omegadex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.texteLisibleSur

/**
 * Puce arrondie d'un type Pokémon (ou d'une étiquette courte).
 * La couleur du texte est choisie automatiquement pour rester lisible sur le fond.
 */
@Composable
fun TypeChip(libelle: String, couleur: Color, modifier: Modifier = Modifier) {
    Text(
        text = libelle,
        modifier = modifier
            .background(couleur, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        color = texteLisibleSur(couleur),
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        ),
    )
}

/** Puce d'un type, à partir de son identifiant (« eau », « feu »…) et de son nom affiché. */
@Composable
fun TypeChip(type: String, libelle: String, modifier: Modifier = Modifier) {
    TypeChip(libelle = libelle, couleur = CouleursTypes.de(type), modifier = modifier)
}

@ApercusClairSombre
@Composable
private fun TypeChipPreview() {
    OmegadexTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeChip(type = "eau", libelle = "Eau")
            TypeChip(type = "feu", libelle = "Feu")
            TypeChip(type = "plante", libelle = "Plante")
            TypeChip(type = "electrik", libelle = "Électrik")
            TypeChip(type = "spectre", libelle = "Spectre")
        }
    }
}
