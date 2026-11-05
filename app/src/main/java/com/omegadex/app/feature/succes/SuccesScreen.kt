package com.omegadex.app.feature.succes

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.succes.EtatSucces
import com.omegadex.app.domain.succes.Succes
import com.omegadex.app.ui.components.Gemme
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme
import java.text.DateFormat
import java.util.Date

/** Vitrine des succès de la run (T18.2) : une gemme par palier, allumée une fois débloquée. */
@Composable
fun SuccesScreen(
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VitrineSuccesViewModel = hiltViewModel(),
) {
    val succes by viewModel.succes.collectAsStateWithLifecycle()
    VitrineSucces(succes.orEmpty(), onRetour, modifier)
}

@Composable
internal fun VitrineSucces(succes: List<EtatSucces>, onRetour: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLONNES),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(COLONNES) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRetour) { Text(stringResource(R.string.succes_retour)) }
                    Text(
                        stringResource(R.string.succes_titre, succes.count { it.debloque }, succes.size),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.succes_sous_titre),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            items(succes, key = { it.succes.name }) { CaseSucces(it) }
        }
    }
}

@Composable
private fun CaseSucces(etat: EtatSucces) {
    val couleurs = OmegadexTheme.couleurs
    val titre = stringResource(titreSucces(etat.succes))
    val condition = pluralStringResource(conditionSucces(etat.succes), etat.succes.seuil, etat.succes.seuil)
    val etatTexte = etat.dateDeblocage?.let {
        stringResource(R.string.succes_debloque_le, DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)))
    } ?: stringResource(R.string.succes_avancement, etat.avancement, etat.succes.seuil)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (etat.debloque) 2.dp else 1.dp, if (etat.debloque) couleurs.gemme else couleurs.trait),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$titre. $condition. $etatTexte"
        },
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Gemme(etat.debloque, Modifier.size(width = 40.dp, height = 36.dp))
            Text(
                titre,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.run { if (etat.debloque) onSurface else onSurfaceVariant },
                minLines = 2,
                maxLines = 2,
            )
            Text(
                condition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            if (etat.debloque) {
                Text(etatTexte, style = MaterialTheme.typography.labelSmall, color = couleurs.gemme)
            } else {
                LinearProgressIndicator(
                    progress = { etat.avancement.toFloat() / etat.succes.seuil },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
                Text(etatTexte, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Carte de l'accueil : nombre de succès et gemmes des derniers débloqués, ouvre la vitrine. */
@Composable
fun CarteSuccesAccueil(
    onOuvrir: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VitrineSuccesViewModel = hiltViewModel(),
) {
    val succes by viewModel.succes.collectAsStateWithLifecycle()
    val liste = succes ?: return
    if (liste.isEmpty()) return
    val debloques = liste.count { it.debloque }
    Surface(
        onClick = onOuvrir,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.succes_carte_titre).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.succes_carte_compte, debloques, liste.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(GEMMES_CARTE) { Gemme(it < debloques) }
            }
        }
    }
}

private const val COLONNES = 3
private const val GEMMES_CARTE = 5

@ApercusClairSombre
@Composable
private fun VitrineSuccesPreview() {
    OmegadexTheme {
        VitrineSucces(
            Succes.entries.take(6).mapIndexed { i, s -> EtatSucces(s, if (i < 2) 0L else null, s.seuil / 2) },
            onRetour = {},
        )
    }
}
