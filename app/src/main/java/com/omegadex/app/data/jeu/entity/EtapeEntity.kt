package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Étape de l'aventure (0 = départ … 12 = post-game). L'identifiant est l'ordre de l'étape.
 * [plafond] est le niveau du Pokémon le plus fort du champion : aucun défi ne demande de le dépasser.
 */
@Entity(tableName = "etape")
data class EtapeEntity(
    @PrimaryKey val id: Int,
    val titre: String,
    val adversaire: String,
    val typeDominant: String?,
    val plafond: Int,
    val lieuPrincipal: String,
)
