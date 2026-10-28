package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.ModeleDefiEntity
import kotlinx.coroutines.flow.Flow

/** Modèles de défis lus par le moteur de défis (T11). */
@Dao
interface ModeleDefiDao {
    @Query("SELECT * FROM modele_defi ORDER BY id")
    fun modeles(): Flow<List<ModeleDefiEntity>>

    @Query("SELECT * FROM modele_defi WHERE etapeId = :etapeId ORDER BY difficulteMin, id")
    fun modelesDeLEtape(etapeId: Int): Flow<List<ModeleDefiEntity>>
}
