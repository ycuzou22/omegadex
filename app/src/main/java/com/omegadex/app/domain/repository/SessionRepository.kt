package com.omegadex.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Joueur connecté sur l'appareil. */
interface SessionRepository {
    /** [EtatSession.Chargement] le temps de relire la session enregistrée au démarrage. */
    val etat: StateFlow<EtatSession>

    /** Identifiant du joueur connecté, ou null (aussi pendant le chargement). */
    val utilisateurConnecte: StateFlow<Long?>

    suspend fun ouvrir(utilisateurId: Long)

    /** Déconnexion : la session est effacée, l'app revient à la liste des comptes. */
    suspend fun fermer()
}

sealed interface EtatSession {
    data object Chargement : EtatSession

    data object Deconnecte : EtatSession

    data class Connecte(val utilisateurId: Long) : EtatSession
}
