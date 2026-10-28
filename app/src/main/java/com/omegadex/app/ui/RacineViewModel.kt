package com.omegadex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.domain.repository.RunActiveRepository
import com.omegadex.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Ce que la racine doit afficher. */
sealed interface EtatRacine {
    /** Session ou run en cours de relecture : simple fond. */
    data object Chargement : EtatRacine
    data object Deconnecte : EtatRacine

    /** Joueur connecté sans aucune run : création obligatoire de la première run. */
    data object SansRun : EtatRacine
    data object Pret : EtatRacine
}

/** Choisit entre les écrans de compte, la création de la première run et l'application. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RacineViewModel @Inject constructor(session: SessionRepository, runActive: RunActiveRepository) : ViewModel() {
    val etat: StateFlow<EtatRacine> = session.etat.flatMapLatest { etat ->
        when (etat) {
            EtatSession.Chargement -> flowOf(EtatRacine.Chargement)
            EtatSession.Deconnecte -> flowOf(EtatRacine.Deconnecte)
            is EtatSession.Connecte -> runActive.runActive(etat.utilisateurId).map { run ->
                if (run == null) EtatRacine.SansRun else EtatRacine.Pret
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EtatRacine.Chargement)
}
