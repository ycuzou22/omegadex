package com.omegadex.app.domain.run

import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.RunActiveRepository
import com.omegadex.app.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Une run du compte avec sa progression, pour la liste « Mes runs ». */
data class ResumeRun(
    val run: Run,
    val etapeEnCours: Etape?,
    val etapesValidees: Int,
    val totalEtapes: Int,
    val active: Boolean,
)

/** Runs du joueur connecté, de la plus récente à la plus ancienne. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListerRunsUseCase @Inject constructor(
    private val session: SessionRepository,
    private val comptes: CompteRepository,
    private val runActive: RunActiveRepository,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<List<ResumeRun>> = session.utilisateurConnecte.flatMapLatest { utilisateurId ->
        if (utilisateurId == null) {
            flowOf(emptyList())
        } else {
            comptes.runs(utilisateurId).flatMapLatest { runs ->
                if (runs.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val suivis = combine(runs.map { progression.etapes(it.id) }) { it.toList() }
                    combine(suivis, runActive.runActive(utilisateurId), jeu.etapes()) { suivi, active, etapes ->
                        runs.mapIndexed { index, run ->
                            val etapeId = ObserverRunEnCoursUseCase.etapeEnCours(suivi[index])
                            ResumeRun(
                                run = run,
                                etapeEnCours = etapes.firstOrNull { it.id == etapeId },
                                etapesValidees = suivi[index].count { it.dateValidation != null },
                                totalEtapes = etapes.size,
                                active = run.id == active,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Fait de [runId] la run active du joueur connecté ; tout l'affichage qui suit la run active se met à jour. */
/**
 * Supprime une run du compte connecté et toute sa progression. Si c'était la run active, la plus récente des
 * runs restantes le devient ; s'il n'en reste aucune, l'app propose d'en créer une. Une run d'un autre compte
 * n'est jamais touchée.
 */
class SupprimerRunUseCase @Inject constructor(
    private val session: SessionRepository,
    private val comptes: CompteRepository,
) {
    suspend operator fun invoke(runId: Long) {
        val utilisateurId = session.utilisateurConnecte.value ?: return
        comptes.supprimerRun(utilisateurId, runId)
    }
}

class ChoisirRunUseCase @Inject constructor(
    private val session: SessionRepository,
    private val runActive: RunActiveRepository,
) {
    suspend operator fun invoke(runId: Long) {
        val utilisateurId = session.utilisateurConnecte.value ?: return
        runActive.choisir(utilisateurId, runId)
    }
}
