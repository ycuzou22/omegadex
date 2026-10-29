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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    /** Recherche et filtres en cours ; gardés tant que le ViewModel vit (rotation, changement d'onglet). */
    private val filtresInternes = MutableStateFlow(FiltresPokedex())

    /** Pokédex complet de la run active, sans filtre. */
    private val pokedexComplet = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) {
            flowOf(PokedexUiState.AucuneRun)
        } else {
            // Run terminée : toutes les zones sont débloquées.
            val etapeId = enCours.etape?.id ?: Int.MAX_VALUE
            combine(observerPokedex(enCours.run.id), jeu.especesAccessibles(etapeId)) { pokedex, accessibles ->
                versUiState(enCours.run.id, pokedex, accessibles)
            }
        }
    }.map { etat ->
        // Les types disponibles ne changent qu'avec la liste : calculés ici, pas à chaque frappe.
        if (etat is PokedexUiState.Pret) etat.copy(typesDisponibles = typesPresents(etat.especes)) else etat
    }

    /** La recherche et les filtres sont appliqués en mémoire : chaque frappe met la grille à jour aussitôt. */
    val uiState: StateFlow<PokedexUiState> = combine(pokedexComplet, filtresInternes) { etat, filtres ->
        if (etat is PokedexUiState.Pret) {
            etat.copy(filtres = filtres, resultats = filtrer(etat.especes, filtres))
        } else {
            etat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), PokedexUiState.Chargement)

    fun rechercher(texte: String) = filtresInternes.update { it.copy(recherche = texte) }

    /** Ajoute ou retire un statut du filtre ; plusieurs statuts peuvent être retenus ensemble. */
    fun basculerStatut(statut: StatutEspece) = filtresInternes.update {
        it.copy(statuts = if (statut in it.statuts) it.statuts - statut else it.statuts + statut)
    }

    /** Retient un seul type ; choisir à nouveau le type retenu retire le filtre. */
    fun choisirType(type: String?) = filtresInternes.update { actuels ->
        actuels.copy(type = if (type == actuels.type) null else type)
    }

    fun effacerFiltres() = filtresInternes.update { FiltresPokedex() }

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
