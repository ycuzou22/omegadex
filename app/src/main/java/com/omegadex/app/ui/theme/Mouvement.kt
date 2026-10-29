package com.omegadex.app.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Réglages Android qui coupent les animations (« Supprimer les animations » les met tous à 0). */
private val REGLAGES_ANIMATION = listOf(
    Settings.Global.ANIMATOR_DURATION_SCALE,
    Settings.Global.TRANSITION_ANIMATION_SCALE,
)

/**
 * Vrai quand l'utilisateur a coupé les animations dans les réglages Android
 * (Accessibilité › Supprimer les animations, ou échelle d'animation à 0 dans les options pour les développeurs).
 * Les composants de la DA remplacent alors leurs animations par un état final immédiat.
 */
fun animationsReduites(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

/**
 * Suit le réglage en direct : le changer pendant que l'app est ouverte coupe ou rétablit les animations
 * sans redémarrage.
 */
fun Context.fluxAnimationsReduites(): Flow<Boolean> = callbackFlow {
    val observateur = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(animationsReduites(this@fluxAnimationsReduites))
        }
    }
    REGLAGES_ANIMATION.forEach {
        contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observateur)
    }
    trySend(animationsReduites(this@fluxAnimationsReduites))
    awaitClose { contentResolver.unregisterContentObserver(observateur) }
}.distinctUntilChanged()

/** État « animations réduites » du thème, mis à jour en direct. */
@Composable
fun animationsReduitesEnDirect(): State<Boolean> {
    val context = LocalContext.current
    val flux = remember(context) { context.fluxAnimationsReduites() }
    return flux.collectAsState(initial = remember(context) { animationsReduites(context) })
}

val LocalAnimationsReduites = compositionLocalOf { false }
