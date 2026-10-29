package com.omegadex.app.feature.aventure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.domain.aventure.CocherQueteUseCase
import com.omegadex.app.domain.aventure.ResultatCochage
import com.omegadex.app.domain.badge.idsValidees
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.domain.pokedex.CapturerEspeceUseCase
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
import com.omegadex.app.domain.pokedex.ResultatCapture
import com.omegadex.app.domain.pokedex.possibleDansRubisOmega
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.ui.animations.CaptureAnimee
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
class AventureViewModel @Inject constructor(
    observerRunEnCours: ObserverRunEnCoursUseCase,
    private val jeu: JeuRepository,
    private val progression: ProgressionRepository,
    private val cocherQuete: CocherQueteUseCase,
    private val observerPokedex: ObserverPokedexUseCase,
    private val capturer: CapturerEspeceUseCase,
) : ViewModel() {
    private val evenementsInternes = Channel<EtapeValidee>(Channel.BUFFERED)
    private val capturesInternes = Channel<CaptureAnimee>(Channel.BUFFERED)

    /** Les touches rapides sur « Attraper » sont traitées l'une après l'autre. */
    private val ecriture = Mutex()

    /** Étapes validées à célébrer, consommées une seule fois par l'écran. */
    val evenements: Flow<EtapeValidee> = evenementsInternes.receiveAsFlow()

    /** Captures à animer (« Capture → Pokédex »), consommées une seule fois par l'écran. */
    val captures: Flow<CaptureAnimee> = capturesInternes.receiveAsFlow()

    val uiState: StateFlow<AventureUiState> = observerRunEnCours().flatMapLatest { enCours ->
        val etape = enCours?.etape
        when {
            enCours == null -> flowOf(AventureUiState.AucuneRun)
            etape == null -> flowOf(AventureUiState.Terminee)
            else -> combine(
                jeu.quetes(etape.id),
                progression.quetesCochees(enCours.run.id),
                jeu.lieuxDebloques(etape.id),
                rencontres(enCours.run.id, etape.id),
            ) { quetes, cochees, lieux, rencontres ->
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
                    rencontres = rencontres,
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
                val validees = progression.etapes(etape.runId).first().idsValidees()
                evenementsInternes.send(EtapeValidee(etape.numero, etape.titre, badge, suivante, validees))
            }
        }
    }

    /**
     * Le joueur a attrapé l'espèce en jeu : elle passe au statut « capturée » et l'animation
     * « Capture → Pokédex » est demandée. Une espèce déjà capturée ne change pas.
     */
    fun attraper(especeId: Int) {
        val etape = uiState.value as? AventureUiState.EtapeEnCours ?: return
        viewModelScope.launch {
            ecriture.withLock {
                // Le Pokédex est lu avant l'écriture : l'animation part de la case encore vide.
                val pokedex = observerPokedex(etape.runId).first()
                if (capturer(etape.runId, especeId) is ResultatCapture.Capturee) {
                    captureAnimee(pokedex, especeId)?.let { capturesInternes.send(it) }
                }
            }
        }
    }

    /** Espèces des lieux débloqués par l'étape, une ligne par espèce, avec leur état de capture dans la run. */
    private fun rencontres(runId: Long, etapeId: Int): Flow<List<RencontreAffichee>> =
        jeu.lieuxDebloques(etapeId).flatMapLatest { lieux ->
            if (lieux.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(lieux.map { jeu.especesDuLieu(it.id) }) { parLieu -> parLieu.toList().flatten() }
            }
        }.combine(jeu.pokedexNational()) { liste, especes ->
            val parId = especes.associateBy { it.id }
            liste.filter { it.possibleDansRubisOmega() }.distinctBy { it.especeId }.map {
                val espece = parId[it.especeId]
                RencontreAffichee(
                    especeId = it.especeId,
                    nom = espece?.nom ?: "#${it.especeId}",
                    type = espece?.types?.firstOrNull(),
                    lieu = it.lieu.nom,
                    niveauMin = it.niveauMin,
                    niveauMax = it.niveauMax,
                    taux = it.taux,
                    capturee = false,
                )
            }
        }.combine(progression.statuts(runId)) { lignes, statuts ->
            lignes.map { it.copy(capturee = (statuts[it.especeId] ?: StatutEspece.NON_VUE) >= StatutEspece.CAPTUREE) }
        }

    private companion object {
        const val ARRET_MS = 5_000L
        const val PREFIXE_BADGE = "Badge"
    }
}
