package com.omegadex.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** Dresseurs battus sans quête associée (dresseurs de route, Conseil 4…), par run : leurs clés. */
interface DresseursBattusRepository {
    fun battus(runId: Long): Flow<Set<String>>

    suspend fun marquer(runId: Long, cle: String, battu: Boolean)
}
