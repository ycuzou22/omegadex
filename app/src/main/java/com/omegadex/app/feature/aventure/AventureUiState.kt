package com.omegadex.app.feature.aventure

import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.rencontre.Debloqueur
import com.omegadex.app.domain.rencontre.Echange

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
        /** Rencontres débloquées à cette étape, groupées par lieu et par ce qui les débloque. */
        val rencontres: List<GroupeRencontresAffiche> = emptyList(),
    ) : AventureUiState
}

/**
 * Rencontres d'un lieu débloquées à l'étape. [debloqueur] non nul : un lieu déjà atteint où une capacité
 * ou un objet (Surf, canne…) rend de nouvelles rencontres possibles.
 */
data class GroupeRencontresAffiche(
    val lieuId: Int,
    val lieu: String,
    val debloqueur: Debloqueur?,
    val lignes: List<RencontreAffichee>,
)

data class LieuAffiche(val id: Int, val nom: String)

/**
 * Espèce qu'on peut rencontrer (ou recevoir par [echange]). [capturee] : déjà au moins capturée dans la run,
 * son image quitte l'ombre et la ligne montre son rubis au lieu du bouton « Attraper » (ou « Reçu »).
 */
data class RencontreAffichee(
    val especeId: Int,
    val nom: String,
    val types: List<String>,
    val methode: MethodeRencontre,
    val niveauMin: Int?,
    val niveauMax: Int?,
    val taux: Int?,
    val capturee: Boolean,
    val echange: Echange? = null,
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
