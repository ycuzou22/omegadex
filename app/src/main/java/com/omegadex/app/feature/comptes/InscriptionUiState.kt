package com.omegadex.app.feature.comptes

import com.omegadex.app.domain.compte.ErreurInscription

data class InscriptionUiState(
    val identifiant: String = "",
    val motDePasse: String = "",
    val confirmation: String = "",
    val enCours: Boolean = false,
    val erreurs: Set<ErreurInscription> = emptySet(),
)
