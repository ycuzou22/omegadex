package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.dresseur.DresseursEtape
import com.omegadex.app.domain.dresseur.ObserverDresseursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Onglet Dresseurs de l'Aventure : les dresseurs de l'étape en cours et leur stratégie. */
@HiltViewModel
class DresseursViewModel @Inject constructor(observerDresseurs: ObserverDresseursUseCase) : ViewModel() {
    val dresseurs: StateFlow<DresseursEtape?> = observerDresseurs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), null)

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
