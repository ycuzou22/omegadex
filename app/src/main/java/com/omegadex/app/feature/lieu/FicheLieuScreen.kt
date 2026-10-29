package com.omegadex.app.feature.lieu

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.TypeLieu
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme

/** Fiche d'un lieu, ouverte depuis l'étape en cours ; toucher une espèce ouvre sa fiche Pokémon. */
@Composable
fun FicheLieuScreen(
    onRetour: () -> Unit,
    onEspece: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FicheLieuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FicheLieuContenu(uiState, onRetour, onEspece, modifier)
}

@Composable
private fun FicheLieuContenu(
    uiState: FicheLieuUiState,
    onRetour: () -> Unit,
    onEspece: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onRetour) { Text(stringResource(R.string.lieu_retour)) }
            }
            when (uiState) {
                FicheLieuUiState.Chargement -> Unit
                FicheLieuUiState.Introuvable -> item { Text(stringResource(R.string.lieu_introuvable)) }
                is FicheLieuUiState.Fiche -> fiche(uiState, onEspece)
            }
        }
    }
}

private fun LazyListScope.fiche(fiche: FicheLieuUiState.Fiche, onEspece: (Int) -> Unit) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(nomTypeLieu(fiche.type)).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(fiche.nom, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
        }
    }
    if (fiche.objets.isNotEmpty()) {
        item { TitreSection(R.string.lieu_objets) }
        item {
            Text(
                fiche.objets.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    item { TitreSection(R.string.lieu_rencontres) }
    if (fiche.rencontres.isEmpty()) {
        item {
            Text(
                stringResource(R.string.lieu_aucune_rencontre),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    fiche.rencontres.forEach { groupe ->
        item(key = "methode-${groupe.methode}") {
            Text(
                stringResource(nomMethodeRencontre(groupe.methode)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(groupe.lignes, key = { "${groupe.methode}-${it.especeId}-${it.niveauMin}-${it.condition}" }) { ligne ->
            CarteRencontre(ligne) { onEspece(ligne.especeId) }
        }
    }
}

@Composable
private fun TitreSection(@StringRes titre: Int) {
    Text(
        stringResource(titre),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun CarteRencontre(ligne: LigneRencontre, onClick: () -> Unit) {
    val ouvrir = stringResource(R.string.lieu_ouvrir_espece, ligne.nom)
    val couleur = ligne.type1?.let { CouleursTypes.de(it) } ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = ouvrir, role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 18.dp, height = 16.dp).background(couleur, FormeGemme))
            Column(Modifier.weight(1f)) {
                Text(ligne.nom, style = MaterialTheme.typography.titleMedium)
                ligne.condition?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                niveaux(ligne)?.let { Text(it, style = OmegadexTheme.chiffres.moyen) }
                ligne.taux?.let {
                    Text(
                        stringResource(R.string.lieu_taux, it),
                        style = OmegadexTheme.chiffres.petit,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun niveaux(ligne: LigneRencontre): String? {
    val min = ligne.niveauMin ?: return null
    val max = ligne.niveauMax ?: min
    return if (min == max) {
        stringResource(R.string.lieu_niveau, min)
    } else {
        stringResource(R.string.lieu_niveaux, min, max)
    }
}

@StringRes
private fun nomTypeLieu(type: TypeLieu) = when (type) {
    TypeLieu.ROUTE -> R.string.lieu_type_route
    TypeLieu.VILLE -> R.string.lieu_type_ville
    TypeLieu.GROTTE -> R.string.lieu_type_grotte
    TypeLieu.MER -> R.string.lieu_type_mer
    TypeLieu.ZONE_ANNEXE -> R.string.lieu_type_zone_annexe
    TypeLieu.LIEU_MIRAGE -> R.string.lieu_type_lieu_mirage
    TypeLieu.BATIMENT -> R.string.lieu_type_batiment
    TypeLieu.HORS_HOENN -> R.string.lieu_type_hors_hoenn
}

private val NOMS_METHODES = mapOf(
    MethodeRencontre.HERBE to R.string.methode_herbe,
    MethodeRencontre.HORDE to R.string.methode_horde,
    MethodeRencontre.SURF to R.string.methode_surf,
    MethodeRencontre.PLONGEE to R.string.methode_plongee,
    MethodeRencontre.PECHE to R.string.methode_peche,
    MethodeRencontre.NAVI_DEX to R.string.methode_navi_dex,
    MethodeRencontre.ROCHE_FRACASSEE to R.string.methode_roche_fracassee,
    MethodeRencontre.CADEAU to R.string.methode_cadeau,
    MethodeRencontre.ECHANGE_EN_JEU to R.string.methode_echange_en_jeu,
    MethodeRencontre.FIXE to R.string.methode_fixe,
    MethodeRencontre.OEUF to R.string.methode_oeuf,
    MethodeRencontre.ECHANGE_REQUIS to R.string.methode_echange_requis,
)

@StringRes
internal fun nomMethodeRencontre(methode: MethodeRencontre): Int = NOMS_METHODES.getValue(methode)

@ApercusClairSombre
@Composable
private fun FicheLieuPreview() {
    OmegadexTheme {
        FicheLieuContenu(
            uiState = FicheLieuUiState.Fiche(
                nom = "Route 101",
                type = TypeLieu.ROUTE,
                rencontres = listOf(
                    GroupeRencontres(
                        MethodeRencontre.HERBE,
                        listOf(
                            LigneRencontre(265, "Chenipotte", "insecte", 2, 3, 45, null),
                            LigneRencontre(263, "Zigzaton", "normal", 2, 3, 45, null),
                        ),
                    ),
                ),
                objets = listOf("Pokédex"),
            ),
            onRetour = {},
            onEspece = {},
        )
    }
}
