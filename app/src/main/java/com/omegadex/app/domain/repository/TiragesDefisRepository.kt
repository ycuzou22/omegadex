package com.omegadex.app.domain.repository

import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.Tirage
import kotlinx.coroutines.flow.Flow

/** Défis tirés à la roulette et pas encore relevés, un par catégorie et par run. */
interface TiragesDefisRepository {
    fun tirages(runId: Long): Flow<Map<CategorieDefi, Tirage>>

    suspend fun enregistrer(runId: Long, categorie: CategorieDefi, tirage: Tirage)

    suspend fun effacer(runId: Long, categorie: CategorieDefi)
}
