package com.omegadex.app.domain.succes

import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.SuccesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** Mesures d'une run, recalculées à chaque capture, étape, défi ou changement d'équipe. */
class ObserverMesuresSuccesUseCase @Inject constructor(private val progression: ProgressionRepository) {
    operator fun invoke(runId: Long): Flow<MesuresSucces> = combine(
        progression.statuts(runId),
        progression.etapes(runId),
        progression.defis(runId),
        progression.equipe(runId),
    ) { statuts, etapes, defis, equipe -> MesuresSucces.de(statuts, etapes, defis, equipe) }
        .distinctUntilChanged()
}

/**
 * Détection après chaque action (T18.1) : à chaque changement des mesures, les succès atteints et pas encore
 * enregistrés sont débloqués. Le flux émet seulement les succès qui viennent d'être débloqués, une seule fois
 * chacun : l'enregistrement en base ignore un succès déjà présent.
 */
class SurveillerSuccesUseCase @Inject constructor(
    private val mesures: ObserverMesuresSuccesUseCase,
    private val succes: SuccesRepository,
) {
    operator fun invoke(runId: Long): Flow<List<Succes>> = mesures(runId)
        // map (et non mapLatest) : un enregistrement en cours n'est jamais annulé, chaque déblocage est émis.
        .map { succes.debloquer(runId, it.atteints()) }
        .filter { it.isNotEmpty() }
}

/** Vitrine : chaque succès, débloqué ou non, avec l'avancement de sa mesure. */
class ObserverSuccesUseCase @Inject constructor(
    private val mesures: ObserverMesuresSuccesUseCase,
    private val succes: SuccesRepository,
) {
    operator fun invoke(runId: Long): Flow<List<EtatSucces>> =
        combine(mesures(runId), succes.succes(runId)) { m, debloques ->
            Succes.entries.map { succes ->
                EtatSucces(succes, debloques[succes], m.valeur(succes).coerceAtMost(succes.seuil))
            }
        }.map { liste -> liste.sortedBy { !it.debloque } }
}
