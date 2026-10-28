package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.VersionDonneesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionDonneesDao {
    @Query("SELECT * FROM version_donnees WHERE id = 1")
    fun version(): Flow<VersionDonneesEntity?>
}
