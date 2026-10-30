package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.defi.ReglesDifficulte
import com.omegadex.app.domain.model.Evolution

/**
 * Évolution par niveau qu'un Pokémon peut faire à son niveau actuel.
 * [precision] : condition que l'app ne peut pas vérifier seule et que le joueur confirme
 * (« femelle », « Attaque > Défense », « avec un Pokémon Ténèbres dans l'équipe »…), sinon null.
 */
data class EvolutionDisponible(val evolution: Evolution, val niveauRequis: Int, val precision: String?)

/** Détection des évolutions par niveau (cahier des charges, écran Équipe). */
object DetectionEvolution {
    private val NIVEAU_EN_TETE = Regex("^\\s*\\d+")

    /**
     * Évolutions par niveau de [especeId] atteintes au [niveau] donné, parmi les évolutions de sa [famille].
     * Les évolutions sans niveau (pierre, échange, bonheur…) ne sont pas détectées : elles dépendent
     * d'une action du joueur en jeu, pas de la saisie d'un niveau.
     */
    fun disponibles(especeId: Int, niveau: Int, famille: List<Evolution>): List<EvolutionDisponible> =
        famille.filter { it.especeSourceId == especeId }.mapNotNull { evolution ->
            ReglesDifficulte.niveauRequis(evolution.condition, evolution.valeur)
                ?.takeIf { niveau >= it }
                ?.let { EvolutionDisponible(evolution, it, precision(evolution.valeur)) }
        }

    /** « 20 (femelle) » → « femelle » ; « 16 » → null. */
    fun precision(valeur: String?): String? = valeur
        ?.replaceFirst(NIVEAU_EN_TETE, "")
        ?.trim()
        ?.removeSurrounding("(", ")")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
