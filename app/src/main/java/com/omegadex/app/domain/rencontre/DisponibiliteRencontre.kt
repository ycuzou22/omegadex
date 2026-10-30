package com.omegadex.app.domain.rencontre

import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre

/** Ce qui rend une rencontre possible plus tard que l'arrivée sur son lieu. */
enum class Debloqueur(val etape: Int) {
    CANNE(EtapesCles.CANNE),
    ECLATE_ROC(EtapesCles.ECLATE_ROC),
    LUNETTES_SABLE(EtapesCles.LUNETTES_SABLE),
    SURF(EtapesCles.SURF),
    SUPER_CANNE(EtapesCles.SUPER_CANNE),
    MEGA_CANNE(EtapesCles.MEGA_CANNE),
    DEVON_SCOPE(EtapesCles.DEVON_SCOPE),
    PLONGEE(EtapesCles.PLONGEE),
    APRES_GROUDON(EtapesCles.APRES_GROUDON),
    POKEDEX_NATIONAL(EtapesCles.POKEDEX_NATIONAL),
    APRES_LIGUE(EtapesCles.APRES_LIGUE),
    APRES_DELTA(EtapesCles.APRES_DELTA),
}

/** Espèce qu'on n'attrape pas dans Rubis Oméga : on la reçoit par échange. */
enum class Echange {
    /** Exclusivité de Saphir Alpha, au même lieu dans l'autre version. */
    SAPHIR_ALPHA,

    /** Espèce absente de Hoenn : échange ou transfert depuis un autre jeu, avec le Pokédex National. */
    HORS_HOENN,
}

/**
 * Étapes de l'aventure (identifiants de la table `etape`) où chaque capacité ou objet devient utilisable.
 * L'étape N est celle où l'on affronte le N-ième champion : un badge gagné à l'étape N sert dès l'étape N + 1.
 */
object EtapesCles {
    /** Canne : donnée par un pêcheur à Myokara, ville de Bastien. */
    const val CANNE = 2

    /** CS06 Éclate-Roc : utilisable avec le Badge Dynamo (Voltère, étape 3). */
    const val ECLATE_ROC = 4

    /** Lunettes Sable (désert de la Route 111, fossiles) : remises après le badge d'Adriane (étape 4). */
    const val LUNETTES_SABLE = 5

    /** CS03 Surf : utilisable avec le Badge Balance (Norman, étape 5). */
    const val SURF = 6

    /** Super Canne : pêcheur de la Route 118, sur la rive est qu'on rejoint en Surf. */
    const val SUPER_CANNE = 6

    /** Méga Canne : pêcheur d'Algatia, ville de Lévy et Tatia. */
    const val MEGA_CANNE = 7

    /** Devon Scope (Kecleon invisibles) : remis par Pierre sur la Route 120, après Alizée (étape 6). */
    const val DEVON_SCOPE = 7

    /** CS07 Plongée : utilisable avec le Badge Esprit (Lévy et Tatia, étape 7). */
    const val PLONGEE = 8

    /** Rencontres « après avoir vaincu (ou capturé) Groudon ou Kyogre » : après l'étape 8. */
    const val APRES_GROUDON = 9

    /** Pokédex National : remis après la Ligue (étape 10). */
    const val POKEDEX_NATIONAL = 11

    /** « Après la Ligue » : cadeaux du Professeur Seko, Terhal… */
    const val APRES_LIGUE = 11

    /** « Après l'Épisode Delta » ou « après avoir battu la Ligue une deuxième fois » : post-game. */
    const val APRES_DELTA = 12
}

/** Quand une rencontre devient possible, et comment on obtient l'espèce. */
data class Disponibilite(
    /** Première étape où la rencontre est possible. */
    val etape: Int,
    /** Ce qui la débloque après l'arrivée sur le lieu, s'il y a quelque chose. */
    val debloqueur: Debloqueur?,
    /** Non nul : l'espèce se reçoit par échange au lieu de s'attraper. */
    val echange: Echange?,
)

