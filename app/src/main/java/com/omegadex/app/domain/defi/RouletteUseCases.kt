package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.TiragesDefisRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Une catégorie de la page Défis : les défis que sa roulette peut tirer ([pool]), le défi en cours, le défi
 * tiré et pas encore relevé, et ce qui reste de relances pendant l'étape (null : illimitées).
 * [queteRequise] : quête à cocher avant que le défi du champion s'ouvre.
 */
data class CaseRoulette(
    val categorie: CategorieDefi,
    val pool: List<DefiPropose>,
    val actif: DefiAffiche?,
    val tirage: DefiPropose?,
    val relancesFaites: Int,
    val relancesRestantes: Int?,
    val queteRequise: Quete?,
) {
    /** La roulette tourne si rien n'est en cours et, quand un défi est déjà tiré, s'il reste une relance. */
    val peutTourner: Boolean
        get() = actif == null && queteRequise == null && pool.isNotEmpty() && (tirage == null || relancesRestantes != 0)
}

/** Les six roulettes de la run active pour l'étape en cours. */
data class Roulette(val difficulte: Difficulte, val etape: Etape, val cases: List<CaseRoulette>)

/** Suit les six roulettes de la run active ; null sans run ou après la dernière étape. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverRouletteUseCase @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val proposerDefis: ProposerDefisUseCase,
    private val observerDefis: ObserverDefisUseCase,
    private val tirages: TiragesDefisRepository,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<Roulette?> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        if (enCours == null || etape == null) return@flatMapLatest flowOf(null)
        val runId = enCours.run.id
        val verrou = combine(jeu.quetes(etape.id), progression.quetesCochees(runId)) { quetes, cochees ->
            VerrouChampion.queteRequise(etape.id, quetes, cochees)
        }
        combine(proposerDefis(), observerDefis(), tirages.tirages(runId), verrou) { proposes, defis, tires, quete ->
            val difficulte = enCours.run.difficulte
            Roulette(
                difficulte,
                etape,
                CategorieDefi.entries.map { categorie ->
                    val actif = defis?.actifs.orEmpty().firstOrNull { CategorieDefi.de(it.modele.type) == categorie }
                    val pool = proposes.filter { CategorieDefi.de(it.modele.type) == categorie }
                    val tirage = tires[categorie]?.takeIf { it.etapeId == etape.id }
                    val faites = tirage?.relances ?: 0
                    CaseRoulette(
                        categorie = categorie,
                        pool = pool,
                        actif = actif,
                        tirage = tirage?.defi?.let { cle -> pool.firstOrNull(cle::designe) }.takeIf { actif == null },
                        relancesFaites = faites,
                        relancesRestantes = RouletteDefis.relancesRestantes(difficulte, faites),
                        queteRequise = quete.takeIf { categorie == CategorieDefi.CHAMPION && actif == null },
                    )
                },
            )
        }
    }
}

/**
 * Fait tourner la roulette d'une catégorie : tire un défi dans son pool et le garde jusqu'à ce qu'il soit relevé.
 * Tourner alors qu'un défi est déjà tiré coûte une relance. Renvoie le défi tiré, ou null si la roulette est bloquée.
 */
class TournerRouletteUseCase @Inject constructor(private val tirages: TiragesDefisRepository) {
    suspend operator fun invoke(
        runId: Long,
        etapeId: Int,
        case: CaseRoulette,
        aleatoire: Random = Random.Default,
    ): DefiPropose? {
        if (!case.peutTourner) return null
        val gagnant = RouletteDefis.tirer(case.pool, case.tirage, aleatoire) ?: return null
        val relances = case.relancesFaites + if (case.tirage != null) 1 else 0
        tirages.enregistrer(runId, case.categorie, Tirage(etapeId, CleDefi.de(gagnant), relances))
        return gagnant
    }
}

/** Relève le défi tiré d'une catégorie : il devient actif, et les relances déjà faites restent comptées. */
class ReleverTirageUseCase @Inject constructor(
    private val accepter: AccepterDefiUseCase,
    private val tirages: TiragesDefisRepository,
) {
    suspend operator fun invoke(runId: Long, etapeId: Int, case: CaseRoulette): Result<Long> {
        val tire = case.tirage ?: return Result.failure(IllegalStateException("aucun défi tiré"))
        return accepter(runId, tire).onSuccess {
            tirages.enregistrer(runId, case.categorie, Tirage(etapeId, null, case.relancesFaites))
        }
    }
}

/** Les actions de la roulette, regroupées pour les écrans. */
class ActionsRoulette @Inject constructor(
    val observer: ObserverRouletteUseCase,
    val tourner: TournerRouletteUseCase,
    val relever: ReleverTirageUseCase,
)
