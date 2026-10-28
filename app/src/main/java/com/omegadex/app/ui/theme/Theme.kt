package com.omegadex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val SchemaClair = lightColorScheme(
    primary = Rubis,
    onPrimary = Blanc,
    primaryContainer = RubisPale,
    onPrimaryContainer = RubisProfond,
    inversePrimary = RubisLumineux,
    secondary = Roche,
    onSecondary = Blanc,
    secondaryContainer = RubisPale,
    onSecondaryContainer = RubisProfond,
    tertiary = Magma,
    onTertiary = Roche,
    tertiaryContainer = Color(0xFFFFE3D2),
    onTertiaryContainer = Color(0xFF5A1E00),
    background = Fond,
    onBackground = Roche,
    surface = Blanc,
    onSurface = Roche,
    surfaceVariant = SurfaceDouce,
    onSurfaceVariant = TexteSecondaire,
    surfaceTint = Rubis,
    inverseSurface = Roche,
    inverseOnSurface = Fond,
    error = RubisProfond,
    onError = Blanc,
    outline = TexteEteint,
    outlineVariant = Trait,
    surfaceBright = Blanc,
    surfaceDim = Color(0xFFE9E2E3),
    surfaceContainerLowest = Blanc,
    surfaceContainerLow = SurfaceDouce,
    surfaceContainer = Color(0xFFF3EEEF),
    surfaceContainerHigh = Piste,
    surfaceContainerHighest = Trait,
)

val SchemaSombre = darkColorScheme(
    primary = RubisLumineux,
    onPrimary = Roche,
    primaryContainer = RubisProfond,
    onPrimaryContainer = RubisPale,
    inversePrimary = Rubis,
    secondary = Fond,
    onSecondary = Roche,
    secondaryContainer = RubisProfond,
    onSecondaryContainer = RubisPale,
    tertiary = LueurMagma,
    onTertiary = Roche,
    tertiaryContainer = Color(0xFF7A2E00),
    onTertiaryContainer = Color(0xFFFFE3D2),
    background = Roche,
    onBackground = Fond,
    surface = RocheSurface,
    onSurface = Fond,
    surfaceVariant = RocheClaire,
    onSurfaceVariant = TexteSurRoche,
    surfaceTint = RubisLumineux,
    inverseSurface = Fond,
    inverseOnSurface = Roche,
    error = RubisLumineux,
    onError = Roche,
    outline = Color(0xFFA0959A),
    outlineVariant = RocheClaire,
    surfaceBright = RocheClaire,
    surfaceDim = Roche,
    surfaceContainerLowest = Color(0xFF0F0B0C),
    surfaceContainerLow = Roche,
    surfaceContainer = RocheSurface,
    surfaceContainerHigh = Color(0xFF2E2528),
    surfaceContainerHighest = RocheClaire,
)

/** Thème « Épuré Oméga ». Pas de couleurs dynamiques : l'identité rubis est conservée sur tous les appareils. */
@Composable
fun OmegadexTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val couleurs = if (darkTheme) CouleursOmegadexSombres else CouleursOmegadexClaires
    val context = LocalContext.current
    val reduites = remember(context) { animationsReduites(context) }
    CompositionLocalProvider(
        LocalCouleursOmegadex provides couleurs,
        LocalChiffresOmegadex provides ChiffresOmegadex,
        LocalAnimationsReduites provides reduites,
    ) {
        MaterialTheme(
            colorScheme = schema(darkTheme),
            typography = TypographieOmegadex,
            shapes = FormesOmegadex,
            content = content,
        )
    }
}

fun schema(sombre: Boolean): ColorScheme = if (sombre) SchemaSombre else SchemaClair

/** Accès aux éléments de la DA qui complètent `MaterialTheme`. */
object OmegadexTheme {
    val couleurs: CouleursOmegadex
        @Composable @ReadOnlyComposable
        get() = LocalCouleursOmegadex.current

    val chiffres: TypographieChiffres
        @Composable @ReadOnlyComposable
        get() = LocalChiffresOmegadex.current

    /** Vrai si l'utilisateur a coupé les animations du système. */
    val animationsReduites: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalAnimationsReduites.current
}
