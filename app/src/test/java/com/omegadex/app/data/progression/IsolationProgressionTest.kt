package com.omegadex.app.data.progression

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Aucune donnée de progression ne peut exister sans utilisateur, et supprimer un compte efface tout. */
@RunWith(RobolectricTestRunner::class)
class IsolationProgressionTest {
    private lateinit var base: ProgressionDatabase
    private lateinit var sql: SupportSQLiteDatabase

    @Before
    fun ouvrir() {
        base =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ProgressionDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sql = base.openHelper.writableDatabase
    }

    @After
    fun fermer() = base.close()

    private fun inserer(table: String, vararg valeurs: Pair<String, Any?>): Long {
        val cv = ContentValues()
        valeurs.forEach { (cle, valeur) ->
            when (valeur) {
                null -> cv.putNull(cle)
                is Long -> cv.put(cle, valeur)
                is Int -> cv.put(cle, valeur)
                is Boolean -> cv.put(cle, valeur)
                else -> cv.put(cle, valeur.toString())
            }
        }
        return sql.insert(table, SQLiteDatabase.CONFLICT_ABORT, cv)
    }

    private fun compter(table: String): Int = sql.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }

    private fun creerRun(utilisateurId: Long) = inserer(
        "run",
        "utilisateurId" to utilisateurId,
        "nom" to "Run",
        "starterId" to 258,
        "difficulte" to "NORMAL",
        "regles" to "",
        "dateDebut" to 0L,
    )

    @Test
    fun `une run sans utilisateur est refusee`() {
        assertThrows(SQLiteConstraintException::class.java) { creerRun(utilisateurId = 999L) }
    }

    @Test
    fun `une progression sans run est refusee`() {
        assertThrows(SQLiteConstraintException::class.java) {
            inserer(
                "progression_espece",
                "runId" to 42L,
                "especeId" to 258,
                "statut" to "VUE",
                "notes" to "",
                "dateMaj" to 0L,
            )
        }
    }

    @Test
    fun `supprimer un utilisateur efface toute sa progression`() {
        val utilisateur = inserer(
            "utilisateur",
            "identifiant" to "yamao",
            "hashMotDePasse" to "h",
            "sel" to "s",
            "dateCreation" to 0L,
        )
        val run = creerRun(utilisateur)
        inserer(
            "progression_etape",
            "runId" to run,
            "etapeId" to 0,
            "deverrouillee" to true,
            "dateDeverrouillage" to 0L,
        )
        inserer("quete_cochee", "runId" to run, "queteId" to 1, "dateCochage" to 0L)
        inserer(
            "progression_espece",
            "runId" to run,
            "especeId" to 258,
            "statut" to "CAPTUREE",
            "notes" to "",
            "dateMaj" to 0L,
        )
        inserer("defi", "runId" to run, "modeleId" to 1, "especeId" to 258, "etat" to "ACTIF", "dateDebut" to 0L)
        inserer("membre_equipe", "runId" to run, "emplacement" to 1, "especeId" to 258, "niveau" to 5)

        sql.delete("utilisateur", "id = ?", arrayOf(utilisateur))

        listOf("run", "progression_etape", "quete_cochee", "progression_espece", "defi", "membre_equipe")
            .forEach { assertEquals("table $it", 0, compter(it)) }
    }
}
