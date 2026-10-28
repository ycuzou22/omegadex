package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omegadex.app.data.progression.entity.ProgressionEspeceEntity
import kotlinx.coroutines.flow.Flow

/** Statut d'entraînement des espèces d'une run (écran Pokédex, fiche, jauges). */
@Dao
interface PokedexDao {
    @Query("SELECT * FROM progression_espece WHERE runId = :runId")
    fun statuts(runId: Long): Flow<List<ProgressionEspeceEntity>>

    @Query("SELECT * FROM progression_espece WHERE runId = :runId AND especeId = :especeId")
    fun statut(runId: Long, especeId: Int): Flow<ProgressionEspeceEntity?>

    @Upsert
    suspend fun enregistrer(progression: ProgressionEspeceEntity)
}
