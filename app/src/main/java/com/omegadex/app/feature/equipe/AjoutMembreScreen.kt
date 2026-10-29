package com.omegadex.app.feature.equipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.ui.components.ChampTexte
import com.omegadex.app.ui.components.brosseGemmeEspece
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Ajout (ou échange) d'un membre : le Pokédex réduit aux espèces capturées hors équipe, le niveau,
 * puis un défi adapté au nouveau membre.
 */
@Composable
fun AjoutMembreScreen(
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AjoutMembreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val etat = uiState
    LaunchedEffect(etat) { if (etat is AjoutMembreUiState.Choix && etat.termine) onRetour() }
    when (etat) {
        AjoutMembreUiState.Chargement -> Box(modifier.fillMaxSize())
        AjoutMembreUiState.AucuneRun -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.equipe_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        is AjoutMembreUiState.Choix -> {
            ListeAjout(etat, onRetour, viewModel::rechercher, viewModel::choisir, modifier)
            etat.choisie?.let { espece ->
                DialogueNiveau(espece, etat.niveauParDefaut, onAjouter = viewModel::ajouter) {
                    viewModel.choisir(null)
                }
            }
            etat.defiPropose?.let { DialogueDefi(it, viewModel::releverDefi, viewModel::ignorerDefi) }
        }
    }
}

@Composable
private fun ListeAjout(
    etat: AjoutMembreUiState.Choix,
    onRetour: () -> Unit,
    onRecherche: (String) -> Unit,
    onChoisir: (EspeceAjoutable) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { TextButton(onClick = onRetour) { Text(stringResource(R.string.defis_retour)) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        etat.remplace?.let { stringResource(R.string.equipe_echanger_titre, it) }
                            ?: stringResource(R.string.equipe_ajouter_titre),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.equipe_ajouter_regle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (etat.equipePleine) {
                item {
                    Text(
                        stringResource(R.string.equipe_pleine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                item {
                    ChampTexte(
                        valeur = etat.recherche,
                        onValeur = onRecherche,
                        libelle = stringResource(R.string.pokedex_recherche),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (etat.especes.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.equipe_aucune_capture),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(etat.especes, key = { it.id }) { espece -> LigneEspece(espece) { onChoisir(espece) } }
            }
        }
    }
}

@Composable
private fun LigneEspece(espece: EspeceAjoutable, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 32.dp, height = 30.dp)
                    .clip(FormeGemme)
                    .background(brosseGemmeEspece(espece.types)),
            )
            Column(Modifier.weight(1f)) {
                Text(espece.nom, style = MaterialTheme.typography.titleSmall)
                Text(
                    espece.types.map { type ->
                        nomDuType(type)?.let { stringResource(it) } ?: type
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                espece.numeroHoenn?.let { stringResource(R.string.equipe_numero_hoenn, it) }
                    ?: stringResource(R.string.equipe_numero_national, espece.id),
                style = OmegadexTheme.chiffres.petit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = OmegadexTheme.couleurs.piste)
    }
}

@Composable
private fun DialogueNiveau(
    espece: EspeceAjoutable,
    niveauParDefaut: Int,
    onAjouter: (Int) -> Unit,
    onAnnuler: () -> Unit,
) {
    var niveau by rememberSaveable(espece.id) { mutableIntStateOf(niveauParDefaut) }
    val descriptionBaisser = stringResource(R.string.equipe_baisser, espece.nom)
    val descriptionMonter = stringResource(R.string.equipe_monter, espece.nom)
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.equipe_niveau_titre, espece.nom)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = { niveau-- },
                    enabled = niveau > 1,
                    modifier = Modifier.semantics { contentDescription = descriptionBaisser },
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(
                    stringResource(R.string.fiche_attaques_niveau, niveau),
                    style = OmegadexTheme.chiffres.moyen,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                TextButton(
                    onClick = { niveau++ },
                    enabled = niveau < NIVEAU_MAX_AJOUT,
                    modifier = Modifier.semantics { contentDescription = descriptionMonter },
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAjouter(niveau) }) { Text(stringResource(R.string.equipe_ajouter)) }
        },
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.equipe_annuler)) } },
    )
}

@Composable
private fun DialogueDefi(defi: DefiProposeVue, onRelever: () -> Unit, onPlusTard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onPlusTard,
        title = { Text(stringResource(R.string.equipe_defi_propose, defi.titre)) },
        text = { Text(defi.enonce) },
        confirmButton = { TextButton(onClick = onRelever) { Text(stringResource(R.string.defis_relever)) } },
        dismissButton = { TextButton(onClick = onPlusTard) { Text(stringResource(R.string.equipe_plus_tard)) } },
    )
}

private const val NIVEAU_MAX_AJOUT = 100
