package com.omegadex.app.feature.fiche

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.rencontre.Echange
import com.omegadex.app.feature.aventure.nomDebloqueur
import com.omegadex.app.feature.lieu.nomMethodeRencontre
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Onglet Lieux : où trouver l'espèce (méthode, niveaux, taux, condition) et l'étape qui débloque chaque lieu.
 * Les lieux pas encore atteints dans la run sont éteints ; « Échange requis » quand l'espèce ne s'obtient
 * que par échange ou transfert.
 */
@Composable
internal fun OngletLieux(fiche: FichePokemonUiState.Fiche) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (fiche.echangeRequis) BandeauEchange()
        if (fiche.lieux.isEmpty() && !fiche.echangeRequis) {
            Text(
                stringResource(R.string.fiche_lieux_aucun),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        fiche.lieux.forEach { LigneLieu(it) }
    }
}

@Composable
private fun BandeauEchange() {
    val magma = OmegadexTheme.couleurs.magma
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, magma),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.methode_echange_requis).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = magma,
            )
            Text(
                stringResource(R.string.fiche_lieux_echange_explication),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LigneLieu(lieu: LieuFiche) {
    val r = lieu.rencontre
    val eteint = lieu.accessible == false
    val couleurTexte = if (eteint) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                r.lieu.nom,
                style = MaterialTheme.typography.titleSmall,
                color = couleurTexte,
                modifier = Modifier.weight(1f),
            )
            niveaux(r.niveauMin, r.niveauMax)?.let {
                Text(it, style = OmegadexTheme.chiffres.petit, color = couleurTexte)
            }
        }
        Text(
            listOfNotNull(
                stringResource(nomMethodeRencontre(r.methode)),
                r.taux?.let { stringResource(R.string.fiche_lieux_taux, it) },
                r.condition,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lieu.etape?.let { etape ->
            Text(
                listOfNotNull(
                    stringResource(
                        if (eteint) R.string.fiche_lieux_etape_a_venir else R.string.fiche_lieux_etape,
                        etape.id,
                        etape.titre,
                    ),
                    lieu.debloqueur?.let { stringResource(nomDebloqueur(it)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = if (eteint) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        if (lieu.echange == Echange.SAPHIR_ALPHA) {
            Text(
                stringResource(R.string.rencontre_echange_saphir),
                style = MaterialTheme.typography.labelMedium,
                color = OmegadexTheme.couleurs.gemme,
            )
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), color = OmegadexTheme.couleurs.piste)
    }
}

@Composable
private fun niveaux(min: Int?, max: Int?): String? = when {
    min == null && max == null -> null
    min == null || max == null || min == max -> stringResource(R.string.fiche_attaques_niveau, min ?: max ?: 0)
    else -> stringResource(R.string.fiche_lieux_niveaux, min, max)
}

/**
 * Onglet Défis : les défis de l'espèce dans la run active (en cours, disponibles selon la difficulté de
 * la run, terminés avec leur résultat) et les notes libres du joueur sur ce Pokémon.
 */
@Composable
internal fun OngletDefis(fiche: FichePokemonUiState.Fiche, onNotes: (String) -> Unit) {
    val defis = fiche.defis
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (defis == null) {
            Text(
                stringResource(R.string.fiche_defis_sans_run),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Text(
            stringResource(R.string.fiche_defis_difficulte, stringResource(titreDifficulte(defis.difficulteRun))),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (defis.vide) {
            Text(stringResource(R.string.fiche_defis_aucun), style = MaterialTheme.typography.bodyMedium)
        }
        GroupeDefis(R.string.defis_onglet_actifs, defis.actifs)
        GroupeDefis(R.string.defis_onglet_disponibles, defis.disponibles)
        GroupeDefis(R.string.defis_onglet_termines, defis.termines)
        Notes(fiche.notes, onNotes)
    }
}

@Composable
private fun GroupeDefis(@StringRes titre: Int, defis: List<DefiFiche>) {
    if (defis.isEmpty()) return
    Text(
        stringResource(titre).uppercase() + " · ${defis.size}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
    defis.forEach { LigneDefi(it) }
}

@Composable
private fun LigneDefi(defi: DefiFiche) {
    val couleurs = OmegadexTheme.couleurs
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val pastille = when (defi.etat) {
            EtatDefi.ACTIF -> couleurs.magma
            EtatDefi.REUSSI -> MaterialTheme.colorScheme.primary
            EtatDefi.RATE -> MaterialTheme.colorScheme.error
            null -> couleurs.piste
        }
        Box(Modifier.size(10.dp).background(pastille, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(defi.titre, style = MaterialTheme.typography.titleSmall)
            Text(
                defi.enonce,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(titreDifficulte(defi.difficulte)), style = MaterialTheme.typography.labelMedium)
            when (defi.etat) {
                EtatDefi.REUSSI -> stringResource(R.string.defis_etat_reussi)
                EtatDefi.RATE -> stringResource(R.string.defis_etat_rate)
                else -> null
            }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = pastille) }
        }
    }
}

@Composable
private fun Notes(notes: String, onNotes: (String) -> Unit) {
    var texte by rememberSaveable { mutableStateOf(notes) }
    OutlinedTextField(
        value = texte,
        onValueChange = {
            texte = it
            onNotes(it)
        },
        label = { Text(stringResource(R.string.fiche_defis_notes)) },
        placeholder = { Text(stringResource(R.string.fiche_defis_notes_aide)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
