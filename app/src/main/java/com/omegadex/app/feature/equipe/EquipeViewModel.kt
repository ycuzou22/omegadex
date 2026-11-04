package com.omegadex.app.feature.equipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.defi.ObserverDefisUseCase
import com.omegadex.app.domain.equipe.AjouterMembreUseCase
import com.omegadex.app.domain.equipe.ChangerNiveauUseCase
import com.omegadex.app.domain.equipe.EvolutionManuelle
import com.omegadex.app.domain.equipe.EvolutionsManuelles
import com.omegadex.app.domain.equipe.FaireEvoluerUseCase
import com.omegadex.app.domain.equipe.ObserverSuggestionsRotationUseCase
import com.omegadex.app.domain.equipe.ResultatEvolution
import com.omegadex.app.domain.equipe.ResultatNiveau
import com.omegadex.app.domain.equipe.RetirerMembreUseCase
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Écran Équipe : les 6 emplacements de la run active, chaque niveau comparé au plafond de l'étape en cours,
 * et la détection d'évolution à chaque niveau saisi (l'évolution confirmée est jouée en plein écran).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EquipeViewModel @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
    observerDefis: ObserverDefisUseCase,
    private val changerNiveau: ChangerNiveauUseCase,
    private val faireEvoluer: FaireEvoluerUseCase,
    private val composition: CompositionEquipe,
) : ViewModel() {
    private val ecritures = Mutex()

    private val _evolutionProposee = MutableStateFlow<EvolutionProposee?>(null)
    val evolutionProposee: StateFlow<EvolutionProposee?> = _evolutionProposee.asStateFlow()

    private val _evolutionEnCours = MutableStateFlow<EvolutionEnCours?>(null)
    val evolutionEnCours: StateFlow<EvolutionEnCours?> = _evolutionEnCours.asStateFlow()

    val uiState: StateFlow<EquipeUiState> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) return@flatMapLatest flowOf(EquipeUiState.AucuneRun)
        val etape = enCours.etape
        combine(
            progression.equipe(enCours.run.id).flatMapLatest { equipe ->
                evolutionsManuelles(equipe).map { equipe to it }
            },
            jeu.pokedexNational(),
            observerDefis(),
            if (etape == null) flowOf(emptyList()) else composition.suggestions(enCours.run.id, etape),
        ) { (equipe, evolutions), especes, defis, suggestions ->
            val parId = especes.associateBy { it.id }
            val plafond = enCours.etape?.plafond
            val actifs = defis?.actifs.orEmpty()
            EquipeUiState.Equipe(
                difficulte = enCours.run.difficulte,
                etapeTitre = enCours.etape?.titre,
                plafond = plafond,
                emplacements = (1..TAILLE_EQUIPE).map { numero ->
                    val membre = equipe.firstOrNull { it.emplacement == numero }
                    EmplacementVue(
                        numero,
                        membre?.let {
                            val espece = parId[it.especeId]
                            MembreVue(
                                especeId = it.especeId,
                                nom = espece?.nom ?: "#${it.especeId}",
                                surnom = it.surnom,
                                types = espece?.types.orEmpty(),
                                niveau = it.niveau,
                                ecartPlafond = plafond?.let { p -> it.niveau - p },
                                defiActif = actifs.firstOrNull { d -> d.defi.especeId == it.especeId }
                                    ?.modele?.titre,
                                evolutions = evolutions[it.especeId].orEmpty().map { e ->
                                    EvolutionVue(vue(parId, e.evolution.especeCibleId), e.methode, e.detail, e.moment)
                                },
                            )
                        },
                    )
                },
                typeAdverse = etape?.typeDominant,
                suggestions = suggestions.map {
                    SuggestionVue(it.espece.id, it.espece.nom, it.espece.types, it.affinite, it.niveau)
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), EquipeUiState.Chargement)

    /** Monte ou baisse d'un niveau ; une évolution par niveau atteinte est proposée au joueur. */
    fun changerNiveau(emplacement: Int, delta: Int) = surLaRun { runId ->
        val membre = progression.equipe(runId).first().firstOrNull { it.emplacement == emplacement } ?: return@surLaRun
        val resultat = changerNiveau(runId, emplacement, membre.niveau + delta)
        if (delta > 0 && resultat is ResultatNiveau.Enregistre && resultat.evolutions.isNotEmpty()) {
            val especes = jeu.pokedexNational().first().associateBy { it.id }
            _evolutionProposee.value = EvolutionProposee(
                emplacement = emplacement,
                avant = vue(especes, membre.especeId),
                cibles = resultat.evolutions.map {
                    CibleEvolution(vue(especes, it.evolution.especeCibleId), it.niveauRequis, it.precision)
                },
            )
        }
    }

    /** Le joueur confirme l'évolution vers [especeCibleId] : elle est enregistrée puis jouée en plein écran. */
    fun faireEvoluer(especeCibleId: Int) {
        val proposition = _evolutionProposee.value ?: return
        _evolutionProposee.value = null
        surLaRun { runId ->
            if (faireEvoluer(runId, proposition.emplacement, especeCibleId) is ResultatEvolution.Evolue) {
                val apres = proposition.cibles.first { it.espece.id == especeCibleId }.espece
                _evolutionEnCours.value = EvolutionEnCours(proposition.avant, apres)
            }
        }
    }

    /**
     * Évolution manuelle confirmée depuis le ruban de la carte (pierre, bonheur, échange…) : enregistrée,
     * avec un niveau de plus si la méthode le demande, puis jouée en plein écran.
     */
    fun evoluerManuellement(emplacement: Int, evolution: EvolutionVue) = surLaRun { runId ->
        val membre = progression.equipe(runId).first().firstOrNull { it.emplacement == emplacement } ?: return@surLaRun
        val especes = jeu.pokedexNational().first().associateBy { it.id }
        val resultat = faireEvoluer(runId, emplacement, evolution.cible.id, evolution.methode.monteDUnNiveau)
        if (resultat is ResultatEvolution.Evolue) {
            _evolutionEnCours.value = EvolutionEnCours(vue(especes, membre.especeId), evolution.cible)
        }
    }

    /** Évolutions manuelles de chaque espèce de l'équipe, lues dans sa famille d'évolution. */
    private fun evolutionsManuelles(equipe: List<MembreEquipe>): Flow<Map<Int, List<EvolutionManuelle>>> {
        val ids = equipe.map { it.especeId }.distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())
        return combine(ids.map { id -> jeu.famille(id).map { id to EvolutionsManuelles.de(id, it) } }) { it.toMap() }
    }

    /** Fait entrer la [suggestion] au niveau du plafond, dans l'[emplacement] donné ou le premier libre. */
    fun ajouterSuggestion(suggestion: SuggestionVue, emplacement: Int?) = surLaRun { runId ->
        composition.ajouter(runId, suggestion.especeId, suggestion.niveau, emplacement)
    }

    /** Retire le membre de l'[emplacement], qui redevient libre. */
    fun retirer(emplacement: Int) = surLaRun { runId -> composition.retirer(runId, emplacement) }

    fun reporterEvolution() {
        _evolutionProposee.value = null
    }

    fun finirAnimation() {
        _evolutionEnCours.value = null
    }

    private fun vue(especes: Map<Int, Espece>, id: Int) =
        EspeceVue(id, especes[id]?.nom ?: "#$id", especes[id]?.types.orEmpty())

    /** Les écritures passent une à une : deux « + » rapides montent bien de deux niveaux. */
    private fun surLaRun(action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            ecritures.withLock {
                val runId = observerRunEnCours().first()?.run?.id ?: return@launch
                action(runId)
            }
        }
    }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}

/** Ce qui change la composition de l'équipe : ajout, retrait et suggestions de rotation. */
class CompositionEquipe @Inject constructor(
    val ajouter: AjouterMembreUseCase,
    val retirer: RetirerMembreUseCase,
    val suggestions: ObserverSuggestionsRotationUseCase,
)
