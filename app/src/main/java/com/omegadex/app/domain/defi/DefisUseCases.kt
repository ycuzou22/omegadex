package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.RegleNational
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

/** Statuts à partir desquels une espèce compte comme capturée. */
private val STATUTS_CAPTURE =
    setOf(StatutEspece.CAPTUREE, StatutEspece.ENTRAINEE, StatutEspece.EVOLUEE, StatutEspece.MAITRISEE)

/** Défis disponibles pour l'étape en cours de la run active, recalculés à chaque changement de progression. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProposerDefisUseCase @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<List<DefiPropose>> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        if (enCours == null || etape == null) return@flatMapLatest flowOf(emptyList())
        val runId = enCours.run.id
        val suivi = combine(
            progression.equipe(runId),
            progression.statuts(runId),
            progression.defis(runId),
        ) { e, s, d -> Triple(e, s, d) }
        val donnees = combine(progression.etapes(runId), jeu.modelesDefis(), jeu.pokedexNational()) { et, m, p ->
            Triple(et, m, p)
        }
        combine(suivi, donnees) { a, b -> a to b }.mapLatest { (suiviRun, donneesJeu) ->
            val (equipe, statuts, defis) = suiviRun
            val (etapes, modeles, especes) = donneesJeu
            val parId = especes.associateBy { it.id }
            val equipeIds = equipe.map { it.especeId }.toSet()
            val captureesIds = statuts.filterValues { it in STATUTS_CAPTURE }.keys
            val contexte = ContexteDefis(
                difficulte = enCours.run.difficulte,
                etape = etape,
                starterNom = parId[enCours.run.starterId]?.nom.orEmpty(),
                especes = (equipeIds + captureesIds).mapNotNull { id ->
                    parId[id]?.let { candidate(it, id in equipeIds, id in captureesIds, parId) }
                },
                defisExistants = defis,
                nationalObtenu = RegleNational.debloque(etapes),
            )
            GenerateurDefis.generer(contexte, modeles)
        }
    }

    private suspend fun candidate(espece: Espece, dansEquipe: Boolean, capturee: Boolean, parId: Map<Int, Espece>) =
        EspeceCandidate(
            id = espece.id,
            nom = espece.nom,
            dansEquipe = dansEquipe,
            capturee = capturee || dansEquipe,
            aUneMega = espece.aUneMega,
            talentCache = jeu.talents(espece.id).first().any { it.cache },
            evolutions = jeu.famille(espece.id).first()
                .filter { it.especeSourceId == espece.id }
                .map { EvolutionPossible(parId[it.especeCibleId]?.nom.orEmpty(), it.condition, it.valeur) },
        )
}

/** Refus d'accepter un défi proposé. */
enum class RefusDefi { ESPECE_DEJA_ENGAGEE, }

/**
 * Accepte un défi proposé : il devient actif dans la run. Un Pokémon ne porte qu'un seul défi espèce
 * actif à la fois ; un second est refusé tant que le premier n'est pas terminé.
 */
class AccepterDefiUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, defi: DefiPropose): Result<Long> {
        val especeId = defi.especeId
        if (especeId != null && especeId in GenerateurDefis.especesOccupees(progression.defis(runId).first())) {
            return Result.failure(IllegalStateException(RefusDefi.ESPECE_DEJA_ENGAGEE.name))
        }
        return Result.success(progression.ajouterDefi(runId, defi.modele.id, especeId, defi.etapeId))
    }
}
