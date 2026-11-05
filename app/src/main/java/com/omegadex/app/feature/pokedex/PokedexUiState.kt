package com.omegadex.app.feature.pokedex

import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.OriginesEspece

/** État de l'écran Pokédex de la run active. */
sealed interface PokedexUiState {
    data object Chargement : PokedexUiState
    data object AucuneRun : PokedexUiState

    /**
     * [national] : le Pokédex national est débloqué (Ligue validée), sinon seul le régional de Hoenn est montré.
     * [vues] et [capturees] comptent les espèces au moins vues et au moins capturées, sur tout le Pokédex.
     * [resultats] : les espèces de [especes] qui répondent aux [filtres] ; c'est ce que montre la grille.
     * [typesDisponibles] : les types présents, proposés dans le filtre par type.
     */
    data class Pret(
        val runId: Long,
        val national: Boolean,
        val especes: List<EspeceAffichee>,
        val vues: Int,
        val capturees: Int,
        val filtres: FiltresPokedex = FiltresPokedex(),
        val resultats: List<EspeceAffichee> = especes,
        val typesDisponibles: List<String> = emptyList(),
        /** Zones où l'on rencontre des Pokémon, dans l'ordre de l'aventure, pour le filtre par zone. */
        val zonesDisponibles: List<Lieu> = emptyList(),
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
    /** Lieux et méthodes de rencontre de l'espèce, pour les filtres zone et méthode. */
    val origines: OriginesEspece = OriginesEspece(),
)
