package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** Succès débloqué dans une run ; [succesId] est le nom de la constante du domaine (« CAPTURES_10 »…). */
@Entity(
    tableName = "succes",
    primaryKeys = ["runId", "succesId"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SuccesEntity(val runId: Long, val succesId: String, val dateDeblocage: Long)
