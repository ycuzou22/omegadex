package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.CategorieAttaque

/** Attaque ; [puissance] et [precision] sont nulles quand elles ne s'appliquent pas. */
@Entity(tableName = "attaque", indices = [Index("nom")])
data class AttaqueEntity(
    @PrimaryKey val id: Int,
    val nom: String,
    val type: String,
    val categorie: CategorieAttaque,
    val puissance: Int?,
    val precision: Int?,
    val pp: Int,
    val description: String,
)
