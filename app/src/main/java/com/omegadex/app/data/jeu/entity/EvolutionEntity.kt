package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.ConditionEvolution

/** Évolution d'une espèce vers une autre ; [valeur] précise la condition (« 16 », « Pierre Feu »…). */
@Entity(
    tableName = "evolution",
    foreignKeys = [
        ForeignKey(entity = EspeceEntity::class, parentColumns = ["id"], childColumns = ["especeSourceId"]),
        ForeignKey(entity = EspeceEntity::class, parentColumns = ["id"], childColumns = ["especeCibleId"]),
    ],
    indices = [Index("especeSourceId"), Index("especeCibleId")],
)
data class EvolutionEntity(
    @PrimaryKey val id: Int,
    val especeSourceId: Int,
    val especeCibleId: Int,
    val condition: ConditionEvolution,
    val valeur: String?,
)
