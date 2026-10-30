package com.omegadex.app.feature.accueil

import com.omegadex.app.domain.accueil.JaugesAccueil
import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.TypeDefi

/** État de l'écran d'accueil (tableau de bord de la run active). */
data class AccueilUiState(
    val version: String = "",
    val joueur: String = "",
    val run: RunAccueil? = null,
    val jauges: JaugesAccueil? = null,
    val defiDuJour: DefiDuJour? = null,
    /** Badges d'arène obtenus dans la run active (null sans run). */
    val badges: Set<BadgeArene>? = null,
)

/** Défi du jour proposé sur l'accueil, avec son bouton « Relever ». */
data class DefiDuJour(val type: TypeDefi, val titre: String, val enonce: String)

/** Résumé de la run active affiché en tête de l'accueil. */
data class RunAccueil(
    val nom: String,
    val difficulte: Difficulte,
    val etapeNumero: Int,
    val etapeTitre: String,
    val totalEtapes: Int,
)
