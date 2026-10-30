package com.omegadex.app.feature.aventure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omegadex.app.R
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.domain.pokedex.RegleNational
import com.omegadex.app.ui.animations.AnimationBoiteBadges
import com.omegadex.app.ui.animations.AnimationEtapeValidee
import com.omegadex.app.ui.animations.TextesEtapeValidee
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son

/**
 * Animation « Étape validée » en plein écran, par-dessus la barre de navigation, comme la maquette.
 * Quand l'étape est une arène, la boîte à badges s'ouvre ensuite et le nouveau badge y est forgé.
 * Un retour arrière passe toute la séquence.
 */
@Composable
internal fun CelebrationEtape(evenement: EtapeValidee, onFin: () -> Unit) {
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
