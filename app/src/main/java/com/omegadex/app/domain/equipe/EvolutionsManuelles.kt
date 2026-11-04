package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Evolution

/** Façon de déclencher une évolution qui ne dépend pas seulement du niveau : chacune a son bouton. */
enum class MethodeEvolution(
    /** Le bouton monte aussi le membre d'un niveau : en jeu, l'évolution se fait à la montée de niveau. */
    val monteDUnNiveau: Boolean,
) {
    PIERRE(false),
    BONHEUR(true),
    BONHEUR_JOUR(true),
    BONHEUR_NUIT(true),
    OBJET_TENU(true),
    CHAMP_MAGNETIQUE(true),
    LIEU(true),
    ECHANGE(false),
    ECHANGE_OBJET(false),
    BEAUTE(true),
    ATTAQUE(true),
    AUTRE(true),
}

/**
 * Évolution que le joueur déclenche lui-même depuis l'Équipe. [detail] porte ce que le bouton doit nommer :
 * la pierre, l'objet tenu, le lieu, l'attaque, ou la condition en clair pour [MethodeEvolution.AUTRE] ;
 * [moment] vaut « de jour » ou « de nuit » pour un objet tenu, sinon null.
 */
data class EvolutionManuelle(
    val evolution: Evolution,
    val methode: MethodeEvolution,
    val detail: String?,
    val moment: String? = null,
)

/**
 * Lit les évolutions sans niveau de la base (pierre, bonheur, échange, objet tenu, lieu, « autre »)
 * et leur associe une méthode. Les évolutions par niveau restent détectées par [DetectionEvolution].
 */
object EvolutionsManuelles {
    private const val CHAMP_MAGNETIQUE = "New Lavandia"
    private val ATTAQUE = Regex("^connaître ((?:\\p{Lu}[^ ]*)(?: \\p{Lu}[^ ]*)*)$")
    private val OBJET_TENU = Regex("^(.+?) tenue? (de jour|de nuit)$")

    /** Évolutions manuelles possibles pour [especeId], parmi les évolutions de sa [famille]. */
    fun de(especeId: Int, famille: List<Evolution>): List<EvolutionManuelle> =
        famille.filter { it.especeSourceId == especeId }.mapNotNull(::lire)

    fun lire(evolution: Evolution): EvolutionManuelle? {
        val valeur = evolution.valeur?.trim()?.takeIf { it.isNotEmpty() }
        return when (evolution.condition) {
            ConditionEvolution.NIVEAU -> null
            ConditionEvolution.PIERRE -> EvolutionManuelle(evolution, MethodeEvolution.PIERRE, valeur)
            ConditionEvolution.BONHEUR -> EvolutionManuelle(evolution, bonheur(valeur), null)
            ConditionEvolution.ECHANGE -> {
                val objet = valeur?.removePrefix("en tenant ")?.trim()
                val methode = if (objet == null) MethodeEvolution.ECHANGE else MethodeEvolution.ECHANGE_OBJET
                EvolutionManuelle(evolution, methode, objet)
            }
            ConditionEvolution.OBJET -> {
                val objet = valeur?.let(OBJET_TENU::find)
                EvolutionManuelle(
                    evolution,
                    MethodeEvolution.OBJET_TENU,
                    objet?.groupValues?.get(1) ?: valeur,
                    objet?.groupValues?.get(2),
                )
            }
            ConditionEvolution.LIEU -> if (valeur == CHAMP_MAGNETIQUE) {
                EvolutionManuelle(evolution, MethodeEvolution.CHAMP_MAGNETIQUE, valeur)
            } else {
                EvolutionManuelle(evolution, MethodeEvolution.LIEU, valeur)
            }
            ConditionEvolution.AUTRE -> autre(evolution, valeur)
        }
    }

    /** Bonheur : de jour, de nuit, ou à toute heure quand la base ne précise rien. */
    private fun bonheur(valeur: String?) = when {
        valeur == null -> MethodeEvolution.BONHEUR
        "jour" in valeur -> MethodeEvolution.BONHEUR_JOUR
        "nuit" in valeur -> MethodeEvolution.BONHEUR_NUIT
        else -> MethodeEvolution.BONHEUR
    }

    private fun autre(evolution: Evolution, valeur: String?): EvolutionManuelle? {
        // Munja apparaît à côté de Ninjask quand Ningale évolue : ce n'est pas une évolution du membre.
        if (valeur != null && valeur.startsWith("place libre")) return null
        if (valeur != null && valeur.startsWith("Beauté")) {
            return EvolutionManuelle(evolution, MethodeEvolution.BEAUTE, null)
        }
        val attaque = valeur?.let(ATTAQUE::find)?.groupValues?.get(1)
        if (attaque != null) return EvolutionManuelle(evolution, MethodeEvolution.ATTAQUE, attaque)
        return EvolutionManuelle(evolution, MethodeEvolution.AUTRE, valeur)
    }
}
