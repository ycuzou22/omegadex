package com.omegadex.app.feature.aventure

/** État de l'écran Aventure : seule l'étape en cours de la run active est montrée. */
sealed interface AventureUiState {
    data object Chargement : AventureUiState
    data object AucuneRun : AventureUiState

    /** Toutes les étapes de la run sont validées. */
    data object Terminee : AventureUiState

    data class EtapeEnCours(
        val runId: Long,
        val numero: Int,
        val total: Int,
        val titre: String,
        val adversaire: String,
        val typeDominant: String?,
        val plafond: Int,
        val lieuPrincipal: String,
        val principales: List<QueteAffichee>,
        val annexes: List<QueteAffichee>,
        val lieux: List<LieuAffiche> = emptyList(),
    ) : AventureUiState
}

data class LieuAffiche(val id: Int, val nom: String)

/** Évènement ponctuel : l'étape vient d'être validée (déclencheur de l'animation « Étape validée », T14.1). */
data class EtapeValidee(val titre: String, val suivante: String?)

data class QueteAffichee(val id: Int, val description: String, val recompense: String?, val cochee: Boolean)
