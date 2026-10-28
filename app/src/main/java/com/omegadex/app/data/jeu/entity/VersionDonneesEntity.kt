package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Version des données de la base jeu (une seule ligne, id = 1), écrite par le script d'import.
 * [version] augmente à chaque nouvelle génération des données ; [sources] liste les sources utilisées.
 */
@Entity(tableName = "version_donnees")
data class VersionDonneesEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    val dateGeneration: String,
    val sources: String,
)
