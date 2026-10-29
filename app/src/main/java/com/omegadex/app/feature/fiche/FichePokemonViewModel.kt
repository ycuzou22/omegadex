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
import kotlinx.coroutines.flow.map
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

    private val statut = observerRunEnCours().flatMapLatest { enCours ->
        enCours?.let { progression.statuts(it.run.id).map { statuts -> statuts[especeId] ?: StatutEspece.NON_VUE } }
            ?: flowOf(null)
    }

    val uiState: StateFlow<FichePokemonUiState> = combine(jeu.espece(especeId), statut) { espece, statutRun ->
        if (espece == null) FichePokemonUiState.Introuvable else FichePokemonUiState.Fiche(espece, statutRun)
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
