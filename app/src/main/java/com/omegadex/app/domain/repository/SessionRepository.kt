package com.omegadex.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Joueur connecté sur l'appareil (null si personne). */
interface SessionRepository {
    val utilisateurConnecte: StateFlow<Long?>

    suspend fun ouvrir(utilisateurId: Long)

    suspend fun fermer()
}
