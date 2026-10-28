package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.MethodeRencontre

/** Possibilité de trouver une espèce dans un lieu, avec la méthode et la fourchette de niveaux. */
@Entity(
    tableName = "rencontre",
    foreignKeys = [
        ForeignKey(entity = LieuEntity::class, parentColumns = ["id"], childColumns = ["lieuId"]),
        ForeignKey(entity = EspeceEntity::class, parentColumns = ["id"], childColumns = ["especeId"]),
    ],
    indices = [Index("lieuId"), Index("especeId")],
)
data class RencontreEntity(
    @PrimaryKey val id: Int,
    val lieuId: Int,
    val especeId: Int,
    val methode: MethodeRencontre,
    val niveauMin: Int?,
    val niveauMax: Int?,
)
