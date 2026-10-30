package com.omegadex.app.feature.equipe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.equipe.Affinite
import com.omegadex.app.ui.components.IconeType
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.cleType
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Carte « Suggestions de rotation » (F10) : captures jamais utilisées, adaptées à l'adversaire de l'étape.
 * « Ajouter » les fait entrer au niveau du plafond dans le premier emplacement libre ; équipe pleine,
 * « Échanger » demande quel membre leur laisse sa place.
 */
@Composable
internal fun CarteSuggestions(equipe: EquipeUiState.Equipe, onAjouter: (SuggestionVue, Int?) -> Unit) {
    val pleine = equipe.membres.size >= TAILLE_EQUIPE
    var aEchanger by rememberSaveable { mutableStateOf<Int?>(null) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 6.dp)) {
            Text(
                stringResource(R.string.equipe_suggestions_titre),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.equipe_suggestions_detail, equipe.suggestions.first().niveau),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp, end = 8.dp),
            )
            equipe.suggestions.forEach { suggestion ->
                LigneSuggestion(suggestion, equipe.typeAdverse, pleine) {
                    if (pleine) aEchanger = suggestion.especeId else onAjouter(suggestion, null)
                }
            }
        }
    }
    val suggestion = equipe.suggestions.firstOrNull { it.especeId == aEchanger }
    if (suggestion != null) {
        DialogueEchange(suggestion, equipe.emplacements, onFermer = { aEchanger = null }) { emplacement ->
            aEchanger = null
            onAjouter(suggestion, emplacement)
        }
    }
}

@Composable
private fun LigneSuggestion(suggestion: SuggestionVue, typeAdverse: String?, pleine: Boolean, onAjouter: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpriteEspece(suggestion.especeId, decouverte = 1f, taille = 44.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            suggestion.types.forEach { type ->
                IconeType(type, taille = 20.dp, description = nomType(type))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.nom,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                texteAffinite(suggestion.affinite, typeAdverse),
                style = MaterialTheme.typography.labelMedium,
                color = couleurAffinite(suggestion.affinite),
            )
        }
        TextButton(onClick = onAjouter) {
            Text(
                stringResource(
                    if (pleine) R.string.equipe_suggestion_echanger else R.string.equipe_suggestion_ajouter,
                ),
            )
        }
    }
}

@Composable
private fun DialogueEchange(
    suggestion: SuggestionVue,
    emplacements: List<EmplacementVue>,
    onFermer: () -> Unit,
    onChoisir: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.equipe_suggestion_echange_titre, suggestion.nom)) },
        text = {
            Column {
                emplacements.forEach { emplacement ->
                    val membre = emplacement.membre ?: return@forEach
                    TextButton(onClick = { onChoisir(emplacement.numero) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                R.string.equipe_suggestion_echange_membre,
                                emplacement.numero,
                                membre.surnom ?: membre.nom,
                                membre.niveau,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onFermer) { Text(stringResource(R.string.equipe_annuler)) } },
    )
}

@Composable
private fun nomType(type: String): String = nomDuType(cleType(type))?.let { stringResource(it) } ?: type

@Composable
private fun texteAffinite(affinite: Affinite, typeAdverse: String?): String {
    val type = typeAdverse?.let { nomType(it) } ?: return stringResource(R.string.equipe_suggestion_jamais_utilise)
    return when (affinite) {
        Affinite.AVANTAGE -> stringResource(R.string.equipe_suggestion_avantage, type)
        Affinite.RESISTANCE -> stringResource(R.string.equipe_suggestion_resistance, type)
        Affinite.NEUTRE -> stringResource(R.string.equipe_suggestion_neutre, type)
        Affinite.FAIBLESSE -> stringResource(R.string.equipe_suggestion_faiblesse, type)
    }
}

@Composable
private fun couleurAffinite(affinite: Affinite): Color = when (affinite) {
    Affinite.AVANTAGE -> MaterialTheme.colorScheme.primary
    Affinite.FAIBLESSE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
