package com.omegadex.app.feature.defis

import com.omegadex.app.domain.defi.BilanDefis
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.TypeDefi

enum class OngletDefis { ACTIFS, DISPONIBLES, TERMINES }

/** Un défi affiché : enregistré ([defiId] non nul) ou seulement proposé. */
data class CarteDefi(
    val cle: String,
    val defiId: Long?,
    val titre: String,
    val enonce: String,
    val type: TypeDefi,
    val difficulte: Difficulte,
    val especeId: Int?,
    val especeNom: String?,
    val etat: EtatDefi?,
)

sealed interface DefisUiState {
    data object Chargement : DefisUiState
    data object AucuneRun : DefisUiState

    data class Espace(
        /** Espèce de la page « Défis de … », null pour l'onglet Défis. */
        val especeNom: String?,
        val difficulteRun: Difficulte,
        val onglet: OngletDefis,
        /** Filtre par difficulté minimale du modèle ; null = toutes. */
        val filtre: Difficulte?,
        val actifs: List<CarteDefi>,
        val disponibles: List<CarteDefi>,
        val termines: List<CarteDefi>,
        val bilan: BilanDefis,
    ) : DefisUiState {
        val cartes: List<CarteDefi>
            get() = when (onglet) {
                OngletDefis.ACTIFS -> actifs
                OngletDefis.DISPONIBLES -> disponibles
                OngletDefis.TERMINES -> termines
            }
    }
}
