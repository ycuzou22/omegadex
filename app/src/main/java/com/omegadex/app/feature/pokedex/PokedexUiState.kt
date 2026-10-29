package com.omegadex.app.feature.pokedex

import com.omegadex.app.domain.model.StatutEspece

/** État de l'écran Pokédex de la run active. */
sealed interface PokedexUiState {
    data object Chargement : PokedexUiState
    data object AucuneRun : PokedexUiState

    /**
     * [national] : le Pokédex national est débloqué (Ligue validée), sinon seul le régional de Hoenn est montré.
     * [vues] et [capturees] comptent les espèces au moins vues et au moins capturées.
     */
    data class Pret(
        val runId: Long,
        val national: Boolean,
        val especes: List<EspeceAffichee>,
        val vues: Int,
        val capturees: Int,
    ) : PokedexUiState
}

/**
 * Évènement ponctuel : le joueur vient de changer le statut d'une espèce.
 * [capture] signale l'entrée de l'espèce dans les capturées : c'est le déclencheur de l'animation
 * « Capture → Pokédex » (T14.2).
 */
data class StatutModifie(val especeId: Int, val nom: String, val avant: StatutEspece, val apres: StatutEspece) {
    val capture: Boolean get() = avant < StatutEspece.CAPTUREE && apres >= StatutEspece.CAPTUREE
}

/**
 * Case de la grille. [numero] est le numéro régional de Hoenn, ou le numéro national quand le national est
 * débloqué. [aPortee] : l'espèce se trouve dans une zone déjà débloquée et n'est pas encore capturée,
 * elle est mise en avant.
 */
data class EspeceAffichee(
    val id: Int,
    val numero: Int,
    val nom: String,
    val types: List<String>,
    val statut: StatutEspece,
    val echangeRequis: Boolean,
    val aPortee: Boolean,
)
