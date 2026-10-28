package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Talent possible d'une espèce ; [cache] vaut vrai pour le talent caché (Navi-Dex). */
@Entity(
    tableName = "espece_talent",
    primaryKeys = ["especeId", "talentId"],
    foreignKeys = [
        ForeignKey(entity = EspeceEntity::class, parentColumns = ["id"], childColumns = ["especeId"]),
        ForeignKey(entity = TalentEntity::class, parentColumns = ["id"], childColumns = ["talentId"]),
    ],
    indices = [Index("talentId")],
)
data class EspeceTalentEntity(val especeId: Int, val talentId: Int, val cache: Boolean)
