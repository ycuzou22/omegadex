package com.omegadex.app.domain.accueil

import com.omegadex.app.domain.defi.BilanDefis
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.RegleNational
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Une jauge : [valeur] sur [total]. */
data class Jauge(val valeur: Int, val total: Int) {
    val progression: Float get() = if (total == 0) 0f else (valeur.toFloat() / total).coerceIn(0f, 1f)
}

/** Les trois jauges de l'accueil. */
data class JaugesAccueil(
    /** Étapes validées sur le nombre d'étapes. */
    val aventure: Jauge,
    /** Espèces capturées sur la taille du Pokédex affiché (Hoenn, puis national après la Ligue). */
    val pokedex: Jauge,
    /** Défis réussis sur les défis relevés. */
    val defis: Jauge,
)

/** Statuts qui comptent comme capturés dans le Pokédex. */
private val STATUTS_CAPTURE =
    setOf(StatutEspece.CAPTUREE, StatutEspece.ENTRAINEE, StatutEspece.EVOLUEE, StatutEspece.MAITRISEE)

/** Jauges de la run active, mises à jour en temps réel à chaque action (quête, capture, défi). */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverJaugesUseCase @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<JaugesAccueil?> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) return@flatMapLatest flowOf(null)
        val runId = enCours.run.id
        val suivi = combine(
            progression.etapes(runId),
            progression.statuts(runId),
            progression.defis(runId),
        ) { e, s, d -> Triple(e, s, d) }
        combine(suivi, jeu.pokedexHoenn(), jeu.pokedexNational()) { (etapes, statuts, defis), hoenn, national ->
            val pokedex = if (RegleNational.debloque(etapes)) national.map { it.id } else hoenn.map { it.id }
            val capturees = statuts.filterValues { it in STATUTS_CAPTURE }.keys
            val bilan = BilanDefis.de(defis)
            JaugesAccueil(
                aventure = Jauge(etapes.count { it.dateValidation != null }, enCours.totalEtapes),
                pokedex = Jauge(pokedex.count { it in capturees }, pokedex.size),
                defis = Jauge(bilan.reussis, bilan.actifs + bilan.termines),
            )
        }
    }
}
