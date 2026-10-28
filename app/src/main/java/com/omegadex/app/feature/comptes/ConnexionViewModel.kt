package com.omegadex.app.feature.comptes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.compte.ConnecterUseCase
import com.omegadex.app.domain.compte.ResultatConnexion
import com.omegadex.app.domain.repository.CompteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConnexionViewModel @Inject constructor(comptes: CompteRepository, private val connecter: ConnecterUseCase) :
    ViewModel() {
    private val etat = MutableStateFlow(ConnexionUiState())
    val uiState: StateFlow<ConnexionUiState> = etat.asStateFlow()

    init {
        viewModelScope.launch {
            comptes.utilisateurs().collect { liste -> etat.update { it.copy(comptes = liste) } }
        }
    }

    fun choisirCompte(identifiant: String) = etat.update {
        it.copy(identifiant = identifiant, motDePasse = "", erreur = null)
    }

    fun saisirIdentifiant(texte: String) = etat.update { it.copy(identifiant = texte, erreur = null) }

    fun saisirMotDePasse(texte: String) = etat.update { it.copy(motDePasse = texte, erreur = null) }

    fun seConnecter() {
        val courant = etat.value
        if (courant.enCours) return
        if (courant.identifiant.isBlank() || courant.motDePasse.isEmpty()) {
            etat.update { it.copy(erreur = ErreurConnexion.ChampsVides) }
            return
        }
        etat.update { it.copy(enCours = true, erreur = null) }
        viewModelScope.launch {
            val motDePasse = courant.motDePasse.toCharArray()
            val resultat = connecter(courant.identifiant, motDePasse)
            motDePasse.fill(' ')
            etat.update {
                when (resultat) {
                    is ResultatConnexion.Reussie -> it.copy(enCours = false, motDePasse = "")
                    ResultatConnexion.IdentifiantsIncorrects ->
                        it.copy(enCours = false, motDePasse = "", erreur = ErreurConnexion.IdentifiantsIncorrects)
                    is ResultatConnexion.TropDeTentatives ->
                        it.copy(
                            enCours = false,
                            motDePasse = "",
                            erreur = ErreurConnexion.Attente(resultat.secondesRestantes),
                        )
                }
            }
        }
    }
}
