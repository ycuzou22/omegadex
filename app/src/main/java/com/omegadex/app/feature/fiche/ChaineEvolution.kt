package com.omegadex.app.feature.fiche

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece

/** Statuts et équipe de la run active, pour situer le joueur dans la chaîne d'évolution. */
data class SuiviRun(val statuts: Map<Int, StatutEspece>, val equipe: List<MembreEquipe>)

/** Une espèce de la famille ; [vue] est faux tant que le joueur ne l'a pas rencontrée (sprite en ombre). */
data class StadeEvolution(val id: Int, val nom: String, val types: List<String>, val vue: Boolean = true)

/** Un palier : de [sourceId] à [cibleId], avec sa condition. */
data class PalierEvolution(val sourceId: Int, val cibleId: Int, val condition: ConditionEvolution, val valeur: String?)

/**
 * Chaîne d'évolution complète d'une espèce : les stades par profondeur (le premier stade d'abord ; une branche
 * comme Tarsal → Gardevoir / Gallame donne plusieurs espèces au même stade) et les paliers avec leurs conditions.
 * [stadeActuel] est l'espèce de la famille où en est le joueur : dans son équipe, sinon la plus évoluée capturée.
 */
data class ChaineEvolution(
    val stades: List<List<StadeEvolution>>,
    val paliers: List<PalierEvolution>,
    val stadeActuel: Int?,
) {
    /** Chaîne sans branche de trois stades au plus : affichée en ligne comme la maquette. */
    val lineaire: Boolean get() = stades.all { it.size == 1 } && stades.size <= STADES_EN_LIGNE

    fun palierVers(cibleId: Int): PalierEvolution? = paliers.firstOrNull { it.cibleId == cibleId }

    companion object {
        private const val STADES_EN_LIGNE = 3

        private val STATUTS_CAPTURE =
            setOf(StatutEspece.CAPTUREE, StatutEspece.ENTRAINEE, StatutEspece.EVOLUEE, StatutEspece.MAITRISEE)

        fun construire(
            especeId: Int,
            famille: List<Evolution>,
            especes: List<Espece>,
            suivi: SuiviRun?,
        ): ChaineEvolution {
            val parId = especes.associateBy { it.id }
            val cibles = famille.map { it.especeCibleId }.toSet()
            val racines = famille.map { it.especeSourceId }.filter { it !in cibles }.distinct()
                .ifEmpty { listOf(especeId) }
            val stades = mutableListOf<List<Int>>()
            var niveau = racines
            while (niveau.isNotEmpty()) {
                stades += niveau
                niveau = niveau
                    .flatMap { id -> famille.filter { it.especeSourceId == id }.map { it.especeCibleId } }
                    .distinct()
            }
            val ordre = stades.flatten()
            val actuel = suivi?.let { s ->
                ordre.lastOrNull { id -> s.equipe.any { it.especeId == id } }
                    ?: ordre.lastOrNull { id -> s.statuts[id] in STATUTS_CAPTURE }
            }
            return ChaineEvolution(
                stades = stades.map { ids ->
                    ids.mapNotNull { id ->
                        parId[id]?.let { espece ->
                            val statut = suivi?.statuts?.get(id)
                            val vue = suivi == null || (statut != null && statut != StatutEspece.NON_VUE)
                            StadeEvolution(id, espece.nom, espece.types, vue)
                        }
                    }
                },
                paliers = famille.map { PalierEvolution(it.especeSourceId, it.especeCibleId, it.condition, it.valeur) },
                stadeActuel = actuel,
            )
        }
    }
}
