package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ModeleDefi
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

/** Un défi enregistré de la run, prêt à afficher. */
data class DefiAffiche(val defi: Defi, val modele: ModeleDefi, val enonce: String, val especeNom: String?)

/** Compteurs des jauges de l'espace Défis. */
data class BilanDefis(val actifs: Int, val reussis: Int, val rates: Int) {
    val termines: Int get() = reussis + rates

    /** Part des défis terminés qui sont réussis (0 quand aucun défi n'est terminé). */
    val tauxReussite: Float get() = if (termines == 0) 0f else reussis.toFloat() / termines

    companion object {
        fun de(defis: List<Defi>) = BilanDefis(
            actifs = defis.count { it.etat == EtatDefi.ACTIF },
            reussis = defis.count { it.etat == EtatDefi.REUSSI },
            rates = defis.count { it.etat == EtatDefi.RATE },
        )
    }
}

/** Défis de la run active : actifs, historique des défis terminés (le plus récent d'abord) et bilan. */
data class DefisDeLaRun(
    val difficulte: Difficulte,
    val actifs: List<DefiAffiche>,
    val historique: List<DefiAffiche>,
    val bilan: BilanDefis,
)

/** Suit les défis enregistrés de la run active ; les jauges se mettent à jour à chaque validation. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverDefisUseCase @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<DefisDeLaRun?> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) return@flatMapLatest flowOf(null)
        combine(
            progression.defis(enCours.run.id),
            jeu.modelesDefis(),
            jeu.etapes(),
            jeu.pokedexNational(),
        ) { defis, modeles, etapes, especes -> Donnees(defis, modeles, etapes, especes) }
            .mapLatest { donnees ->
                val defis = donnees.defis
                val etapes = donnees.etapes
                // Les défis tirés de la banque par famille ne sont pas dans les modèles saisis à la main.
                val connus = donnees.modeles.map { it.id }.toSet()
                val manquants = defis.map { it.modeleId }.filter { it !in connus }
                val banque = if (manquants.isEmpty()) emptyList() else jeu.modelesDefisParIds(manquants).first()
                val parModele = (donnees.modeles + banque).associateBy { it.id }
                val parEtape = etapes.associateBy { it.id }
                val noms = donnees.especes.associate { it.id to it.nom }
                val etapeCourante = enCours.etape ?: etapes.lastOrNull()
                val starter = noms[enCours.run.starterId].orEmpty()
                val affiches = defis.mapNotNull { defi ->
                    val modele = parModele[defi.modeleId] ?: return@mapNotNull null
                    val etape = defi.etapeId?.let { parEtape[it] } ?: etapeCourante ?: return@mapNotNull null
                    val especeNom = defi.especeId?.let { noms[it] }
                    val evolution = if (defi.especeId != null) evolutionDe(defi.especeId, noms) else null
                    val enonce = GenerateurDefis.enonce(modele, etape, starter, especeNom, evolution)
                    DefiAffiche(defi, modele, enonce, especeNom)
                }
                DefisDeLaRun(
                    difficulte = enCours.run.difficulte,
                    actifs = affiches.filter { it.defi.etat == EtatDefi.ACTIF }.sortedBy { it.defi.dateDebut },
                    historique = affiches.filter { it.defi.etat != EtatDefi.ACTIF }
                        .sortedByDescending { it.defi.dateFin ?: it.defi.dateDebut },
                    bilan = BilanDefis.de(defis),
                )
            }
    }

    private suspend fun evolutionDe(especeId: Int, noms: Map<Int, String>): EvolutionPossible? =
        jeu.famille(especeId).first().firstOrNull { it.especeSourceId == especeId }?.let {
            EvolutionPossible(noms[it.especeCibleId].orEmpty(), it.condition, it.valeur)
        }

    private data class Donnees(
        val defis: List<Defi>,
        val modeles: List<ModeleDefi>,
        val etapes: List<Etape>,
        val especes: List<Espece>,
    )
}

/**
 * Le joueur valide lui-même ses défis : un défi actif devient réussi ou raté (date de fin enregistrée).
 * Un défi déjà terminé ne change plus d'état : il reste dans l'historique.
 */
class ValiderDefiUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, defiId: Long, reussi: Boolean): Boolean {
        val defi = progression.defis(runId).first().firstOrNull { it.id == defiId }
        if (defi?.etat != EtatDefi.ACTIF) return false
        progression.changerEtatDefi(runId, defiId, if (reussi) EtatDefi.REUSSI else EtatDefi.RATE)
        return true
    }
}

/**
 * Retente un défi raté : un nouveau défi actif reprend le même modèle, la même espèce et la même étape ;
 * le défi raté reste dans l'historique. Refusé si le Pokémon porte déjà un défi espèce actif.
 */
class RetenterDefiUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, defiId: Long): Long? {
        val defis = progression.defis(runId).first()
        val rate = defis.firstOrNull { it.id == defiId && it.etat == EtatDefi.RATE } ?: return null
        if (rate.especeId != null && rate.especeId in GenerateurDefis.especesOccupees(defis)) return null
        val dejaRetente = defis.any {
            it.etat == EtatDefi.ACTIF &&
                it.modeleId == rate.modeleId &&
                it.especeId == rate.especeId &&
                it.etapeId == rate.etapeId
        }
        if (dejaRetente) return null
        return progression.ajouterDefi(runId, rate.modeleId, rate.especeId, rate.etapeId)
    }
}

/** Les trois actions du joueur sur ses défis, regroupées pour les écrans. */
class ActionsSurDefis @Inject constructor(
    val accepter: AccepterDefiUseCase,
    val valider: ValiderDefiUseCase,
    val retenter: RetenterDefiUseCase,
)
