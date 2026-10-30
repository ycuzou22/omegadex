package com.omegadex.app.feature.pc

import com.omegadex.app.domain.equipe.ResultatPc
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon

sealed interface PcUiState {
    data object Chargement : PcUiState
    data object AucuneRun : PcUiState

    data class Pc(
        /** Boîte affichée, de 1 à [nombreBoites]. */
        val boite: Int,
        val nombreBoites: Int,
        /** Les places de la boîte affichée, dans l'ordre ; une place libre vaut null. */
        val cases: List<PokemonVue?>,
        /** Les 6 emplacements de l'équipe ; un emplacement libre vaut null. */
        val equipe: List<PokemonVue?>,
        /** Pokémon touché, dont la fiche s'affiche sous la boîte. */
        val fiche: FichePc?,
        /** Dernier déplacement refusé, expliqué dans la fiche. */
        val refus: ResultatPc?,
    ) : PcUiState {
        val occupees: Int get() = cases.count { it != null }
    }
}

data class PokemonVue(
    val place: PlacePokemon,
    val especeId: Int,
    val nom: String,
    val surnom: String?,
    val types: List<String>,
    val niveau: Int,
    val nature: Nature?,
    val talent: String?,
)

data class FichePc(
    val pokemon: PokemonVue,
    /** Talents de l'espèce, à proposer au joueur. */
    val talents: List<String>,
) {
    val dansEquipe: Boolean get() = pokemon.place is PlacePokemon.Equipe
}
