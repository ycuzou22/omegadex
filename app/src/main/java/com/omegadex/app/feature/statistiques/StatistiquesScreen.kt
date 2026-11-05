package com.omegadex.app.feature.statistiques

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.statistiques.BilanDefis
import com.omegadex.app.domain.statistiques.DefisEspece
import com.omegadex.app.domain.statistiques.DureeEtape
import com.omegadex.app.domain.statistiques.StatistiquesRun
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme
import kotlin.math.roundToInt

/** Écran Statistiques (T20.2) : temps par étape, bilan des défis et Pokémon les plus sollicités. */
@Composable
fun StatistiquesScreen(
    onRetour: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatistiquesViewModel = hiltViewModel(),
) {
    val statistiques by viewModel.statistiques.collectAsStateWithLifecycle()
    ContenuStatistiques(statistiques ?: StatistiquesRun(), onRetour, modifier)
}

@Composable
internal fun ContenuStatistiques(stats: StatistiquesRun, onRetour: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onRetour) { Text(stringResource(R.string.stats_retour)) }
            Text(
                stringResource(R.string.stats_titre),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            CarteStats(stringResource(R.string.stats_defis)) { BilanDefisVue(stats.bilan) }
            CarteStats(stringResource(R.string.stats_temps)) { DureesVue(stats) }
            CarteStats(stringResource(R.string.stats_especes)) { EspecesVue(stats.especes) }
        }
    }
}

@Composable
private fun CarteStats(titre: String, contenu: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                titre.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            contenu()
        }
    }
}

/** Taux de réussite en grand, puis une barre partagée réussis / ratés. */
@Composable
private fun BilanDefisVue(bilan: BilanDefis) {
    val couleurs = OmegadexTheme.couleurs
    val taux = bilan.tauxReussite
    Text(
        taux?.let { stringResource(R.string.stats_taux, (it * CENT).roundToInt()) }
            ?: stringResource(R.string.stats_taux_vide),
        style = OmegadexTheme.chiffres.moyen,
    )
    if (bilan.termines > 0) {
        val description = stringResource(R.string.stats_bilan, bilan.reussis, bilan.rates)
        Row(
            Modifier
                .fillMaxWidth()
                .height(HAUTEUR_BARRE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            if (bilan.reussis > 0) {
                Box(Modifier.weight(bilan.reussis.toFloat()).fillMaxHeight().background(couleurs.gemme))
            }
            if (bilan.rates > 0) {
                Box(Modifier.weight(bilan.rates.toFloat()).fillMaxHeight().background(couleurs.magma))
            }
        }
    }
    Text(
        stringResource(R.string.stats_bilan_detail, bilan.reussis, bilan.rates, bilan.actifs),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Une barre par étape validée, proportionnelle à sa durée ; la plus longue fait toute la largeur. */
@Composable
private fun DureesVue(stats: StatistiquesRun) {
    if (stats.durees.isEmpty()) {
        TexteVide(stringResource(R.string.stats_temps_vide))
        return
    }
    Text(
        stringResource(R.string.stats_temps_total, duree(stats.dureeTotaleMs)),
        style = MaterialTheme.typography.bodyMedium,
    )
    val max = stats.etapeLaPlusLongue?.dureeMs?.coerceAtLeast(1) ?: 1
    stats.durees.forEach { BarreDuree(it, it.dureeMs.toFloat() / max) }
}

@Composable
private fun BarreDuree(etape: DureeEtape, part: Float) {
    val couleurs = OmegadexTheme.couleurs
    val texte = duree(etape.dureeMs)
    Column(
        Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                etape.titre,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                texte,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(HAUTEUR_BARRE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(part.coerceIn(PART_MIN, 1f))
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(couleurs.gemmeClaire, couleurs.gemme)), CircleShape),
            )
        }
    }
}

/** Les espèces qui ont relevé le plus de défis, avec leurs réussites. */
@Composable
private fun EspecesVue(especes: List<DefisEspece>) {
    if (especes.isEmpty()) {
        TexteVide(stringResource(R.string.stats_especes_vide))
        return
    }
    val max = especes.maxOf { it.total }.coerceAtLeast(1)
    especes.forEachIndexed { rang, espece ->
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("${rang + 1}", style = OmegadexTheme.chiffres.moyen, modifier = Modifier.width(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(espece.nom, style = MaterialTheme.typography.titleSmall)
                Box(
                    Modifier
                        .fillMaxWidth(espece.total.toFloat() / max)
                        .height(HAUTEUR_BARRE_FINE)
                        .clip(CircleShape)
                        .background(OmegadexTheme.couleurs.magma),
                )
            }
            Text(
                pluralStringResource(R.plurals.stats_defis_espece, espece.total, espece.total, espece.reussis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TexteVide(texte: String) {
    Text(texte, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** « 2 j 5 h », « 3 h 20 min », « 12 min » ou « moins d'une minute ». */
@Composable
private fun duree(ms: Long): String {
    val minutes = ms / MS_PAR_MINUTE
    val heures = minutes / MINUTES_PAR_HEURE
    val jours = heures / HEURES_PAR_JOUR
    return when {
        jours > 0 -> stringResource(R.string.stats_duree_jours, jours, heures % HEURES_PAR_JOUR)
        heures > 0 -> stringResource(R.string.stats_duree_heures, heures, minutes % MINUTES_PAR_HEURE)
        minutes > 0 -> stringResource(R.string.stats_duree_minutes, minutes)
        else -> stringResource(R.string.stats_duree_courte)
    }
}

/** Carte de l'accueil qui ouvre les statistiques, avec le taux de réussite des défis. */
@Composable
fun CarteStatistiquesAccueil(
    onOuvrir: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatistiquesViewModel = hiltViewModel(),
) {
    val stats by viewModel.statistiques.collectAsStateWithLifecycle()
    val taux = stats?.bilan?.tauxReussite
    Surface(
        onClick = onOuvrir,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.stats_titre).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.stats_carte), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                taux?.let { stringResource(R.string.stats_taux, (it * CENT).roundToInt()) } ?: "—",
                style = OmegadexTheme.chiffres.moyen,
            )
        }
    }
}

private const val CENT = 100
private const val PART_MIN = 0.04f
private const val MS_PAR_MINUTE = 60_000L
private const val MINUTES_PAR_HEURE = 60L
private const val HEURES_PAR_JOUR = 24L
private val HAUTEUR_BARRE = 12.dp
private val HAUTEUR_BARRE_FINE = 6.dp

@ApercusClairSombre
@Composable
private fun StatistiquesPreview() {
    OmegadexTheme {
        ContenuStatistiques(
            StatistiquesRun(
                durees = listOf(DureeEtape(0, "Départ", 7_200_000L), DureeEtape(1, "Roxanne", 90_000_000L)),
                bilan = BilanDefis(reussis = 9, rates = 3, actifs = 2),
                especes = listOf(DefisEspece(252, "Arcko", 4, 1, 1), DefisEspece(263, "Zigzaton", 2, 1, 0)),
            ),
            onRetour = {},
        )
    }
}
