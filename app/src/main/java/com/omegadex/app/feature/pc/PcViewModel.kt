package com.omegadex.app.feature.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.equipe.DefinirDetailsUseCase
import com.omegadex.app.domain.equipe.DeposerAuPcUseCase
import com.omegadex.app.domain.equipe.NOMBRE_BOITES
import com.omegadex.app.domain.equipe.ResultatPc
import com.omegadex.app.domain.equipe.RetirerDuPcUseCase
import com.omegadex.app.domain.equipe.TAILLE_BOITE
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
import com.omegadex.app.domain.model.PokemonPc
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * PC de stockage de la run active : des boîtes de [TAILLE_BOITE] places, l'équipe en dessous, et la fiche du
 * Pokémon touché (niveau, nature, talent) avec l'action pour le déposer au PC ou le remettre dans l'équipe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PcViewModel @Inject constructor(
    private val observerRunEnCours: ObserverRunEnCoursUseCase,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
    private val deposerAuPc: DeposerAuPcUseCase,
    private val retirerDuPc: RetirerDuPcUseCase,
    private val definirDetails: DefinirDetailsUseCase,
) : ViewModel() {
    private val ecritures = Mutex()
    private val boite = MutableStateFlow(1)
    private val selection = MutableStateFlow<PlacePokemon?>(null)
    private val refus = MutableStateFlow<ResultatPc?>(null)

    val uiState: StateFlow<PcUiState> = observerRunEnCours().flatMapLatest { enCours ->
        if (enCours == null) return@flatMapLatest flowOf(PcUiState.AucuneRun)
        val runId = enCours.run.id
        val contenu = combine(progression.equipe(runId), progression.pc(runId), jeu.pokedexNational()) { e, p, n ->
            Contenu(e, p, n.associateBy { it.id })
        }
        combine(contenu, boite, selection, refus, talentsDeLaSelection(contenu)) { c, b, choix, r, talents ->
            val vues = c.vues()
            val choisi = choix?.let { place -> vues.firstOrNull { it.place == place } }
            PcUiState.Pc(
                boite = b,
                nombreBoites = NOMBRE_BOITES,
                cases = c.pc.filter { it.boite == b }.map { c.vue(it) }.let { dansBoite ->
                    List(TAILLE_BOITE) { dansBoite.getOrNull(it) }
                },
                equipe = (1..TAILLE_EQUIPE).map { n ->
                    c.equipe.firstOrNull { it.emplacement == n }?.let { c.vue(it) }
                },
                fiche = choisi?.let { FichePc(it, talents) },
                refus = r,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), PcUiState.Chargement)

    fun choisirBoite(numero: Int) {
        boite.value = numero.coerceIn(1, NOMBRE_BOITES)
        selection.value = null
        refus.value = null
    }

    fun boiteSuivante() = choisirBoite(boite.value % NOMBRE_BOITES + 1)

    fun boitePrecedente() = choisirBoite((boite.value + NOMBRE_BOITES - 2) % NOMBRE_BOITES + 1)

    fun selectionner(place: PlacePokemon) {
        selection.value = place
        refus.value = null
    }

    /** Dépose le Pokémon choisi dans la boîte affichée, ou le remet dans l'équipe. */
    fun deplacer() {
        val place = selection.value ?: return
        surLaRun { runId ->
            val resultat = when (place) {
                is PlacePokemon.Equipe -> deposerAuPc(runId, place.emplacement, boite.value)
                is PlacePokemon.Pc -> retirerDuPc(runId, place.id)
            }
            if (resultat == ResultatPc.Fait) {
                selection.value = null
                refus.value = null
            } else {
                refus.value = resultat
            }
        }
    }

    fun definirNature(nature: Nature?) = modifierDetails { pokemon -> nature to pokemon.talent }

    fun definirTalent(talent: String?) = modifierDetails { pokemon -> pokemon.nature to talent }

    private fun modifierDetails(nouveau: (PokemonVue) -> Pair<Nature?, String?>) {
        val pokemon = (uiState.value as? PcUiState.Pc)?.fiche?.pokemon ?: return
        val (nature, talent) = nouveau(pokemon)
        surLaRun { runId -> definirDetails(runId, pokemon.place, nature, talent) }
    }

    private fun talentsDeLaSelection(contenu: Flow<Contenu>): Flow<List<String>> = combine(contenu, selection) {
            c,
            choix,
        ->
        choix?.let { place -> c.vues().firstOrNull { it.place == place }?.especeId }
    }.distinctUntilChanged().flatMapLatest { especeId ->
        if (especeId == null) flowOf(emptyList()) else jeu.talents(especeId).map { liste -> liste.map { it.nom } }
    }

    private fun surLaRun(action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            ecritures.withLock {
                val runId = observerRunEnCours().first()?.run?.id ?: return@launch
                action(runId)
            }
        }
    }

    private class Contenu(val equipe: List<MembreEquipe>, val pc: List<PokemonPc>, val especes: Map<Int, Espece>) {
        fun vue(membre: MembreEquipe) = vue(
            PlacePokemon.Equipe(membre.emplacement),
            membre.especeId,
            membre.surnom,
            membre.niveau,
            membre.nature,
            membre.talent,
        )

        fun vue(pokemon: PokemonPc) = vue(
            PlacePokemon.Pc(pokemon.id),
            pokemon.especeId,
            pokemon.surnom,
            pokemon.niveau,
            pokemon.nature,
            pokemon.talent,
        )

        fun vues(): List<PokemonVue> = equipe.map { vue(it) } + pc.map { vue(it) }

        private fun vue(place: PlacePokemon, id: Int, surnom: String?, niveau: Int, nature: Nature?, talent: String?) =
            PokemonVue(
                place = place,
                especeId = id,
                nom = especes[id]?.nom ?: "#$id",
                surnom = surnom,
                types = especes[id]?.types.orEmpty(),
                niveau = niveau,
                nature = nature,
                talent = talent,
            )
    }

    private companion object {
        const val ARRET_MS = 5_000L
        const val TAILLE_EQUIPE = 6
    }
}
