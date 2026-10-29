package com.omegadex.app.feature.aventure

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omegadex.app.R
import com.omegadex.app.ui.animations.AnimationEtapeValidee
import com.omegadex.app.ui.animations.TextesEtapeValidee

/**
 * Animation « Étape validée » en plein écran, par-dessus la barre de navigation, comme la maquette.
 * Un retour arrière la passe.
 */
@Composable
internal fun CelebrationEtape(evenement: EtapeValidee, onFin: () -> Unit) {
    val textes = TextesEtapeValidee(
        surtitre = stringResource(R.string.anim_etape_validee, evenement.numero),
        titre = evenement.badge ?: evenement.titre,
        sousTitre = evenement.suivante?.let { stringResource(R.string.anim_prochaine_etape, it) }
            ?: stringResource(R.string.anim_aventure_terminee),
    )
    Dialog(
        onDismissRequest = onFin,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AnimationEtapeValidee(textes, onFin)
    }
}
