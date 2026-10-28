package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AventureViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    jeu: JeuRepository,
    private val progression: ProgressionRepository,
) : ViewModel() {
    val uiState: StateFlow<AventureUiState> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        if (enCours == null || etape == null) {
            flowOf(AventureUiState.AucuneRun)
        } else {
            combine(jeu.quetes(etape.id), progression.quetesCochees(enCours.run.id)) { quetes, cochees ->
                val affichees = quetes.sortedBy { it.ordre }.map {
                    it.type to QueteAffichee(it.id, it.description, it.recompense, it.id in cochees)
                }
                AventureUiState.EtapeEnCours(
                    runId = enCours.run.id,
                    numero = etape.id,
                    total = enCours.totalEtapes,
                    titre = etape.titre,
                    adversaire = etape.adversaire,
                    typeDominant = etape.typeDominant,
                    plafond = etape.plafond,
                    lieuPrincipal = etape.lieuPrincipal,
                    principales = affichees.filter { it.first == TypeQuete.PRINCIPALE }.map { it.second },
                    annexes = affichees.filter { it.first == TypeQuete.ANNEXE }.map { it.second },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), AventureUiState.Chargement)

    /** Coche ou décoche une quête de l'étape affichée ; l'état est enregistré dans la base progression. */
    fun cocher(queteId: Int, cochee: Boolean) {
        val etape = uiState.value as? AventureUiState.EtapeEnCours ?: return
        viewModelScope.launch { progression.cocherQuete(etape.runId, queteId, cochee) }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
