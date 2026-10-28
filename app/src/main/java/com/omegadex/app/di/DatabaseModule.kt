package com.omegadex.app.di

import android.content.Context
import androidx.room.Room
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ChiffrementProgression
import com.omegadex.app.data.progression.MigrationsProgression
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.securite.FournisseurCleBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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

    /**
     * Base progression : jamais recréée, seulement migrée, et chiffrée par SQLCipher (AES-256).
     * La phrase secrète est protégée par l'Android Keystore ; une base créée en clair par une version
     * antérieure est chiffrée au premier lancement, sans perte de données.
     */
    @Provides
    @Singleton
    fun progressionDatabase(@ApplicationContext context: Context, cle: FournisseurCleBase): ProgressionDatabase {
        ChiffrementProgression.chargerBibliotheque()
        val phrase = cle.phraseSecrete()
        ChiffrementProgression.chiffrerSiNecessaire(context.getDatabasePath(ProgressionDatabase.NOM_FICHIER), phrase)
        return Room.databaseBuilder(context, ProgressionDatabase::class.java, ProgressionDatabase.NOM_FICHIER)
            .openHelperFactory(SupportOpenHelperFactory(phrase.toByteArray(Charsets.UTF_8)))
            .addMigrations(*MigrationsProgression.TOUTES)
            .build()
    }
}
