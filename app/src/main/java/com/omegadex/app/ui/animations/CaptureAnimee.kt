package com.omegadex.app.ui.animations

import com.omegadex.app.domain.model.StatutEspece

/**
 * Capture à animer (« Capture → Pokédex », T14.2) : l'espèce [especeId] vient d'être attrapée.
 * [cases] est le morceau de la grille du Pokédex autour d'elle, rangé comme dans l'écran Pokédex (3 colonnes) ;
 * [capturees] compte les espèces capturées avant cette capture, sur les [total] du Pokédex affiché.
 */
data class CaptureAnimee(
    val especeId: Int,
    val nom: String,
    val national: Boolean,
    val cases: List<CaseCapture>,
    val capturees: Int,
    val total: Int,
)

/** Une case de la grille, avec le statut de l'espèce [avant] et [apres] la capture. */
data class CaseCapture(
    val especeId: Int,
    val numero: Int,
    val nom: String,
    val types: List<String>,
    val avant: StatutEspece,
    val apres: StatutEspece,
)
