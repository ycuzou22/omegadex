package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Difficulte

/**
 * Règles de difficulté et de plafond de niveau (cahier des charges, « Niveaux de difficulté »).
 * Le plafond d'une étape est le niveau du Pokémon le plus fort de son champion.
 *
 * - **Règle générale** : aucun défi ne demande de dépasser le plafond ; aucun Pokémon de l'équipe
 *   ne le dépasse pendant l'étape.
 * - **Évolution** : un défi d'évolution n'est proposé que si l'évolution est atteignable sous le plafond.
 * - **Hardcore** : en plus des défis Normal et Difficile, tous les Pokémon disponibles avant l'arène
 *   doivent être montés exactement au niveau du plafond, pas au-delà.
 */
object ReglesDifficulte {
    private val NIVEAU_EN_TETE = Regex("^\\s*(\\d+)")

    /** Une run propose les défis de sa difficulté et de toutes les difficultés inférieures. */
    fun estProposable(difficulteRun: Difficulte, difficulteMinModele: Difficulte): Boolean =
        difficulteRun.ordinal >= difficulteMinModele.ordinal

    /** Règle générale : aucun niveau ne dépasse le plafond. */
    fun respectePlafond(niveaux: Collection<Int>, plafond: Int): Boolean = niveaux.all { it <= plafond }

    /** Pokémon (par niveau) qui dépassent le plafond, pour l'expliquer au joueur. */
    fun auDessusDuPlafond(niveaux: Collection<Int>, plafond: Int): List<Int> = niveaux.filter { it > plafond }

    /**
     * Règle Hardcore : chaque Pokémon disponible avant l'arène est exactement au niveau du plafond.
     * Une liste vide ne respecte pas la règle (il faut au moins un Pokémon).
     */
    fun respecteHardcore(niveauxDisponibles: Collection<Int>, plafond: Int): Boolean =
        niveauxDisponibles.isNotEmpty() && niveauxDisponibles.all { it == plafond }

    /** Niveaux encore à gagner pour satisfaire la règle Hardcore (0 quand elle est respectée ou impossible). */
    fun niveauxManquantsHardcore(niveauxDisponibles: Collection<Int>, plafond: Int): Int =
        niveauxDisponibles.sumOf { (plafond - it).coerceAtLeast(0) }

    /** Niveau exigé par une condition d'évolution (« 16 », « 20 (Attaque > Défense) »…), sinon null. */
    fun niveauRequis(condition: ConditionEvolution, valeur: String?): Int? = if (condition ==
        ConditionEvolution.NIVEAU
    ) {
        valeur?.let { NIVEAU_EN_TETE.find(it)?.groupValues?.get(1)?.toInt() }
    } else {
        null
    }

    /**
     * Une évolution est atteignable sous le plafond si elle ne demande pas un niveau supérieur au plafond.
     * Les évolutions sans niveau (pierre, échange, bonheur…) sont toujours atteignables.
     */
    fun evolutionAtteignable(condition: ConditionEvolution, valeur: String?, plafond: Int): Boolean =
        niveauRequis(condition, valeur)?.let { it <= plafond } ?: true

    /** Niveau maximal qu'un défi peut fixer : le plafond, ou le plafond diminué d'un écart (« plafond − 5 »). */
    fun niveauMaximalDuDefi(plafond: Int, ecartSousPlafond: Int = 0): Int {
        require(ecartSousPlafond >= 0) { "Un défi ne peut pas viser au-dessus du plafond (écart $ecartSousPlafond)" }
        return plafond - ecartSousPlafond
    }
}
