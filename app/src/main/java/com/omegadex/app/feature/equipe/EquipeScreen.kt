package com.omegadex.app.feature.equipe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.animations.EspeceAnimee
import com.omegadex.app.ui.animations.EvolutionPleinEcran
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.brosseGemmeEspece
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Écran Équipe : 6 emplacements. Chaque membre affiche son niveau comparé au plafond de l'étape en cours
 * (alerte magma au-dessus) et son défi actif ; − et + changent le niveau et proposent l'évolution atteinte.
 */
@Composable
fun EquipeScreen(
    onEspece: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EquipeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val proposee by viewModel.evolutionProposee.collectAsStateWithLifecycle()
    val enCours by viewModel.evolutionEnCours.collectAsStateWithLifecycle()
    EquipeContenu(
        uiState,
        ActionsEquipe(onEspece = onEspece, onNiveau = viewModel::changerNiveau),
        modifier,
    )
    proposee?.let {
        DialogueEvolution(it, onEvoluer = viewModel::faireEvoluer, onPlusTard = viewModel::reporterEvolution)
    }
    enCours?.let {
        EvolutionPleinEcran(
            avant = EspeceAnimee(it.avant.nom, it.avant.types),
            apres = EspeceAnimee(it.apres.nom, it.apres.types),
            onFin = viewModel::finirAnimation,
        )
    }
}

@Composable
internal fun EquipeContenu(uiState: EquipeUiState, actions: ActionsEquipe, modifier: Modifier = Modifier) {
    when (uiState) {
        EquipeUiState.Chargement -> Box(modifier.fillMaxSize())
        EquipeUiState.AucuneRun -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.equipe_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        is EquipeUiState.Equipe -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { EnTete(uiState) }
                if (uiState.auDessusDuPlafond > 0) item { AlertePlafond(uiState.auDessusDuPlafond) }
                items(uiState.emplacements, key = { it.numero }) { emplacement ->
                    val membre = emplacement.membre
                    if (membre == null) {
                        EmplacementLibre(emplacement.numero)
                    } else {
                        CarteMembre(emplacement.numero, membre, uiState.plafond, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnTete(equipe: EquipeUiState.Equipe) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.equipe_titre_compte, equipe.membres.size, TAILLE_EQUIPE),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        val plafond = equipe.plafond
        Text(
            if (plafond != null && equipe.etapeTitre != null) {
                stringResource(R.string.equipe_plafond_etape, equipe.etapeTitre, plafond)
            } else {
                stringResource(R.string.equipe_sans_plafond)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(
                if (equipe.difficulte == Difficulte.HARDCORE) R.string.equipe_regle_hardcore else R.string.equipe_regle,
                stringResource(titreDifficulte(equipe.difficulte)),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertePlafond(nombre: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            pluralStringResource(R.plurals.equipe_alerte_plafond, nombre, nombre),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CarteMembre(numero: Int, membre: MembreVue, plafond: Int?, actions: ActionsEquipe) {
    val couleurs = OmegadexTheme.couleurs
    val ecart = membre.ecartPlafond
    val auDessus = ecart != null && ecart > 0
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (auDessus) 2.dp else 1.dp, if (auDessus) couleurs.magma else couleurs.trait),
        onClick = { actions.onEspece(membre.especeId) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 44.dp, height = 41.dp)
                    .clip(FormeGemme)
                    .background(brosseGemmeEspece(membre.types)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    membre.surnom?.let { stringResource(R.string.equipe_surnom, it, membre.nom) } ?: membre.nom,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    membre.types.forEach { type ->
                        TypeChip(type = type, libelle = nomDuType(type)?.let { stringResource(it) } ?: type)
                    }
                }
                Text(
                    membre.defiActif?.let { stringResource(R.string.equipe_defi_actif, it) }
                        ?: stringResource(R.string.equipe_aucun_defi, numero),
                    style = MaterialTheme.typography.bodySmall,
                    color = membre.defiActif?.let { couleurs.magma } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ReglageNiveau(numero, membre, plafond, actions.onNiveau)
        }
    }
}

@Composable
private fun ReglageNiveau(numero: Int, membre: MembreVue, plafond: Int?, onNiveau: (Int, Int) -> Unit) {
    val ecart = membre.ecartPlafond
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoutonNiveau("−", stringResource(R.string.equipe_baisser, membre.nom), membre.niveau > 1) {
                onNiveau(numero, -1)
            }
            Text(
                stringResource(R.string.fiche_attaques_niveau, membre.niveau),
                style = OmegadexTheme.chiffres.moyen,
            )
            BoutonNiveau("+", stringResource(R.string.equipe_monter, membre.nom), membre.niveau < NIVEAU_MAX) {
                onNiveau(numero, 1)
            }
        }
        if (plafond != null && ecart != null) {
            Text(
                when {
                    ecart > 0 -> stringResource(R.string.equipe_au_dessus, ecart)
                    ecart == 0 -> stringResource(R.string.equipe_au_plafond)
                    else -> stringResource(R.string.equipe_sous_plafond, plafond)
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    ecart > 0 -> MaterialTheme.colorScheme.error
                    ecart == 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun BoutonNiveau(symbole: String, description: String, actif: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = actif,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbole, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EmplacementLibre(numero: Int) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(R.string.equipe_emplacement_libre, numero),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun DialogueEvolution(proposee: EvolutionProposee, onEvoluer: (Int) -> Unit, onPlusTard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onPlusTard,
        title = { Text(stringResource(R.string.equipe_evolution_titre, proposee.avant.nom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                proposee.cibles.forEach { cible ->
                    Text(
                        listOfNotNull(
                            stringResource(R.string.equipe_evolution_cible, cible.espece.nom, cible.niveauRequis),
                            cible.precision,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                proposee.cibles.forEach { cible ->
                    TextButton(onClick = { onEvoluer(cible.espece.id) }) {
                        Text(stringResource(R.string.equipe_evoluer_en, cible.espece.nom))
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onPlusTard) { Text(stringResource(R.string.equipe_plus_tard)) } },
    )
}

private const val NIVEAU_MAX = 100
