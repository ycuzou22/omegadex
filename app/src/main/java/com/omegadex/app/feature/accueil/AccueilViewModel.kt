package com.omegadex.app.feature.accueil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.InfosAppRepository
import com.omegadex.app.domain.repository.SessionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccueilViewModel @Inject constructor(
    infosAppRepository: InfosAppRepository,
    comptes: CompteRepository,
    private val session: SessionRepository,
    observerRunEnCours: ObserverRunEnCoursUseCase,
) : ViewModel() {
    private val version = infosAppRepository.version()

    val uiState: StateFlow<AccueilUiState> = combine(
        session.utilisateurConnecte,
        comptes.utilisateurs(),
        observerRunEnCours(),
    ) { id, liste, enCours ->
        AccueilUiState(
            version = version,
            joueur = liste.firstOrNull { it.id == id }?.identifiant.orEmpty(),
            run = enCours?.let {
                RunAccueil(
                    nom = it.run.nom,
                    difficulte = it.run.difficulte,
                    etapeNumero = it.etape?.id ?: 0,
                    etapeTitre = it.etape?.titre.orEmpty(),
                    totalEtapes = it.totalEtapes,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), AccueilUiState(version = version))

    /** Ferme la session : l'app revient à l'écran de connexion et à la liste des comptes. */
    fun changerDeJoueur() {
        viewModelScope.launch { session.fermer() }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
