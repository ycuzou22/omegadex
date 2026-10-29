package com.omegadex.app.domain.badge

import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Les 8 badges d'arène de Hoenn, dans l'ordre de la boîte. Un badge est obtenu quand l'étape de son arène
 * ([etapeId], données `tools/import/donnees/etapes_rosa.json`) est validée. [type] est la clé du type de
 * l'arène (couleur du badge). Les dessins sont originaux, sans reprendre ceux du jeu.
 */
enum class BadgeArene(val etapeId: Int, val type: String) {
    ROCHE(1, "roche"),
    POING(2, "combat"),
    DYNAMO(3, "electrik"),
    CHALEUR(4, "feu"),
    BALANCE(5, "normal"),
    PLUME(6, "vol"),
    ESPRIT(7, "psy"),
    PLUIE(9, "eau"),
    ;

    companion object {
        /** Badge remporté en validant l'étape [etapeId], ou null si ce n'est pas une arène. */
        fun pourEtape(etapeId: Int): BadgeArene? = entries.firstOrNull { it.etapeId == etapeId }

        /** Badges obtenus d'après les étapes validées. */
        fun obtenus(etapesValidees: Set<Int>): Set<BadgeArene> = entries.filter { it.etapeId in etapesValidees }.toSet()
    }
}

/** Identifiants des étapes validées d'une progression. */
fun List<ProgressionEtape>.idsValidees(): Set<Int> = filter { it.dateValidation != null }.map { it.etapeId }.toSet()

/** Badges obtenus dans une run, mis à jour à chaque étape validée. */
class ObserverBadgesUseCase @Inject constructor(private val progression: ProgressionRepository) {
    operator fun invoke(runId: Long): Flow<Set<BadgeArene>> =
        progression.etapes(runId).map { BadgeArene.obtenus(it.idsValidees()) }.distinctUntilChanged()
}
