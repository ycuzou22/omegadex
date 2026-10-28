package com.omegadex.app.feature.defis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.defi.ActionsSurDefis
import com.omegadex.app.domain.defi.DefiAffiche
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.ProposerDefisUseCase
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

/** Espace Défis (onglet) et page « Défis de … » d'un Pokémon (avec l'argument [ARGUMENT_ESPECE]). */
@HiltViewModel
class DefisViewModel @Inject constructor(
    etatNavigation: SavedStateHandle,
    observerDefis: ObserverDefisUseCase,
    proposerDefis: ProposerDefisUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val actions: ActionsSurDefis,
    jeu: JeuRepository,
) : ViewModel() {
    private val especeId: Int? = etatNavigation[ARGUMENT_ESPECE]
    private val onglet = MutableStateFlow(OngletDefis.ACTIFS)
    private val filtre = MutableStateFlow<Difficulte?>(null)
    private val propositions = mutableMapOf<String, DefiPropose>()
    private val refus = Channel<Unit>(Channel.CONFLATED)

    /** Émis quand un défi est refusé (le Pokémon relève déjà un défi). */
    val refusEspece: Flow<Unit> = refus.receiveAsFlow()

    val uiState: StateFlow<DefisUiState> = combine(
        observerDefis(),
        proposerDefis(),
        onglet,
        filtre,
        especeId?.let { id -> jeu.espece(id).map { it?.nom } } ?: flowOf(null),
    ) { run, proposes, o, f, nomEspece ->
        if (run == null) return@combine DefisUiState.AucuneRun
        val disponibles = proposes.filter { especeId == null || it.especeId == especeId }
        // Les propositions restent connues : un « Relever » tardif sur une carte déjà disparue est refusé proprement.
        synchronized(propositions) { disponibles.forEach { propositions[cle(it)] = it } }
        fun garder(carte: CarteDefi) =
            (especeId == null || carte.especeId == especeId) && (f == null || carte.difficulte == f)
        DefisUiState.Espace(
            especeNom = nomEspece,
            difficulteRun = run.difficulte,
            onglet = o,
            filtre = f,
            actifs = run.actifs.map { carte(it) }.filter(::garder),
            disponibles = disponibles.map { carte(it) }.filter(::garder),
            termines = run.historique.map { carte(it) }.filter(::garder),
            bilan = run.bilan,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), DefisUiState.Chargement)

    fun choisirOnglet(nouvel: OngletDefis) {
        onglet.value = nouvel
    }

    fun filtrer(difficulte: Difficulte?) {
        filtre.value = difficulte
    }

    fun relever(cle: String) {
        val propose = synchronized(propositions) { propositions[cle] } ?: return
        executerSurLaRun { runId ->
            if (actions.accepter(runId, propose).isFailure) refus.trySend(Unit) else onglet.value = OngletDefis.ACTIFS
        }
    }

    fun valider(defiId: Long, reussi: Boolean) = executerSurLaRun { actions.valider(it, defiId, reussi) }

    fun retenter(defiId: Long) = executerSurLaRun { runId ->
        if (actions.retenter(runId, defiId) == null) refus.trySend(Unit) else onglet.value = OngletDefis.ACTIFS
    }

    private fun executerSurLaRun(action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            action(runId)
        }
    }

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
        cle = cle(propose),
        defiId = null,
        titre = propose.modele.titre,
        enonce = propose.enonce,
        type = propose.modele.type,
        difficulte = propose.modele.difficulteMin,
        especeId = propose.especeId,
        especeNom = propose.especeNom,
        etat = null,
    )

    private fun cle(propose: DefiPropose) = "propose-${propose.modele.id}-${propose.especeId}-${propose.etapeId}"

    companion object {
        const val ARGUMENT_ESPECE = "especeId"
        private const val ARRET_MS = 5_000L
    }
}
