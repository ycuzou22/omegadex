package com.omegadex.app.feature.fiche

import com.omegadex.app.domain.model.AttaqueApprise
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.model.TalentEspece

/** Les six rubriques de la fiche, dans l'ordre de la maquette E · Fiche Pokémon. */
enum class OngletFiche { INFOS, STATS, EVOLUTION, ATTAQUES, LIEUX, DEFIS }

sealed interface FichePokemonUiState {
    data object Chargement : FichePokemonUiState
    data object Introuvable : FichePokemonUiState

    data class Fiche(
        val espece: Espece,
        /** Statut d'entraînement dans la run active ; null sans run. */
        val statut: StatutEspece?,
        val talents: List<TalentEspece> = emptyList(),
        /** Stade de la famille qui peut méga-évoluer (l'espèce elle-même ou une évolution), s'il y en a un. */
        val stadeMega: String? = null,
        val evolution: ChaineEvolution = ChaineEvolution(emptyList(), emptyList(), null),
        val attaques: List<AttaqueApprise> = emptyList(),
        /** Niveau du Pokémon de l'équipe de cette espèce (le plus haut s'il y en a plusieurs). */
        val niveauActuel: Int? = null,
        val lieux: List<LieuFiche> = emptyList(),
        /** L'espèce ne s'obtient que par échange ou transfert. */
        val echangeRequis: Boolean = false,
        /** Défis de l'espèce dans la run active ; null sans run. */
        val defis: DefisFiche? = null,
        val notes: String = "",
    ) : FichePokemonUiState
}
