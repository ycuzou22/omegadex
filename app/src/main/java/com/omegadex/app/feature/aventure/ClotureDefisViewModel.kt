package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.defi.DefisDeLaRun
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.defi.ValiderDefiUseCase
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Défis de la run une fois chargés ([run] vaut null s'il n'y a pas de run en cours). */
data class DefisCharges(val run: DefisDeLaRun?)

/**
 * Après la validation d'une étape, le combat a eu lieu : le joueur dit si ses défis de combat contre cet
 * adversaire sont réussis ou ratés. [defis] vaut null tant que les défis chargent.
 */
@HiltViewModel
class ClotureDefisViewModel @Inject constructor(
    observerDefis: ObserverDefisUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val valider: ValiderDefiUseCase,
) : ViewModel() {
    val defis: StateFlow<DefisCharges?> = observerDefis()
        .map { DefisCharges(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun clore(defiId: Long, reussi: Boolean) {
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            valider(runId, defiId, reussi)
        }
    }
}
