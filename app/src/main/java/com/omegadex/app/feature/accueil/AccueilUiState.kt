package com.omegadex.app.feature.accueil

import com.omegadex.app.domain.model.Difficulte

/** État de l'écran d'accueil. Il s'enrichira avec le tableau de bord (T13). */
data class AccueilUiState(val version: String = "", val joueur: String = "", val run: RunAccueil? = null)

/** Résumé de la run active affiché en tête de l'accueil. */
data class RunAccueil(
    val nom: String,
    val difficulte: Difficulte,
    val etapeNumero: Int,
    val etapeTitre: String,
    val totalEtapes: Int,
)
