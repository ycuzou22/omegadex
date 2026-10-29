package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.ModeleDefi
import kotlin.random.Random

/**
 * Banque de défis par famille d'évolution (`tools/import/import_banque_defis.py`, `docs/modeles-defis.md`).
 *
 * Environ 200 défis par famille, rangés dans `modele_defi` aux identifiants `100000 + racine × 1000 + n`.
 * Chaque défi vise un membre de la famille (paramètre `cible`). Pour un Pokémon de l'équipe, le moteur
 * **tire au sort** un seul défi parmi ceux de sa banque qui sont débloqués ; le tirage ne change pas à chaque
 * ouverture : il dépend de la run, du Pokémon, de l'étape et du nombre de défis que ce Pokémon a déjà terminés.
 */
object BanqueDefis {
    /** Premier identifiant de la banque ; en dessous, les modèles saisis à la main (`defis.json`). */
    const val ID_DEBUT = 100_000

    /** Nombre d'identifiants réservés à chaque famille. */
    const val TAILLE_PLAGE = 1_000

    fun estDeLaBanque(modeleId: Int): Boolean = modeleId >= ID_DEBUT

    /** Espèce visée par un défi de la banque. */
    fun cible(modele: ModeleDefi): Int? = modele.parametres["cible"]?.toIntOrNull()

    /**
     * Racine de la famille de [especeId] : la plus petite espèce de la famille qui n'est l'évolution d'aucune
     * autre. [famille] est le résultat de `JeuRepository.famille(especeId)` (vide pour une espèce sans évolution).
     */
    fun racine(especeId: Int, famille: List<Evolution>): Int {
        val evoluees = famille.map { it.especeCibleId }.toSet()
        val membres = famille.flatMap { listOf(it.especeSourceId, it.especeCibleId) }.toSet() + especeId
        return (membres - evoluees).minOrNull() ?: membres.min()
    }

    /** Graine du tirage : stable pour une run, un Pokémon et une étape, renouvelée à chaque défi terminé. */
    fun graine(runId: Long, especeId: Int, etapeId: Int, defisTermines: Int): Long =
        listOf(runId, especeId.toLong(), etapeId.toLong(), defisTermines.toLong())
            .fold(GRAINE_INITIALE) { acc, valeur -> acc * MULTIPLICATEUR + valeur }

    /**
     * Défis de la banque que [espece] peut relever maintenant : difficulté, étape et conditions remplies, jamais
     * relevés dans la run (un défi raté se reprend avec « Retenter », pas par un nouveau tirage).
     */
    fun eligibles(banque: List<ModeleDefi>, espece: EspeceCandidate, contexte: ContexteDefis): List<ModeleDefi> {
        val dejaReleves = contexte.defisExistants.filter { it.especeId == espece.id }.map { it.modeleId }.toSet()
        return banque.filter { modele ->
            cible(modele) == espece.id &&
                modele.id !in dejaReleves &&
                GenerateurDefis.estDebloque(modele, contexte) &&
                conditionsRemplies(modele, espece)
        }
    }

    /**
     * Tire un défi de la banque pour [espece], ou null si le Pokémon relève déjà un défi espèce
     * ou si aucun défi de sa banque n'est débloqué.
     */
    fun tirer(
        banque: List<ModeleDefi>,
        espece: EspeceCandidate,
        contexte: ContexteDefis,
        graine: Long,
    ): DefiPropose? {
        if (espece.id in GenerateurDefis.especesOccupees(contexte.defisExistants)) return null
        val candidats = eligibles(banque, espece, contexte)
        if (candidats.isEmpty()) return null
        val modele = candidats[Random(graine).nextInt(candidats.size)]
        return DefiPropose(
            modele = modele,
            especeId = espece.id,
            etapeId = null,
            enonce = GenerateurDefis.enonce(modele, contexte.etape, contexte.starterNom, espece.nom),
            especeNom = espece.nom,
        )
    }

    private fun conditionsRemplies(modele: ModeleDefi, espece: EspeceCandidate): Boolean =
        modele.conditions.all { condition ->
            when (condition) {
                ConditionDeblocage.ESPECE_CAPTUREE -> espece.capturee
                ConditionDeblocage.ESPECE_DANS_EQUIPE -> espece.dansEquipe
                ConditionDeblocage.MEGA_DISPONIBLE -> espece.aUneMega
                ConditionDeblocage.TALENT_CACHE -> espece.talentCache
                // L'étape de déblocage de la banque tient déjà compte du plafond, des Concours et du Pokédex.
                ConditionDeblocage.EVOLUTION_SOUS_PLAFOND,
                ConditionDeblocage.CONCOURS_OUVERTS,
                ConditionDeblocage.POKEDEX_NATIONAL,
                -> true
            }
        }

    private const val GRAINE_INITIALE = 17L
    private const val MULTIPLICATEUR = 1_000_003L
}
