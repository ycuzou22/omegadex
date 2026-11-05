package com.omegadex.app.feature.statistiques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.domain.statistiques.ObserverStatistiquesUseCase
import com.omegadex.app.domain.statistiques.StatistiquesRun
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Statistiques de la run en cours ; null pendant le chargement. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatistiquesViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    observerStatistiques: ObserverStatistiquesUseCase,
) : ViewModel() {
    val statistiques: StateFlow<StatistiquesRun?> = observerRunEnCours()
        .map { it?.run?.id }
        .distinctUntilChanged()
        .flatMapLatest { runId -> if (runId == null) flowOf(StatistiquesRun()) else observerStatistiques(runId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), null)

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
