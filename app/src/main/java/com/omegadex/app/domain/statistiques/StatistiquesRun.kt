package com.omegadex.app.domain.statistiques

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Durée d'une étape validée, de son déverrouillage à sa validation. */
data class DureeEtape(val etapeId: Int, val titre: String, val dureeMs: Long)

/** Défis relevés par une espèce dans la run. */
data class DefisEspece(val especeId: Int, val nom: String, val reussis: Int, val rates: Int, val actifs: Int) {
    val total: Int get() = reussis + rates + actifs
}

/** Bilan des défis : le taux de réussite ne compte que les défis terminés (réussis ou ratés). */
data class BilanDefis(val reussis: Int = 0, val rates: Int = 0, val actifs: Int = 0) {
    val termines: Int get() = reussis + rates

    /** Entre 0 et 1, ou null tant qu'aucun défi n'est terminé. */
    val tauxReussite: Float? get() = if (termines == 0) null else reussis.toFloat() / termines
}

/** Statistiques d'une run (F13). */
data class StatistiquesRun(
    val durees: List<DureeEtape> = emptyList(),
    val bilan: BilanDefis = BilanDefis(),
    /** Espèces qui ont relevé le plus de défis, de la plus sollicitée à la moins sollicitée. */
    val especes: List<DefisEspece> = emptyList(),
) {
    /** Durée totale des étapes validées. */
    val dureeTotaleMs: Long get() = durees.sumOf { it.dureeMs }

    /** Étape validée la plus longue, s'il y en a une. */
    val etapeLaPlusLongue: DureeEtape? get() = durees.maxByOrNull { it.dureeMs }

    companion object {
        const val ESPECES_MAX = 5

        private val ORDRE_ESPECES =
            compareByDescending<DefisEspece> { it.total }.thenByDescending { it.reussis }.thenBy { it.nom }

        /**
         * Agrège la progression d'une run : durée de chaque étape validée (dans l'ordre des étapes), bilan des défis,
         * et les [ESPECES_MAX] espèces qui ont relevé le plus de défis (à égalité : le plus de réussites, puis le nom).
         */
        fun de(
            etapes: List<ProgressionEtape>,
            defis: List<Defi>,
            titres: Map<Int, String>,
            noms: Map<Int, String>,
        ): StatistiquesRun {
            val durees = etapes
                .filter { it.dateDeverrouillage != null && it.dateValidation != null }
                .sortedBy { it.etapeId }
                .map { e ->
                    val debut = requireNotNull(e.dateDeverrouillage)
                    val duree = (requireNotNull(e.dateValidation) - debut).coerceAtLeast(0)
                    DureeEtape(e.etapeId, titres[e.etapeId] ?: "#${e.etapeId}", duree)
                }
            val bilan = BilanDefis(
                reussis = defis.count { it.etat == EtatDefi.REUSSI },
                rates = defis.count { it.etat == EtatDefi.RATE },
                actifs = defis.count { it.etat == EtatDefi.ACTIF },
            )
            val especes = defis.filter { it.especeId != null }
                .groupBy { requireNotNull(it.especeId) }
                .map { (id, liste) ->
                    DefisEspece(
                        especeId = id,
                        nom = noms[id] ?: "#$id",
                        reussis = liste.count { it.etat == EtatDefi.REUSSI },
                        rates = liste.count { it.etat == EtatDefi.RATE },
                        actifs = liste.count { it.etat == EtatDefi.ACTIF },
                    )
                }
                .sortedWith(ORDRE_ESPECES)
                .take(ESPECES_MAX)
            return StatistiquesRun(durees, bilan, especes)
        }
    }
}

/** Statistiques de la run, mises à jour à chaque étape validée et à chaque défi. */
class ObserverStatistiquesUseCase @Inject constructor(
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(runId: Long): Flow<StatistiquesRun> = combine(
        progression.etapes(runId),
        progression.defis(runId),
        jeu.etapes(),
        jeu.pokedexNational(),
    ) { etapes, defis, etapesJeu, especes ->
        StatistiquesRun.de(
            etapes,
            defis,
            etapesJeu.associate { it.id to it.titre },
            especes.associate { it.id to it.nom },
        )
    }
}
