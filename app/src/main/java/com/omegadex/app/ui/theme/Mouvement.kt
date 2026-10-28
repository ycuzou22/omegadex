package com.omegadex.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Vrai quand l'utilisateur a coupé les animations dans les réglages Android
 * (Accessibilité › Supprimer les animations, ou échelle d'animation à 0 dans les options pour les développeurs).
 * Les composants de la DA remplacent alors leurs animations par un état final immédiat.
 */
fun animationsReduites(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

val LocalAnimationsReduites = staticCompositionLocalOf { false }
