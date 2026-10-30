package com.omegadex.app.data.progression

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Stratégie de migration de la base progression, qui n'est jamais recréée :
 * 1. chaque changement de schéma augmente [ProgressionDatabase.VERSION] et ajoute ici une
 *    migration `Migration(n, n + 1)` (ou une `@AutoMigration` quand Room sait la déduire) ;
 * 2. le schéma exporté `app/schemas/…/n+1.json` est versionné avec la migration ;
 * 3. `MigrationsProgressionTest` migre une base créée à chaque version précédente jusqu'à la dernière
 *    et vérifie le schéma et les données.
 * Aucune migration destructive n'est autorisée sur cette base.
 */
object MigrationsProgression {
    /** Version 2 : nature et talent des membres de l'équipe, et boîtes du PC. */
    val DE_1_A_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `membre_equipe` ADD COLUMN `nature` TEXT")
            db.execSQL("ALTER TABLE `membre_equipe` ADD COLUMN `talent` TEXT")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pokemon_pc` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`runId` INTEGER NOT NULL, `boite` INTEGER NOT NULL, `especeId` INTEGER NOT NULL, " +
                    "`surnom` TEXT, `niveau` INTEGER NOT NULL, `nature` TEXT, `talent` TEXT, " +
                    "FOREIGN KEY(`runId`) REFERENCES `run`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pokemon_pc_runId` ON `pokemon_pc` (`runId`)")
        }
    }

    val TOUTES: Array<Migration> = arrayOf(DE_1_A_2)
}
