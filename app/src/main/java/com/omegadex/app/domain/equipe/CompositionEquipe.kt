package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Résultat de l'ajout (ou de l'échange) d'un membre de l'équipe. */
sealed interface ResultatAjout {
    data class Ajoute(val emplacement: Int) : ResultatAjout

    /** Seule une espèce capturée (ou plus avancée) peut entrer dans l'équipe. */
    data object NonCapturee : ResultatAjout

    data object EquipePleine : ResultatAjout

    data object NiveauInvalide : ResultatAjout
}

/**
 * Ajoute une espèce capturée à l'équipe : dans l'[emplacement] donné (le membre qui l'occupait est
 * échangé contre elle) ou, sans emplacement, dans le premier libre.
 */
class AjouterMembreUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(
        runId: Long,
        especeId: Int,
        niveau: Int,
        emplacement: Int? = null,
        surnom: String? = null,
    ): ResultatAjout {
        if (niveau !in NIVEAUX) return ResultatAjout.NiveauInvalide
        val statut = progression.statuts(runId).first()[especeId]
        if (statut == null || statut < StatutEspece.CAPTUREE) return ResultatAjout.NonCapturee
        val occupes = progression.equipe(runId).first().map { it.emplacement }.toSet()
        val cible = emplacement ?: (EMPLACEMENTS.firstOrNull { it !in occupes } ?: return ResultatAjout.EquipePleine)
        progression.placerMembre(runId, MembreEquipe(cible, especeId, surnom?.takeIf { it.isNotBlank() }, niveau))
        return ResultatAjout.Ajoute(cible)
    }

    private companion object {
        val NIVEAUX = 1..100
        val EMPLACEMENTS = 1..6
    }
}

/** Retire un membre : son emplacement redevient libre. */
class RetirerMembreUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, emplacement: Int) = progression.retirerMembre(runId, emplacement)
}
