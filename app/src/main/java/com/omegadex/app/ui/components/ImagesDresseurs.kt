package com.omegadex.app.ui.components

import android.content.Context
import android.graphics.Bitmap
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
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Images des dresseurs, fournies localement par le propriétaire du projet : `assets/dresseurs/<clé>.jpg|png|webp`,
 * sinon l'image de repli du dresseur (le personnage ou sa classe : `flora.jpg`, `gamin.png`…).
 * Les noms sont comparés sans tenir compte des tirets, soulignés, majuscules ni accents : `pierre_rochard.JPG`,
 * `pierre-rochard.jpg` et `Pierre Rochard.png` désignent la même image. Les soulignés sont conseillés, car
 * Android refuse les tirets dans les ressources. Le dossier est ignoré par Git (voir `assets/dresseurs/LISEZMOI.md`).
 */
object ImagesDresseurs {
    private const val DOSSIER = "dresseurs"
    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    /** Côté maximal visé d'une image décodée, en pixels (largement assez pour la fiche et la liste). */
    private const val COTE_MAX_PX = 768

    /** Image du dresseur [cle], ou à défaut celle de [repli] ; null si aucun fichier n'est fourni. */
    suspend fun charger(context: Context, cle: String, repli: String = cle): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val fichiers = try {
                context.assets.list(DOSSIER).orEmpty().toSet()
            } catch (_: IOException) {
                emptySet()
            }
            val nom = trouverFichier(fichiers, cle, repli) ?: return@withContext null
            try {
                decoderReduite(context, "$DOSSIER/$nom")?.asImageBitmap()
            } catch (_: IOException) {
                null
            }
        }

    /** Le fichier image de [cle], sinon celui de [repli], parmi [fichiers] ; null si aucun ne correspond. */
    fun trouverFichier(fichiers: Collection<String>, cle: String, repli: String = cle): String? {
        val images = fichiers.filter { it.substringAfterLast('.', "").lowercase() in EXTENSIONS }
            .groupBy { normaliser(it.substringBeforeLast('.')) }
        return listOf(cle, repli).firstNotNullOfOrNull { images[normaliser(it)]?.minOrNull() }
    }

    /** « Pierre-Rochard », « pierre_rochard » et « pierre rochard » → « pierre_rochard ». */
    fun normaliser(nom: String): String =
        Normalizer.normalize(nom.trim().lowercase(), Normalizer.Form.NFD)
            .replace(ACCENTS, "")
            .replace("œ", "oe")
            .replace("&", "et")
            .replace(SEPARATEURS, "_")
            .trim('_')

    private val ACCENTS = Regex("\\p{Mn}+")
    private val SEPARATEURS = Regex("[^a-z0-9]+")

    /**
     * Décode l'image réduite à [COTE_MAX_PX] environ : une photo de plusieurs milliers de pixels, décodée en
     * entier, peut dépasser la mémoire ou la taille qu'Android accepte de dessiner et faire planter l'app.
     */
    private fun decoderReduite(context: Context, chemin: String): Bitmap? {
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(chemin).use { BitmapFactory.decodeStream(it, null, bornes) }
        if (bornes.outWidth <= 0 || bornes.outHeight <= 0) return null
        var echelle = 1
        while (maxOf(bornes.outWidth, bornes.outHeight) / (echelle * 2) >= COTE_MAX_PX) echelle *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = echelle }
        return context.assets.open(chemin).use { BitmapFactory.decodeStream(it, null, options) }
    }
}

/** Charge l'image du dresseur hors du fil principal ; null tant qu'elle charge ou si elle n'existe pas. */
@Composable
fun rememberImageDresseur(cle: String, repli: String = cle): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var image by remember(cle, repli) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cle, repli) { image = ImagesDresseurs.charger(context, cle, repli) }
    return image
}
