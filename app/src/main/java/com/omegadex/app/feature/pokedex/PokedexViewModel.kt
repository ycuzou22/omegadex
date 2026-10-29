package com.omegadex.app.feature.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
import com.omegadex.app.domain.pokedex.Pokedex
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokedexViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    observerPokedex: ObserverPokedexUseCase,
    jeu: JeuRepository,
    private val progression: ProgressionRepository,
) : ViewModel() {
    private val evenementsInternes = Channel<StatutModifie>(Channel.BUFFERED)

    /** Changements de statut, consommés une seule fois par l'écran (déclencheur de l'animation T14.2). */
    val evenements: Flow<StatutModifie> = evenementsInternes.receiveAsFlow()

    /** Les touches rapides sont traitées l'une après l'autre, chacune sur le statut déjà enregistré. */
    private val ecriture = Mutex()

    val uiState: StateFlow<PokedexUiState> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) {
            flowOf(PokedexUiState.AucuneRun)
        } else {
            // Run terminée : toutes les zones sont débloquées.
            val etapeId = enCours.etape?.id ?: Int.MAX_VALUE
            combine(observerPokedex(enCours.run.id), jeu.especesAccessibles(etapeId)) { pokedex, accessibles ->
                versUiState(enCours.run.id, pokedex, accessibles)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), PokedexUiState.Chargement)

    /** Une touche : le statut de l'espèce passe au cran suivant (Vue → Capturée → … → Maîtrisée). */
    fun avancerStatut(especeId: Int) = changerStatut(especeId, StatutEspece::suivant)

    /** Appui long : le statut revient d'un cran, pour corriger une touche de trop. */
    fun reculerStatut(especeId: Int) = changerStatut(especeId, StatutEspece::precedent)

    private fun changerStatut(especeId: Int, transition: (StatutEspece) -> StatutEspece) {
        val etat = uiState.value as? PokedexUiState.Pret ?: return
        val espece = etat.especes.firstOrNull { it.id == especeId } ?: return
        viewModelScope.launch {
            ecriture.withLock {
                val avant = progression.statuts(etat.runId).first()[especeId] ?: StatutEspece.NON_VUE
                val apres = transition(avant)
                if (apres != avant) {
                    progression.definirStatut(etat.runId, especeId, apres)
                    evenementsInternes.send(StatutModifie(especeId, espece.nom, avant, apres))
                }
            }
        }
    }

    private fun versUiState(runId: Long, pokedex: Pokedex, accessibles: Set<Int>): PokedexUiState {
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
            runId = runId,
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

/** Statut suivant dans l'ordre d'entraînement ; Maîtrisée reste Maîtrisée. */
fun StatutEspece.suivant(): StatutEspece = StatutEspece.entries.getOrElse(ordinal + 1) { this }

/** Statut précédent ; Non vue reste Non vue. */
fun StatutEspece.precedent(): StatutEspece = StatutEspece.entries.getOrElse(ordinal - 1) { this }
