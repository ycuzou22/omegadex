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
 * Écrit les sauvegardes dans le fichier choisi par le joueur avec le sélecteur de fichiers Android
 * (`ActivityResultContracts.CreateDocument("application/json")`), via le ContentResolver.
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
}
