package com.omegadex.app.feature.fiche

import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.StatutEspece

/** Les six rubriques de la fiche, dans l'ordre de la maquette E · Fiche Pokémon. */
enum class OngletFiche { INFOS, STATS, EVOLUTION, ATTAQUES, LIEUX, DEFIS }

sealed interface FichePokemonUiState {
    data object Chargement : FichePokemonUiState
    data object Introuvable : FichePokemonUiState

    data class Fiche(
        val espece: Espece,
        /** Statut d'entraînement dans la run active ; null sans run. */
        val statut: StatutEspece?,
    ) : FichePokemonUiState
}
