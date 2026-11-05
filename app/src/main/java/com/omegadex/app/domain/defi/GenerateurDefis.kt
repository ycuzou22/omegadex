package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.TypeDefi

/** Évolution possible d'une espèce, avec le nom de l'espèce obtenue. */
data class EvolutionPossible(val cibleNom: String, val condition: ConditionEvolution, val valeur: String?)

/** Espèce qui peut porter un défi « espèce » : capturée par le joueur ou présente dans son équipe. */
data class EspeceCandidate(
    val id: Int,
    val nom: String,
    val dansEquipe: Boolean,
    val capturee: Boolean,
    val aUneMega: Boolean,
    val talentCache: Boolean,
    val evolutions: List<EvolutionPossible>,
)

/** Ce que le moteur sait de la run au moment de proposer des défis. */
data class ContexteDefis(
    val difficulte: Difficulte,
    val etape: Etape,
    val starterNom: String,
    val especes: List<EspeceCandidate>,
    val defisExistants: List<Defi>,
    val nationalObtenu: Boolean,
    /** Faux tant que l'avant-dernière quête principale de l'étape n'est pas cochée (voir [VerrouChampion]). */
    val championOuvert: Boolean = true,
    /**
     * Arène visée par les défis d'évolution : l'étape en cours, ou la prochaine arène quand l'étape n'a pas
     * de badge (Partie 0 : on fait évoluer ses Pokémon avant Roxanne, pas avant le rival).
     */
    val arene: Etape = etape,
)

/** Défi proposé au joueur, pas encore accepté. [enonce] est le gabarit rempli. */
data class DefiPropose(
    val modele: ModeleDefi,
    val especeId: Int?,
    val etapeId: Int?,
    val enonce: String,
    val especeNom: String? = null,
)

/**
 * Moteur de défis (T11) : instancie les modèles pour l'étape en cours d'une run.
 *
 * - un modèle n'est proposé qu'à partir de sa difficulté minimale et de son étape de déblocage ;
 * - les défis d'étape (`ETAPE`, `ROTATION`, `MONOTYPE`) visent l'étape en cours ;
 * - les défis « espèce » (`ESPECE`, `EVOLUTION`, `NAVI_DEX`, `MEGA`, `CONCOURS`) visent une espèce capturée
 *   ou de l'équipe, **un seul défi espèce actif par Pokémon** ;
 * - aucun défi ne fixe un niveau au-dessus du plafond, et une évolution n'est proposée que si elle est
 *   atteignable sous le plafond ;
 * - un défi déjà actif ou réussi n'est pas reproposé (un défi raté peut être retenté) ;
 * - **un seul défi en cours par catégorie** ([CategorieDefi]) ; le défi du champion ne s'ouvre qu'à
 *   l'avant-dernière quête principale de l'étape ([ContexteDefis.championOuvert]) ;
 * - **un seul défi de combat par adversaire** : dès qu'un défi d'étape, Monotype ou Rotation est accepté pour
 *   l'étape en cours, les autres défis de combat de cette étape ne sont plus proposés (ils reviennent s'il est raté).
 */
object GenerateurDefis {
    /** Types de défis attachés à une espèce. */
    val TYPES_ESPECE = setOf(TypeDefi.ESPECE, TypeDefi.EVOLUTION, TypeDefi.NAVI_DEX, TypeDefi.MEGA, TypeDefi.CONCOURS)

    /** Types de défis qui portent sur le combat contre l'adversaire de l'étape. */
    val TYPES_COMBAT = setOf(TypeDefi.ETAPE, TypeDefi.MONOTYPE, TypeDefi.ROTATION)

    fun generer(contexte: ContexteDefis, modeles: List<ModeleDefi>): List<DefiPropose> {
        val types = modeles.associate { it.id to it.type }
        val combatFerme = !contexte.championOuvert || combatEngage(contexte.defisExistants, contexte.etape.id, types)
        val occupees = categoriesOccupees(contexte.defisExistants, types)
        return modeles.filter { estDebloque(it, contexte) }.flatMap { modele ->
            when {
                CategorieDefi.de(modele.type) in occupees -> emptyList()
                modele.type in TYPES_ESPECE -> defisEspece(modele, contexte)
                combatFerme -> emptyList()
                else -> listOfNotNull(defiEtape(modele, contexte))
            }
        }
    }

    /** Catégories qui ont déjà un défi en cours : un seul défi actif par catégorie. */
    fun categoriesOccupees(defis: List<Defi>, types: Map<Int, TypeDefi>): Set<CategorieDefi> =
        defis.filter { it.etat == EtatDefi.ACTIF }.mapNotNull { types[it.modeleId]?.let(CategorieDefi::de) }.toSet()

    /** Vrai si un défi de combat de l'étape [etapeId] est en cours ou réussi (un défi raté libère le combat). */
    fun combatEngage(defis: List<Defi>, etapeId: Int, types: Map<Int, TypeDefi>): Boolean = defis.any {
        it.etapeId == etapeId && it.etat != EtatDefi.RATE && types[it.modeleId] in TYPES_COMBAT
    }