/**
 * Disponibilité d'une rencontre selon l'avancement : son lieu doit être atteint et sa méthode utilisable
 * (badges, CS, cannes, Pokédex National). Les conditions viennent de la colonne `condition` importée de
 * Poképédia (« Super Canne », « après avoir vaincu Groudon ou Kyogre », « Saphir Alpha uniquement »…).
 */
object DisponibiliteRencontre {
    fun de(rencontre: Rencontre): Disponibilite {
        val condition = rencontre.condition.orEmpty()
        val echange = when {
            rencontre.methode == MethodeRencontre.ECHANGE_REQUIS -> Echange.HORS_HOENN
            condition.contains(SAPHIR_ALPHA, ignoreCase = true) -> Echange.SAPHIR_ALPHA
            else -> null
        }
        val exigences = listOfNotNull(
            debloqueurDeLaMethode(rencontre.methode, condition),
            Debloqueur.APRES_GROUDON.takeIf { APRES_GROUDON.containsMatchIn(condition) },
            Debloqueur.PLONGEE.takeIf { condition.contains(PLONGEE_REQUISE, ignoreCase = true) },
            Debloqueur.DEVON_SCOPE.takeIf { condition.contains(DEVON_SCOPE, ignoreCase = true) },
            Debloqueur.LUNETTES_SABLE.takeIf { condition.contains(FOSSILE, ignoreCase = true) },
            debloqueurDeLaLigue(condition),
            Debloqueur.POKEDEX_NATIONAL.takeIf { echange == Echange.HORS_HOENN },
        )
        val plusTardive = exigences.maxByOrNull { it.etape }
        val etapeLieu = rencontre.lieu.etapeDeblocageId
        return Disponibilite(
            etape = maxOf(etapeLieu, plusTardive?.etape ?: etapeLieu),
            debloqueur = plusTardive?.takeIf { it.etape > etapeLieu },
            echange = echange,
        )
    }

    /** La rencontre est-elle possible à l'[etape] donnée ? */
    fun possibleA(rencontre: Rencontre, etape: Int): Boolean = de(rencontre).etape <= etape

    private fun debloqueurDeLaLigue(condition: String): Debloqueur? = when {
        APRES_DELTA.containsMatchIn(condition) -> Debloqueur.APRES_DELTA
        condition.contains(APRES_LIGUE, ignoreCase = true) -> Debloqueur.APRES_LIGUE
        else -> null
    }

    /** Cadeau « Pokémon de départ » : seul le starter choisi pour la run est une vraie rencontre. */
    fun estUnAutreStarter(rencontre: Rencontre, starterId: Int?): Boolean =
        rencontre.condition?.contains(STARTER, ignoreCase = true) == true && rencontre.especeId != starterId

    private fun debloqueurDeLaMethode(methode: MethodeRencontre, condition: String): Debloqueur? = when (methode) {
        MethodeRencontre.SURF -> Debloqueur.SURF
        MethodeRencontre.PLONGEE -> Debloqueur.PLONGEE
        MethodeRencontre.ROCHE_FRACASSEE -> Debloqueur.ECLATE_ROC
        MethodeRencontre.PECHE -> when {
            condition.contains(MEGA_CANNE, ignoreCase = true) -> Debloqueur.MEGA_CANNE
            condition.contains(SUPER_CANNE, ignoreCase = true) -> Debloqueur.SUPER_CANNE
            else -> Debloqueur.CANNE
        }
        else -> null
    }

    private const val SAPHIR_ALPHA = "Saphir Alpha uniquement"
    private const val MEGA_CANNE = "Méga Canne"
    private const val SUPER_CANNE = "Super Canne"
    private const val PLONGEE_REQUISE = "Plongée requise"
    private const val DEVON_SCOPE = "Devon Scope"
    private const val FOSSILE = "fossile ranimé"
    private const val APRES_LIGUE = "après la Ligue"
    private const val STARTER = "Pokémon de départ"
    private val APRES_DELTA = Regex("après l'Épisode Delta|Ligue une deuxième fois", RegexOption.IGNORE_CASE)
    private val APRES_GROUDON = Regex("après avoir (vaincu|capturé) Groudon", RegexOption.IGNORE_CASE)
}
