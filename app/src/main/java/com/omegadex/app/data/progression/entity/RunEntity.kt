package com.omegadex.app.data.progression.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.Difficulte

/**
 * Partie d'un utilisateur. Supprimer l'utilisateur supprime ses runs et, en cascade,
 * toute leur progression. [starterId] et les autres identifiants de la base jeu ne sont pas
 * des clés étrangères : les deux bases sont séparées.
 */
@Entity(
    tableName = "run",
    foreignKeys = [
        ForeignKey(
            entity = UtilisateurEntity::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("utilisateurId")],
)
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val utilisateurId: Long,
    val nom: String,
    val starterId: Int,
    val difficulte: Difficulte,
    /** Règles optionnelles choisies à la création (liste séparée par des virgules). */
    val regles: String = "",
    val dateDebut: Long,
    val dateFin: Long? = null,
)
