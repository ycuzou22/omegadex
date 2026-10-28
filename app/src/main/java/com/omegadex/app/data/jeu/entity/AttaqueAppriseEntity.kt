package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.omegadex.app.domain.model.MethodeApprentissage

/**
 * Attaque apprise par une espèce dans Rubis Oméga. [niveau] vaut 0 hors apprentissage par niveau
 * (et 1 pour les attaques de départ) ; [numeroCt] identifie la CT / CS le cas échéant.
 */
@Entity(
    tableName = "attaque_apprise",
    primaryKeys = ["especeId", "attaqueId", "methode", "niveau"],
    foreignKeys = [
        ForeignKey(entity = EspeceEntity::class, parentColumns = ["id"], childColumns = ["especeId"]),
        ForeignKey(entity = AttaqueEntity::class, parentColumns = ["id"], childColumns = ["attaqueId"]),
    ],
    indices = [Index("attaqueId")],
)
data class AttaqueAppriseEntity(
    val especeId: Int,
    val attaqueId: Int,
    val methode: MethodeApprentissage,
    val niveau: Int,
    val numeroCt: String?,
)
