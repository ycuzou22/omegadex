package com.omegadex.app.domain.run

import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.ProgressionEtape
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

/** Run active du joueur connecté et l'étape où il en est. */
data class RunEnCours(val run: Run, val etape: Etape?, val totalEtapes: Int)

/**
 * Suit la run active du joueur connecté : tout l'affichage qui en dépend se met à jour
 * quand le joueur change de run ou de compte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverRunEnCoursUseCase @Inject constructor(
    private val session: SessionRepository,
    private val runActive: RunActiveRepository,
    private val comptes: CompteRepository,
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    operator fun invoke(): Flow<RunEnCours?> = session.utilisateurConnecte.flatMapLatest { utilisateurId ->
        if (utilisateurId == null) {
            flowOf(null)
        } else {
            runActive.runActive(utilisateurId).flatMapLatest { runId ->
                if (runId == null) {
                    flowOf(null)
                } else {
                    combine(comptes.run(utilisateurId, runId), progression.etapes(runId), jeu.etapes()) {
                            run,
                            suivi,
                            etapes,
                        ->
                        run?.let {
                            RunEnCours(it, etapes.firstOrNull { e -> e.id == etapeEnCours(suivi) }, etapes.size)
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** Plus petite étape déverrouillée et non validée ; sinon celle qui suit la dernière validée. */
        fun etapeEnCours(suivi: List<ProgressionEtape>): Int =
            suivi.filter { it.deverrouillee && it.dateValidation == null }.minOfOrNull { it.etapeId }
                ?: suivi.filter { it.dateValidation != null }.maxOfOrNull { it.etapeId + 1 }
                ?: ETAPE_DE_DEPART
    }
}
