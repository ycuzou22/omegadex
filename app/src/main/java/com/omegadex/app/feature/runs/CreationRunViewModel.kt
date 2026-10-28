package com.omegadex.app.feature.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.run.CreerRunUseCase
import com.omegadex.app.domain.run.RegleOptionnelle
import com.omegadex.app.domain.run.ResultatCreationRun
import com.omegadex.app.domain.run.Starter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CreationRunViewModel @Inject constructor(private val creerRun: CreerRunUseCase) : ViewModel() {
    private val etat = MutableStateFlow(CreationRunUiState())
    val uiState: StateFlow<CreationRunUiState> = etat.asStateFlow()

    fun saisirNom(texte: String) = etat.update { it.copy(nom = texte, erreurs = emptySet()) }

    fun choisirStarter(starter: Starter) = etat.update { it.copy(starter = starter, erreurs = emptySet()) }

    fun choisirDifficulte(difficulte: Difficulte) = etat.update { it.copy(difficulte = difficulte) }

    fun basculerRegle(regle: RegleOptionnelle, active: Boolean) = etat.update {
        it.copy(regles = if (active) it.regles + regle else it.regles - regle)
    }

    /** [nomParDefaut] est utilisé si le joueur n'a pas modifié le champ. */
    fun creer(nomParDefaut: String) {
        val courant = etat.value
        if (courant.enCours || courant.creee) return
        etat.update { it.copy(enCours = true) }
        viewModelScope.launch {
            val resultat = creerRun(courant.nom ?: nomParDefaut, courant.starter, courant.difficulte, courant.regles)
            etat.update {
                when (resultat) {
                    is ResultatCreationRun.Creee -> it.copy(enCours = false, creee = true)
                    is ResultatCreationRun.Refusee -> it.copy(enCours = false, erreurs = resultat.erreurs)
                }
            }
        }
    }
}
