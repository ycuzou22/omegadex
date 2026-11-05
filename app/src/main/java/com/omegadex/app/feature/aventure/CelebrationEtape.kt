package com.omegadex.app.feature.aventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omegadex.app.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.domain.defi.DefiAffiche
import com.omegadex.app.domain.defi.defisAClore
import com.omegadex.app.domain.pokedex.RegleNational
import com.omegadex.app.ui.animations.AnimationBoiteBadges
import com.omegadex.app.ui.animations.AnimationEtapeValidee
import com.omegadex.app.ui.animations.TextesEtapeValidee
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son

/**
 * Animation « Étape validée » en plein écran, par-dessus la barre de navigation, comme la maquette.
 * Quand l'étape est une arène, la boîte à badges s'ouvre ensuite et le nouveau badge y est forgé.
 * Un retour arrière passe toute la séquence. Si des défis de combat contre cet adversaire sont encore actifs,
 * le joueur dit ensuite s'ils sont réussis ou ratés.
 */
@Composable
internal fun CelebrationEtape(
    evenement: EtapeValidee,
    onFin: () -> Unit,
    cloture: ClotureDefisViewModel = hiltViewModel(),
) {
    val charges by cloture.defis.collectAsStateWithLifecycle()
    var bilanDemande by remember(evenement) { mutableStateOf(false) }
    val aClore = defisAClore(charges?.run, evenement.numero)
    when {
        !bilanDemande -> AnimationCelebration(evenement) { if (aClore.isEmpty()) onFin() else bilanDemande = true }
        // Tous les défis sont tranchés : le bilan se ferme seul.
        aClore.isEmpty() -> LaunchedEffect(Unit) { onFin() }
        else -> BilanDefisCombat(aClore, cloture::clore, onFin)
    }
}

@Composable
private fun AnimationCelebration(evenement: EtapeValidee, onFin: () -> Unit) {
    val textes = TextesEtapeValidee(
        surtitre = stringResource(R.string.anim_etape_validee, evenement.numero),
        titre = evenement.badge ?: evenement.titre,
        sousTitre = evenement.suivante?.let { stringResource(R.string.anim_prochaine_etape, it) }
            ?: stringResource(R.string.anim_aventure_terminee),
    )
    val badge = BadgeArene.pourEtape(evenement.numero)
    val sons = LocalSons.current
    // La Ligue validée débloque le Pokédex national : son spécial, joué une seule fois, à la fin de la célébration.
    val terminer = {
        if (evenement.numero == RegleNational.ETAPE_LIGUE) sons.jouer(Son.NATIONAL)
        onFin()
    }
    var boiteOuverte by remember(evenement) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = terminer,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        if (boiteOuverte && badge != null) {
            AnimationBoiteBadges(BadgeArene.obtenus(evenement.etapesValidees) + badge, badge, terminer)
        } else {
            AnimationEtapeValidee(textes, onFin = { if (badge != null) boiteOuverte = true else terminer() })
        }
    }
}

/**
 * Bilan des défis de combat de l'étape validée : chaque défi se ferme d'un « Réussi » ou d'un « Raté ».
 * Le bilan se ferme seul quand tous sont tranchés ; « Plus tard » les laisse actifs dans l'espace Défis.
 */
@Composable
private fun BilanDefisCombat(
    defis: List<DefiAffiche>,
    onClore: (defiId: Long, reussi: Boolean) -> Unit,
    onFin: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFin,
        title = { Text(stringResource(R.string.cloture_defis_titre)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.cloture_defis_texte))
                defis.forEach { affiche ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(affiche.modele.titre, style = MaterialTheme.typography.titleSmall)
                        Text(affiche.enonce, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { onClore(affiche.defi.id, true) }, Modifier.weight(1f)) {
                                Text(stringResource(R.string.defis_reussi))
                            }
                            OutlinedButton(onClick = { onClore(affiche.defi.id, false) }, Modifier.weight(1f)) {
                                Text(stringResource(R.string.defis_rate))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onFin) { Text(stringResource(R.string.cloture_defis_plus_tard)) } },
    )
}
