package com.omegadex.app.feature.accueil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.ui.components.EcranEnConstruction
import com.omegadex.app.ui.theme.OmegadexTheme

@Composable
fun AccueilScreen(modifier: Modifier = Modifier, viewModel: AccueilViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccueilContenu(uiState = uiState, modifier = modifier)
}

@Composable
private fun AccueilContenu(uiState: AccueilUiState, modifier: Modifier = Modifier) {
    EcranEnConstruction(
        titre = stringResource(R.string.app_nom),
        detail = stringResource(R.string.accueil_version, uiState.version),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AccueilPreview() {
    OmegadexTheme {
        AccueilContenu(uiState = AccueilUiState(version = "0.1.0"))
    }
}
