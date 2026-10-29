package com.omegadex.app.domain.espece

/**
 * Table des types de la 6ᵉ génération (Rubis Oméga / Saphir Alpha), avec les clés de type de l'app.
 * Seuls les multiplicateurs différents de 1 sont listés : attaquant → (défenseur → multiplicateur).
 */
@Suppress("MagicNumber") // la table elle-même : des données du jeu
object TableDesTypes {
    val TYPES = listOf(
        "normal", "feu", "eau", "plante", "electrik", "glace", "combat", "poison", "sol",
        "vol", "psy", "insecte", "roche", "spectre", "dragon", "tenebres", "acier", "fee",
    )

    private val TABLE: Map<String, Map<String, Float>> = mapOf(
        "normal" to mapOf("roche" to 0.5f, "spectre" to 0f, "acier" to 0.5f),
        "feu" to mapOf(
            "feu" to 0.5f,
            "eau" to 0.5f,
            "plante" to 2f,
            "glace" to 2f,
            "insecte" to 2f,
            "roche" to 0.5f,
            "dragon" to 0.5f,
            "acier" to 2f,
        ),
        "eau" to mapOf("feu" to 2f, "eau" to 0.5f, "plante" to 0.5f, "sol" to 2f, "roche" to 2f, "dragon" to 0.5f),
        "plante" to mapOf(
            "feu" to 0.5f, "eau" to 2f, "plante" to 0.5f, "poison" to 0.5f, "sol" to 2f,
            "vol" to 0.5f, "insecte" to 0.5f, "roche" to 2f, "dragon" to 0.5f, "acier" to 0.5f,
        ),
        "electrik" to mapOf(
            "eau" to 2f,
            "plante" to 0.5f,
            "electrik" to 0.5f,
            "sol" to 0f,
            "vol" to 2f,
            "dragon" to 0.5f,
        ),
        "glace" to mapOf(
            "feu" to 0.5f,
            "eau" to 0.5f,
            "plante" to 2f,
            "glace" to 0.5f,
            "sol" to 2f,
            "vol" to 2f,
            "dragon" to 2f,
            "acier" to 0.5f,
        ),
        "combat" to mapOf(
            "normal" to 2f, "glace" to 2f, "poison" to 0.5f, "vol" to 0.5f, "psy" to 0.5f, "insecte" to 0.5f,
            "roche" to 2f, "spectre" to 0f, "tenebres" to 2f, "acier" to 2f, "fee" to 0.5f,
        ),
        "poison" to mapOf(
            "plante" to 2f,
            "poison" to 0.5f,
            "sol" to 0.5f,
            "roche" to 0.5f,
            "spectre" to 0.5f,
            "acier" to 0f,
            "fee" to 2f,
        ),
        "sol" to mapOf(
            "feu" to 2f,
            "plante" to 0.5f,
            "electrik" to 2f,
            "poison" to 2f,
            "vol" to 0f,
            "insecte" to 0.5f,
            "roche" to 2f,
            "acier" to 2f,
        ),
        "vol" to mapOf(
            "plante" to 2f,
            "electrik" to 0.5f,
            "combat" to 2f,
            "insecte" to 2f,
            "roche" to 0.5f,
            "acier" to 0.5f,
        ),
        "psy" to mapOf("combat" to 2f, "poison" to 2f, "psy" to 0.5f, "tenebres" to 0f, "acier" to 0.5f),
        "insecte" to mapOf(
            "feu" to 0.5f, "plante" to 2f, "combat" to 0.5f, "poison" to 0.5f, "vol" to 0.5f,
            "psy" to 2f, "spectre" to 0.5f, "tenebres" to 2f, "acier" to 0.5f, "fee" to 0.5f,
        ),
        "roche" to mapOf(
            "feu" to 2f,
            "glace" to 2f,
            "combat" to 0.5f,
            "sol" to 0.5f,
            "vol" to 2f,
            "insecte" to 2f,
            "acier" to 0.5f,
        ),
        "spectre" to mapOf("normal" to 0f, "psy" to 2f, "spectre" to 2f, "tenebres" to 0.5f),
        "dragon" to mapOf("dragon" to 2f, "acier" to 0.5f, "fee" to 0f),
        "tenebres" to mapOf("combat" to 0.5f, "psy" to 2f, "spectre" to 2f, "tenebres" to 0.5f, "fee" to 0.5f),
        "acier" to mapOf(
            "feu" to 0.5f,
            "eau" to 0.5f,
            "electrik" to 0.5f,
            "glace" to 2f,
            "roche" to 2f,
            "acier" to 0.5f,
            "fee" to 2f,
        ),
        "fee" to mapOf(
            "feu" to 0.5f,
            "combat" to 2f,
            "poison" to 0.5f,
            "dragon" to 2f,
            "tenebres" to 2f,
            "acier" to 0.5f,
        ),
    )

    fun multiplicateur(attaque: String, defense: String): Float = TABLE[attaque]?.get(defense) ?: 1f

    /** Multiplicateur reçu par une espèce de types [defenses] pour chaque type d'attaque. */
    fun efficacites(defenses: List<String>): Map<String, Float> = TYPES.associateWith { attaque ->
        defenses.fold(1f) { total, defense -> total * multiplicateur(attaque, defense) }
    }
}
