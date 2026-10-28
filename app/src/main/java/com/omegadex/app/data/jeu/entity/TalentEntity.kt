package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "talent")
data class TalentEntity(@PrimaryKey val id: Int, val nom: String, val description: String)
