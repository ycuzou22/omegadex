package com.omegadex.app.feature.defis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.defi.ActionsRoulette
import com.omegadex.app.domain.defi.ActionsSurDefis
import com.omegadex.app.domain.defi.CaseRoulette
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.DefiAffiche
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.Roulette
import com.omegadex.app.domain.defi.RouletteDefis
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Espace Défis (onglet, avec ses six roulettes) et page « Défis de … » d'un Pokémon (argument [ARGUMENT_ESPECE]). */
@HiltViewModel
class DefisViewModel @Inject constructor(
    etatNavigation: SavedStateHandle,
    observerDefis: ObserverDefisUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val actions: ActionsSurDefis,
    private val roulette: ActionsRoulette,
    jeu: JeuRepository,
) : ViewModel() {
    private val especeId: Int? = etatNavigation[ARGUMENT_ESPECE]
    private val onglet = MutableStateFlow(if (especeId == null) OngletDefis.ROULETTE else OngletDefis.ACTIFS)
    private val filtre = MutableStateFlow<Difficulte?>(null)
    private val refus = Channel<Unit>(Channel.CONFLATED)
    private val tours = Channel<TourDeRoulette>(Channel.BUFFERED)

    /** Émis quand un défi est refusé (le Pokémon ou la catégorie porte déjà un défi). */
    val refusEspece: Flow<Unit> = refus.receiveAsFlow()

    /** Émis à chaque tour de roulette, pour l'animer. */
    val toursDeRoulette: Flow<TourDeRoulette> = tours.receiveAsFlow()

    val uiState: StateFlow<DefisUiState> = combine(
        observerDefis(),
        if (especeId == null) roulette.observer() else flowOf(null),
        onglet,
        filtre,
        especeId?.let { id -> jeu.espece(id).map { it?.nom } } ?: flowOf(null),
    ) { run, roues, o, f, nomEspece ->
        if (run == null) return@combine DefisUiState.AucuneRun
        fun garder(carte: CarteDefi) =
            (especeId == null || carte.especeId == especeId) && (f == null || carte.difficulte == f)
        val rouletteUi = roues?.let(::rouletteUi)
        DefisUiState.Espace(
            especeNom = nomEspece,
            difficulteRun = run.difficulte,
            onglet = if (rouletteUi == null && o == OngletDefis.ROULETTE) OngletDefis.ACTIFS else o,
            filtre = f,
            actifs = run.actifs.map { carte(it) }.filter(::garder),
            termines = run.historique.map { carte(it) }.filter(::garder),
            bilan = run.bilan,
            roulette = rouletteUi,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), DefisUiState.Chargement)

    fun choisirOnglet(nouvel: OngletDefis) {
        onglet.value = nouvel
    }

    fun filtrer(difficulte: Difficulte?) {
        filtre.value = difficulte
    }

    /** Fait tourner la roulette d'une catégorie ; le tour est émis dans [toursDeRoulette]. */
    fun tourner(categorie: CategorieDefi) = surLaRoulette(categorie) { runId, roues, case ->
        val gagnant = roulette.tourner(runId, roues.etape.id, case) ?: return@surLaRoulette
        tours.send(TourDeRoulette(categorie, carte(gagnant), RouletteDefis.rouleau(case.pool, gagnant).map(::carte)))
    }

    /** Relève le défi tiré d'une catégorie. */
    fun releverTirage(categorie: CategorieDefi) = surLaRoulette(categorie) { runId, roues, case ->
        if (roulette.relever(runId, roues.etape.id, case).isFailure) refus.trySend(Unit)
    }

    fun valider(defiId: Long, reussi: Boolean) = executerSurLaRun { actions.valider(it, defiId, reussi) }

    fun retenter(defiId: Long) = executerSurLaRun { runId ->
        if (actions.retenter(runId, defiId) == null) refus.trySend(Unit) else onglet.value = OngletDefis.ACTIFS
    }

    private fun surLaRoulette(categorie: CategorieDefi, action: suspend (Long, Roulette, CaseRoulette) -> Unit) =
        executerSurLaRun { runId ->
            val roues = roulette.observer().first() ?: return@executerSurLaRun
            action(runId, roues, roues.cases.first { it.categorie == categorie })
        }

    private fun executerSurLaRun(action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            action(runId)
        }
    }

    private fun rouletteUi(roues: Roulette) = RouletteUi(
        numeroEtape = roues.etape.id,
        adversaire = roues.etape.adversaire,
        cases = roues.cases.map { case ->
            val etat = when {
                case.actif != null -> EtatCase.EN_COURS
                case.tirage != null -> EtatCase.TIRE
                case.queteRequise != null -> EtatCase.VERROU
                case.pool.isEmpty() -> EtatCase.VIDE
                else -> EtatCase.LIBRE
            }
            CaseRouletteUi(
                categorie = case.categorie,
                etat = etat,
                nombre = case.pool.size,
                defi = case.actif?.let { carte(it) } ?: case.tirage?.let { carte(it) },
                relancesRestantes = case.relancesRestantes,
                peutTourner = case.peutTourner,
                queteRequise = case.queteRequise?.description,
            )
        },
    )

    private fun carte(affiche: DefiAffiche) = CarteDefi(
        cle = "defi-${affiche.defi.id}",
        defiId = affiche.defi.id,
        titre = affiche.modele.titre,
        enonce = affiche.enonce,
        type = affiche.modele.type,
        difficulte = affiche.modele.difficulteMin,
        especeId = affiche.defi.especeId,
        especeNom = affiche.especeNom,
        etat = affiche.defi.etat,
    )

    private fun carte(propose: DefiPropose) = CarteDefi(
        cle = "propose-${propose.modele.id}-${propose.especeId}-${propose.etapeId}",
        defiId = null,
        titre = propose.modele.titre,
        enonce = propose.enonce,
        type = propose.modele.type,
        difficulte = propose.modele.difficulteMin,
        especeId = propose.especeId,
        especeNom = propose.especeNom,
        etat = null,
    )

    companion object {
        const val ARGUMENT_ESPECE = "especeId"
        private const val ARRET_MS = 5_000L
    }
}
