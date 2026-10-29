package com.omegadex.app.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.omegadex.app.ui.theme.CouleursTypes

private const val FIN_REFLET = 0.38f
private const val FIN_CORPS_ESPECE = 0.7f
private const val PART_BLANC = 0.4f
private const val PART_OMBRE = 0.35f

/**
 * Gemme aux couleurs d'une espèce (maquettes Capture et Évolution) : reflet clair, type principal,
 * puis second type (ou une ombre du premier).
 */
fun brosseGemmeEspece(types: List<String>): Brush {
    val principal = CouleursTypes.de(types.firstOrNull().orEmpty())
    val second = types.getOrNull(1)?.let { CouleursTypes.de(it) } ?: lerp(principal, Color.Black, PART_OMBRE)
    val reflet = lerp(principal, Color.White, PART_BLANC)
    return Brush.linearGradient(
        0f to reflet,
        FIN_REFLET to reflet,
        FIN_REFLET to principal,
        FIN_CORPS_ESPECE to principal,
        FIN_CORPS_ESPECE to second,
        1f to second,
    )
}
