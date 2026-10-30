package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.omegadex.app.data.progression.entity.PokemonPcEntity
import kotlinx.coroutines.flow.Flow

/** Boîtes du PC d'une run. */
@Dao
interface PcDao {
    @Query("SELECT * FROM pokemon_pc WHERE runId = :runId ORDER BY boite, id")
    fun pc(runId: Long): Flow<List<PokemonPcEntity>>

    @Query("SELECT * FROM pokemon_pc WHERE runId = :runId AND id = :id")
    suspend fun pokemon(runId: Long, id: Long): PokemonPcEntity?

    @Query("SELECT COUNT(*) FROM pokemon_pc WHERE runId = :runId AND boite = :boite")
    suspend fun nombreDansBoite(runId: Long, boite: Int): Int

    @Insert
    suspend fun ranger(pokemon: PokemonPcEntity): Long

    @Update
    suspend fun modifier(pokemon: PokemonPcEntity)

    @Query("DELETE FROM pokemon_pc WHERE runId = :runId AND id = :id")
    suspend fun sortir(runId: Long, id: Long)
}
