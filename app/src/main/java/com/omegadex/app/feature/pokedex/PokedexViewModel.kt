package com.omegadex.app.feature.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
import com.omegadex.app.domain.pokedex.Pokedex
import com.omegadex.app.domain.repository.JeuRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokedexViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    observerPokedex: ObserverPokedexUseCase,
    jeu: JeuRepository,
) : ViewModel() {
    val uiState: StateFlow<PokedexUiState> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) {
            flowOf(PokedexUiState.AucuneRun)
        } else {
            // Run terminée : toutes les zones sont débloquées.
            val etapeId = enCours.etape?.id ?: Int.MAX_VALUE
            combine(observerPokedex(enCours.run.id), jeu.especesAccessibles(etapeId), ::versUiState)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), PokedexUiState.Chargement)

    private fun versUiState(pokedex: Pokedex, accessibles: Set<Int>): PokedexUiState {
        val especes = pokedex.entrees.map { entree ->
            EspeceAffichee(
                id = entree.espece.id,
                numero = if (pokedex.national) entree.espece.id else entree.espece.numeroHoenn ?: entree.espece.id,
                nom = entree.espece.nom,
                types = entree.espece.types,
                statut = entree.statut,
                echangeRequis = entree.echangeRequis,
                aPortee = entree.espece.id in accessibles && entree.statut < StatutEspece.CAPTUREE,
            )
        }
        return PokedexUiState.Pret(
            national = pokedex.national,
            especes = especes,
            vues = especes.count { it.statut >= StatutEspece.VUE },
            capturees = especes.count { it.statut >= StatutEspece.CAPTUREE },
        )
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}
