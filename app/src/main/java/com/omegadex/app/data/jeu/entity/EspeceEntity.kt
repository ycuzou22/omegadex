package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Espèce Pokémon. L'identifiant est le numéro national ; [numeroHoenn] est nul
 * pour les espèces absentes du Pokédex régional (visibles après la Ligue).
 * [ratioMale] est la part de mâles entre 0 et 1, nulle pour une espèce asexuée.
 */
@Entity(tableName = "espece", indices = [Index("numeroHoenn"), Index("nom")])
data class EspeceEntity(
    @PrimaryKey val id: Int,
    val numeroHoenn: Int?,
    val nom: String,
    val type1: String,
    val type2: String?,
    val categorie: String,
    val tailleM: Double,
    val poidsKg: Double,
    val pv: Int,
    val attaque: Int,
    val defense: Int,
    val attaqueSpeciale: Int,
    val defenseSpeciale: Int,
    val vitesse: Int,
    val evDonnes: String,
    val tauxCapture: Int,
    val ratioMale: Double?,
    val groupesOeufs: String,
    val aUneMega: Boolean,
)
