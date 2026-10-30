package com.omegadex.app.data.sauvegarde

import android.content.Context
import android.net.Uri
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Écrit et relit les sauvegardes dans les fichiers choisis par le joueur avec le sélecteur de fichiers
 * Android (`ActivityResultContracts.CreateDocument("application/json")` pour l'export), via le ContentResolver.
 */
class FichiersSauvegardeAndroid @Inject constructor(@ApplicationContext private val context: Context) :
    FichiersSauvegarde {
    override suspend fun ecrire(destination: String, sauvegarde: Sauvegarde) = withContext(Dispatchers.IO) {
        val contenu = SauvegardeJson.encoder(sauvegarde)
        // « wt » : un fichier existant choisi à nouveau est tronqué, pas complété.
        val flux = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
            ?: throw IOException("Impossible d'ouvrir $destination en écriture")
        flux.bufferedWriter(Charsets.UTF_8).use { it.write(contenu) }
    }

    /** Fichier choisi avec `ActivityResultContracts.OpenDocument()`. */
    override suspend fun lire(source: String): Sauvegarde = withContext(Dispatchers.IO) {
        val flux = context.contentResolver.openInputStream(Uri.parse(source))
            ?: throw IOException("Impossible d'ouvrir $source en lecture")
        val texte = flux.bufferedReader(Charsets.UTF_8).use { it.readText() }
        SauvegardeJson.decoder(texte)
    }
}
