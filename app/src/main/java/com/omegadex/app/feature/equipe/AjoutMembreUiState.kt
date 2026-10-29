package com.omegadex.app.feature.equipe

import com.omegadex.app.domain.model.StatutEspece

sealed interface AjoutMembreUiState {
    data object Chargement : AjoutMembreUiState
    data object AucuneRun : AjoutMembreUiState

    data class Choix(
        /** Nom du membre remplacé quand on échange un emplacement occupé. */
        val remplace: String?,
        val emplacement: Int?,
        val equipePleine: Boolean,
        val recherche: String,
        /** Espèces capturées hors équipe, seules autorisées. */
        val especes: List<EspeceAjoutable>,
        val niveauParDefaut: Int,
        /** Espèce choisie, en attente du niveau. */
        val choisie: EspeceAjoutable?,
        /** Défi proposé au nouveau membre, une fois ajouté. */
        val defiPropose: DefiProposeVue?,
        /** L'ajout est fini : l'écran se ferme. */
        val termine: Boolean,
    ) : AjoutMembreUiState
}

data class EspeceAjoutable(
    val id: Int,
    val numeroHoenn: Int?,
    val nom: String,
    val types: List<String>,
    val statut: StatutEspece,
)

data class DefiProposeVue(val titre: String, val enonce: String)
