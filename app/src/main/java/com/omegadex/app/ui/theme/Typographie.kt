package com.omegadex.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.omegadex.app.R

/** Une graisse de Manrope, extraite de la police variable embarquée (hors ligne). */
@OptIn(ExperimentalTextApi::class)
private fun manrope(graisse: FontWeight) = Font(
    resId = R.font.manrope,
    weight = graisse,
    variationSettings = FontVariation.Settings(FontVariation.weight(graisse.weight)),
)

/** Manrope : textes et titres. */
val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

/** IBM Plex Mono : chiffres (numéros de Pokédex, niveaux, pourcentages). */
val PlexMono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium))

private fun style(taille: Int, graisse: FontWeight, interligne: Int, espacement: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontSize = taille.sp,
    fontWeight = graisse,
    lineHeight = interligne.sp,
    letterSpacing = espacement.em,
)

val TypographieOmegadex = Typography(
    displayLarge = style(42, FontWeight.ExtraBold, 44, -0.03),
    displayMedium = style(36, FontWeight.ExtraBold, 40, -0.03),
    displaySmall = style(32, FontWeight.ExtraBold, 36, -0.03),
    headlineLarge = style(30, FontWeight.ExtraBold, 34, -0.027),
    headlineMedium = style(24, FontWeight.ExtraBold, 30, -0.02),
    headlineSmall = style(20, FontWeight.ExtraBold, 26, -0.01),
    titleLarge = style(18, FontWeight.ExtraBold, 24),
    titleMedium = style(15, FontWeight.ExtraBold, 20),
    titleSmall = style(13, FontWeight.ExtraBold, 18),
    bodyLarge = style(16, FontWeight.Medium, 24),
    bodyMedium = style(14, FontWeight.Medium, 21),
    bodySmall = style(13, FontWeight.Medium, 19),
    labelLarge = style(15, FontWeight.ExtraBold, 20),
    labelMedium = style(12, FontWeight.ExtraBold, 16, 0.066),
    labelSmall = style(11, FontWeight.Bold, 14),
)

/** Styles propres à la DA, en plus de l'échelle Material 3. */
@Immutable
data class TypographieChiffres(val grand: TextStyle, val moyen: TextStyle, val petit: TextStyle)

val ChiffresOmegadex = TypographieChiffres(
    grand = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    moyen = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    petit = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)

val LocalChiffresOmegadex = staticCompositionLocalOf { ChiffresOmegadex }
