package com.omegadex.app.feature.comptes

import com.omegadex.app.domain.model.Utilisateur

data class ConnexionUiState(
    val comptes: List<Utilisateur> = emptyList(),
    val identifiant: String = "",
    val motDePasse: String = "",
    val enCours: Boolean = false,
    val erreur: ErreurConnexion? = null,
)

sealed interface ErreurConnexion {
    data object IdentifiantsIncorrects : ErreurConnexion

    data class Attente(val secondes: Long) : ErreurConnexion

    data object ChampsVides : ErreurConnexion
}
