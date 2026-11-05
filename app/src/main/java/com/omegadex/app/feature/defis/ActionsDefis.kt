package com.omegadex.app.feature.defis

import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.model.Difficulte

/** Actions de l'espace Défis, regroupées pour garder des signatures courtes. */
class ActionsDefis(
    val onOnglet: (OngletDefis) -> Unit,
    val onFiltre: (Difficulte?) -> Unit,
    val onValider: (Long, Boolean) -> Unit,
    val onRetenter: (Long) -> Unit,
    val onEspece: (Int) -> Unit,
    val onRetour: (() -> Unit)?,
)

/** Ce que le joueur peut faire depuis la grille des roulettes. */
internal class ActionsRoulettes(
    val onTourner: (CategorieDefi) -> Unit,
    val onVoirTirage: (CategorieDefi) -> Unit,
    val onVoirEnCours: () -> Unit,
)
