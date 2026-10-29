package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.aventure.CocherQueteUseCase
import com.omegadex.app.domain.aventure.ResultatCochage
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AventureViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    private val jeu: JeuRepository,
    progression: ProgressionRepository,
    private val cocherQuete: CocherQueteUseCase,
) : ViewModel() {
    private val evenementsInternes = Channel<EtapeValidee>(Channel.BUFFERED)

    /** Étapes validées à célébrer, consommées une seule fois par l'écran. */
    val evenements: Flow<EtapeValidee> = evenementsInternes.receiveAsFlow()

    val uiState: StateFlow<AventureUiState> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        when {
            enCours == null -> flowOf(AventureUiState.AucuneRun)
            etape == null -> flowOf(AventureUiState.Terminee)
            else -> combine(
                jeu.quetes(etape.id),
                progression.quetesCochees(enCours.run.id),
                jeu.lieuxDebloques(etape.id),
            ) { quetes, cochees, lieux ->
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
                    lieux = lieux.map { LieuAffiche(it.id, it.nom) },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), AventureUiState.Chargement)

    /**
     * Coche ou décoche une quête de l'étape affichée. Quand la dernière quête principale est cochée,
     * l'étape est validée, la suivante s'affiche et un évènement [EtapeValidee] est émis.
     */
    fun cocher(queteId: Int, cochee: Boolean) {
        val etape = uiState.value as? AventureUiState.EtapeEnCours ?: return
        viewModelScope.launch {
            val resultat = cocherQuete(etape.runId, etape.numero, queteId, cochee)
            if (resultat is ResultatCochage.EtapeValidee) {
                val suivante = resultat.etapeSuivanteId?.let { jeu.etape(it).first()?.titre }
                val badge = jeu.quetes(etape.numero).first()
                    .filter { it.type == TypeQuete.PRINCIPALE }
                    .flatMap { it.recompense.orEmpty().split(",") }
                    .map { it.trim() }
                    .firstOrNull { it.startsWith(PREFIXE_BADGE) }
                evenementsInternes.send(EtapeValidee(etape.numero, etape.titre, badge, suivante))
            }
        }
    }

    private companion object {
        const val ARRET_MS = 5_000L
        const val PREFIXE_BADGE = "Badge"
    }
}
