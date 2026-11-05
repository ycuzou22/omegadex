package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omegadex.app.data.progression.entity.SuccesEntity
import kotlinx.coroutines.flow.Flow

/** Succès débloqués d'une run. */
@Dao
interface SuccesDao {
    @Query("SELECT * FROM succes WHERE runId = :runId ORDER BY dateDeblocage")
    fun succes(runId: Long): Flow<List<SuccesEntity>>

    /** Renvoie -1 pour un succès déjà débloqué : il garde sa première date. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun debloquer(succes: SuccesEntity): Long
}
