package com.omegadex.app.feature.comptes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.compte.ErreurInscription
import com.omegadex.app.domain.compte.LONGUEUR_MIN_MOT_DE_PASSE
import com.omegadex.app.ui.components.ChampTexte
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

@Composable
fun InscriptionScreen(
    onConnexion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InscriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InscriptionContenu(
        uiState = uiState,
        onIdentifiant = viewModel::saisirIdentifiant,
        onMotDePasse = viewModel::saisirMotDePasse,
        onConfirmation = viewModel::saisirConfirmation,
        onCreer = viewModel::creerCompte,
        onConnexion = onConnexion,
        modifier = modifier,
    )
}

@Composable
private fun InscriptionContenu(
    uiState: InscriptionUiState,
    onIdentifiant: (String) -> Unit,
    onMotDePasse: (String) -> Unit,
    onConfirmation: (String) -> Unit,
    onCreer: () -> Unit,
    onConnexion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val e = uiState.erreurs
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EnTeteCompte(stringResource(R.string.inscription_sous_titre), Modifier.fillMaxWidth().padding(top = 24.dp))
            ChampTexte(
                uiState.identifiant,
                onIdentifiant,
                stringResource(R.string.champ_identifiant),
                erreur = when {
                    ErreurInscription.IDENTIFIANT_PRIS in e -> stringResource(R.string.inscription_erreur_pris)
                    ErreurInscription.IDENTIFIANT_INVALIDE in e -> stringResource(
                        R.string.inscription_erreur_identifiant,
                    )
                    else -> null
                },
            )
            ChampTexte(
                uiState.motDePasse,
                onMotDePasse,
                stringResource(R.string.champ_mot_de_passe),
                secret = true,
                erreur = if (ErreurInscription.MOT_DE_PASSE_TROP_COURT in e) {
                    stringResource(R.string.inscription_erreur_court, LONGUEUR_MIN_MOT_DE_PASSE)
                } else {
                    null
                },
            )
            ChampTexte(
                uiState.confirmation,
                onConfirmation,
                stringResource(R.string.champ_confirmation),
                secret = true,
                actionClavier = ImeAction.Done,
                onValider = onCreer,
                erreur = if (ErreurInscription.CONFIRMATION_DIFFERENTE in
                    e
                ) {
                    stringResource(R.string.inscription_erreur_confirmation)
                } else {
                    null
                },
            )
            Text(
                stringResource(R.string.inscription_info, LONGUEUR_MIN_MOT_DE_PASSE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OmegaButton(
                texte = stringResource(
                    if (uiState.enCours) R.string.inscription_en_cours else R.string.inscription_bouton,
                ),
                onClick = onCreer,
                avecOmega = true,
                actif = !uiState.enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onConnexion, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.inscription_vers_connexion))
            }
        }
    }
}

@ApercusClairSombre
@Composable
private fun InscriptionPreview() {
    OmegadexTheme {
        InscriptionContenu(
            uiState = InscriptionUiState(identifiant = "yo", erreurs = setOf(ErreurInscription.IDENTIFIANT_INVALIDE)),
            onIdentifiant = {},
            onMotDePasse = {},
            onConfirmation = {},
            onCreer = {},
            onConnexion = {},
        )
    }
}
