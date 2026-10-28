package com.omegadex.app.ui

import androidx.lifecycle.ViewModel
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** État de la session, pour choisir entre les écrans de compte et l'application. */
@HiltViewModel
class RacineViewModel @Inject constructor(session: SessionRepository) : ViewModel() {
    val session: StateFlow<EtatSession> = session.etat
}
