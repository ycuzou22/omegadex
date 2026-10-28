package com.omegadex.app.data.progression

import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase

/** Outils SQLCipher de la base progression. */
object ChiffrementProgression {
    private val ENTETE_SQLITE_CLAIR = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** Charge la bibliothèque native SQLCipher ; à appeler avant toute ouverture de la base. */
    fun chargerBibliotheque() = System.loadLibrary("sqlcipher")

    /** Vrai si [fichier] est une base SQLite lisible sans clé (en-tête standard en clair). */
    fun estEnClair(fichier: File): Boolean {
        if (!fichier.isFile || fichier.length() < ENTETE_SQLITE_CLAIR.size) return false
        val entete = ByteArray(ENTETE_SQLITE_CLAIR.size)
        fichier.inputStream().use { it.read(entete) }
        return entete.contentEquals(ENTETE_SQLITE_CLAIR)
    }

    /**
     * Chiffre sur place une base progression créée avant le chiffrement (versions antérieures de l'app),
     * sans perdre de données : export SQLCipher vers une copie chiffrée, puis remplacement du fichier.
     * Ne fait rien si le fichier n'existe pas ou s'il est déjà chiffré.
     */
    fun chiffrerSiNecessaire(fichier: File, phraseSecrete: String) {
        if (!estEnClair(fichier)) return
        val copie = File(fichier.path + SUFFIXE_COPIE).also { it.delete() }
        // CREATE_IF_NECESSARY : sans ce drapeau, SQLite refuse de créer le fichier de la copie lors de l'ATTACH.
        val drapeaux = SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        val clair = SQLiteDatabase.openDatabase(fichier.path, "", null, drapeaux, null, null)
        try {
            val version = clair.version
            clair.execSQL("ATTACH DATABASE '${echapper(copie.path)}' AS chiffree KEY '${echapper(phraseSecrete)}'")
            clair.rawQuery("SELECT sqlcipher_export('chiffree')", null).use { it.moveToFirst() }
            clair.execSQL("PRAGMA chiffree.user_version = $version")
            clair.execSQL("DETACH DATABASE chiffree")
        } finally {
            clair.close()
        }
        listOf("", "-wal", "-shm", "-journal").forEach { File(fichier.path + it).delete() }
        check(copie.renameTo(fichier)) { "Impossible de remplacer la base progression par sa copie chiffrée" }
    }

    private fun echapper(texte: String) = texte.replace("'", "''")

    private const val SUFFIXE_COPIE = ".chiffree"
}
