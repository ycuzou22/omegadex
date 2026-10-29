package com.omegadex.app.domain.pokedex

import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Résultat d'une capture déclarée par le joueur. */
sealed interface ResultatCapture {
    /** L'espèce entre dans les capturées ; [avant] est son statut précédent (non vue ou vue). */
    data class Capturee(val avant: StatutEspece) : ResultatCapture

    /** L'espèce était déjà au moins capturée : rien ne change. */
    data object DejaCapturee : ResultatCapture
}

/**
 * Marque une espèce comme capturée dans la run. Un statut plus avancé (entraînée, évoluée, maîtrisée)
 * n'est jamais abaissé.
 */
class CapturerEspeceUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, especeId: Int): ResultatCapture {
        val avant = progression.statuts(runId).first()[especeId] ?: StatutEspece.NON_VUE
        if (avant >= StatutEspece.CAPTUREE) return ResultatCapture.DejaCapturee
        progression.definirStatut(runId, especeId, StatutEspece.CAPTUREE)
        return ResultatCapture.Capturee(avant)
    }
}

/**
 * Rencontre que le joueur peut faire dans Rubis Oméga : ni échange requis, ni rencontre réservée à Saphir Alpha
 * (même règle que les espèces « à portée » du Pokédex).
 */
fun Rencontre.possibleDansRubisOmega(): Boolean =
    methode != MethodeRencontre.ECHANGE_REQUIS && condition?.contains(SAPHIR_ALPHA_UNIQUEMENT) != true

private const val SAPHIR_ALPHA_UNIQUEMENT = "Saphir Alpha uniquement"
