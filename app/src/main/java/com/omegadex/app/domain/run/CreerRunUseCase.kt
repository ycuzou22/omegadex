package com.omegadex.app.domain.run

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.RunActiveRepository
import com.omegadex.app.domain.repository.SessionRepository
import javax.inject.Inject

/** Les trois starters de Hoenn proposés à la création d'une run (numéros du Pokédex national). */
@Suppress("MagicNumber") // numéros du Pokédex : des données, pas des seuils
enum class Starter(val especeId: Int, val type: String) {
    ARCKO(252, "plante"),
    POUSSIFEU(255, "feu"),
    GOBOU(258, "eau"),
    ;

    companion object {
        fun de(especeId: Int): Starter? = entries.firstOrNull { it.especeId == especeId }
    }
}

/** Règles optionnelles qu'un joueur peut s'imposer ; la clé est enregistrée avec la run. */
enum class RegleOptionnelle(val cle: String) {
    NUZLOCKE("nuzlocke"),
    MONOTYPE("monotype"),
    SANS_OBJETS("sans_objets"),
    SANS_MULTI_EXP("sans_multi_exp"),
    SURNOMS("surnoms"),
    ;

    companion object {
        fun de(cle: String): RegleOptionnelle? = entries.firstOrNull { it.cle == cle }
    }
}

enum class ErreurCreationRun { NOM_VIDE, NOM_TROP_LONG, STARTER_MANQUANT, NON_CONNECTE }

sealed interface ResultatCreationRun {
    data class Creee(val runId: Long) : ResultatCreationRun
    data class Refusee(val erreurs: Set<ErreurCreationRun>) : ResultatCreationRun
}

const val LONGUEUR_MAX_NOM_RUN = 30

/** Première étape de l'aventure (départ de Bourg-en-Vol), déverrouillée à la création. */
const val ETAPE_DE_DEPART = 0

/**
 * Crée une run pour le joueur connecté, déverrouille l'étape 0 et en fait la run active.
 * La date de début est celle de la création.
 */
class CreerRunUseCase @Inject constructor(
    private val comptes: CompteRepository,
    private val progression: ProgressionRepository,
    private val session: SessionRepository,
    private val runActive: RunActiveRepository,
) {
    suspend operator fun invoke(
        nom: String,
        starter: Starter?,
        difficulte: Difficulte,
        regles: Set<RegleOptionnelle>,
    ): ResultatCreationRun {
        val utilisateurId = session.utilisateurConnecte.value
            ?: return ResultatCreationRun.Refusee(setOf(ErreurCreationRun.NON_CONNECTE))
        val nomPropre = nom.trim()
        val erreurs = buildSet {
            if (nomPropre.isEmpty()) add(ErreurCreationRun.NOM_VIDE)
            if (nomPropre.length > LONGUEUR_MAX_NOM_RUN) add(ErreurCreationRun.NOM_TROP_LONG)
            if (starter == null) add(ErreurCreationRun.STARTER_MANQUANT)
        }
        if (erreurs.isNotEmpty() || starter == null) return ResultatCreationRun.Refusee(erreurs)
        val runId = comptes.creerRun(
            utilisateurId = utilisateurId,
            nom = nomPropre,
            starterId = starter.especeId,
            difficulte = difficulte,
            regles = RegleOptionnelle.entries.filter { it in regles }.map { it.cle },
        )
        progression.deverrouillerEtape(runId, ETAPE_DE_DEPART)
        runActive.choisir(utilisateurId, runId)
        return ResultatCreationRun.Creee(runId)
    }
}
