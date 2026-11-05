package com.omegadex.app.feature.defis

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.ui.theme.OmegadexTheme

/** Ce que le joueur peut faire depuis la grille des roulettes. */
internal class ActionsRoulettes(
    val onTourner: (CategorieDefi) -> Unit,
    val onVoirTirage: (CategorieDefi) -> Unit,
    val onVoirEnCours: () -> Unit,
)

/** Les six roulettes, deux par ligne : une tuile par catégorie (proposition 1 du canvas). */
@Composable
internal fun GrilleRoulettes(roulette: RouletteUi, actions: ActionsRoulettes, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        roulette.cases.chunked(PAR_LIGNE).forEach { ligne ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ligne.forEach { case -> TuileRoulette(case, actions, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun TuileRoulette(case: CaseRouletteUi, actions: ActionsRoulettes, modifier: Modifier = Modifier) {
    val enCours = case.etat == EtatCase.EN_COURS
    val clic = if (enCours) {
        Modifier.clickable(role = Role.Button, onClick = actions.onVoirEnCours)
    } else {
        Modifier
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (enCours) {
            BorderStroke(2.dp, OmegadexTheme.couleurs.magma)
        } else {
            BorderStroke(1.dp, OmegadexTheme.couleurs.trait)
        },
        modifier = modifier.then(clic),
    ) {
        Column(
            Modifier.heightIn(min = HAUTEUR_TUILE).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val compte = nombreDefis(case.nombre)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeCategorie(case.categorie)
                Spacer(Modifier.weight(1f))
                Text(
                    case.nombre.toString(),
                    style = OmegadexTheme.chiffres.petit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = compte },
                )
            }
            Text(stringResource(nomCategorie(case.categorie)), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            EtatTuile(case, actions)
        }
    }
}

@Composable
private fun EtatTuile(case: CaseRouletteUi, actions: ActionsRoulettes) {
    val nom = stringResource(nomCategorie(case.categorie))
    val secondaire = MaterialTheme.colorScheme.onSurfaceVariant
    when (case.etat) {
        EtatCase.LIBRE -> BoutonRoulette(
            texte = stringResource(R.string.roulette_bouton),
            description = stringResource(R.string.roulette_bouton_description, nom),
            onClick = { actions.onTourner(case.categorie) },
        )
        EtatCase.TIRE -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiquette(stringResource(R.string.roulette_tire))
            TitreDefi(case.defi?.titre.orEmpty())
            BoutonRoulette(
                texte = stringResource(R.string.roulette_voir_tirage),
                description = stringResource(R.string.roulette_voir_tirage_description, nom),
                onClick = { actions.onVoirTirage(case.categorie) },
                contour = true,
            )
        }
        EtatCase.EN_COURS -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Etiquette(stringResource(R.string.roulette_en_cours))
            TitreDefi(case.defi?.titre.orEmpty())
        }
        EtatCase.VERROU -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_cadenas), null, Modifier.size(16.dp), tint = secondaire)
            Text(
                stringResource(R.string.roulette_verrou, case.queteRequise.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = secondaire,
            )
        }
        EtatCase.VIDE -> Text(
            stringResource(R.string.roulette_vide),
            style = MaterialTheme.typography.bodySmall,
            color = secondaire,
        )
    }
}

@Composable
private fun Etiquette(texte: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(OmegadexTheme.couleurs.magma, CircleShape))
        Text(texte.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TitreDefi(titre: String) {
    Text(titre, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** Bouton en pilule avec la flèche de roulette ; [contour] pour l'action secondaire. */
@Composable
internal fun BoutonRoulette(
    texte: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contour: Boolean = false,
) {
    val schema = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (contour) schema.surface else schema.primary,
        contentColor = if (contour) schema.primary else schema.onPrimary,
        border = if (contour) BorderStroke(2.dp, schema.primary) else null,
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp).semantics { contentDescription = description },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_roulette), null, Modifier.size(16.dp))
            Text(texte, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Pictogramme de la catégorie dans un carré rubis pâle. */
@Composable
internal fun IconeCategorie(categorie: CategorieDefi, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(iconeCategorie(categorie)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** « 4 défis dans la roulette ». */
@Composable
internal fun nombreDefis(nombre: Int): String = pluralStringResource(R.plurals.roulette_nombre, nombre, nombre)

private val NOMS_CATEGORIES = mapOf(
    CategorieDefi.CHAMPION to R.string.categorie_defi_champion,
    CategorieDefi.ESPECE to R.string.categorie_defi_espece,
    CategorieDefi.EVOLUTION to R.string.categorie_defi_evolution,
    CategorieDefi.NAVI_DEX to R.string.categorie_defi_navi_dex,
    CategorieDefi.MEGA to R.string.categorie_defi_mega,
    CategorieDefi.CONCOURS to R.string.categorie_defi_concours,
)

private val ICONES_CATEGORIES = mapOf(
    CategorieDefi.CHAMPION to R.drawable.ic_nav_defis,
    CategorieDefi.ESPECE to R.drawable.ic_defi_espece,
    CategorieDefi.EVOLUTION to R.drawable.ic_defi_evolution,
    CategorieDefi.NAVI_DEX to R.drawable.ic_defi_navi_dex,
    CategorieDefi.MEGA to R.drawable.ic_defi_mega,
    CategorieDefi.CONCOURS to R.drawable.ic_defi_concours,
)

@StringRes
internal fun nomCategorie(categorie: CategorieDefi): Int = NOMS_CATEGORIES.getValue(categorie)

@DrawableRes
private fun iconeCategorie(categorie: CategorieDefi): Int = ICONES_CATEGORIES.getValue(categorie)

private const val PAR_LIGNE = 2
private val HAUTEUR_TUILE = 148.dp
