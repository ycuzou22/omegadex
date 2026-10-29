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

    /** Banque de la famille de racine [racine] : identifiants 100000 + racine × 1000 + n (voir `BanqueDefis`). */
    @Query("SELECT * FROM modele_defi WHERE id > 100000 + :racine * 1000 AND id < 101000 + :racine * 1000 ORDER BY id")
    fun banque(racine: Int): Flow<List<ModeleDefiEntity>>

    /** Modèles des défis déjà enregistrés, quelle que soit leur origine. */
    @Query("SELECT * FROM modele_defi WHERE id IN (:ids) ORDER BY id")
    fun parIds(ids: List<Int>): Flow<List<ModeleDefiEntity>>
}
