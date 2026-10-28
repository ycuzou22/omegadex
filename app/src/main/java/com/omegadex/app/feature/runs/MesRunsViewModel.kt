package com.omegadex.app.feature.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.run.ChoisirRunUseCase
import com.omegadex.app.domain.run.ListerRunsUseCase
import com.omegadex.app.domain.run.ResumeRun
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MesRunsViewModel @Inject constructor(listerRuns: ListerRunsUseCase, private val choisirRun: ChoisirRunUseCase) :
    ViewModel() {
    val runs: StateFlow<List<ResumeRun>> =
        listerRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), emptyList())

    /** Rend la run active puis appelle [ensuite] (retour à l'accueil, déjà à jour). */
    fun choisir(runId: Long, ensuite: () -> Unit) {
        viewModelScope.launch {
            choisirRun(runId)
            ensuite()
        }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
