package com.omegadex.app.feature.sauvegarde

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

/** Types proposés par le sélecteur à l'import : certains gestionnaires présentent le JSON comme du binaire. */
private val TYPES_IMPORT = arrayOf("application/json", "application/octet-stream", "text/plain")
private const val TYPE_EXPORT = "application/json"

/**
 * Carte « Sauvegarde » de l'écran Mes runs : exporter la progression du compte dans un fichier JSON,
 * ou importer une sauvegarde dans le compte connecté, avec le sélecteur de fichiers Android.
 */
@Composable
fun CarteSauvegarde(modifier: Modifier = Modifier, viewModel: SauvegardeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sons = LocalSons.current
    LaunchedEffect(uiState.message) {
        uiState.message?.let { sons.jouer(if (it.estUneErreur()) Son.ERREUR else Son.CONFIRMATION) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TYPE_EXPORT)) { uri ->
        uri?.let { viewModel.exporter(it.toString()) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importer(it.toString()) }
    }
    CarteSauvegardeContenu(
        uiState = uiState,
        onExporter = {
            viewModel.effacerMessage()
            exporter.launch(viewModel.nomFichierPropose())
        },
        onImporter = {
            viewModel.effacerMessage()
            importer.launch(TYPES_IMPORT)
        },
        modifier = modifier,
    )
}

@Composable
private fun CarteSauvegardeContenu(
    uiState: SauvegardeUiState,
    onExporter: () -> Unit,
    onImporter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.sauvegarde_titre),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.sauvegarde_explication),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OmegaButton(
                texte = stringResource(R.string.sauvegarde_exporter),
                onClick = onExporter,
                variante = VarianteBouton.SOMBRE,
                actif = !uiState.enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            OmegaButton(
                texte = stringResource(R.string.sauvegarde_importer),
                onClick = onImporter,
                variante = VarianteBouton.CONTOUR,
                actif = !uiState.enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.message?.let { message ->
                Text(
                    texteMessage(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.estUneErreur()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

private fun MessageSauvegarde.estUneErreur() =
    this !is MessageSauvegarde.Exportee && this !is MessageSauvegarde.Importee

@Composable
private fun texteMessage(message: MessageSauvegarde): String = when (message) {
    is MessageSauvegarde.Exportee ->
        pluralStringResource(R.plurals.sauvegarde_exportee, message.nombreRuns, message.nombreRuns)
    is MessageSauvegarde.Importee -> {
        val importees = if (message.runsImportees == 0) {
            stringResource(R.string.sauvegarde_rien_importe)
        } else {
            pluralStringResource(R.plurals.sauvegarde_importee, message.runsImportees, message.runsImportees)
        }
        if (message.runsIgnorees == 0) {
            importees
        } else {
            importees + " " + pluralStringResource(
                R.plurals.sauvegarde_ignorees,
                message.runsIgnorees,
                message.runsIgnorees,
            )
        }
    }
    is MessageSauvegarde.FichierInvalide -> stringResource(R.string.sauvegarde_fichier_invalide, message.raison)
    MessageSauvegarde.EchecEcriture -> stringResource(R.string.sauvegarde_echec_ecriture)
    MessageSauvegarde.EchecLecture -> stringResource(R.string.sauvegarde_echec_lecture)
    MessageSauvegarde.NonConnecte -> stringResource(R.string.sauvegarde_non_connecte)
}

@ApercusClairSombre
@Composable
private fun CarteSauvegardePreview() {
    OmegadexTheme {
        CarteSauvegardeContenu(
            uiState = SauvegardeUiState(message = MessageSauvegarde.Importee(runsImportees = 2, runsIgnorees = 1)),
            onExporter = {},
            onImporter = {},
        )
    }
}
