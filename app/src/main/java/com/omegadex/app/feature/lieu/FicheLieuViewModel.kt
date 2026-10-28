package com.omegadex.app.feature.lieu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.repository.JeuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Fiche d'un lieu : rencontres avec méthode et niveaux, objets importants obtenus sur place. */
@HiltViewModel
class FicheLieuViewModel @Inject constructor(etatNavigation: SavedStateHandle, jeu: JeuRepository) : ViewModel() {
    private val lieuId: Int = checkNotNull(etatNavigation[ARGUMENT_LIEU])

    val uiState: StateFlow<FicheLieuUiState> = combine(
        jeu.lieu(lieuId),
        jeu.especesDuLieu(lieuId),
        jeu.pokedexNational(),
        jeu.quetesDuLieu(lieuId),
    ) { lieu, rencontres, especes, quetes ->
        if (lieu == null) return@combine FicheLieuUiState.Introuvable
        val parId = especes.associateBy { it.id }
        FicheLieuUiState.Fiche(
            nom = lieu.nom,
            type = lieu.type,
            rencontres = rencontres.groupBy { it.methode }.toSortedMap().map { (methode, lignes) ->
                GroupeRencontres(
                    methode,
                    lignes.map {
                        val espece = parId[it.especeId]
                        LigneRencontre(
                            it.especeId,
                            espece?.nom ?: "#${it.especeId}",
                            espece?.types?.firstOrNull(),
                            it.niveauMin,
                            it.niveauMax,
                            it.taux,
                            it.condition,
                        )
                    },
                )
            },
            objets = quetes.mapNotNull { it.recompense }.flatMap { it.split(",") }.map { it.trim() }.distinct(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), FicheLieuUiState.Chargement)

    companion object {
        const val ARGUMENT_LIEU = "lieuId"
        private const val ARRET_MS = 5_000L
    }
}
