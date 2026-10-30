package com.omegadex.app.feature.pc

import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon

/** Actions de l'écran du PC. */
class ActionsPc(
    val onFermer: () -> Unit,
    /** Affiche la boîte donnée (de 1 au nombre de boîtes). */
    val onBoite: (Int) -> Unit,
    val onChoisir: (PlacePokemon) -> Unit,
    val onDeplacer: () -> Unit,
    val onNature: (Nature?) -> Unit,
    val onTalent: (String?) -> Unit,
)
