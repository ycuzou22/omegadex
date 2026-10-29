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
        val rencontres: List<RencontreAffichee> = emptyList(),
    ) : AventureUiState
}

data class LieuAffiche(val id: Int, val nom: String)

/**
 * Espèce qu'on peut rencontrer dans les lieux de l'étape (une ligne par espèce, au premier lieu où elle apparaît).
 * [capturee] : déjà au moins capturée dans la run, la ligne montre son rubis au lieu du bouton « Attraper ».
 */
data class RencontreAffichee(
    val especeId: Int,
    val nom: String,
    val type: String?,
    val lieu: String,
    val niveauMin: Int?,
    val niveauMax: Int?,
    val taux: Int?,
    val capturee: Boolean,
)

/** Évènement ponctuel : l'étape vient d'être validée (déclencheur de l'animation « Étape validée », T14.1). */
data class EtapeValidee(
    val numero: Int,
    val titre: String,
    /** Badge obtenu (récompense « Badge … » d'une quête principale), s'il y en a un. */
    val badge: String?,
    val suivante: String?,
    /** Étapes validées de la run, celle-ci comprise (badges déjà dans la boîte). */
    val etapesValidees: Set<Int> = emptySet(),
)

data class QueteAffichee(val id: Int, val description: String, val recompense: String?, val cochee: Boolean)
