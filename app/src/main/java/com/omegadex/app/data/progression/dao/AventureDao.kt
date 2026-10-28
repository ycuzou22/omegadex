package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.QueteCocheeEntity
import kotlinx.coroutines.flow.Flow

/** Avancement des étapes et quêtes cochées d'une run (écrans Accueil et Aventure). */
@Dao
interface AventureDao {
    @Query("SELECT * FROM progression_etape WHERE runId = :runId ORDER BY etapeId")
    fun etapes(runId: Long): Flow<List<ProgressionEtapeEntity>>

    @Upsert
    suspend fun enregistrerEtape(etape: ProgressionEtapeEntity)

    @Query("SELECT queteId FROM quete_cochee WHERE runId = :runId")
    fun quetesCochees(runId: Long): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun cocher(quete: QueteCocheeEntity)

    @Query("DELETE FROM quete_cochee WHERE runId = :runId AND queteId = :queteId")
    suspend fun decocher(runId: Long, queteId: Int)
}
