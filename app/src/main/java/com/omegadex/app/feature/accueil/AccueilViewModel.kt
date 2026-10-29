package com.omegadex.app.feature.accueil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.accueil.DefiDuJourUseCase
import com.omegadex.app.domain.accueil.ObserverJaugesUseCase
import com.omegadex.app.domain.badge.ObserverBadgesUseCase
import com.omegadex.app.domain.defi.AccepterDefiUseCase
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.InfosAppRepository
import com.omegadex.app.domain.repository.SessionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccueilViewModel @Inject constructor(
    infosAppRepository: InfosAppRepository,
    comptes: CompteRepository,
    private val session: SessionRepository,
    observerJauges: ObserverJaugesUseCase,
    defiDuJour: DefiDuJourUseCase,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val accepterDefi: AccepterDefiUseCase,
    observerBadges: ObserverBadgesUseCase,
) : ViewModel() {
    private var defiPropose: DefiPropose? = null

    private val version = infosAppRepository.version()

    val uiState: StateFlow<AccueilUiState> = combine(
        session.utilisateurConnecte,
        comptes.utilisateurs(),
        observerRunEnCours(),
        observerJauges(),
        defiDuJour(),
    ) { id, liste, enCours, jauges, defi ->
        defiPropose = defi
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
            jauges = jauges,
            defiDuJour = defi?.let { DefiDuJour(it.modele.type, it.modele.titre, it.enonce) },
        )
    }.combine(badgesDeLaRun(observerBadges)) { etat, badges -> etat.copy(badges = badges) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), AccueilUiState(version = version))

    private fun badgesDeLaRun(observerBadges: ObserverBadgesUseCase) = observerRunEnCours().flatMapLatest { enCours ->
        enCours?.let { observerBadges(it.run.id) } ?: flowOf(null)
    }

    /** Relève le défi du jour : il devient actif dans l'espace Défis et un autre défi du jour est proposé. */
    fun releverDefiDuJour() {
        val defi = defiPropose ?: return
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            accepterDefi(runId, defi)
        }
    }

    /** Ferme la session : l'app revient à l'écran de connexion et à la liste des comptes. */
    fun changerDeJoueur() {
        viewModelScope.launch { session.fermer() }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
