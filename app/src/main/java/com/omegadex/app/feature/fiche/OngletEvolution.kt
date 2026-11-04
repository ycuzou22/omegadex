package com.omegadex.app.feature.fiche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.brosseGemmeEspece
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Onglet Évolution : chaîne complète avec la condition de chaque palier. Le stade où en est le joueur
 * (son équipe, sinon la capture la plus évoluée) est en rubis, les autres sont éteints ; l'espèce
 * de la fiche est entourée. Toucher un stade ouvre sa fiche.
 */
@Composable
internal fun OngletEvolution(fiche: FichePokemonUiState.Fiche, onEspece: (Int) -> Unit) {
    val chaine = fiche.evolution
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            chaine.paliers.isEmpty() -> Text(
                stringResource(R.string.fiche_evolution_aucune),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            chaine.lineaire -> ChaineEnLigne(chaine, fiche.espece.id, onEspece)
            else -> ChaineParStades(chaine, fiche.espece.id, onEspece)
        }
        if (chaine.paliers.isNotEmpty() && chaine.stadeActuel == null) {
            Text(
                stringResource(R.string.fiche_evolution_pas_capture),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Maquette : « Gobou → niv. 16 → Flobio → niv. 36 → Laggron ». */
@Composable
private fun ChaineEnLigne(chaine: ChaineEvolution, ficheId: Int, onEspece: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chaine.stades.flatten().forEachIndexed { index, stade ->
            if (index > 0) {
                chaine.palierVers(stade.id)?.let { palier ->
                    Text(
                        "→ " + texteCondition(palier.condition, palier.valeur),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    )
                }
            }
            CarteStade(stade, actuel = stade.id == chaine.stadeActuel, fiche = stade.id == ficheId) {
                onEspece(stade.id)
            }
        }
    }
}

/** Chaîne à branches ou longue : un stade par ligne, chaque espèce avec la condition qui y mène. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChaineParStades(chaine: ChaineEvolution, ficheId: Int, onEspece: (Int) -> Unit) {
    chaine.stades.forEachIndexed { index, stades ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.fiche_evolution_stade, index + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stades.forEach { stade ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CarteStade(stade, actuel = stade.id == chaine.stadeActuel, fiche = stade.id == ficheId) {
                            onEspece(stade.id)
                        }
                        chaine.palierVers(stade.id)?.let { palier ->
                            Text(
                                texteCondition(palier.condition, palier.valeur),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(96.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarteStade(stade: StadeEvolution, actuel: Boolean, fiche: Boolean, onClick: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val etat = if (actuel) stringResource(R.string.fiche_evolution_stade_actuel) else ""
    Column(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = etat }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Le sprite de l'espèce (en ombre tant qu'elle n'est pas vue) est posé sur sa gemme, comme au Pokédex.
        Box(Modifier.size(width = 76.dp, height = 72.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(width = 64.dp, height = 60.dp)
                    .then(if (fiche) Modifier.border(BorderStroke(2.dp, couleurs.magma), FormeGemme) else Modifier)
                    .padding(if (fiche) 3.dp else 0.dp)
                    .clip(FormeGemme)
                    .then(
                        if (actuel) {
                            Modifier.background(brosseGemmeEspece(stade.types))
                        } else {
                            Modifier.background(couleurs.piste)
                        },
                    ),
            )
            SpriteEspece(especeId = stade.id, decouverte = if (stade.vue) 1f else 0f, taille = 64.dp)
        }
        Text(stade.nom, style = MaterialTheme.typography.titleSmall)
    }
}

/** Condition d'un palier : « niv. 16 », « Pierre Eau », « Échange », « Bonheur », lieu ou condition spéciale. */
@Composable
internal fun texteCondition(condition: ConditionEvolution, valeur: String?): String = when (condition) {
    ConditionEvolution.NIVEAU -> stringResource(R.string.evolution_niveau, valeur.orEmpty())
    ConditionEvolution.PIERRE -> valeur ?: stringResource(R.string.evolution_pierre)
    ConditionEvolution.BONHEUR -> listOfNotNull(stringResource(R.string.evolution_bonheur), valeur).joinToString(" · ")
    ConditionEvolution.ECHANGE -> listOfNotNull(stringResource(R.string.evolution_echange), valeur).joinToString(" · ")
    ConditionEvolution.OBJET -> valeur ?: stringResource(R.string.evolution_objet)
    ConditionEvolution.LIEU -> stringResource(R.string.evolution_lieu, valeur.orEmpty())
    ConditionEvolution.AUTRE -> valeur ?: stringResource(R.string.evolution_autre)
}
