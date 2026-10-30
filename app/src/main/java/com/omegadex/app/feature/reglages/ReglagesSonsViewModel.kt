package com.omegadex.app.feature.reglages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.repository.Reglages
import com.omegadex.app.domain.repository.ReglagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Réglages sons, vibrations et ambiance de la carte de Mes runs. */
@HiltViewModel
class ReglagesSonsViewModel @Inject constructor(private val repository: ReglagesRepository) : ViewModel() {
    val reglages: StateFlow<Reglages> =
        repository.reglages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), Reglages())

    fun activerSons(actifs: Boolean) = viewModelScope.launch { repository.activerSons(actifs) }

    fun activerVibrations(actives: Boolean) = viewModelScope.launch { repository.activerVibrations(actives) }

    fun activerAmbiance(active: Boolean) = viewModelScope.launch { repository.activerAmbiance(active) }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
