package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.domain.model.EtatDefi
import kotlinx.coroutines.flow.Flow

/** Défis d'une run (écran Défis, fiche Pokémon, équipe). */
@Dao
interface DefiDao {
    @Query("SELECT * FROM defi WHERE runId = :runId ORDER BY dateDebut DESC")
    fun defis(runId: Long): Flow<List<DefiEntity>>

    @Query("SELECT * FROM defi WHERE runId = :runId AND especeId = :especeId ORDER BY dateDebut DESC")
    fun defisDeLEspece(runId: Long, especeId: Int): Flow<List<DefiEntity>>

    @Insert
    suspend fun ajouter(defi: DefiEntity): Long

    @Query("UPDATE defi SET etat = :etat, dateFin = :dateFin WHERE id = :id AND runId = :runId")
    suspend fun changerEtat(runId: Long, id: Long, etat: EtatDefi, dateFin: Long?)
}
