package com.omegadex.app.feature.aventure

/** État de l'écran Aventure : seule l'étape en cours de la run active est montrée. */
sealed interface AventureUiState {
    data object Chargement : AventureUiState
    data object AucuneRun : AventureUiState

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
    ) : AventureUiState
}

data class QueteAffichee(val id: Int, val description: String, val recompense: String?, val cochee: Boolean)
