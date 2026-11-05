package com.omegadex.app.feature.defis

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.RouletteDefis
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.OmegadexTheme

/** Roulette ouverte : [tour] à animer, ou null pour revoir un défi déjà tiré. */
internal data class FeuilleOuverte(val categorie: CategorieDefi, val tour: TourDeRoulette?)

/** Ce que le joueur peut faire une fois le défi tiré. */
internal class ActionsTirage(
    val onRelever: (CategorieDefi) -> Unit,
    val onRelancer: (CategorieDefi) -> Unit,
    val onFermer: () -> Unit,
)

/**
 * Roulette d'une catégorie, en feuille du bas : le rouleau de cartes défile puis ralentit sur le défi tiré,
 * qui s'affiche avec « Relever le défi », « Relancer » (s'il reste une relance) et « Pas maintenant ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeuilleRoulette(feuille: FeuilleOuverte, case: CaseRouletteUi?, actions: ActionsTirage) {
    ModalBottomSheet(
        onDismissRequest = actions.onFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EnTeteFeuille(feuille.categorie, case?.nombre ?: 0)
            var fini by remember(feuille.tour) { mutableStateOf(feuille.tour == null) }
            feuille.tour?.let { tour -> Rouleau(tour, onFin = { fini = true }) }
            val tire = feuille.tour?.gagnant ?: case?.defi
            if (!fini) {
                Text(
                    stringResource(R.string.roulette_tourne),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else if (tire != null) {
                CarteTirage(tire, case, feuille.categorie, actions)
            }
        }
    }
}

@Composable
private fun EnTeteFeuille(categorie: CategorieDefi, nombre: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconeCategorie(categorie)
        Column {
            Text(
                stringResource(R.string.roulette_titre, stringResource(nomCategorie(categorie))),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                nombreDefis(nombre),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Rouleau de cartes : il défile et s'arrête dans le cadre magma sur la carte gagnante. Décoratif pour TalkBack. */
@Composable
private fun Rouleau(tour: TourDeRoulette, onFin: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val cible = with(LocalDensity.current) { (HAUTEUR_CARTE * (RouletteDefis.ARRET_ROULEAU - 1)).toPx() }
    val decalage = remember(tour) { Animatable(0f) }
    val reduites = OmegadexTheme.animationsReduites
    val sons = LocalSons.current
    LaunchedEffect(tour) {
        if (reduites) {
            decalage.snapTo(cible)
        } else {
            decalage.animateTo(cible, tween(DUREE_ROULEAU_MS, easing = RALENTI))
        }
        sons.jouer(Son.CLIC)
        onFin()
    }
    val forme = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().height(HAUTEUR_FENETRE).clip(forme).background(couleurs.piste, forme)
            .clearAndSetSemantics {},
    ) {
        Column(
            Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                .padding(horizontal = 14.dp)
                .graphicsLayer { translationY = -decalage.value },
        ) {
            tour.rouleau.forEach { carte -> CarteRouleau(carte) }
        }
        Box(
            Modifier.fillMaxWidth().height(FONDU).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(couleurs.piste, Color.Transparent))),
        )
        Box(
            Modifier.fillMaxWidth().height(FONDU).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, couleurs.piste))),
        )
        Box(
            Modifier.align(Alignment.Center).padding(horizontal = 6.dp).fillMaxWidth().height(HAUTEUR_CARTE)
                .border(3.dp, couleurs.magma, RoundedCornerShape(16.dp)),
        )
    }
}

@Composable
private fun CarteRouleau(carte: CarteDefi) {
    Box(Modifier.fillMaxWidth().height(HAUTEUR_CARTE).padding(vertical = 4.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_gem),
                    contentDescription = null,
                    tint = OmegadexTheme.couleurs.gemme,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    carte.titre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                PuceDifficulte(carte.difficulte)
            }
        }
    }
}

/** Le défi tiré, annoncé à TalkBack quand il apparaît. */
@Composable
private fun CarteTirage(tire: CarteDefi, case: CaseRouletteUi?, categorie: CategorieDefi, actions: ActionsTirage) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, OmegadexTheme.couleurs.magma),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.roulette_resultat).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    PuceDifficulte(tire.difficulte)
                }
                Text(tire.titre, style = MaterialTheme.typography.titleLarge)
                Text(
                    tire.enonce,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OmegaButton(
                stringResource(R.string.roulette_relever),
                onClick = { actions.onRelever(categorie) },
                avecOmega = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (case?.peutTourner == true) {
                    OmegaButton(
                        stringResource(R.string.roulette_relancer),
                        onClick = { actions.onRelancer(categorie) },
                        variante = VarianteBouton.CONTOUR,
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = actions.onFermer) { Text(stringResource(R.string.roulette_pas_maintenant)) }
            }
            case?.relancesRestantes?.let { restantes ->
                Text(
                    if (restantes == 0) {
                        stringResource(R.string.roulette_sans_relance)
                    } else {
                        pluralStringResource(R.plurals.roulette_relances_restantes, restantes, restantes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Pastille de difficulté : discrète en Normal, rubis pâle en Difficile, roche et lueur en Hardcore. */
@Composable
internal fun PuceDifficulte(difficulte: Difficulte, modifier: Modifier = Modifier) {
    val couleurs = OmegadexTheme.couleurs
    val schema = MaterialTheme.colorScheme
    val (fond, texte) = when (difficulte) {
        Difficulte.NORMAL -> schema.secondaryContainer to schema.onSecondaryContainer
        Difficulte.DIFFICILE -> schema.primaryContainer to schema.onPrimaryContainer
        Difficulte.HARDCORE -> couleurs.roche to couleurs.lueurMagma
    }
    Text(
        stringResource(titreDifficulte(difficulte)).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = texte,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.background(fond, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private val HAUTEUR_CARTE = 64.dp
private val HAUTEUR_FENETRE = 196.dp
private val FONDU = 36.dp
private const val DUREE_ROULEAU_MS = 3_200
private val RALENTI = CubicBezierEasing(0.12f, 0.72f, 0.1f, 1f)
