package com.omegadex.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.omegadex.app.domain.badge.BadgeArene
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Images des badges d'arène, fournies localement par le propriétaire du projet : `assets/badges/<n>.png`,
 * n de 1 à 8 dans l'ordre de la boîte (voir `assets/badges/LISEZMOI.md`). Le dossier est ignoré par Git :
 * sans fichier, l'app dessine le badge original ([dessinerBadge]).
 */
object ImagesBadges {
    private const val DOSSIER = "badges"

    private val cache = ConcurrentHashMap<BadgeArene, ImageBitmap>()

    @Volatile
    private var presents: Set<String>? = null

    /** Nom du fichier attendu pour [badge] : « 1.png » pour le Badge Roche… « 8.png » pour le Badge Pluie. */
    fun fichier(badge: BadgeArene): String = "${badge.ordinal + 1}.png"

    fun enCache(badge: BadgeArene): ImageBitmap? = cache[badge]

    /** Image du badge, ou null si aucun fichier n'est fourni pour lui. */
    suspend fun charger(context: Context, badge: BadgeArene): ImageBitmap? = withContext(Dispatchers.IO) {
        cache[badge]?.let { return@withContext it }
        val nom = fichier(badge)
        if (nom !in disponibles(context)) return@withContext null
        try {
            context.assets.open("$DOSSIER/$nom").use { flux ->
                BitmapFactory.decodeStream(flux)?.asImageBitmap()?.also { cache[badge] = it }
            }
        } catch (_: IOException) {
            null
        }
    }

    /** Fichiers présents dans le dossier, lus une seule fois. */
    private fun disponibles(context: Context): Set<String> = presents ?: try {
        context.assets.list(DOSSIER).orEmpty().toSet()
    } catch (_: IOException) {
        emptySet()
    }.also { presents = it }
}

/** Charge l'image du badge hors du fil principal ; null tant qu'elle charge ou si elle n'existe pas. */
@Composable
fun rememberImageBadge(badge: BadgeArene): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val image by produceState(ImagesBadges.enCache(badge), badge) {
        if (value == null) value = ImagesBadges.charger(context, badge)
    }
    return image
}
