package com.omegadex.app.feature.accueil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.accueil.Jauge
import com.omegadex.app.domain.accueil.JaugesAccueil
import com.omegadex.app.ui.components.ProgressGaugeRing
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

private const val POURCENT = 100

/**
 * Les trois jauges de la maquette E · Accueil : Aventure (anneau rubis et facette), Pokédex (magma et cercle),
 * Défis (roche et Ω). Valeurs en IBM Plex Mono ; chaque carte est lue d'un seul tenant par TalkBack.
 */
@Composable
fun RangeeJauges(jauges: JaugesAccueil, modifier: Modifier = Modifier) {
    val couleurs = OmegadexTheme.couleurs
    val aventure = stringResource(R.string.accueil_jauge_aventure)
    val pokedex = stringResource(R.string.accueil_jauge_pokedex)
    val defis = stringResource(R.string.accueil_jauge_defis)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val pourcentage = (jauges.aventure.progression * POURCENT).toInt()
        CarteJauge(
            libelle = aventure,
            valeur = stringResource(R.string.accueil_pourcentage, pourcentage),
            description = stringResource(R.string.accueil_jauge_description_pourcentage, aventure, pourcentage),
            jauge = jauges.aventure,
            couleur = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        CarteJauge(
            libelle = pokedex,
            valeur = fraction(jauges.pokedex),
            description = descriptionFraction(pokedex, jauges.pokedex),
            jauge = jauges.pokedex,
            couleur = couleurs.magma,
            modifier = Modifier.weight(1f),
            centre = { CercleCentral(couleurs.magma) },
        )
        CarteJauge(
            libelle = defis,
            valeur = fraction(jauges.defis),
            description = descriptionFraction(defis, jauges.defis),
            jauge = jauges.defis,
            couleur = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            centre = {
                Icon(
                    painterResource(R.drawable.ic_omega),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

@Composable
private fun fraction(jauge: Jauge) = stringResource(R.string.accueil_fraction, jauge.valeur, jauge.total)

@Composable
private fun descriptionFraction(libelle: String, jauge: Jauge) =
    stringResource(R.string.accueil_jauge_description_fraction, libelle, jauge.valeur, jauge.total)

@Composable
private fun CarteJauge(
    libelle: String,
    valeur: String,
    description: String,
    jauge: Jauge,
    couleur: Color,
    modifier: Modifier = Modifier,
    centre: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (centre == null) {
                ProgressGaugeRing(jauge.progression, description, couleur = couleur)
            } else {
                ProgressGaugeRing(jauge.progression, description, couleur = couleur, centre = centre)
            }
            Text(valeur, style = OmegadexTheme.chiffres.grand, maxLines = 1)
            Text(
                libelle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CercleCentral(couleur: Color) {
    Canvas(Modifier.size(12.dp)) {
        drawCircle(couleur, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(width = 2.5.dp.toPx()))
    }
}

@ApercusClairSombre
@Composable
private fun RangeeJaugesPreview() {
    OmegadexTheme {
        RangeeJauges(
            JaugesAccueil(Jauge(3, 13), Jauge(41, 211), Jauge(12, 40)),
            Modifier.padding(16.dp),
        )
    }
}
