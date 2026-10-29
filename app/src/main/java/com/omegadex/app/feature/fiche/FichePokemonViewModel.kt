package com.omegadex.app.feature.fiche

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.feature.pokedex.precedent
import com.omegadex.app.feature.pokedex.suivant
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Fiche Pokémon : l'espèce vient de la base jeu, son statut de la run active. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FichePokemonViewModel @Inject constructor(
    etatNavigation: SavedStateHandle,
    jeu: JeuRepository,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
) : ViewModel() {
    val especeId: Int = checkNotNull(etatNavigation[ARGUMENT_ESPECE])

    /** Statuts et équipe de la run active ; null sans run. */
    private val suivi = observerRunEnCours().flatMapLatest { enCours ->
        enCours?.let { combine(progression.statuts(it.run.id), progression.equipe(it.run.id), ::SuiviRun) }
            ?: flowOf(null)
    }

    val uiState: StateFlow<FichePokemonUiState> = combine(
        jeu.espece(especeId),
        suivi,
        jeu.talents(especeId),
        jeu.famille(especeId),
        jeu.pokedexNational(),
    ) { espece, suiviRun, talents, famille, especes ->
        if (espece == null) return@combine FichePokemonUiState.Introuvable
        val membres = (famille.flatMap { listOf(it.especeSourceId, it.especeCibleId) } + espece.id).distinct()
        val stadeMega = especes.filter { it.id in membres && it.aUneMega }
            .minByOrNull { if (it.id == espece.id) 0 else 1 }
            ?.nom
        FichePokemonUiState.Fiche(
            espece = espece,
            statut = suiviRun?.let { it.statuts[especeId] ?: StatutEspece.NON_VUE },
            talents = talents,
            stadeMega = stadeMega,
            evolution = ChaineEvolution.construire(espece.id, famille, especes.filter { it.id in membres }, suiviRun),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), FichePokemonUiState.Chargement)

    /** Une touche sur les rubis : statut suivant ; appui long : statut précédent (comme dans le Pokédex). */
    fun avancerStatut() = changerStatut(StatutEspece::suivant)

    fun reculerStatut() = changerStatut(StatutEspece::precedent)

    private fun changerStatut(transition: (StatutEspece) -> StatutEspece) {
        val actuel = (uiState.value as? FichePokemonUiState.Fiche)?.statut ?: return
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            progression.definirStatut(runId, especeId, transition(actuel))
        }
    }

    companion object {
        const val ARGUMENT_ESPECE = "especeId"
        private const val ARRET_MS = 5_000L
    }
}
