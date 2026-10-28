package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.TypeLieu

/** Lieu de Hoenn, rattaché à l'étape à partir de laquelle il devient accessible. */
@Entity(
    tableName = "lieu",
    foreignKeys = [
        ForeignKey(entity = EtapeEntity::class, parentColumns = ["id"], childColumns = ["etapeDeblocageId"]),
    ],
    indices = [Index("etapeDeblocageId"), Index("nom")],
)
data class LieuEntity(@PrimaryKey val id: Int, val nom: String, val type: TypeLieu, val etapeDeblocageId: Int)
