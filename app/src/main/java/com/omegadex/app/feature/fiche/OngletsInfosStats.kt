package com.omegadex.app.feature.fiche

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.espece.TableDesTypes
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.OmegadexTheme
import java.text.NumberFormat
import java.util.Locale

/** Échelle des barres de stats de la maquette (une stat de 150 remplit la barre). */
private const val STAT_PLEINE = 150f
private const val POURCENT = 100.0
private const val DUREE_BARRE_MS = 600
private val EASE_BARRE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val BLEU_MALE = Color(0xFF2F6FED)
private val ROSE_FEMELLE = Color(0xFFE05AA0)
private val TEXTE_MALE = Color(0xFF1F4FB3)
private val TEXTE_FEMELLE = Color(0xFFA3226A)
private const val DEMI = 0.5f
private const val QUART = 0.25f
private const val MILIEU = 0.5f
private val MULTIPLICATEURS_AFFICHES = listOf(4f, 2f, DEMI, QUART, 0f)

private val francais = Locale.FRANCE

private fun decimal(valeur: Double, chiffres: Int = 1): String = NumberFormat.getNumberInstance(francais).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = chiffres
}.format(valeur)

/** Onglet Infos : taille, poids, talents (dont caché), capture, ratio, groupes d'œufs, Méga. */
@Composable
internal fun OngletInfos(fiche: FichePokemonUiState.Fiche) {
    val espece = fiche.espece
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val taille = stringResource(R.string.fiche_metres, decimal(espece.tailleM))
        LigneInfo(R.string.fiche_taille, taille, chiffre = true)
        LigneInfo(R.string.fiche_poids, stringResource(R.string.fiche_kilos, decimal(espece.poidsKg)), chiffre = true)
        LigneTalents(fiche)
        LigneInfo(R.string.fiche_taux_capture, espece.tauxCapture.toString(), chiffre = true)
        LigneInfo(R.string.fiche_groupes_oeufs, espece.groupesOeufs.joinToString(", "))
        LigneInfo(
            R.string.fiche_mega,
            when (fiche.stadeMega) {
                null -> stringResource(R.string.fiche_mega_non)
                espece.nom -> stringResource(R.string.fiche_mega_oui)
                else -> stringResource(R.string.fiche_mega_stade, fiche.stadeMega)
            },
        )
        RatioSexes(espece.ratioMale)
    }
}

@Composable
private fun LigneInfo(titre: Int, valeur: String, chiffre: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            stringResource(titre),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(150.dp),
        )
        Text(
            valeur,
            style = if (chiffre) OmegadexTheme.chiffres.moyen else MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LigneTalents(fiche: FichePokemonUiState.Fiche) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.fiche_talents),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(150.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            fiche.talents.sortedBy { it.cache }.forEach { talent ->
                Text(
                    if (talent.cache) stringResource(R.string.fiche_talent_cache, talent.nom) else talent.nom,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (talent.cache) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    talent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Barre mâle / femelle de la maquette ; « Asexué » si l'espèce n'a pas de sexe. */
@Composable
private fun RatioSexes(ratioMale: Double?) {
    if (ratioMale == null) {
        LigneInfo(R.string.fiche_sexe, stringResource(R.string.fiche_asexue))
        return
    }
    val male = decimal(ratioMale * POURCENT)
    val femelle = decimal((1 - ratioMale) * POURCENT)
    val description = stringResource(R.string.fiche_ratio_description, male, femelle)
    Column(
        Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.fiche_male, male),
                style = MaterialTheme.typography.labelLarge,
                color = TEXTE_MALE,
            )
            Box(Modifier.weight(1f))
            Text(
                stringResource(R.string.fiche_femelle, femelle),
                style = MaterialTheme.typography.labelLarge,
                color = TEXTE_FEMELLE,
            )
        }
        Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
            if (ratioMale > 0) Box(Modifier.weight(ratioMale.toFloat()).fillMaxHeight().background(BLEU_MALE))
            if (ratioMale < 1) Box(Modifier.weight((1 - ratioMale).toFloat()).fillMaxHeight().background(ROSE_FEMELLE))
        }
    }
}

/** Onglet Stats : stats de base en barres, total, EV donnés et efficacités des types. */
@Composable
internal fun OngletStats(fiche: FichePokemonUiState.Fiche) {
    val stats = fiche.espece.stats
    val lignes = listOf(
        R.string.stat_pv to stats.pv,
        R.string.stat_attaque to stats.attaque,
        R.string.stat_defense to stats.defense,
        R.string.stat_attaque_speciale to stats.attaqueSpeciale,
        R.string.stat_defense_speciale to stats.defenseSpeciale,
        R.string.stat_vitesse to stats.vitesse,
    )
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        lignes.forEach { (nom, valeur) -> BarreStat(stringResource(nom), valeur) }
        HorizontalDivider(color = OmegadexTheme.couleurs.piste)
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.stat_total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(stats.total.toString(), style = OmegadexTheme.chiffres.moyen)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.fiche_ev),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(fiche.espece.evDonnes, style = MaterialTheme.typography.titleSmall)
        }
        Efficacites(fiche.espece)
    }
}

@Composable
private fun BarreStat(nom: String, valeur: Int) {
    val reduites = OmegadexTheme.animationsReduites
    val remplissage = remember(valeur) { Animatable(if (reduites) 1f else 0f) }
    LaunchedEffect(valeur) {
        if (!reduites) remplissage.animateTo(1f, tween(DUREE_BARRE_MS, easing = EASE_BARRE))
    }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            nom,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            valeur.toString(),
            style = OmegadexTheme.chiffres.moyen,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(OmegadexTheme.couleurs.piste)) {
            Box(
                Modifier
                    .fillMaxWidth((valeur / STAT_PLEINE).coerceAtMost(1f))
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = remplissage.value
                        transformOrigin = TransformOrigin(0f, MILIEU)
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** Faiblesses, résistances et immunités, regroupées par multiplicateur (×4, ×2, ×½, ×¼, ×0). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Efficacites(espece: Espece) {
    val efficacites = TableDesTypes.efficacites(espece.types)
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.fiche_efficacites), style = MaterialTheme.typography.titleMedium)
        MULTIPLICATEURS_AFFICHES.forEach { multiplicateur ->
            val types = efficacites.filterValues { it == multiplicateur }.keys
            if (types.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        libelleMultiplicateur(multiplicateur),
                        style = OmegadexTheme.chiffres.moyen,
                        modifier = Modifier.width(40.dp).padding(top = 4.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        types.forEach { type -> TypeChip(type, nomDuType(type)?.let { stringResource(it) } ?: type) }
                    }
                }
            }
        }
    }
}

@Composable
private fun libelleMultiplicateur(multiplicateur: Float): String = when (multiplicateur) {
    DEMI -> stringResource(R.string.fiche_fois_demi)
    QUART -> stringResource(R.string.fiche_fois_quart)
    else -> stringResource(R.string.fiche_fois, multiplicateur.toInt())
}
