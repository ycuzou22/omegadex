package com.omegadex.app.data.progression

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import com.omegadex.app.data.progression.entity.ProgressionEspeceEntity
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.QueteCocheeEntity
import com.omegadex.app.data.progression.entity.RunEntity
import com.omegadex.app.data.progression.entity.UtilisateurEntity

/**
 * Base « progression » : comptes et parties des joueurs. Elle n'est jamais écrasée :
 * chaque changement de schéma passe par une migration testée.
 * Toute ligne dépend d'une run, elle-même rattachée à un utilisateur (suppression en cascade).
 */
@Database(
    entities = [
        UtilisateurEntity::class,
        RunEntity::class,
        ProgressionEtapeEntity::class,
        QueteCocheeEntity::class,
        ProgressionEspeceEntity::class,
        DefiEntity::class,
        MembreEquipeEntity::class,
    ],
    version = ProgressionDatabase.VERSION,
    exportSchema = true,
)
abstract class ProgressionDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 1
        const val NOM_FICHIER = "omegadex_progression.db"
    }
}
