package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.Nature

/** Pokémon rangé dans une boîte du PC de la run. */
@Entity(
    tableName = "pokemon_pc",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId")],
)
data class PokemonPcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val boite: Int,
    val especeId: Int,
    val surnom: String? = null,
    val niveau: Int,
    val nature: Nature? = null,
    val talent: String? = null,
)
