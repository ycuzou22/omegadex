package com.omegadex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Thème provisoire de l'application.
 * La direction artistique « Épuré Oméga » (couleurs, typographies, formes) arrive avec T02.1.
 */
@Composable
fun OmegadexTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(primary = RubisClair)
    } else {
        lightColorScheme(primary = Rubis)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
