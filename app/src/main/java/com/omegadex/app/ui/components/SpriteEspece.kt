package com.omegadex.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Images des espèces, fournies localement par le propriétaire du projet : `assets/sprites/<numéro national>.png`.
 * Le dossier est ignoré par Git (voir `assets/sprites/LISEZMOI.md`) : sans fichier, l'app montre [spriteParDefaut].
 */
object Sprites {
    private const val DOSSIER = "sprites"
    private const val TAILLE_CACHE = 96

    private val cache = LruCache<Int, ImageBitmap>(TAILLE_CACHE)

    @Volatile
    private var presents: Set<Int>? = null

    fun enCache(especeId: Int): ImageBitmap? = cache.get(especeId)

    /** Image de l'espèce, ou null si aucun fichier n'est fourni pour elle. */
    suspend fun charger(context: Context, especeId: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(especeId)?.let { return@withContext it }
        if (especeId !in disponibles(context)) return@withContext null
        try {
            context.assets.open("$DOSSIER/$especeId.png").use { flux ->
                BitmapFactory.decodeStream(flux)?.asImageBitmap()?.also { cache.put(especeId, it) }
            }
        } catch (_: IOException) {
            null
        }
    }

    /** Numéros pour lesquels un fichier existe, lus une seule fois. */
    private fun disponibles(context: Context): Set<Int> = presents ?: try {
        context.assets.list(DOSSIER).orEmpty()
            .mapNotNull { it.removeSuffix(".png").takeIf { _ -> it.endsWith(".png") }?.toIntOrNull() }
            .toSet()
    } catch (_: IOException) {
        emptySet()
    }.also { presents = it }
}

/** Charge l'image de l'espèce hors du fil principal ; null tant qu'elle charge ou si elle n'existe pas. */
@Composable
fun rememberSprite(especeId: Int): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val image by produceState(Sprites.enCache(especeId), especeId) {
        if (value == null) value = Sprites.charger(context, especeId)
    }
    return image
}

/**
 * Image par défaut, originale (créature en pixel art 16 × 16, rubis au front), montrée tant qu'aucun fichier
 * n'est fourni pour l'espèce.
 */
private val CREATURE_PAR_DEFAUT = listOf(
    "................",
    "......kk........",
    ".....kRRk.......",
    "....kRrrRk......",
    "...kkkRRkkkk....",
    "..kBBBBBBBBBk...",
    ".kBBBBBBBBBBBk..",
    ".kBBwkBBBwkBBk..",
    ".kBBkkBBBkkBBk..",
    ".kBBBBBBBBBBBBk.",
    ".kBBBBbbbbBBBBk.",
    "..kBBbbbbbbBBk..",
    "..kBBbbbbbbBBk..",
    "...kBBBBBBBBk...",
    "...kBk...kBk....",
    "...kk.....kk....",
)
private val PALETTE_PAR_DEFAUT = mapOf(
    'k' to 0xFF141012.toInt(),
    'B' to 0xFF7AC7B4.toInt(),
    'b' to 0xFFE8F6EF.toInt(),
    'w' to 0xFFFFFFFF.toInt(),
    'R' to 0xFFA80F2C.toInt(),
    'r' to 0xFFFF7A8C.toInt(),
)

/** L'image par défaut, construite une fois à partir de [CREATURE_PAR_DEFAUT]. */
val spriteParDefaut: ImageBitmap by lazy {
    val cote = CREATURE_PAR_DEFAUT.size
    val image = Bitmap.createBitmap(cote, cote, Bitmap.Config.ARGB_8888)
    CREATURE_PAR_DEFAUT.forEachIndexed { y, ligne ->
        ligne.forEachIndexed { x, pixel -> PALETTE_PAR_DEFAUT[pixel]?.let { image.setPixel(x, y, it) } }
    }
    image.asImageBitmap()
}

/**
 * Image d'une espèce. [decouverte] va de 0 (ombre unie : espèce jamais vue) à 1 (couleurs) ; entre les deux,
 * l'ombre s'efface sur les couleurs (découvrement de l'animation de capture). Sans image fournie, l'image par
 * défaut prend la place. Décorative : le nom de l'espèce est lu par la case qui la contient.
 */
@Composable
fun SpriteEspece(especeId: Int, decouverte: Float, modifier: Modifier = Modifier, taille: Dp = 56.dp) {
    val image = rememberSprite(especeId) ?: spriteParDefaut
    val ombre = MaterialTheme.colorScheme.onSurface
    val d = decouverte.coerceIn(0f, 1f)
    Box(modifier.size(taille), contentAlignment = Alignment.Center) {
        if (d < 1f) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(ombre.copy(alpha = OPACITE_OMBRE)),
                modifier = Modifier.fillMaxSize().alpha(1f - d),
            )
        }
        if (d > 0f) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize().alpha(d),
            )
        }
    }
}

private const val OPACITE_OMBRE = 0.82f
