package com.omegadex.app.feature.equipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.defi.AccepterDefiUseCase
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.defi.ProposerDefisUseCase
import com.omegadex.app.domain.equipe.AjouterMembreUseCase
import com.omegadex.app.domain.equipe.ResultatAjout
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ajout d'un membre depuis le Pokédex : seules les espèces capturées (hors équipe) sont proposées.
 * Avec un emplacement occupé ([ARGUMENT_EMPLACEMENT]), l'espèce choisie remplace le membre (échange).
 * Après l'ajout, un défi adapté au nouveau membre est proposé.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AjoutMembreViewModel @Inject constructor(
    etatNavigation: SavedStateHandle,
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    progression: ProgressionRepository,
    jeu: JeuRepository,
    private val ajouterMembre: AjouterMembreUseCase,
    private val proposerDefis: ProposerDefisUseCase,
    private val accepterDefi: AccepterDefiUseCase,
) : ViewModel() {
    private val emplacement: Int? =
        etatNavigation.get<Int>(ARGUMENT_EMPLACEMENT)?.takeIf { it in 1..TAILLE_EQUIPE }
    private val recherche = MutableStateFlow("")
    private val etapes = MutableStateFlow(EtapesAjout())

    val uiState: StateFlow<AjoutMembreUiState> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) return@flatMapLatest flowOf(AjoutMembreUiState.AucuneRun)
        val runId = enCours.run.id
        combine(
            progression.statuts(runId),
            progression.equipe(runId),
            jeu.pokedexNational(),
            recherche,
            etapes,
        ) { statuts, equipe, especes, texte, etape ->
            val dansEquipe = equipe.map { it.especeId }.toSet()
            val remplace = emplacement?.let { e -> equipe.firstOrNull { it.emplacement == e } }
            AjoutMembreUiState.Choix(
                // Une fois l'ajout lancé, le titre ne bascule pas sur « Échanger <le nouveau membre> ».
                remplace = if (etape.fige) {
                    etape.remplace
                } else {
                    remplace?.let { m -> especes.firstOrNull { it.id == m.especeId }?.nom }
                },
                emplacement = emplacement,
                equipePleine = emplacement == null && equipe.size >= TAILLE_EQUIPE,
                recherche = texte,
                especes = especes
                    .filter { (statuts[it.id] ?: StatutEspece.NON_VUE) >= StatutEspece.CAPTUREE }
                    .filter { it.id !in dansEquipe && correspond(it, texte.trim()) }
                    .map { EspeceAjoutable(it.id, it.numeroHoenn, it.nom, it.types, statuts.getValue(it.id)) },
                niveauParDefaut = minOf(enCours.etape?.plafond ?: NIVEAU_PAR_DEFAUT, NIVEAU_PAR_DEFAUT),
                choisie = etape.choisie,
                defiPropose = etape.defi?.let { DefiProposeVue(it.modele.titre, it.enonce) },
                termine = etape.termine,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), AjoutMembreUiState.Chargement)

    private fun correspond(espece: Espece, texte: String) = texte.isEmpty() ||
        espece.nom.contains(texte, ignoreCase = true) ||
        espece.numeroHoenn?.toString() == texte ||
        espece.id.toString() == texte

    fun rechercher(texte: String) {
        recherche.value = texte
    }

    fun choisir(espece: EspeceAjoutable?) = etapes.update { it.copy(choisie = espece) }

    /** Ajoute l'espèce choisie au [niveau] donné, puis propose un défi adapté à ce nouveau membre. */
    fun ajouter(niveau: Int) {
        val espece = etapes.value.choisie ?: return
        val titre = (uiState.value as? AjoutMembreUiState.Choix)?.remplace
        etapes.update { it.copy(fige = true, remplace = titre) }
        surLaRun { runId ->
            val resultat = ajouterMembre(runId, espece.id, niveau, emplacement)
            if (resultat !is ResultatAjout.Ajoute) {
                etapes.update { it.copy(fige = false, choisie = null) }
                return@surLaRun
            }
            val defi = proposerDefis().first().firstOrNull { it.especeId == espece.id }
            etapes.update { it.copy(choisie = null, defi = defi, termine = defi == null) }
        }
    }

    fun releverDefi() {
        val defi = etapes.value.defi ?: return
        surLaRun { runId ->
            accepterDefi(runId, defi)
            etapes.update { it.copy(defi = null, termine = true) }
        }
    }

    fun ignorerDefi() = etapes.update { it.copy(defi = null, termine = true) }

    private fun surLaRun(action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            val runId = observerRunEnCours().first()?.run?.id ?: return@launch
            action(runId)
        }
    }

    private data class EtapesAjout(
        val choisie: EspeceAjoutable? = null,
        val defi: DefiPropose? = null,
        val termine: Boolean = false,
        /** Titre figé au lancement de l'ajout. */
        val fige: Boolean = false,
        val remplace: String? = null,
    )

    companion object {
        const val ARGUMENT_EMPLACEMENT = "emplacement"
        private const val NIVEAU_PAR_DEFAUT = 5
        private const val ARRET_MS = 5_000L
    }
}
