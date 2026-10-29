package com.omegadex.app.ui.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/** Rôle d'un tracé du pictogramme : plein ou trait, blanc ou dans la couleur du type (détail creusé). */
internal enum class Couche { PLEIN, DETAIL, TRAIT, TRAIT_LARGE, TRAIT_DETAIL }

internal class Element(val couche: Couche, donnees: String) {
    val chemin: Path by lazy { PathParser().parsePathString(donnees).toPath() }
}

/**
 * Pictogrammes originaux des 18 types (maquette « Logos de types », style 4), dessinés dans un repère de 24 × 24.
 * Généré depuis la maquette : modifier les tracés là-bas puis régénérer ce fichier.
 */
internal object PictogrammesTypes {
    const val REPERE = 24f

    private val dessins: Map<String, List<Element>> = mapOf(
        "normal" to listOf(
            Element(Couche.PLEIN, "M7.2 16A4.8 4.0 0.0 1 0 16.8 16A4.8 4.0 0.0 1 0 7.2 16Z"),
            Element(Couche.PLEIN, "M3.6999999999999997 10.0a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0Z"),
            Element(Couche.PLEIN, "M7.5 5.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0Z"),
            Element(Couche.PLEIN, "M12.3 5.6a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0Z"),
            Element(Couche.PLEIN, "M16.099999999999998 10.0a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0Z"),
        ),
        "feu" to listOf(
            Element(
                Couche.PLEIN,
                "M12 22c-4.2 0-7-2.8-7-6.4 0-3.4 2.5-5.3 3.4-8.8 1.7 1.5 2.4 3.2 2.4 4.9 1.1-1.3 " +
                "1.7-3 1.6-5.3 3.2 2.3 5.6 5.7 5.6 9.2 0 3.6-2.8 6.4-6 6.4z",
            ),
            Element(
                Couche.DETAIL,
                "M12 22c-1.8 0-3-1.3-3-2.9 0-1.7 1.4-2.7 3-4.6 1.6 1.9 3 2.9 3 4.6 0 1.6-1.2 2.9-3 " +
                "2.9z",
            ),
        ),
        "eau" to listOf(
            Element(Couche.TRAIT, "M2.5 8c2.4-2.4 4.7-2.4 7.1 0s4.7 2.4 7.1 0 3.6-1.8 4.8-.8"),
            Element(Couche.TRAIT, "M2.5 13c2.4-2.4 4.7-2.4 7.1 0s4.7 2.4 7.1 0 3.6-1.8 4.8-.8"),
            Element(Couche.TRAIT, "M2.5 18c2.4-2.4 4.7-2.4 7.1 0s4.7 2.4 7.1 0 3.6-1.8 4.8-.8"),
        ),
        "plante" to listOf(
            Element(Couche.TRAIT, "M12 22v-9.5"),
            Element(Couche.PLEIN, "M12 14c-5.4 0-8.2-3.2-8.2-7.6 4.4 0 8.2 2.2 8.2 7.6z"),
            Element(Couche.PLEIN, "M12 11.6c0-4.9 3.2-8.2 8.2-8.2 0 4.9-3.2 8.2-8.2 8.2z"),
        ),
        "electrik" to listOf(
            Element(Couche.PLEIN, "M14 1.5 4.5 13.5h6L8.8 22.5l10.7-13h-6.3z"),
        ),
        "glace" to listOf(
            Element(Couche.PLEIN, "M12 1.5 19.5 12 12 22.5 4.5 12z"),
            Element(Couche.TRAIT_DETAIL, "M12 1.5v21M4.5 12h15"),
        ),
        "combat" to listOf(
            Element(
                Couche.PLEIN,
                "M12 1.8l2.3 5.9 5.6-2.9-2.2 6 6 2-6 1.9 2.4 6-5.7-2.7L12 22.2l-2.4-6.2-5.7 2.7 " +
                "2.4-6-6-1.9 6-2-2.2-6 5.6 2.9z",
            ),
            Element(Couche.DETAIL, "M9.4 12.0a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0Z"),
        ),
        "poison" to listOf(
            Element(
                Couche.TRAIT,
                "M9 2.5h6M10.2 2.5v5.6L4.8 17.4a2.6 2.6 0 0 0 2.3 3.9h9.8a2.6 2.6 0 0 0 " +
                "2.3-3.9l-5.4-9.3V2.5",
            ),
            Element(Couche.PLEIN, "M8.2 16.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z"),
            Element(Couche.PLEIN, "M13.100000000000001 14.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0Z"),
        ),
        "sol" to listOf(
            Element(Couche.PLEIN, "M1.5 20.5C4.6 13.8 7.8 10 12 10s7.4 3.8 10.5 10.5z"),
            Element(Couche.TRAIT_DETAIL, "M7 16.2h3.2M13.3 14h4M10 18.6h5.4"),
            Element(Couche.TRAIT, "M12 3v3.5M7.2 4.6l1.5 2.8M16.8 4.6l-1.5 2.8"),
        ),
        "vol" to listOf(
            Element(Couche.PLEIN, "M21 2.5C11.6 3.6 6.2 8.9 5.2 18.4L3 21.5l2.8-1.6C15.2 18.8 20.2 12.2 21 2.5z"),
            Element(Couche.TRAIT_DETAIL, "M5.8 19.9 16 8.6M10.4 14.8l5.2.4M8.4 12l1.6-3"),
        ),
        "psy" to listOf(
            Element(
                Couche.TRAIT,
                "M12.0 11.4 L12.4 11.2 L12.9 11.4 L13.4 11.9 L13.6 12.6 L13.2 13.5 L12.4 14.2 L11.3 " +
                "14.4 L10.1 14.0 L9.1 12.9 L8.8 11.4 L9.2 9.8 L10.5 8.5 L12.4 7.9 L14.4 8.3 L16.1 9.8 " +
                "L16.9 11.9 L16.6 14.4 L15.1 16.5 L12.7 17.7 L9.8 17.6 L7.2 16.1 L5.6 13.4 L5.5 10.1 " +
                "L6.9 7.1 L9.7 5.0 L13.3 4.5 L16.9 5.8 L19.5 8.7 L20.4 12.6 L19.3 16.6 L16.4 19.8 " +
                "L12.2 21.2 L12.2 21.2",
            ),
        ),
        "insecte" to listOf(
            Element(Couche.TRAIT, "M10 7.2 6.8 2.8M14 7.2l3.2-4.4"),
            Element(Couche.PLEIN, "M12 7A7.4 6.6 90.0 1 0 12 21.8A7.4 6.6 90.0 1 0 12 7Z"),
            Element(Couche.TRAIT_DETAIL, "M12 7.2v14.4"),
            Element(Couche.DETAIL, "M7.7 13.4a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"),
            Element(Couche.DETAIL, "M13.7 16.6a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"),
        ),
        "roche" to listOf(
            Element(Couche.PLEIN, "M3.5 19 6.6 8.2l6.6-4.6 7.3 6.6-1.2 8.8z"),
            Element(Couche.TRAIT_DETAIL, "M6.6 8.2l6.4 3.4 7.5-1.4M13 11.6 11 19"),
        ),
        "spectre" to listOf(
            Element(Couche.TRAIT, "M12 2v2.5M8.5 5.5h7M9 5.5v2.3M15 5.5v2.3"),
            Element(Couche.TRAIT, "M7 7.8h10l1 11.4H6z"),
            Element(
                Couche.PLEIN,
                "M12 18c-1.9 0-3.1-1.3-3.1-2.9 0-1.9 1.6-2.8 3.1-5 1.5 2.2 3.1 3.1 3.1 5 0 1.6-1.2 " +
                "2.9-3.1 2.9z",
            ),
            Element(Couche.TRAIT, "M5 21.5h14"),
        ),
        "dragon" to listOf(
            Element(
                Couche.PLEIN,
                "M4.6 22.4C4 18.6 4.2 15.4 5.6 12.6L2.4 3.4l7 4.4-1.2-6.2 5.4 4.6c2.8.1 5.4 1.2 7.6 " +
                "2.9l1.6 2.1-1.2 1.6-6.2.7-2 1 6.4 2.2-.7 1.7-6.8.2-2.2 4.4z",
            ),
            Element(Couche.PLEIN, "M5.4 16.6 1.6 16.2l3.9-1.8zM6.2 20.4l-3.4.6 3.2-2.2z"),
            Element(Couche.DETAIL, "M13.2 9.6l3-1 .4 1.1-2.9.6z"),
            Element(Couche.DETAIL, "M20.4 9.9a0.6 0.6 0 1 0 1.2 0a0.6 0.6 0 1 0 -1.2 0Z"),
            Element(Couche.DETAIL, "M16.6 12.5l.5 1.2.6-1.3zM18.6 16.3l.5-1.2.6 1.3z"),
        ),
        "tenebres" to listOf(
            Element(Couche.TRAIT, "M2.8000000000000007 12.0a9.2 9.2 0 1 0 18.4 0a9.2 9.2 0 1 0 -18.4 0Z"),
            Element(Couche.PLEIN, "M6.4 12.0a5.6 5.6 0 1 0 11.2 0a5.6 5.6 0 1 0 -11.2 0Z"),
            Element(Couche.TRAIT, "M12 .8v2.4M12 20.8v2.4M.8 12h2.4M20.8 12h2.4"),
        ),
        "acier" to listOf(
            Element(
                Couche.PLEIN,
                "M19.00 12.00 L21.32 13.85 L20.78 15.64 L17.82 15.89 L16.95 16.95 L17.28 19.90 L15.64 " +
                "20.78 L13.37 18.87 L12.00 19.00 L10.15 21.32 L8.36 20.78 L8.11 17.82 L7.05 16.95 " +
                "L4.10 17.28 L3.22 15.64 L5.13 13.37 L5.00 12.00 L2.68 10.15 L3.22 8.36 L6.18 8.11 " +
                "L7.05 7.05 L6.72 4.10 L8.36 3.22 L10.63 5.13 L12.00 5.00 L13.85 2.68 L15.64 3.22 " +
                "L15.89 6.18 L16.95 7.05 L19.90 6.72 L20.78 8.36 L18.87 10.63Z",
            ),
            Element(Couche.DETAIL, "M9.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0Z"),
        ),
        "fee" to listOf(
            Element(Couche.PLEIN, "M12 2A4.6 3.3 90.0 1 0 12 11.2A4.6 3.3 90.0 1 0 12 2Z"),
            Element(Couche.PLEIN, "M21.511 8.91A4.6 3.3 162.0 1 0 12.761 11.753A4.6 3.3 162.0 1 0 21.511 8.91Z"),
            Element(Couche.PLEIN, "M17.878 20.09A4.6 3.3 234.0 1 0 12.47 12.647A4.6 3.3 234.0 1 0 17.878 20.09Z"),
            Element(Couche.PLEIN, "M6.122 20.09A4.6 3.3 306.0 1 0 11.53 12.647A4.6 3.3 306.0 1 0 6.122 20.09Z"),
            Element(Couche.PLEIN, "M2.489 8.91A4.6 3.3 378.0 1 0 11.239 11.753A4.6 3.3 378.0 1 0 2.489 8.91Z"),
            Element(Couche.DETAIL, "M9.4 12.0a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0Z"),
        ),
    )

    fun de(type: String): List<Element> = dessins[type].orEmpty()
}
