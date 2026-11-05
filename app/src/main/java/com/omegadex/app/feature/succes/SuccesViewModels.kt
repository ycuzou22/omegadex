package com.omegadex.app.feature.succes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.succes.EtatSucces
import com.omegadex.app.domain.succes.ObserverSuccesUseCase
import com.omegadex.app.domain.succes.Succes
import com.omegadex.app.domain.succes.SurveillerSuccesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Vitrine des succès de la run en cours (écran Succès et carte de l'accueil). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VitrineSuccesViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    observerSucces: ObserverSuccesUseCase,
) : ViewModel() {
    /** null tant que la run n'est pas chargée ; vide sans run. */
    val succes: StateFlow<List<EtatSucces>?> = observerRunEnCours()
        .map { it?.run?.id }
        .distinctUntilChanged()
        .flatMapLatest { runId -> if (runId == null) flowOf(emptyList()) else observerSucces(runId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), null)

    private companion object {
        const val ARRET_MS = 5_000L
    }
}

/**
 * Détection des succès pendant que l'app est ouverte (T18.1) et file des annonces de déblocage :
 * chaque succès débloqué est annoncé une fois, l'un après l'autre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnnonceSuccesViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    surveiller: SurveillerSuccesUseCase,
) : ViewModel() {
    private val file = MutableStateFlow<List<Succes>>(emptyList())

    /** Annonces à jouer, la première est à l'écran. */
    val annonces: StateFlow<List<Succes>> = file.asStateFlow()

    init {
        viewModelScope.launch {
            observerRunEnCours()
                .map { it?.run?.id }
                .distinctUntilChanged()
                .flatMapLatest { runId -> if (runId == null) emptyFlow() else surveiller(runId) }
                .collect { nouveaux -> file.update { it + nouveaux } }
        }
    }

    /** L'annonce affichée est terminée (fin de l'animation ou toucher) : on passe à la suivante. */
    fun suivante() = file.update { it.drop(1) }
}
