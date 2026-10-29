package com.omegadex.app.feature.fiche

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.model.AttaqueApprise
import com.omegadex.app.domain.model.CategorieAttaque
import com.omegadex.app.domain.model.MethodeApprentissage
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Onglet Attaques : les attaques de Rubis Oméga par méthode (niveau, CT/CS, reproduction, donneur).
 * Dans la liste par niveau, un repère « niveau actuel » sépare ce que le Pokémon de l'équipe connaît déjà
 * de ce qu'il apprendra.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OngletAttaques(fiche: FichePokemonUiState.Fiche) {
    var methode by rememberSaveable { mutableStateOf(MethodeApprentissage.NIVEAU) }
    val parMethode = fiche.attaques.groupBy { it.methode }
    val liste = trier(parMethode[methode].orEmpty(), methode)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MethodeApprentissage.entries.forEach { m ->
                FiltreMethode(
                    texte = stringResource(
                        R.string.fiche_attaques_filtre,
                        stringResource(nomMethode(m)),
                        parMethode[m].orEmpty().size,
                    ),
                    choisi = m == methode,
                ) { methode = m }
            }
        }
        if (liste.isEmpty()) {
            Text(
                stringResource(R.string.fiche_attaques_aucune),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val repere = fiche.niveauActuel?.takeIf { methode == MethodeApprentissage.NIVEAU }
        val indexRepere = repere?.let { niveau ->
            liste.indexOfFirst { it.niveau > niveau }.takeIf { it >= 0 } ?: liste.size
        }
        liste.forEachIndexed { index, apprise ->
            if (index == indexRepere) RepereNiveau(repere)
            LigneAttaque(apprise)
        }
        if (indexRepere == liste.size && liste.isNotEmpty()) RepereNiveau(repere)
    }
}

private fun trier(liste: List<AttaqueApprise>, methode: MethodeApprentissage): List<AttaqueApprise> = when (methode) {
    MethodeApprentissage.NIVEAU -> liste.sortedWith(compareBy({ it.niveau }, { it.attaque.nom }))
    MethodeApprentissage.CT_CS -> liste.sortedWith(compareBy({ ordreCt(it.numeroCt) }, { it.attaque.nom }))
    else -> liste.sortedBy { it.attaque.nom }
}

/** Les CT d'abord (CT01 à CT100), puis les CS. */
private fun ordreCt(numero: String?): Int {
    val valeur = numero?.drop(2)?.toIntOrNull() ?: return Int.MAX_VALUE
    return if (numero.startsWith("CS")) DECALAGE_CS + valeur else valeur
}

private const val DECALAGE_CS = 1_000

@Composable
private fun FiltreMethode(texte: String, choisi: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (choisi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (choisi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (choisi) null else BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = Modifier.selectable(selected = choisi, role = Role.Tab, onClick = onClick),
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun RepereNiveau(niveau: Int) {
    val magma = OmegadexTheme.couleurs.magma
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.fiche_attaques_niveau_actuel, niveau).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = magma,
            modifier = Modifier.semantics { heading() },
        )
        Box(Modifier.weight(1f).height(2.dp).background(magma, CircleShape))
    }
}

@Composable
private fun LigneAttaque(apprise: AttaqueApprise) {
    val attaque = apprise.attaque
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (apprise.methode) {
                    MethodeApprentissage.NIVEAU -> stringResource(R.string.fiche_attaques_niveau, apprise.niveau)
                    MethodeApprentissage.CT_CS -> apprise.numeroCt.orEmpty()
                    else -> ""
                },
                style = OmegadexTheme.chiffres.petit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(if (apprise.methode in COLONNE_NUMERO) 64.dp else 0.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    attaque.nom,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(10.dp).background(CouleursTypes.de(attaque.type), CircleShape))
                    Text(
                        listOf(
                            nomDuType(attaque.type)?.let { stringResource(it) } ?: attaque.type,
                            stringResource(nomCategorie(attaque.categorie)),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(
                    R.string.fiche_attaques_valeurs,
                    attaque.puissance?.toString() ?: TIRET,
                    attaque.precision?.let { stringResource(R.string.fiche_attaques_precision, it) } ?: TIRET,
                    attaque.pp,
                ),
                style = OmegadexTheme.chiffres.petit,
            )
        }
        HorizontalDivider(color = OmegadexTheme.couleurs.piste)
    }
}

private const val TIRET = "—"
private val COLONNE_NUMERO = setOf(MethodeApprentissage.NIVEAU, MethodeApprentissage.CT_CS)

@StringRes
private fun nomMethode(methode: MethodeApprentissage) = when (methode) {
    MethodeApprentissage.NIVEAU -> R.string.fiche_attaques_par_niveau
    MethodeApprentissage.CT_CS -> R.string.fiche_attaques_ct_cs
    MethodeApprentissage.REPRODUCTION -> R.string.fiche_attaques_reproduction
    MethodeApprentissage.DONNEUR -> R.string.fiche_attaques_donneur
}

@StringRes
private fun nomCategorie(categorie: CategorieAttaque) = when (categorie) {
    CategorieAttaque.PHYSIQUE -> R.string.categorie_physique
    CategorieAttaque.SPECIALE -> R.string.categorie_speciale
    CategorieAttaque.STATUT -> R.string.categorie_statut
}
