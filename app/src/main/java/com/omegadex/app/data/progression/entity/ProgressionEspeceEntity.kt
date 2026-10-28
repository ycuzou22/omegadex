package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.omegadex.app.domain.model.StatutEspece

/** Statut d'entraînement d'une espèce dans une run (absente de la table = non vue). */
@Entity(
    tableName = "progression_espece",
    primaryKeys = ["runId", "especeId"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressionEspeceEntity(
    val runId: Long,
    val especeId: Int,
    val statut: StatutEspece,
    val notes: String = "",
    val dateMaj: Long,
)
