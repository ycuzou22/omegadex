package com.omegadex.app.domain.accueil

import com.omegadex.app.core.Horloge
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.defi.GenerateurDefis
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Défi du jour de l'accueil : un défi disponible choisi de façon stable pour la journée (il ne change pas
 * à chaque ouverture), en privilégiant les défis espèce, plus variés que le défi d'étape.
 */
class DefiDuJourUseCase @Inject constructor(
    private val proposerDefis: ProposerDefisUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val horloge: Horloge,
) {
    operator fun invoke(): Flow<DefiPropose?> = combine(proposerDefis(), observerRunEnCours()) { proposes, enCours ->
        choisir(proposes, jour = horloge.maintenant() / MS_PAR_JOUR, graine = enCours?.run?.id ?: 0L)
    }

    companion object {
        private const val MS_PAR_JOUR = 86_400_000L

        /** Choix déterministe pour un jour et une run donnés. */
        fun choisir(proposes: List<DefiPropose>, jour: Long, graine: Long): DefiPropose? {
            val especes = proposes.filter { it.modele.type in GenerateurDefis.TYPES_ESPECE }
            val candidats = especes.ifEmpty { proposes }
                .sortedWith(compareBy({ it.modele.id }, { it.especeId ?: 0 }, { it.etapeId ?: 0 }))
            if (candidats.isEmpty()) return null
            return candidats[Math.floorMod(jour + graine, candidats.size.toLong()).toInt()]
        }
    }
}
