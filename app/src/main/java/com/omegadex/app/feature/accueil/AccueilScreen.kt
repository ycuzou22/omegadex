package com.omegadex.app.feature.accueil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.accueil.Jauge
import com.omegadex.app.domain.accueil.JaugesAccueil
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.feature.aventure.AventureUiState
import com.omegadex.app.feature.aventure.AventureViewModel
import com.omegadex.app.feature.aventure.CarteEtape
import com.omegadex.app.feature.aventure.CelebrationEtape
import com.omegadex.app.feature.aventure.EtapeValidee
import com.omegadex.app.feature.defis.nomTypeDefi
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.components.BoiteBadges
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

@Composable
fun AccueilScreen(
    onMesRuns: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccueilViewModel = hiltViewModel(),
    aventureViewModel: AventureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aventure by aventureViewModel.uiState.collectAsStateWithLifecycle()
    var celebration by remember { mutableStateOf<EtapeValidee?>(null) }
    // Les quêtes cochées depuis l'accueil valident l'étape comme dans l'Aventure, avec la même animation.
    LaunchedEffect(aventureViewModel) { aventureViewModel.evenements.collect { celebration = it } }
    celebration?.let { CelebrationEtape(it) { celebration = null } }
    Box(modifier.fillMaxSize()) {
        AccueilContenu(
            uiState = uiState,
            etape = aventure as? AventureUiState.EtapeEnCours,
            actions = ActionsAccueil(
                onChangerDeJoueur = viewModel::changerDeJoueur,
                onMesRuns = onMesRuns,
                onCocher = aventureViewModel::cocher,
                onRelever = viewModel::releverDefiDuJour,
            ),
        )
    }
}

@Composable
private fun AccueilContenu(
    uiState: AccueilUiState,
    etape: AventureUiState.EtapeEnCours?,
    actions: ActionsAccueil,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EnTeteAccueil(uiState, actions.onChangerDeJoueur, actions.onMesRuns)
            etape?.let { CarteEtape(it, actions.onCocher) }
            uiState.jauges?.let { RangeeJauges(it) }
            uiState.badges?.let { BoiteBadges(it, Modifier.fillMaxWidth()) }
            uiState.defiDuJour?.let { CarteDefiDuJour(it, actions.onRelever) }
            OmegaButton(
                stringResource(R.string.accueil_changer_run),
                onClick = actions.onMesRuns,
                variante = VarianteBouton.CONTOUR,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.accueil_version, uiState.version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** Carte « Défi du jour · type » de la maquette, avec le bouton « Relever ». */
@Composable
private fun CarteDefiDuJour(defi: DefiDuJour, onRelever: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.accueil_defi_du_jour, stringResource(nomTypeDefi(defi.type))).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(defi.enonce, style = MaterialTheme.typography.bodyMedium)
            }
            OmegaButton(stringResource(R.string.defis_relever), onClick = onRelever, variante = VarianteBouton.SOMBRE)
        }
    }
}

/** « Run … · difficulté » (vers Mes runs), salutation, et pastille du joueur pour changer de joueur. */
@Composable
private fun EnTeteAccueil(uiState: AccueilUiState, onChangerDeJoueur: () -> Unit, onMesRuns: () -> Unit) {
    val changerDeJoueur = stringResource(R.string.accueil_deconnexion)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            uiState.run?.let { run ->
                Text(
                    stringResource(R.string.accueil_run, run.nom, stringResource(titreDifficulte(run.difficulte))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(
                        onClickLabel = stringResource(R.string.accueil_changer_run),
                        role = Role.Button,
                        onClick = onMesRuns,
                    ),
                )
            }
            Text(
                stringResource(R.string.accueil_bonjour, uiState.joueur),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        Surface(
            onClick = onChangerDeJoueur,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = changerDeJoueur },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(uiState.joueur.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
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
                jauges = JaugesAccueil(Jauge(3, 13), Jauge(41, 211), Jauge(12, 40)),
                defiDuJour = DefiDuJour(
                    TypeDefi.EVOLUTION,
                    "Évolution sous le plafond",
                    "Faire évoluer Gobou en Flobio avant Voltère, sans dépasser le niveau 21",
                ),
            ),
            etape = null,
            actions = ActionsAccueil({}, {}, { _, _ -> }, {}),
        )
    }
}
