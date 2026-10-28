package com.omegadex.app.feature.accueil

import androidx.lifecycle.ViewModel
import com.omegadex.app.domain.repository.InfosAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AccueilViewModel @Inject constructor(infosAppRepository: InfosAppRepository) : ViewModel() {
    private val etat = MutableStateFlow(AccueilUiState(version = infosAppRepository.version()))
    val uiState: StateFlow<AccueilUiState> = etat.asStateFlow()
}
