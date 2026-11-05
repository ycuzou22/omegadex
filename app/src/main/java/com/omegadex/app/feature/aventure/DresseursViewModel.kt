package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.dresseur.DresseursEtape
import com.omegadex.app.domain.dresseur.ObserverDresseursUseCase
import com.omegadex.app.domain.repository.DresseursBattusRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Onglet Dresseurs de l'Aventure : les dresseurs de l'étape en cours, leur stratégie et ceux déjà battus. */
@HiltViewModel
class DresseursViewModel @Inject constructor(
    observerDresseurs: ObserverDresseursUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val battus: DresseursBattusRepository,
) : ViewModel() {
    val dresseurs: StateFlow<DresseursEtape?> = observerDresseurs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), null)

    /** Marque (ou démarque) battu un dresseur sans quête associée. */
    fun marquerBattu(cle: String, battu: Boolean) {
        viewModelScope.launch {
            val run = observerRunEnCours().first()?.run ?: return@launch
            battus.marquer(run.id, cle, battu)
        }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
