package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.TypeQuete

/** Quête d'une étape, dans l'ordre du guide. Les captures de zone sont aussi des quêtes. */
@Entity(
    tableName = "quete",
    foreignKeys = [
        ForeignKey(entity = EtapeEntity::class, parentColumns = ["id"], childColumns = ["etapeId"]),
        ForeignKey(entity = LieuEntity::class, parentColumns = ["id"], childColumns = ["lieuId"]),
    ],
    indices = [Index("etapeId"), Index("lieuId")],
)
data class QueteEntity(
    @PrimaryKey val id: Int,
    val etapeId: Int,
    val lieuId: Int?,
    val type: TypeQuete,
    val ordre: Int,
    val description: String,
    val recompense: String?,
)
