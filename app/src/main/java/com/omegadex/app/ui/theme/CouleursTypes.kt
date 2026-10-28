package com.omegadex.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Couleur de fond des puces de type Pokémon, indexée par l'identifiant du type (en minuscules, sans accent). */
object CouleursTypes {
    val parType: Map<String, Color> = mapOf(
        "normal" to Color(0xFFA8A77A),
        "feu" to Color(0xFFEE8130),
        "eau" to Color(0xFF6390F0),
        "plante" to Color(0xFF7AC74C),
        "electrik" to Color(0xFFF7D02C),
        "glace" to Color(0xFF96D9D6),
        "combat" to Color(0xFFC22E28),
        "poison" to Color(0xFFA33EA1),
        "sol" to Color(0xFFE2BF65),
        "vol" to Color(0xFFA98FF3),
        "psy" to Color(0xFFF95587),
        "insecte" to Color(0xFFA6B91A),
        "roche" to Color(0xFFB6A136),
        "spectre" to Color(0xFF735797),
        "dragon" to Color(0xFF6F35FC),
        "tenebres" to Color(0xFF705746),
        "acier" to Color(0xFFB7B7CE),
        "fee" to Color(0xFFD685AD),
    )

    fun de(type: String): Color = parType[type.lowercase()] ?: Trait
}

/** Choisit, entre la roche et le blanc, la couleur de texte la plus contrastée sur [fond]. */
fun texteLisibleSur(fond: Color): Color {
    val l = fond.luminance()
    val surBlanc = (1f + 0.05f) / (l + 0.05f)
    val surRoche = (l + 0.05f) / (Roche.luminance() + 0.05f)
    return if (surBlanc >= surRoche) Blanc else Roche
}
