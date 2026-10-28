package com.omegadex.app.data.progression

import androidx.room.migration.Migration

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
    /** Aucune migration tant que la base est en version 1. */
    val TOUTES: Array<Migration> = emptyArray()
}
