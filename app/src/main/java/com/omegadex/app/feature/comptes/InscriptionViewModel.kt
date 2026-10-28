package com.omegadex.app.feature.comptes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.compte.InscrireUseCase
import com.omegadex.app.domain.compte.ResultatInscription
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class InscriptionViewModel @Inject constructor(private val inscrire: InscrireUseCase) : ViewModel() {
    private val etat = MutableStateFlow(InscriptionUiState())
    val uiState: StateFlow<InscriptionUiState> = etat.asStateFlow()

    fun saisirIdentifiant(texte: String) = etat.update { it.copy(identifiant = texte, erreurs = emptySet()) }

    fun saisirMotDePasse(texte: String) = etat.update { it.copy(motDePasse = texte, erreurs = emptySet()) }

    fun saisirConfirmation(texte: String) = etat.update { it.copy(confirmation = texte, erreurs = emptySet()) }

    fun creerCompte() {
        val courant = etat.value
        if (courant.enCours) return
        etat.update { it.copy(enCours = true) }
        viewModelScope.launch {
            val motDePasse = courant.motDePasse.toCharArray()
            val confirmation = courant.confirmation.toCharArray()
            val resultat = inscrire(courant.identifiant, motDePasse, confirmation)
            motDePasse.fill(' ')
            confirmation.fill(' ')
            etat.update {
                when (resultat) {
                    is ResultatInscription.Reussie -> it.copy(enCours = false, motDePasse = "", confirmation = "")
                    is ResultatInscription.Refusee -> it.copy(enCours = false, erreurs = resultat.erreurs)
                }
            }
        }
    }
}
