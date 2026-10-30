package com.omegadex.app.feature.fiche

/** Actions de la fiche Pokémon, regroupées pour garder des signatures courtes. */
class ActionsFiche(
    val onRetour: () -> Unit,
    val onDefis: () -> Unit,
    val onAvancerStatut: () -> Unit,
    val onReculerStatut: () -> Unit,
    val onEspece: (Int) -> Unit,
    val onNotes: (String) -> Unit,
)
