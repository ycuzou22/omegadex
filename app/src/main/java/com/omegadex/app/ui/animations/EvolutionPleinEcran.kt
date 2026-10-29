package com.omegadex.app.ui.animations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omegadex.app.R
import com.omegadex.app.ui.components.nomDuType

/**
 * Évolution plein écran, par-dessus toute l'app (barre de navigation comprise), comme la maquette.
 * À déclencher quand un Pokémon de l'équipe évolue ; [detail] peut annoncer un défi réussi grâce à l'évolution.
 */
@Composable
fun EvolutionPleinEcran(avant: EspeceAnimee, apres: EspeceAnimee, onFin: () -> Unit, detail: String? = null) {
    val textes = TextesEvolution(
        intro = stringResource(R.string.anim_evolution_intro, avant.nom),
        surtitre = stringResource(R.string.anim_evolution_surtitre),
        titre = stringResource(R.string.anim_evolution_titre, avant.nom, apres.nom),
        detail = detail,
        continuer = stringResource(R.string.anim_continuer),
        nomsTypes = apres.types.map { type -> nomDuType(type)?.let { stringResource(it) } ?: type },
    )
    Dialog(
        onDismissRequest = onFin,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AnimationEvolution(avant, apres, textes, onFin)
    }
}
