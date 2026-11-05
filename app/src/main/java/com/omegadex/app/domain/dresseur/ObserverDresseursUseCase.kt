package com.omegadex.app.domain.dresseur

import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.repository.DresseursRepository
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Un dresseur de l'étape, son équipe, la stratégie pour le battre et s'il est déjà battu. */
data class FicheDresseur(
    val dresseur: Dresseur,
    val equipe: List<PokemonEnCombat>,
    val strategie: Strategie,
    val battu: Boolean,
)

/** Les dresseurs de l'étape en cours, dans l'ordre de rencontre (le champion en dernier). */
data class DresseursEtape(val etape: Etape, val fiches: List<FicheDresseur>) {
    val battus: Int get() = fiches.count { it.battu }
}

/** Suit les dresseurs de l'étape en cours de la run active ; null sans run ou après la dernière étape. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverDresseursUseCase @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val dresseurs: DresseursRepository,
    private val jeu: JeuRepository,
    private val progression: ProgressionRepository,
) {
    operator fun invoke(): Flow<DresseursEtape?> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        if (enCours == null || etape == null) return@flatMapLatest flowOf(null)
        val runId = enCours.run.id
        combine(
            dresseurs.dresseurs(),
            jeu.pokedexNational(),
            progression.equipe(runId),
            progression.quetesCochees(runId),
        ) { tous, especes, equipe, cochees ->
            val parId = especes.associateBy { it.id }
            val mienne = equipe.mapNotNull { m -> parId[m.especeId]?.let { PokemonEnCombat(it, m.niveau) } }
            val fiches = tous.filter { it.etapeId == etape.id }.map { d ->
                val adverse = d.equipe.mapNotNull { p ->
                    parId[p.especeId]?.let { PokemonEnCombat(it, p.niveau, p.attaques) }
                }
                FicheDresseur(d, adverse, StrategieCombat.calculer(adverse, d.type, mienne), d.queteId in cochees)
            }
            DresseursEtape(etape, fiches)
        }
    }
}
