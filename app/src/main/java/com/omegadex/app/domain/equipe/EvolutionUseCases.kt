package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Résultat de la saisie d'un niveau. */
sealed interface ResultatNiveau {
    /**
     * Niveau enregistré. Si [evolutions] n'est pas vide, l'écran propose l'évolution
     * (et lance l'animation plein écran, T14.3) ; le joueur confirme avec [FaireEvoluerUseCase].
     */
    data class Enregistre(val evolutions: List<EvolutionDisponible>) : ResultatNiveau

    data object MembreAbsent : ResultatNiveau

    data object NiveauInvalide : ResultatNiveau
}

/** Résultat d'une évolution confirmée par le joueur. */
sealed interface ResultatEvolution {
    data class Evolue(val especeSourceId: Int, val especeCibleId: Int) : ResultatEvolution

    data object MembreAbsent : ResultatEvolution

    /** L'espèce choisie n'est pas une évolution de celle du membre. */
    data object EvolutionImpossible : ResultatEvolution
}

/** Enregistre le niveau d'un membre de l'équipe et détecte les évolutions par niveau atteintes. */
class ChangerNiveauUseCase @Inject constructor(
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    suspend operator fun invoke(runId: Long, emplacement: Int, niveau: Int): ResultatNiveau {
        if (niveau !in NIVEAUX) return ResultatNiveau.NiveauInvalide
        val membre = progression.equipe(runId).first().firstOrNull { it.emplacement == emplacement }
            ?: return ResultatNiveau.MembreAbsent
        progression.placerMembre(runId, membre.copy(niveau = niveau))
        val famille = jeu.famille(membre.especeId).first()
        return ResultatNiveau.Enregistre(DetectionEvolution.disponibles(membre.especeId, niveau, famille))
    }

    private companion object {
        val NIVEAUX = 1..100
    }
}

/**
 * Fait évoluer un membre de l'équipe : il change d'espèce en gardant son surnom et son niveau
 * (augmenté d'un niveau si [monterDUnNiveau] : bonheur, objet tenu, lieu, beauté, attaque apprise).
 * L'espèce d'origine passe au statut « évoluée » et la nouvelle au moins à « capturée » ;
 * un statut n'est jamais abaissé.
 */
class FaireEvoluerUseCase @Inject constructor(
    private val progression: ProgressionRepository,
    private val jeu: JeuRepository,
) {
    suspend operator fun invoke(
        runId: Long,
        emplacement: Int,
        especeCibleId: Int,
        monterDUnNiveau: Boolean = false,
    ): ResultatEvolution {
        val membre = progression.equipe(runId).first().firstOrNull { it.emplacement == emplacement }
            ?: return ResultatEvolution.MembreAbsent
        val possible = jeu.famille(membre.especeId).first().any {
            it.especeSourceId == membre.especeId && it.especeCibleId == especeCibleId
        }
        if (!possible) return ResultatEvolution.EvolutionImpossible

        val niveau = if (monterDUnNiveau) (membre.niveau + 1).coerceAtMost(NIVEAU_MAX) else membre.niveau
        progression.placerMembre(runId, membre.copy(especeId = especeCibleId, niveau = niveau))
        val statuts = progression.statuts(runId).first()
        relever(runId, membre.especeId, statuts[membre.especeId], StatutEspece.EVOLUEE)
        relever(runId, especeCibleId, statuts[especeCibleId], StatutEspece.CAPTUREE)
        return ResultatEvolution.Evolue(membre.especeId, especeCibleId)
    }

    private companion object {
        const val NIVEAU_MAX = 100
    }

    private suspend fun relever(runId: Long, especeId: Int, actuel: StatutEspece?, minimum: StatutEspece) {
        if (actuel == null || actuel < minimum) progression.definirStatut(runId, especeId, minimum)
    }
}
