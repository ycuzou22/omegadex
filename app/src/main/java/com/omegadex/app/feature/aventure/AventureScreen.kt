package com.omegadex.app.feature.aventure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.ui.components.CarteRubis
import com.omegadex.app.ui.components.GemCheckbox
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.PlexMono
import com.omegadex.app.ui.theme.RubisPale

/** Aventure : seule l'étape en cours de la run active est visible, avec ses quêtes. */
@Composable
fun AventureScreen(
    onLieu: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AventureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val annonces = remember { SnackbarHostState() }
    val ressources = LocalResources.current
    // En attendant l'animation « Étape validée » (T14.1), la validation est annoncée par un message.
    LaunchedEffect(viewModel) {
        viewModel.evenements.collect { evenement ->
            annonces.showSnackbar(
                evenement.suivante?.let { ressources.getString(R.string.aventure_etape_validee, evenement.titre, it) }
                    ?: ressources.getString(R.string.aventure_derniere_validee, evenement.titre),
            )
        }
    }
    Box(modifier.fillMaxSize()) {
        AventureContenu(uiState = uiState, onCocher = viewModel::cocher, onLieu = onLieu)
        SnackbarHost(annonces, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun AventureContenu(
    uiState: AventureUiState,
    onCocher: (Int, Boolean) -> Unit,
    onLieu: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AventureUiState.Chargement -> Box(modifier.fillMaxSize())
        AventureUiState.AucuneRun -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.aventure_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        AventureUiState.Terminee -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.aventure_terminee), style = MaterialTheme.typography.titleLarge)
        }
        is AventureUiState.EtapeEnCours -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { CarteEtape(uiState, onCocher) }
                if (uiState.lieux.isNotEmpty()) {
                    item { LieuxEtape(uiState.lieux, onLieu) }
                }
                if (uiState.annexes.isNotEmpty()) {
                    item { EnTeteAnnexes(uiState.annexes) }
                    items(uiState.annexes, key = { it.id }) { quete ->
                        LigneQuete(quete) { onCocher(quete.id, it) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CarteEtape(
    etape: AventureUiState.EtapeEnCours,
    onCocher: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CarteRubis(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aventure_etape, etape.numero, etape.total).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RubisPale,
                modifier = Modifier.weight(1f),
            )
            etape.typeDominant?.let { type ->
                TypeChip(type, nomDuType(type)?.let { stringResource(it) } ?: type)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                etape.titre,
                style = MaterialTheme.typography.displaySmall,
                color = Blanc,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.aventure_plafond, etape.lieuPrincipal))
                    append(" ")
                    withStyle(SpanStyle(fontFamily = PlexMono, color = Blanc)) {
                        append(stringResource(R.string.aventure_niveau, etape.plafond))
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = RubisPale,
            )
            if (etape.adversaire != etape.titre) {
                Text(
                    stringResource(R.string.aventure_adversaire, etape.adversaire),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RubisPale,
                )
            }
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.aventure_principales),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                etape.principales.forEach { quete ->
                    LigneQuete(quete) { onCocher(quete.id, it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LieuxEtape(lieux: List<LieuAffiche>, onLieu: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.aventure_lieux),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lieux.forEach { lieu ->
                Surface(
                    onClick = { onLieu(lieu.id) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
                ) {
                    Text(
                        lieu.nom,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnTeteAnnexes(annexes: List<QueteAffichee>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aventure_annexes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                stringResource(R.string.aventure_compteur, annexes.count { it.cochee }, annexes.size),
                style = OmegadexTheme.chiffres.moyen,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.aventure_annexes_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LigneQuete(quete: QueteAffichee, onCocher: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        GemCheckbox(coche = quete.cochee, onCocheChange = onCocher, libelle = quete.description)
        quete.recompense?.let {
            Text(
                stringResource(R.string.aventure_recompense, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp),
            )
        }
    }
}

@ApercusClairSombre
@Composable
private fun AventurePreview() {
    OmegadexTheme {
        AventureContenu(
            uiState = AventureUiState.EtapeEnCours(
                runId = 1,
                numero = 3,
                total = 13,
                titre = "Voltère",
                adversaire = "Voltère",
                typeDominant = "electrik",
                plafond = 21,
                lieuPrincipal = "Lavandia",
                principales = listOf(
                    QueteAffichee(1, "Rejoindre Lavandia par la Route 110", null, true),
                    QueteAffichee(2, "Battre le rival sur la Route 110", null, false),
                    QueteAffichee(3, "Vaincre Voltère", "Badge Dynamo", false),
                ),
                annexes = listOf(QueteAffichee(4, "Capturer Dynavolt (Route 110)", null, false)),
                lieux = listOf(LieuAffiche(1, "Lavandia"), LieuAffiche(2, "Route 110")),
            ),
            onCocher = { _, _ -> },
            onLieu = {},
        )
    }
}
