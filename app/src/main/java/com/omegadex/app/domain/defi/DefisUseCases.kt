package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.EtatDefi
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

/**
 * Défis disponibles pour l'étape en cours de la run active, recalculés à chaque changement de progression.
 *
 * Défis d'étape : tous ceux des modèles saisis à la main. Défis espèce : pour chaque Pokémon de l'équipe dont la
 * famille a une banque (`BanqueDefis`), **un seul défi tiré au sort** dans cette banque ; pour les autres
 * Pokémon capturés, les modèles espèce saisis à la main.
 */
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
        val championOuvert = combine(jeu.quetes(etape.id), progression.quetesCochees(runId)) { quetes, cochees ->
            VerrouChampion.queteRequise(etape.id, quetes, cochees) == null
        }
        val tout = combine(suivi, donnees, championOuvert) { a, b, c -> Triple(a, b, c) }
        tout.mapLatest { (suiviRun, donneesJeu, ouvert) ->
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
                championOuvert = ouvert,
            )
            val tirages = tirer(contexte, runId, defis)
            GenerateurDefis.generer(contexte, modeles).filterNot {
                it.modele.type in GenerateurDefis.TYPES_ESPECE && it.especeId?.let { id -> id in tirages } == true
            } + tirages.values.filterNotNull()
        }
    }

    /** Défi tiré dans la banque de chaque membre de l'équipe ; seuls les membres dont la famille a une banque. */
    private suspend fun tirer(contexte: ContexteDefis, runId: Long, defis: List<Defi>): Map<Int, DefiPropose?> =
        contexte.especes.filter { it.dansEquipe }.mapNotNull { espece ->
            val racine = BanqueDefis.racine(espece.id, jeu.famille(espece.id).first())
            val banque = jeu.banqueDefis(racine).first()
            if (banque.isEmpty()) return@mapNotNull null
            val termines = defis.count { it.especeId == espece.id && it.etat != EtatDefi.ACTIF }
            val graine = BanqueDefis.graine(runId, espece.id, contexte.etape.id, termines)
            espece.id to BanqueDefis.tirer(banque, espece, contexte, graine)
        }.toMap()

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
enum class RefusDefi { ESPECE_DEJA_ENGAGEE, CATEGORIE_OCCUPEE }

/**
 * Accepte un défi proposé : il devient actif dans la run. Un Pokémon ne porte qu'un seul défi espèce
 * actif à la fois, et chaque catégorie qu'un seul défi en cours ; un second est refusé tant que le premier
 * n'est pas terminé.
 */
class AccepterDefiUseCase @Inject constructor(
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    suspend operator fun invoke(runId: Long, defi: DefiPropose): Result<Long> {
        val defis = progression.defis(runId).first()
        val especeId = defi.especeId
        if (especeId != null && especeId in GenerateurDefis.especesOccupees(defis)) {
            return Result.failure(IllegalStateException(RefusDefi.ESPECE_DEJA_ENGAGEE.name))
        }
        val types = jeu.modelesDefis().first().associate { it.id to it.type }
        if (CategorieDefi.de(defi.modele.type) in GenerateurDefis.categoriesOccupees(defis, types)) {
            return Result.failure(IllegalStateException(RefusDefi.CATEGORIE_OCCUPEE.name))
        }
        return Result.success(progression.ajouterDefi(runId, defi.modele.id, especeId, defi.etapeId))
    }
}
