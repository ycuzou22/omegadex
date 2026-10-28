package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import kotlinx.coroutines.flow.Flow

/** Équipe actuelle d'une run (écran Équipe). */
@Dao
interface EquipeDao {
    @Query("SELECT * FROM membre_equipe WHERE runId = :runId ORDER BY emplacement")
    fun equipe(runId: Long): Flow<List<MembreEquipeEntity>>

    @Upsert
    suspend fun placer(membre: MembreEquipeEntity)

    @Query("DELETE FROM membre_equipe WHERE runId = :runId AND emplacement = :emplacement")
    suspend fun retirer(runId: Long, emplacement: Int)
}
