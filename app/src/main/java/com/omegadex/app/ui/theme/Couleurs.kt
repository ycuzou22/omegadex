package com.omegadex.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Palette « Épuré Oméga » : rubis, magma de Groudon et roche volcanique.
val Rubis = Color(0xFFA80F2C)
val RubisProfond = Color(0xFF6F0A1E)
val RubisEclat = Color(0xFFE2465F)
val RubisLumineux = Color(0xFFFF7A8C)
val RubisPale = Color(0xFFFFE3E8)
val Magma = Color(0xFFFF6B2C)
val LueurMagma = Color(0xFFFFB15C)
val Roche = Color(0xFF1A1416)
val RocheSurface = Color(0xFF241C1F)
val RocheClaire = Color(0xFF3A3034)
val Fond = Color(0xFFF6F3F3)
val Blanc = Color(0xFFFFFFFF)
val SurfaceDouce = Color(0xFFFBF7F7)
val Trait = Color(0xFFE6DCDD)
val Piste = Color(0xFFEFE6E7)
val TexteSecondaire = Color(0xFF5E5559)
val TexteEteint = Color(0xFF8A7F82)
val TexteSurRoche = Color(0xFFD9CDCF)

/**
 * Couleurs de la DA qui n'ont pas d'équivalent dans le schéma Material 3.
 * Le magma est décoratif (veines, éclats, facettes) : il ne sert jamais de couleur de texte sur fond clair.
 */
@Immutable
data class CouleursOmegadex(
    /** Veines et éclats de magma. */
    val magma: Color,
    /** Lueur au cœur des éclats et des icônes sur fond rubis. */
    val lueurMagma: Color,
    /** Couleur du Ω posé sur un bouton rubis (lueur en clair, roche sur le rubis lumineux du thème sombre). */
    val omegaSurRubis: Color,
    /** Trois tons du rubis taillé : facette claire, corps, facette sombre. */
    val gemmeClaire: Color,
    val gemme: Color,
    val gemmeSombre: Color,
    /** Gemme ou case pas encore obtenue. */
    val gemmeEteinte: Color,
    /** Fond des jauges. */
    val piste: Color,
    /** Bordures fines des cartes et des boutons secondaires. */
    val trait: Color,
    /** Texte terminé ou désactivé (quête cochée). */
    val texteEteint: Color,
    /** Bloc sombre « roche » des cartes mises en avant (étape en cours, arène). */
    val roche: Color,
    val texteSurRoche: Color,
)

val CouleursOmegadexClaires = CouleursOmegadex(
    magma = Magma,
    lueurMagma = LueurMagma,
    omegaSurRubis = LueurMagma,
    gemmeClaire = RubisEclat,
    gemme = Rubis,
    gemmeSombre = RubisProfond,
    gemmeEteinte = Trait,
    piste = Piste,
    trait = Trait,
    texteEteint = TexteEteint,
    roche = Roche,
    texteSurRoche = TexteSurRoche,
)

val CouleursOmegadexSombres = CouleursOmegadex(
    magma = Magma,
    lueurMagma = LueurMagma,
    omegaSurRubis = Roche,
    gemmeClaire = RubisLumineux,
    gemme = RubisEclat,
    gemmeSombre = Rubis,
    gemmeEteinte = RocheClaire,
    piste = RocheClaire,
    trait = RocheClaire,
    texteEteint = Color(0xFFA0959A),
    roche = Color(0xFF0F0B0C),
    texteSurRoche = TexteSurRoche,
)

val LocalCouleursOmegadex = staticCompositionLocalOf { CouleursOmegadexClaires }
