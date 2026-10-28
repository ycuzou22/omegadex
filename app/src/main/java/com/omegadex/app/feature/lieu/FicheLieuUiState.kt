package com.omegadex.app.feature.lieu

import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.TypeLieu

sealed interface FicheLieuUiState {
    data object Chargement : FicheLieuUiState
    data object Introuvable : FicheLieuUiState

    data class Fiche(
        val nom: String,
        val type: TypeLieu,
        val rencontres: List<GroupeRencontres>,
        val objets: List<String>,
    ) : FicheLieuUiState
}

/** Rencontres d'une même méthode (hautes herbes, pêche…), dans l'ordre du jeu. */
data class GroupeRencontres(val methode: MethodeRencontre, val lignes: List<LigneRencontre>)

data class LigneRencontre(
    val especeId: Int,
    val nom: String,
    val type1: String?,
    val niveauMin: Int?,
    val niveauMax: Int?,
    val taux: Int?,
    val condition: String?,
)
