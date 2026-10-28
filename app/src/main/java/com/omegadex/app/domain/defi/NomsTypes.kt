package com.omegadex.app.domain.defi

/** Noms français des types, pour remplir le marqueur `{type}` des énoncés de défis. */
object NomsTypes {
    private val NOMS = mapOf(
        "normal" to "Normal",
        "feu" to "Feu",
        "eau" to "Eau",
        "plante" to "Plante",
        "electrik" to "Électrik",
        "glace" to "Glace",
        "combat" to "Combat",
        "poison" to "Poison",
        "sol" to "Sol",
        "vol" to "Vol",
        "psy" to "Psy",
        "insecte" to "Insecte",
        "roche" to "Roche",
        "spectre" to "Spectre",
        "dragon" to "Dragon",
        "tenebres" to "Ténèbres",
        "acier" to "Acier",
        "fee" to "Fée",
    )

    fun nom(cle: String): String = NOMS[cle.trim().lowercase()] ?: cle.trim()

    /** « eau,sol » → « Eau ou Sol ». */
    fun liste(cles: String): String = cles.split(",").filter { it.isNotBlank() }.joinToString(" ou ") { nom(it) }
}
