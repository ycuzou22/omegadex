package com.omegadex.app.feature.equipe

import com.omegadex.app.domain.equipe.Affinite
import com.omegadex.app.domain.model.Difficulte

/** Nombre d'emplacements de l'équipe. */
const val TAILLE_EQUIPE = 6

sealed interface EquipeUiState {
    data object Chargement : EquipeUiState
    data object AucuneRun : EquipeUiState

    data class Equipe(
        val difficulte: Difficulte,
        val etapeTitre: String?,
        /** Niveau du Pokémon le plus fort du prochain champion ; null après la dernière étape. */
        val plafond: Int?,
        /** Les [TAILLE_EQUIPE] emplacements, dans l'ordre ; un emplacement libre a un membre null. */
        val emplacements: List<EmplacementVue>,
        /** Type dominant de l'adversaire de l'étape en cours (clé de type), s'il en a un. */
        val typeAdverse: String? = null,
        /** Captures jamais utilisées à faire entrer dans l'équipe (F10). */
        val suggestions: List<SuggestionVue> = emptyList(),
    ) : EquipeUiState {
        val membres: List<MembreVue> get() = emplacements.mapNotNull { it.membre }
        val auDessusDuPlafond: Int get() = membres.count { it.ecartPlafond != null && it.ecartPlafond > 0 }
    }
}

data class EmplacementVue(val numero: Int, val membre: MembreVue?)

data class MembreVue(
    val especeId: Int,
    val nom: String,
    val surnom: String?,
    val types: List<String>,
    val niveau: Int,
    /** Niveau moins le plafond : positif au-dessus, négatif en dessous ; null sans plafond. */
    val ecartPlafond: Int?,
    /** Titre du défi actif de ce Pokémon, s'il en relève un. */
    val defiActif: String?,
)

/** Évolution proposée après la saisie d'un niveau ; le joueur confirme ou remet à plus tard. */
data class EvolutionProposee(val emplacement: Int, val avant: EspeceVue, val cibles: List<CibleEvolution>)

data class EspeceVue(val id: Int, val nom: String, val types: List<String>)

data class CibleEvolution(val espece: EspeceVue, val niveauRequis: Int, val precision: String?)

/** Évolution confirmée, jouée en plein écran. */
data class EvolutionEnCours(val avant: EspeceVue, val apres: EspeceVue)

/** Suggestion de rotation : espèce capturée jamais utilisée, à ajouter au [niveau] du plafond. */
data class SuggestionVue(
    val especeId: Int,
    val nom: String,
    val types: List<String>,
    val affinite: Affinite,
    val niveau: Int,
)
