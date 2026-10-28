package com.omegadex.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** Run affichée dans l'application pour chaque compte ; elle est retrouvée au redémarrage. */
interface RunActiveRepository {
    /**
     * Run active du compte : la dernière choisie si elle existe encore, sinon la plus récente,
     * ou null si le compte n'a aucune run.
     */
    fun runActive(utilisateurId: Long): Flow<Long?>

    suspend fun choisir(utilisateurId: Long, runId: Long)
}
