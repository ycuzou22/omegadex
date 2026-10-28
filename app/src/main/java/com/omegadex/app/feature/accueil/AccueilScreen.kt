package com.omegadex.app.feature.accueil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

@Composable
fun AccueilScreen(
    onMesRuns: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccueilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccueilContenu(
        uiState = uiState,
        onChangerDeJoueur = viewModel::changerDeJoueur,
        onMesRuns = onMesRuns,
        modifier = modifier,
    )
}

@Composable
private fun AccueilContenu(
    uiState: AccueilUiState,
    onChangerDeJoueur: () -> Unit,
    onMesRuns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        uiState.run?.let { run ->
            Text(
                stringResource(R.string.accueil_run, run.nom, stringResource(titreDifficulte(run.difficulte))),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(stringResource(R.string.accueil_bonjour, uiState.joueur), style = MaterialTheme.typography.headlineMedium)
        uiState.run?.let { run ->
            Text(
                stringResource(R.string.accueil_etape, run.etapeNumero, run.totalEtapes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(run.etapeTitre, style = MaterialTheme.typography.bodyLarge)
            OmegaButton(
                stringResource(R.string.accueil_changer_run),
                onClick = onMesRuns,
                variante = VarianteBouton.SOMBRE,
            )
        }
        Text(
            stringResource(R.string.accueil_version, uiState.version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OmegaButton(
            stringResource(R.string.accueil_deconnexion),
            onClick = onChangerDeJoueur,
            variante = VarianteBouton.CONTOUR,
        )
    }
}

@ApercusClairSombre
@Composable
private fun AccueilPreview() {
    OmegadexTheme {
        AccueilContenu(
            uiState = AccueilUiState(
                version = "0.2.0",
                joueur = "Yamao",
                run = RunAccueil("Rubis Oméga", Difficulte.DIFFICILE, 3, "Voltère", 13),
            ),
            onChangerDeJoueur = {},
            onMesRuns = {},
        )
    }
}
