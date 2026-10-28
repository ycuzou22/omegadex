package com.omegadex.app.data.jeu

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omegadex.app.data.jeu.dao.AttaqueDao
import com.omegadex.app.data.jeu.dao.EspeceDao
import com.omegadex.app.data.jeu.dao.EtapeDao
import com.omegadex.app.data.jeu.dao.RencontreDao
import com.omegadex.app.data.jeu.entity.AttaqueAppriseEntity
import com.omegadex.app.data.jeu.entity.AttaqueEntity
import com.omegadex.app.data.jeu.entity.EspeceEntity
import com.omegadex.app.data.jeu.entity.EspeceTalentEntity
import com.omegadex.app.data.jeu.entity.EtapeEntity
import com.omegadex.app.data.jeu.entity.EvolutionEntity
import com.omegadex.app.data.jeu.entity.LieuEntity
import com.omegadex.app.data.jeu.entity.QueteEntity
import com.omegadex.app.data.jeu.entity.RencontreEntity
import com.omegadex.app.data.jeu.entity.TalentEntity

/**
 * Base « jeu » : données de Rubis Oméga, en lecture seule, livrées avec l'application
 * et remplacées à chaque mise à jour des données. Elle ne contient rien du joueur.
 */
@Database(
    entities = [
        EtapeEntity::class,
        LieuEntity::class,
        QueteEntity::class,
        EspeceEntity::class,
        TalentEntity::class,
        EspeceTalentEntity::class,
        EvolutionEntity::class,
        RencontreEntity::class,
        AttaqueEntity::class,
        AttaqueAppriseEntity::class,
    ],
    version = JeuDatabase.VERSION,
    exportSchema = true,
)
abstract class JeuDatabase : RoomDatabase() {
    abstract fun etapeDao(): EtapeDao

    abstract fun especeDao(): EspeceDao

    abstract fun attaqueDao(): AttaqueDao

    abstract fun rencontreDao(): RencontreDao

    companion object {
        const val VERSION = 1
        const val NOM_FICHIER = "omegadex_jeu.db"
    }
}
