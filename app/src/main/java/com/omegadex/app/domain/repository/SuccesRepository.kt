package com.omegadex.app.domain.repository

import com.omegadex.app.domain.succes.Succes
import kotlinx.coroutines.flow.Flow

/** Succès débloqués des runs du joueur connecté. */
interface SuccesRepository {
    /** Succès débloqués de la run, avec leur date de déblocage. */
    fun succes(runId: Long): Flow<Map<Succes, Long>>

    /**
     * Enregistre les succès [atteints] ; renvoie seulement ceux qui n'étaient pas encore débloqués
     * (un succès n'est débloqué qu'une fois et garde sa première date).
     */
    suspend fun debloquer(runId: Long, atteints: Set<Succes>): List<Succes>
}
