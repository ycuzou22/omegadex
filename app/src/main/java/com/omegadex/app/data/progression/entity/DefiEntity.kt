package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.EtatDefi

/** Défi tiré d'un modèle, ciblant une espèce ou une étape, avec son état dans la run. */
@Entity(
    tableName = "defi",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId"), Index("especeId"), Index("etapeId")],
)
data class DefiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val modeleId: Int,
    val especeId: Int?,
    val etapeId: Int?,
    val etat: EtatDefi,
    val dateDebut: Long,
    val dateFin: Long? = null,
)
