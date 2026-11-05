package com.omegadex.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Images des dresseurs, fournies localement par le propriétaire du projet : `assets/dresseurs/<clé>.jpg|png|webp`.
 * Le dossier est ignoré par Git (voir `assets/dresseurs/LISEZMOI.md`).
 */
object ImagesDresseurs {
    private const val DOSSIER = "dresseurs"
    private val EXTENSIONS = listOf("jpg", "png", "webp")

    /** Image du dresseur [cle], ou null si aucun fichier n'est fourni. */
    suspend fun charger(context: Context, cle: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val fichiers = try {
            context.assets.list(DOSSIER).orEmpty().toSet()
        } catch (_: IOException) {
            emptySet()
        }
        val nom = EXTENSIONS.map { "$cle.$it" }.firstOrNull { it in fichiers } ?: return@withContext null
        try {
            context.assets.open("$DOSSIER/$nom").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: IOException) {
            null
        }
    }
}

/** Charge l'image du dresseur hors du fil principal ; null tant qu'elle charge ou si elle n'existe pas. */
@Composable
fun rememberImageDresseur(cle: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var image by remember(cle) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cle) { image = ImagesDresseurs.charger(context, cle) }
    return image
}
