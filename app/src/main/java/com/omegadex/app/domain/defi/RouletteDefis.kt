package com.omegadex.app.domain.defi

import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.domain.model.TypeQuete
import kotlin.random.Random

/** Les six catégories de la page Défis : chacune a sa roulette et ne porte qu'un défi en cours à la fois. */
enum class CategorieDefi(val types: Set<TypeDefi>) {
    CHAMPION(setOf(TypeDefi.ETAPE, TypeDefi.MONOTYPE, TypeDefi.ROTATION)),
    ESPECE(setOf(TypeDefi.ESPECE)),
    EVOLUTION(setOf(TypeDefi.EVOLUTION)),
    NAVI_DEX(setOf(TypeDefi.NAVI_DEX)),
    MEGA(setOf(TypeDefi.MEGA)),
    CONCOURS(setOf(TypeDefi.CONCOURS)),
    ;

    companion object {
        fun de(type: TypeDefi): CategorieDefi = entries.first { type in it.types }
    }
}

/** Désigne un défi proposé : son modèle, et l'espèce ou l'étape qu'il vise. */
data class CleDefi(val modeleId: Int, val especeId: Int?, val etapeId: Int?) {
    fun designe(propose: DefiPropose): Boolean =
        propose.modele.id == modeleId && propose.especeId == especeId && propose.etapeId == etapeId

    companion object {
        fun de(propose: DefiPropose) = CleDefi(propose.modele.id, propose.especeId, propose.etapeId)
    }
}

/**
 * État de la roulette d'une catégorie pendant l'étape [etapeId] : le défi tiré et pas encore relevé
 * ([defi], null une fois relevé) et les relances déjà faites pendant l'étape.
 */
data class Tirage(val etapeId: Int, val defi: CleDefi?, val relances: Int)

/** Règles de la roulette des défis. */
object RouletteDefis {
    /** Longueur du rouleau affiché et carte sur laquelle il s'arrête. */
    const val LONGUEUR_ROULEAU = 24
    const val ARRET_ROULEAU = 20

    /** Relances par catégorie et par étape : illimitées (null) en Normal, une en Difficile, aucune en Hardcore. */
    fun relancesPermises(difficulte: Difficulte): Int? = when (difficulte) {
        Difficulte.NORMAL -> null
        Difficulte.DIFFICILE -> 1
        Difficulte.HARDCORE -> 0
    }

    /** Relances encore possibles (null : illimitées) après [faites] relances pendant l'étape. */
    fun relancesRestantes(difficulte: Difficulte, faites: Int): Int? =
        relancesPermises(difficulte)?.let { (it - faites).coerceAtLeast(0) }

    /**
     * Tire un défi dans [pool]. Une relance ne retombe pas sur le défi [precedent] quand il y a un autre choix.
     */
    fun tirer(pool: List<DefiPropose>, precedent: DefiPropose?, aleatoire: Random): DefiPropose? {
        val candidats = pool.filterNot { precedent != null && it == precedent }.ifEmpty { pool }
        return candidats.randomOrNull(aleatoire)
    }

    /**
     * Cartes du rouleau : le [pool] défile en boucle et la carte [ARRET_ROULEAU] est le [gagnant],
     * sans deux cartes identiques côte à côte quand le pool en compte plusieurs.
     */
    fun rouleau(pool: List<DefiPropose>, gagnant: DefiPropose): List<DefiPropose> {
        val cartes = if (gagnant in pool) pool else pool + gagnant
        val indexGagnant = cartes.indexOf(gagnant)
        return List(LONGUEUR_ROULEAU) { i -> cartes[Math.floorMod(indexGagnant - ARRET_ROULEAU + i, cartes.size)] }
    }
}

/**
 * Le défi d'un champion d'arène s'ouvre quand l'avant-dernière quête principale avant le badge est cochée
 * (Roxanne : « Atteindre Mérouville »). Une étape sans badge, ou avec une seule quête principale, est ouverte.
 */
object VerrouChampion {
    /** Quête principale qu'il reste à cocher pour ouvrir le défi du champion ; null s'il est ouvert. */
    fun queteRequise(etapeId: Int, quetes: List<Quete>, cochees: Set<Int>): Quete? {
        if (BadgeArene.pourEtape(etapeId) == null) return null
        val principales = quetes.filter { it.type == TypeQuete.PRINCIPALE }.sortedBy { it.ordre }
        val requise = principales.getOrNull(principales.size - 2) ?: return null
        return requise.takeIf { it.id !in cochees }
    }
}

/** Prochaine arène : l'étape en cours si elle donne un badge, sinon la première étape à badge qui suit. */
object ProchaineArene {
    fun de(etape: Etape, etapes: List<Etape>): Etape =
        etapes.sortedBy { it.id }.firstOrNull { it.id >= etape.id && BadgeArene.pourEtape(it.id) != null } ?: etape
}
