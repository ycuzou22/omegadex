package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** Quête cochée dans une run (les quêtes cochées de la progression d'une étape). */
@Entity(
    tableName = "quete_cochee",
    primaryKeys = ["runId", "queteId"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class QueteCocheeEntity(val runId: Long, val queteId: Int, val dateCochage: Long)