    /** Étape dont un défi prend le plafond et le champion : l'arène visée pour un défi d'évolution. */
    fun etapeVisee(modele: ModeleDefi, contexte: ContexteDefis): Etape =
        if (modele.type == TypeDefi.EVOLUTION) contexte.arene else contexte.etape

    /** Difficulté, étape de déblocage et conditions qui ne dépendent pas d'une espèce. */
    fun estDebloque(modele: ModeleDefi, contexte: ContexteDefis): Boolean =
        ReglesDifficulte.estProposable(contexte.difficulte, modele.difficulteMin) &&
            etapeVisee(modele, contexte).id >= modele.etapeMin &&
            (modele.type != TypeDefi.ETAPE || modele.etapeId == contexte.etape.id) &&
            (ConditionDeblocage.POKEDEX_NATIONAL !in modele.conditions || contexte.nationalObtenu)

    /**
     * Espèces ayant déjà un défi espèce actif : elles n'en reçoivent pas d'autre.
     * Seuls les défis espèce portent une espèce.
     */
    fun especesOccupees(defis: List<Defi>): Set<Int> =
        defis.filter { it.etat == EtatDefi.ACTIF }.mapNotNull { it.especeId }.toSet()

    private fun defiEtape(modele: ModeleDefi, contexte: ContexteDefis): DefiPropose? {
        val etapeId = contexte.etape.id
        if (dejaEngage(contexte.defisExistants, modele.id, especeId = null, etapeId = etapeId)) return null
        return DefiPropose(modele, null, etapeId, enonce(modele, contexte.etape, contexte.starterNom))
    }

    private fun defisEspece(modele: ModeleDefi, contexte: ContexteDefis): List<DefiPropose> {
        val occupees = especesOccupees(contexte.defisExistants)
        val etape = etapeVisee(modele, contexte)
        return contexte.especes.mapNotNull { espece ->
            val evolution = evolutionAtteignable(espece, etape.plafond)
            val possible = conditionsEspece(modele, espece, evolution) &&
                espece.id !in occupees &&
                !dejaEngage(contexte.defisExistants, modele.id, espece.id, etapeId = null)
            if (!possible) return@mapNotNull null
            DefiPropose(
                modele,
                espece.id,
                null,
                enonce(modele, etape, contexte.starterNom, espece.nom, evolution),
                espece.nom,
            )
        }
    }

    private fun conditionsEspece(modele: ModeleDefi, espece: EspeceCandidate, evolution: EvolutionPossible?): Boolean =
        modele.conditions.all { condition ->
            when (condition) {
                ConditionDeblocage.ESPECE_CAPTUREE -> espece.capturee
                ConditionDeblocage.ESPECE_DANS_EQUIPE -> espece.dansEquipe
                ConditionDeblocage.EVOLUTION_SOUS_PLAFOND -> evolution != null
                ConditionDeblocage.MEGA_DISPONIBLE -> espece.aUneMega
                ConditionDeblocage.TALENT_CACHE -> espece.talentCache
                ConditionDeblocage.CONCOURS_OUVERTS, ConditionDeblocage.POKEDEX_NATIONAL -> true
            }
        } &&
            (modele.type != TypeDefi.EVOLUTION || evolution != null)

    /** Première évolution de l'espèce atteignable sans dépasser le plafond. */
    fun evolutionAtteignable(espece: EspeceCandidate, plafond: Int): EvolutionPossible? =
        espece.evolutions.firstOrNull { ReglesDifficulte.evolutionAtteignable(it.condition, it.valeur, plafond) }

    private fun dejaEngage(defis: List<Defi>, modeleId: Int, especeId: Int?, etapeId: Int?) = defis.any {
        it.modeleId == modeleId && it.especeId == especeId && it.etapeId == etapeId && it.etat != EtatDefi.RATE
    }

    private fun ecart(modele: ModeleDefi) = modele.parametres["ecart"]?.toIntOrNull() ?: 0

    /**
     * Énoncé d'un défi : le gabarit du modèle rempli pour une étape (et une espèce pour un défi espèce).
     * Sert aussi à réafficher les défis déjà enregistrés.
     */
    fun enonce(
        modele: ModeleDefi,
        etape: Etape,
        starterNom: String,
        especeNom: String? = null,
        evolution: EvolutionPossible? = null,
    ): String {
        val niveau = evolution?.let { ReglesDifficulte.niveauRequis(it.condition, it.valeur) }
            ?: ReglesDifficulte.niveauMaximalDuDefi(etape.plafond, ecart(modele))
        val valeurs = buildMap {
            put("adversaire", etape.adversaire)
            put("plafond", etape.plafond.toString())
            put("lieu", etape.lieuPrincipal)
            put("etape", etape.titre)
            put("starter", starterNom)
            put("niveau", niveau.toString())
            especeNom?.let { put("espece", it) }
            evolution?.let { put("evolution", it.cibleNom) }
            modele.parametres.forEach { (cle, valeur) ->
                put(cle, if (cle == "type") NomsTypes.liste(valeur) else valeur)
            }
        }
        return MARQUEUR.replace(modele.gabarit) { valeurs[it.groupValues[1]] ?: it.value }
    }

    private val MARQUEUR = Regex("\\{(\\w+)\\}")
}
