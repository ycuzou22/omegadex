package com.omegadex.app.data.progression

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Chaque version exportée de la base progression doit s'ouvrir avec la version actuelle de Room,
 * après application des migrations, sans perdre de données.
 * À chaque nouvelle version : ajouter un test `migrer de n vers n + 1` qui insère des données
 * dans l'ancien schéma et vérifie qu'elles sont conservées.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationsProgressionTest {
    private val fichier: File = ApplicationProvider.getApplicationContext<android.content.Context>().getDatabasePath(
        NOM,
    )

    @After
    fun nettoyer() {
        fichier.parentFile?.listFiles()?.filter { it.name.startsWith(NOM) }?.forEach { it.delete() }
    }

    /** Crée une base au schéma exporté de [version], comme l'aurait laissée une ancienne version de l'app. */
    private fun creerBaseVersion(version: Int): SQLiteDatabase {
        val schema = File(DOSSIER_SCHEMAS, "$version.json")
        assertTrue("Schéma introuvable : ${schema.absolutePath}", schema.exists())
        val base = JSONObject(schema.readText()).getJSONObject("database")
        fichier.parentFile?.mkdirs()
        val sql = SQLiteDatabase.openOrCreateDatabase(fichier, null)
        val entites = base.getJSONArray("entities")
        for (i in 0 until entites.length()) {
            val entite = entites.getJSONObject(i)
            val table = entite.getString("tableName")
            sql.execSQL(entite.getString("createSql").replace(JETON_TABLE, table))
            val index = entite.optJSONArray("indices") ?: continue
            for (j in 0 until index.length()) {
                sql.execSQL(
                    index.getJSONObject(j).getString("createSql").replace(JETON_TABLE, table),
                )
            }
        }
        val reglages = base.getJSONArray("setupQueries")
        for (i in 0 until reglages.length()) sql.execSQL(reglages.getString(i))
        sql.version = version
        return sql
    }

    @Test
    fun `chaque version exportee s'ouvre en version actuelle sans perte`() {
        (1..ProgressionDatabase.VERSION).forEach { version ->
            creerBaseVersion(version).apply {
                execSQL(
                    "INSERT INTO utilisateur (identifiant, hashMotDePasse, sel, dateCreation) " +
                        "VALUES ('yamao', 'h', 's', 0)",
                )
                close()
            }
            val base = Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                ProgressionDatabase::class.java,
                NOM,
            )
                .addMigrations(*MigrationsProgression.TOUTES)
                .allowMainThreadQueries()
                .build()
            val nombre = base.openHelper.readableDatabase.query("SELECT COUNT(*) FROM utilisateur").use {
                it.moveToFirst()
                it.getInt(0)
            }
            assertEquals("version $version", 1, nombre)
            base.close()
            nettoyer()
        }
    }

    @Test
    fun `migrer de 1 vers 2 garde l'equipe et ajoute le PC`() {
        creerBaseVersion(1).apply {
            execSQL(
                "INSERT INTO utilisateur (identifiant, hashMotDePasse, sel, dateCreation) VALUES ('y', 'h', 's', 0)",
            )
            execSQL(
                "INSERT INTO run (utilisateurId, nom, starterId, difficulte, regles, dateDebut) " +
                    "VALUES (1, 'Run', 252, 'NORMAL', '', 0)",
            )
            execSQL(
                "INSERT INTO membre_equipe (runId, emplacement, especeId, surnom, niveau) " +
                    "VALUES (1, 1, 253, 'Gecko', 18)",
            )
            close()
        }
        val base = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProgressionDatabase::class.java,
            NOM,
        )
            .addMigrations(*MigrationsProgression.TOUTES)
            .allowMainThreadQueries()
            .build()
        val sql = base.openHelper.writableDatabase
        sql.query("SELECT especeId, surnom, niveau, nature, talent FROM membre_equipe").use {
            assertTrue(it.moveToFirst())
            assertEquals(253, it.getInt(0))
            assertEquals("Gecko", it.getString(1))
            assertEquals(18, it.getInt(2))
            assertTrue(it.isNull(3))
            assertTrue(it.isNull(4))
        }
        sql.execSQL("INSERT INTO pokemon_pc (runId, boite, especeId, niveau, nature) VALUES (1, 1, 258, 5, 'CALME')")
        sql.execSQL("DELETE FROM run")
        sql.query("SELECT COUNT(*) FROM pokemon_pc").use {
            it.moveToFirst()
            assertEquals("le PC suit la run (suppression en cascade)", 0, it.getInt(0))
        }
        base.close()
    }

    private companion object {
        const val NOM = "migration-test.db"
        const val JETON_TABLE = "\${TABLE_NAME}"
        val DOSSIER_SCHEMAS = File("schemas/com.omegadex.app.data.progression.ProgressionDatabase")
    }
}
