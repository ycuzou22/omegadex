package com.omegadex.app.feature.pokedex

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.OmegadexTheme

/** Pokédex de la run active : la grille de toutes les espèces, colorée selon leur statut. */
@Composable
fun PokedexScreen(modifier: Modifier = Modifier, viewModel: PokedexViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PokedexContenu(uiState = uiState, modifier = modifier)
}

@Composable
private fun PokedexContenu(uiState: PokedexUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        PokedexUiState.Chargement -> Box(modifier.fillMaxSize())
        PokedexUiState.AucuneRun -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pokedex_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        is PokedexUiState.Pret -> GrillePokedex(uiState, modifier)
    }
}

@Composable
private fun GrillePokedex(etat: PokedexUiState.Pret, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(LARGEUR_CASE),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "en-tete", span = { GridItemSpan(maxLineSpan) }, contentType = "en-tete") {
            EnTetePokedex(etat)
        }
        items(etat.especes, key = { it.id }, contentType = { "espece" }) { espece ->
            CaseEspece(espece, etat.national)
        }
    }
}

@Composable
private fun EnTetePokedex(etat: PokedexUiState.Pret) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(if (etat.national) R.string.pokedex_titre_national else R.string.pokedex_titre_hoenn),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.pokedex_compteur, etat.vues, etat.capturees, etat.especes.size),
            style = OmegadexTheme.chiffres.petit,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.pokedex_legende_portee),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Case d'une espèce. Une espèce jamais vue apparaît en silhouette (fond éteint, nom estompé, sans types) ;
 * une espèce à portée et pas encore capturée a une bordure rubis.
 */
@Composable
private fun CaseEspece(espece: EspeceAffichee, national: Boolean) {
    val couleurs = OmegadexTheme.couleurs
    val silhouette = espece.statut == StatutEspece.NON_VUE
    val numero = stringResource(R.string.pokedex_numero, espece.numero)
    val statut = stringResource(libelleStatut(espece.statut))
    val portee = stringResource(R.string.pokedex_a_portee)
    val echange = stringResource(R.string.pokedex_echange)
    val description = buildList {
        add("$numero ${espece.nom}")
        add(statut)
        if (espece.aPortee) add(portee)
        if (espece.echangeRequis && national) add(echange)
    }.joinToString(", ")
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (silhouette) couleurs.piste else MaterialTheme.colorScheme.surface,
        border = if (espece.aPortee) BorderStroke(2.dp, couleurs.gemme) else BorderStroke(1.dp, couleurs.trait),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HAUTEUR_CASE)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(numero, style = OmegadexTheme.chiffres.petit, color = couleurs.texteEteint)
            Text(
                espece.nom,
                style = MaterialTheme.typography.titleSmall,
                color = if (silhouette) couleurs.texteEteint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!silhouette) PastillesTypes(espece.types)
            RubisStatut(espece.statut)
            if (espece.echangeRequis && national) {
                Text(echange, style = MaterialTheme.typography.labelSmall, color = couleurs.texteEteint)
            }
        }
    }
}

/** Les types en pastilles de couleur (la case est trop étroite pour les puces complètes). */
@Composable
private fun PastillesTypes(types: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        types.forEach { type ->
            Box(Modifier.size(10.dp).background(CouleursTypes.de(type), CircleShape))
            Text(
                nomDuType(type)?.let { stringResource(it) } ?: type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Rang du statut en 5 petits rubis : 0 pour « non vu », 5 pour « maîtrisé ». */
@Composable
private fun RubisStatut(statut: StatutEspece) {
    val couleurs = OmegadexTheme.couleurs
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(StatutEspece.MAITRISEE.ordinal) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (index < statut.ordinal) couleurs.gemme else couleurs.gemmeEteinte,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@StringRes
fun libelleStatut(statut: StatutEspece): Int = when (statut) {
    StatutEspece.NON_VUE -> R.string.statut_non_vue
    StatutEspece.VUE -> R.string.statut_vue
    StatutEspece.CAPTUREE -> R.string.statut_capturee
    StatutEspece.ENTRAINEE -> R.string.statut_entrainee
    StatutEspece.EVOLUEE -> R.string.statut_evoluee
    StatutEspece.MAITRISEE -> R.string.statut_maitrisee
}

private val LARGEUR_CASE = 104.dp
private val HAUTEUR_CASE = 112.dp

@ApercusClairSombre
@Composable
private fun PokedexPreview() {
    OmegadexTheme {
        PokedexContenu(
            PokedexUiState.Pret(
                national = false,
                especes = listOf(
                    EspeceAffichee(252, 1, "Arcko", listOf("plante"), StatutEspece.ENTRAINEE, false, false),
                    EspeceAffichee(253, 2, "Massko", listOf("plante"), StatutEspece.VUE, false, true),
                    EspeceAffichee(254, 3, "Jungko", listOf("plante"), StatutEspece.NON_VUE, false, false),
                    EspeceAffichee(263, 15, "Zigzaton", listOf("normal"), StatutEspece.NON_VUE, false, true),
                    EspeceAffichee(280, 30, "Tarsal", listOf("psy", "fee"), StatutEspece.CAPTUREE, false, false),
                ),
                vues = 3,
                capturees = 2,
            ),
        )
    }
}
