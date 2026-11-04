package com.omegadex.app.domain.pokedex

import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.TypeLieu
import com.omegadex.app.domain.repository.JeuRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Façons d'obtenir une espèce proposées par le filtre « Méthode » du Pokédex (F12). */
enum class MethodeFiltre {
    HERBE,
    HORDE,
    SURF,
    PECHE,
    PLONGEE,
    ROCHE_FRACASSEE,
    NAVI_DEX,

    /** Rencontrée dans au moins un lieu mirage (îles, forêts, grottes et monts mirages). */
    LIEU_MIRAGE,

    /** Rencontre fixe, cadeau ou échange en jeu. */
    FIXE_OU_CADEAU,

    /** Pokémon légendaire ou fabuleux, quelle que soit la façon de le rencontrer. */
    LEGENDAIRE,
}

/** Où et comment une espèce s'obtient dans Rubis Oméga. */
data class OriginesEspece(val lieux: Set<Int> = emptySet(), val methodes: Set<MethodeFiltre> = emptySet()) {
    /** Vrai si l'espèce se trouve au lieu [zoneId] (null : toute zone) et par la [methode] (null : toute méthode). */
    fun correspond(zoneId: Int?, methode: MethodeFiltre?): Boolean =
        (zoneId == null || zoneId in lieux) && (methode == null || methode in methodes)
}

/** Construit les origines de chaque espèce à partir des rencontres de Rubis Oméga. */
object OriginesEspeces {
    /** Légendaires et fabuleux par numéro national (l'identifiant d'espèce de la base est ce numéro). */
    @Suppress("MagicNumber") // numéros du Pokédex national
    val LEGENDAIRES: Set<Int> = setOf(144, 145, 146, 150, 151, 243, 244, 245, 249, 250, 251) +
        (377..386) + (480..494) + (638..649) + (716..721)

    private val METHODES: Map<MethodeRencontre, MethodeFiltre> = mapOf(
        MethodeRencontre.HERBE to MethodeFiltre.HERBE,
        MethodeRencontre.HORDE to MethodeFiltre.HORDE,
        MethodeRencontre.SURF to MethodeFiltre.SURF,
        MethodeRencontre.PECHE to MethodeFiltre.PECHE,
        MethodeRencontre.PLONGEE to MethodeFiltre.PLONGEE,
        MethodeRencontre.ROCHE_FRACASSEE to MethodeFiltre.ROCHE_FRACASSEE,
        MethodeRencontre.NAVI_DEX to MethodeFiltre.NAVI_DEX,
        MethodeRencontre.FIXE to MethodeFiltre.FIXE_OU_CADEAU,
        MethodeRencontre.CADEAU to MethodeFiltre.FIXE_OU_CADEAU,
        MethodeRencontre.ECHANGE_EN_JEU to MethodeFiltre.FIXE_OU_CADEAU,
    )

    /**
     * Index espèce → origines. Seules les rencontres possibles dans Rubis Oméga comptent (pas les exclusivités
     * de Saphir Alpha) ; un légendaire porte [MethodeFiltre.LEGENDAIRE] même sans rencontre (échange requis).
     */
    fun de(rencontres: List<Rencontre>, especes: Collection<Int> = emptyList()): Map<Int, OriginesEspece> {
        val parEspece = rencontres.filter { it.possibleDansRubisOmega() }.groupBy { it.especeId }
        return (parEspece.keys + especes).associateWith { id ->
            val liste = parEspece[id].orEmpty()
            val methodes = liste.mapNotNull { METHODES[it.methode] }.toMutableSet()
            if (liste.any { it.lieu.type == TypeLieu.LIEU_MIRAGE }) methodes += MethodeFiltre.LIEU_MIRAGE
            if (id in LEGENDAIRES) methodes += MethodeFiltre.LEGENDAIRE
            OriginesEspece(liste.map { it.lieu.id }.toSet(), methodes)
        }
    }
}

/** Origines de toutes les espèces, pour les filtres zone et méthode du Pokédex. */
class ObserverOriginesUseCase @Inject constructor(private val jeu: JeuRepository) {
    operator fun invoke(): Flow<Map<Int, OriginesEspece>> =
        jeu.toutesLesRencontres().map { OriginesEspeces.de(it, OriginesEspeces.LEGENDAIRES) }
}
