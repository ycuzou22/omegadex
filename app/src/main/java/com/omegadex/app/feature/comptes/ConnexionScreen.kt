package com.omegadex.app.feature.comptes

import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Utilisateur
import com.omegadex.app.ui.components.ChampTexte
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

@Composable
fun ConnexionScreen(
    onInscription: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnexionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnexionContenu(
        uiState = uiState,
        onCompte = viewModel::choisirCompte,
        onIdentifiant = viewModel::saisirIdentifiant,
        onMotDePasse = viewModel::saisirMotDePasse,
        onConnexion = viewModel::seConnecter,
        onInscription = onInscription,
        modifier = modifier,
    )
}

@Composable
private fun ConnexionContenu(
    uiState: ConnexionUiState,
    onCompte: (String) -> Unit,
    onIdentifiant: (String) -> Unit,
    onMotDePasse: (String) -> Unit,
    onConnexion: () -> Unit,
    onInscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            EnTeteCompte(stringResource(R.string.connexion_sous_titre), Modifier.fillMaxWidth().padding(top = 24.dp))
            if (uiState.comptes.isNotEmpty()) {
                Text(
                    stringResource(R.string.connexion_comptes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                uiState.comptes.forEach { compte ->
                    LigneCompte(
                        compte,
                        choisi = compte.identifiant.equals(
                            uiState.identifiant.trim(),
                            ignoreCase = true,
                        ),
                        onClick = {
                            onCompte(compte.identifiant)
                        },
                    )
                }
            }
            ChampTexte(uiState.identifiant, onIdentifiant, stringResource(R.string.champ_identifiant))
            ChampTexte(
                uiState.motDePasse,
                onMotDePasse,
                stringResource(R.string.champ_mot_de_passe),
                secret = true,
                actionClavier = ImeAction.Done,
                onValider = onConnexion,
            )
            uiState.erreur?.let { erreur ->
                Text(
                    text = when (erreur) {
                        ErreurConnexion.IdentifiantsIncorrects -> stringResource(R.string.connexion_erreur_identifiants)
                        ErreurConnexion.ChampsVides -> stringResource(R.string.connexion_erreur_vide)
                        is ErreurConnexion.Attente -> stringResource(R.string.connexion_erreur_attente, erreur.secondes)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            OmegaButton(
                texte = stringResource(if (uiState.enCours) R.string.connexion_en_cours else R.string.connexion_bouton),
                onClick = onConnexion,
                avecOmega = true,
                actif = !uiState.enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onInscription, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.connexion_vers_inscription))
            }
        }
    }
}

@Composable
private fun LigneCompte(compte: Utilisateur, choisi: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (choisi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 32.dp, height = 30.dp)
                    .background(MaterialTheme.colorScheme.primary, FormeGemme),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    compte.identifiant.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(compte.identifiant, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@ApercusClairSombre
@Composable
private fun ConnexionPreview() {
    OmegadexTheme {
        ConnexionContenu(
            uiState = ConnexionUiState(
                comptes = listOf(Utilisateur(1, "Yamao", 0), Utilisateur(2, "Invité", 0)),
                identifiant = "Yamao",
                erreur = ErreurConnexion.IdentifiantsIncorrects,
            ),
            onCompte = {},
            onIdentifiant = {},
            onMotDePasse = {},
            onConnexion = {},
            onInscription = {},
        )
    }
}
