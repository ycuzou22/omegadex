package com.omegadex.app.data.securite

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Fournit la phrase secrète qui chiffre la base progression. */
fun interface FournisseurCleBase {
    /** Toujours la même phrase pour un appareil donné, tant que l'application n'est pas désinstallée. */
    fun phraseSecrete(): String
}

/**
 * Phrase secrète aléatoire (256 bits) générée au premier lancement.
 * Elle n'est jamais écrite en clair : seule sa version chiffrée par une clé AES-GCM de l'Android Keystore
 * est enregistrée. La clé Keystore ne quitte pas le matériel sécurisé de l'appareil et n'est pas sauvegardée.
 */
@Singleton
class CleBaseKeystore @Inject constructor(@ApplicationContext private val context: Context) : FournisseurCleBase {
    private val phrase: String by lazy { lireOuCreer() }

    override fun phraseSecrete(): String = phrase

    private fun lireOuCreer(): String {
        val preferences = context.getSharedPreferences(FICHIER_PREFERENCES, Context.MODE_PRIVATE)
        val chiffree = preferences.getString(CLE_PHRASE, null)
        val iv = preferences.getString(CLE_IV, null)
        if (chiffree != null && iv != null) {
            return dechiffrer(Base64.decode(chiffree, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP))
        }
        val octets = ByteArray(TAILLE_PHRASE_OCTETS).also { SecureRandom().nextBytes(it) }
        val nouvelle = octets.joinToString("") { "%02x".format(it) }
        val chiffrement = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, cleKeystore()) }
        val resultat = chiffrement.doFinal(nouvelle.toByteArray(Charsets.UTF_8))
        preferences.edit(commit = true) {
            putString(CLE_PHRASE, Base64.encodeToString(resultat, Base64.NO_WRAP))
            putString(CLE_IV, Base64.encodeToString(chiffrement.iv, Base64.NO_WRAP))
        }
        return nouvelle
    }

    private fun dechiffrer(chiffree: ByteArray, iv: ByteArray): String {
        val dechiffrement = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, cleKeystore(), GCMParameterSpec(TAILLE_TAG_BITS, iv))
        }
        return String(dechiffrement.doFinal(chiffree), Charsets.UTF_8)
    }

    private fun cleKeystore(): SecretKey {
        val magasin = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (magasin.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generateur = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generateur.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(TAILLE_CLE_BITS)
                .build(),
        )
        return generateur.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "omegadex_base_progression"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FICHIER_PREFERENCES = "securite"
        const val CLE_PHRASE = "phrase_base_chiffree"
        const val CLE_IV = "phrase_base_iv"
        const val TAILLE_PHRASE_OCTETS = 32
        const val TAILLE_CLE_BITS = 256
        const val TAILLE_TAG_BITS = 128
    }
}
