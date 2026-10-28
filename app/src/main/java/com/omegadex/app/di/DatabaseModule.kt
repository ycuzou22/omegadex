package com.omegadex.app.di

import android.content.Context
import androidx.room.Room
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.MigrationsProgression
import com.omegadex.app.data.progression.ProgressionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Ouvre les deux bases Room de l'application. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Base jeu : copiée depuis les assets au premier lancement (disponible hors ligne) et
     * recopiée quand sa version change. Elle est en lecture seule par construction :
     * ses DAO ne contiennent que des requêtes SELECT, aucune insertion ni modification.
     */
    @Provides
    @Singleton
    fun jeuDatabase(@ApplicationContext context: Context): JeuDatabase =
        Room.databaseBuilder(context, JeuDatabase::class.java, JeuDatabase.NOM_FICHIER)
            .createFromAsset(JeuDatabase.CHEMIN_ASSET)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    /** Base progression : jamais recréée, seulement migrée. */
    @Provides
    @Singleton
    fun progressionDatabase(@ApplicationContext context: Context): ProgressionDatabase =
        Room.databaseBuilder(context, ProgressionDatabase::class.java, ProgressionDatabase.NOM_FICHIER)
            .addMigrations(*MigrationsProgression.TOUTES)
            .build()
}
