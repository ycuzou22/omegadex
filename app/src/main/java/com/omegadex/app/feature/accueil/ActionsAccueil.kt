package com.omegadex.app.feature.accueil

/** Actions de l'accueil, regroupées pour garder des signatures courtes. */
class ActionsAccueil(
    val onChangerDeJoueur: () -> Unit,
    val onMesRuns: () -> Unit,
    val onCocher: (Int, Boolean) -> Unit,
    val onRelever: () -> Unit,
)
