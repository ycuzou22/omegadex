package com.omegadex.app.data.progression

import android.content.Context
import android.database.sqlite.SQLiteDatabase as SQLiteClair
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omegadex.app.data.progression.entity.UtilisateurEntity
import com.omegadex.app.data.securite.CleBaseKeystore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLiteChiffree
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Sur un vrai appareil : le fichier de la base progression n'est pas lisible sans la clé du Keystore. */
@RunWith(AndroidJUnit4::class)
class ChiffrementProgressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var fichier: File

    @Before
    fun preparer() {
        ChiffrementProgression.chargerBibliotheque()
        fichier = context.getDatabasePath("test_chiffrement.db")
        supprimerBase()
    }

    @After
    fun supprimerBase() {
        listOf("", "-wal", "-shm", "-journal", ".chiffree").forEach { File(fichier.path + it).delete() }
    }

    private fun ouvrir(phrase: String) = Room.databaseBuilder(context, ProgressionDatabase::class.java, fichier.path)
        .openHelperFactory(SupportOpenHelperFactory(phrase.toByteArray(Charsets.UTF_8)))
        .build()

    private fun creerCompte(base: ProgressionDatabase) = runBlocking {
        base.compteDao().creerUtilisateur(
            UtilisateurEntity(identifiant = "Yamao", hashMotDePasse = "h", sel = "s", dateCreation = 1),
        )
    }

    @Test
    fun la_cle_keystore_est_stable_et_jamais_stockee_en_clair() {
        val phrase = CleBaseKeystore(context).phraseSecrete()
        assertEquals(LONGUEUR_PHRASE_HEX, phrase.length)
        assertEquals(phrase, CleBaseKeystore(context).phraseSecrete())
        val stockees = context.getSharedPreferences("securite", Context.MODE_PRIVATE).all.values
        assertTrue(stockees.isNotEmpty())
        assertFalse(stockees.any { it.toString().contains(phrase) })
    }

    @Test
    fun le_fichier_n_est_pas_lisible_sans_la_cle() {
        val phrase = CleBaseKeystore(context).phraseSecrete()
        ouvrir(phrase).also { creerCompte(it) }.close()

        assertFalse(ChiffrementProgression.estEnClair(fichier))
        assertFalse(String(fichier.readBytes(), Charsets.ISO_8859_1).contains("Yamao"))
        assertThrows(SQLiteException::class.java) {
            SQLiteClair.openDatabase(fichier.path, null, SQLiteClair.OPEN_READONLY).use {
                it.rawQuery("SELECT count(*) FROM sqlite_master", null).use { curseur -> curseur.moveToFirst() }
            }
        }
        assertThrows(Exception::class.java) {
            val base = ouvrir("mauvaise-cle")
            try {
                runBlocking { base.compteDao().utilisateurs().first() }
            } finally {
                base.close()
            }
        }
        val base = ouvrir(phrase)
        try {
            assertEquals(
                listOf("Yamao"),
                runBlocking {
                    base.compteDao().utilisateurs().first().map { it.identifiant }
                },
            )
        } finally {
            base.close()
        }
    }

    @Test
    fun une_ancienne_base_en_clair_est_chiffree_sans_perte() {
        SQLiteClair.openOrCreateDatabase(fichier, null).use {
            it.execSQL("CREATE TABLE note (texte TEXT)")
            it.execSQL("INSERT INTO note VALUES ('Gecko capturé')")
            it.version = 1
        }
        assertTrue(ChiffrementProgression.estEnClair(fichier))

        ChiffrementProgression.chiffrerSiNecessaire(fichier, "phrase-de-test")

        assertFalse(ChiffrementProgression.estEnClair(fichier))
        val base = SQLiteChiffree.openDatabase(
            fichier.path,
            "phrase-de-test",
            null,
            SQLiteChiffree.OPEN_READWRITE,
            null,
            null,
        )
        try {
            assertEquals(1, base.version)
            base.rawQuery("SELECT texte FROM note", null).use {
                assertTrue(it.moveToFirst())
                assertEquals("Gecko capturé", it.getString(0))
            }
        } finally {
            base.close()
        }
        val tailleAvant = fichier.length()
        ChiffrementProgression.chiffrerSiNecessaire(fichier, "phrase-de-test")
        assertNotEquals(0L, tailleAvant)
        assertEquals(tailleAvant, fichier.length())
    }

    private companion object {
        /** 32 octets aléatoires écrits en hexadécimal. */
        const val LONGUEUR_PHRASE_HEX = 64
    }
}
