package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** Membre de l'équipe actuelle, dans l'un des 6 emplacements (1 à 6). */
@Entity(
    tableName = "membre_equipe",
    primaryKeys = ["runId", "emplacement"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MembreEquipeEntity(
    val runId: Long,
    val emplacement: Int,
    val especeId: Int,
    val surnom: String? = null,
    val niveau: Int,
)
