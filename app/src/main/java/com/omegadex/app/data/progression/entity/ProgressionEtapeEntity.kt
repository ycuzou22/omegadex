package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** Avancement d'une étape dans une run : déverrouillée, puis validée quand ses quêtes principales sont faites. */
@Entity(
    tableName = "progression_etape",
    primaryKeys = ["runId", "etapeId"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressionEtapeEntity(
    val runId: Long,
    val etapeId: Int,
    val deverrouillee: Boolean,
    val dateDeverrouillage: Long?,
    val dateValidation: Long? = null,
)
