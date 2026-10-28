package com.omegadex.app.feature.defis

import com.omegadex.app.domain.model.Difficulte

/** Actions de l'espace Défis, regroupées pour garder des signatures courtes. */
class ActionsDefis(
    val onOnglet: (OngletDefis) -> Unit,
    val onFiltre: (Difficulte?) -> Unit,
    val onRelever: (String) -> Unit,
    val onValider: (Long, Boolean) -> Unit,
    val onRetenter: (Long) -> Unit,
    val onEspece: (Int) -> Unit,
    val onRetour: (() -> Unit)?,
)
