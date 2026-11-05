package com.omegadex.app.feature.defis

import com.omegadex.app.domain.defi.BilanDefis
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.TypeDefi

/** Onglets de l'espace Défis ; la page « Défis de … » d'un Pokémon n'a pas de roulette. */
enum class OngletDefis { ROULETTE, ACTIFS, TERMINES }

/** Un défi affiché : enregistré ([defiId] non nul) ou seulement proposé (tiré à la roulette). */
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

/** Ce que montre la tuile d'une catégorie. */
enum class EtatCase {
    /** La roulette peut tourner. */
    LIBRE,

    /** Un défi est tiré et attend d'être relevé. */
    TIRE,

    /** Un défi de la catégorie est en cours. */
    EN_COURS,

    /** Défi du champion pas encore ouvert. */
    VERROU,

    /** Aucun défi à tirer pour l'instant. */
    VIDE,
}

/** Tuile d'une catégorie sur la grille des roulettes. */
data class CaseRouletteUi(
    val categorie: CategorieDefi,
    val etat: EtatCase,
    /** Nombre de défis dans la roulette. */
    val nombre: Int,
    /** Défi en cours ou défi tiré, selon [etat]. */
    val defi: CarteDefi?,
    /** Relances encore possibles pendant l'étape ; null : illimitées. */
    val relancesRestantes: Int?,
    val peutTourner: Boolean,
    /** Quête à cocher pour ouvrir le défi du champion. */
    val queteRequise: String?,
)

/** Grille des six roulettes pour l'étape en cours. */
data class RouletteUi(val numeroEtape: Int, val adversaire: String, val cases: List<CaseRouletteUi>) {
    val occupees: Int get() = cases.count { it.etat == EtatCase.EN_COURS }
}

/** Un tour de roulette à animer : le rouleau défile et s'arrête sur [gagnant]. */
data class TourDeRoulette(val categorie: CategorieDefi, val gagnant: CarteDefi, val rouleau: List<CarteDefi>)

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
        val termines: List<CarteDefi>,
        val bilan: BilanDefis,
        /** Les six roulettes ; null sur la page d'un Pokémon ou après la dernière étape. */
        val roulette: RouletteUi?,
    ) : DefisUiState {
        val onglets: List<OngletDefis>
            get() = if (roulette == null) listOf(OngletDefis.ACTIFS, OngletDefis.TERMINES) else OngletDefis.entries

        val cartes: List<CarteDefi>
            get() = when (onglet) {
                OngletDefis.ACTIFS -> actifs
                OngletDefis.TERMINES -> termines
                OngletDefis.ROULETTE -> emptyList()
            }
    }
}
