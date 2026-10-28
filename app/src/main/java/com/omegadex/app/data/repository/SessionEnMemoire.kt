package com.omegadex.app.data.repository

import com.omegadex.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session gardée en mémoire le temps du processus (rendue persistante par T06.2). */
@Singleton
class SessionEnMemoire @Inject constructor() : SessionRepository {
    private val etat = MutableStateFlow<Long?>(null)
    override val utilisateurConnecte: StateFlow<Long?> = etat.asStateFlow()

    override suspend fun ouvrir(utilisateurId: Long) {
        etat.value = utilisateurId
    }

    override suspend fun fermer() {
        etat.value = null
    }
}
