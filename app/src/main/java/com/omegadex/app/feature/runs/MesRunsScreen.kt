package com.omegadex.app.feature.runs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.run.ResumeRun
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.feature.reglages.CarteReglagesSons
import com.omegadex.app.feature.sauvegarde.CarteSauvegarde
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.ProgressGauge
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.texteLisibleSur
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Liste des runs du compte avec leur progression ; toucher une run en fait la run active.
 * En bas, la carte Sauvegarde exporte ou importe la progression du compte.
 */
@Composable
fun MesRunsScreen(
    onRetour: () -> Unit,
    onNouvelleRun: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MesRunsViewModel = hiltViewModel(),
) {
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    MesRunsContenu(
        runs = runs,
        onChoisir = { viewModel.choisir(it, onRetour) },
        onNouvelleRun = onNouvelleRun,
        onRetour = onRetour,
        modifier = modifier,
        sauvegarde = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CarteSauvegarde()
                CarteReglagesSons()
            }
        },
    )
}

@Composable
private fun MesRunsContenu(
    runs: List<ResumeRun>,
    onChoisir: (Long) -> Unit,
    onNouvelleRun: () -> Unit,
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
    sauvegarde: @Composable () -> Unit = {},
) {
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.mes_runs_titre),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    TextButton(onClick = onRetour) { Text(stringResource(R.string.mes_runs_retour)) }
                }
            }
            items(runs, key = { it.run.id }) { resume -> CarteRun(resume) { onChoisir(resume.run.id) } }
            item {
                Spacer(Modifier.size(4.dp))
                OmegaButton(
                    texte = stringResource(R.string.mes_runs_nouvelle),
                    onClick = onNouvelleRun,
                    avecOmega = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Export et import de la progression du compte (T15).
            item { sauvegarde() }
        }
    }
}

@Composable
private fun CarteRun(resume: ResumeRun, onClick: () -> Unit) {
    val run = resume.run
    val progressionTexte = stringResource(R.string.mes_runs_progression, resume.etapesValidees, resume.totalEtapes)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (resume.active) BorderStroke(2.dp, OmegadexTheme.couleurs.magma) else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = resume.active }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GemmeStarter(Starter.de(run.starterId))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(run.nom, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(titreDifficulte(run.difficulte)) + " · " +
                            stringResource(R.string.mes_runs_debut, dateCourte(run.dateDebut)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (resume.active) {
                    Text(
                        stringResource(R.string.mes_runs_active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            resume.etapeEnCours?.let { etape ->
                Text(
                    stringResource(R.string.mes_runs_etape, etape.id, etape.titre),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ProgressGauge(
                progression = if (resume.totalEtapes == 0) 0f else resume.etapesValidees.toFloat() / resume.totalEtapes,
                description = progressionTexte,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                progressionTexte,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GemmeStarter(starter: Starter?) {
    val couleur = starter?.let { CouleursTypes.de(it.type) } ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(width = 36.dp, height = 34.dp).background(couleur, FormeGemme),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            starter?.let { stringResource(nomStarter(it)).take(1) }.orEmpty(),
            color = texteLisibleSur(couleur),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun dateCourte(epochMillis: Long): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

@ApercusClairSombre
@Composable
private fun MesRunsPreview() {
    val depart = Etape(0, "Départ", "Rival", null, 5, "Bourg-en-Vol")
    val voltere = Etape(3, "Voltère", "Voltère", "electrik", 24, "Lavandia")
    OmegadexTheme {
        MesRunsContenu(
            runs = listOf(
                ResumeRun(
                    Run(2, 1, "Nuzlocke Gobou", 258, Difficulte.HARDCORE, emptyList(), 0, null),
                    depart,
                    0,
                    13,
                    true,
                ),
                ResumeRun(
                    Run(1, 1, "Rubis Oméga", 252, Difficulte.NORMAL, emptyList(), 0, null),
                    voltere,
                    3,
                    13,
                    false,
                ),
            ),
            onChoisir = {},
            onNouvelleRun = {},
            onRetour = {},
        )
    }
}
