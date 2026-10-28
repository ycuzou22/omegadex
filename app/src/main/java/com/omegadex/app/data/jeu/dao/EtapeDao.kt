package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.EtapeEntity
import com.omegadex.app.data.jeu.entity.LieuEntity
import com.omegadex.app.data.jeu.entity.QueteEntity
import kotlinx.coroutines.flow.Flow

/** Étapes de l'aventure, leurs quêtes et leurs lieux (écrans Accueil et Aventure). */
@Dao
interface EtapeDao {
    @Query("SELECT * FROM etape ORDER BY id")
    fun etapes(): Flow<List<EtapeEntity>>

    @Query("SELECT * FROM etape WHERE id = :id")
    fun etape(id: Int): Flow<EtapeEntity?>

    @Query(
        "SELECT * FROM quete WHERE etapeId = :etapeId ORDER BY CASE type WHEN 'PRINCIPALE' THEN 0 ELSE 1 END, ordre",
    )
    fun quetes(etapeId: Int): Flow<List<QueteEntity>>

    /** Quêtes qui se déroulent dans un lieu (dont les objets importants qu'on y obtient). */
    @Query(
        "SELECT * FROM quete WHERE lieuId = :lieuId " +
            "ORDER BY etapeId, CASE type WHEN 'PRINCIPALE' THEN 0 ELSE 1 END, ordre",
    )
    fun quetesDuLieu(lieuId: Int): Flow<List<QueteEntity>>

    @Query("SELECT * FROM lieu WHERE etapeDeblocageId = :etapeId ORDER BY nom")
    fun lieuxDebloques(etapeId: Int): Flow<List<LieuEntity>>

    @Query("SELECT * FROM lieu WHERE id = :id")
    fun lieu(id: Int): Flow<LieuEntity?>
}
