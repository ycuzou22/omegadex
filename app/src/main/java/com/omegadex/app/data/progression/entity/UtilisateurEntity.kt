package com.omegadex.app.data.progression.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Compte local. Le mot de passe n'est jamais stocké : seuls son empreinte ([hashMotDePasse])
 * et le [sel] utilisé sont conservés (algorithme défini avec T06).
 */
@Entity(tableName = "utilisateur", indices = [Index(value = ["identifiant"], unique = true)])
data class UtilisateurEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unique sans tenir compte de la casse : « Yamao » et « yamao » sont le même compte. */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val identifiant: String,
    val hashMotDePasse: String,
    val sel: String,
    val dateCreation: Long,
)
