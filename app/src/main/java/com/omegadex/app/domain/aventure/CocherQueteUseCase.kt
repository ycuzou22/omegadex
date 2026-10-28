package com.omegadex.app.domain.aventure

import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Ce qu'a provoqué le cochage d'une quête. */
sealed interface ResultatCochage {
    data object Enregistre : ResultatCochage

    /**
     * Toutes les quêtes principales de [etapeId] sont faites : l'étape est validée et
     * [etapeSuivanteId] déverrouillée (null après la dernière étape). Déclenche l'animation « Étape validée ».
     */
    data class EtapeValidee(val etapeId: Int, val etapeSuivanteId: Int?) : ResultatCochage
}

/**
 * Coche ou décoche une quête de l'étape en cours, puis applique la règle de déblocage :
 * l'étape n+1 se déverrouille dès que toutes les quêtes principales de l'étape n sont cochées.
 * Les quêtes annexes ne bloquent jamais.
 */
class CocherQueteUseCase @Inject constructor(
    private val jeu: JeuRepository,
    private val progression: ProgressionRepository,
) {
    suspend operator fun invoke(runId: Long, etapeId: Int, queteId: Int, cochee: Boolean): ResultatCochage {
        progression.cocherQuete(runId, queteId, cochee)
        if (!cochee) return ResultatCochage.Enregistre
        val quetes = jeu.quetes(etapeId).first()
        val cochees = progression.quetesCochees(runId).first()
        val dejaValidee = progression.etapes(runId).first().any { it.etapeId == etapeId && it.dateValidation != null }
        if (dejaValidee || !ReglesDeblocage.principalesTerminees(quetes, cochees)) return ResultatCochage.Enregistre

        progression.validerEtape(runId, etapeId)
        val suivante = jeu.etapes().first().map { it.id }.filter { it > etapeId }.minOrNull()
        suivante?.let { progression.deverrouillerEtape(runId, it) }
        return ResultatCochage.EtapeValidee(etapeId, suivante)
    }
}

/** Règle de déblocage des étapes, isolée pour être testée seule. */
object ReglesDeblocage {
    /** Vrai si l'étape a au moins une quête principale et qu'elles sont toutes cochées ; les annexes sont ignorées. */
    fun principalesTerminees(quetes: List<Quete>, cochees: Set<Int>): Boolean {
        val principales = quetes.filter { it.type == TypeQuete.PRINCIPALE }
        return principales.isNotEmpty() && principales.all { it.id in cochees }
    }
}
