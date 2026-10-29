package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.ModeleDefiEntity
import kotlinx.coroutines.flow.Flow

/** Modèles de défis lus par le moteur de défis (T11). */
@Dao
interface ModeleDefiDao {
    /**
     * Modèles saisis à la main (`defis.json`). Les identifiants à partir de 100000 sont la banque de défis
     * par famille d'évolution (`tools/import/import_banque_defis.py`), qui se lit à part, famille par famille.
     */
    @Query("SELECT * FROM modele_defi WHERE id < 100000 ORDER BY id")
    fun modeles(): Flow<List<ModeleDefiEntity>>

    @Query("SELECT * FROM modele_defi WHERE etapeId = :etapeId ORDER BY difficulteMin, id")
    fun modelesDeLEtape(etapeId: Int): Flow<List<ModeleDefiEntity>>
}
