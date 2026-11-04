package com.omegadex.app.domain.succes

import com.omegadex.app.domain.badge.BadgeArene
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece

/** Ce que mesure un succès. */
enum class FamilleSucces { CAPTURES, MAITRISES, EVOLUTIONS, BADGES, AVENTURE, DEFIS, EQUIPE }

/**
 * Paliers de succès d'une run (F11). Un succès est atteint quand sa mesure arrive à [seuil] ; une fois
 * enregistré, il reste débloqué même si la mesure redescend (Pokémon relâché, défi remis en jeu…).
 * L'[name] sert d'identifiant en base : ne jamais renommer une constante.
 */
@Suppress("MagicNumber") // les paliers eux-mêmes : des données de jeu
enum class Succes(val famille: FamilleSucces, val seuil: Int) {
    CAPTURES_10(FamilleSucces.CAPTURES, 10),
    CAPTURES_50(FamilleSucces.CAPTURES, 50),
    CAPTURES_100(FamilleSucces.CAPTURES, 100),
    CAPTURES_200(FamilleSucces.CAPTURES, 200),
    MAITRISES_1(FamilleSucces.MAITRISES, 1),
    MAITRISES_10(FamilleSucces.MAITRISES, 10),
    MAITRISES_50(FamilleSucces.MAITRISES, 50),
    EVOLUTIONS_10(FamilleSucces.EVOLUTIONS, 10),
    EVOLUTIONS_30(FamilleSucces.EVOLUTIONS, 30),
    BADGES_1(FamilleSucces.BADGES, 1),
    BADGES_4(FamilleSucces.BADGES, 4),
    BADGES_8(FamilleSucces.BADGES, 8),
    LIGUE(FamilleSucces.AVENTURE, MesuresSucces.ETAPE_LIGUE),
    AVENTURE_TERMINEE(FamilleSucces.AVENTURE, MesuresSucces.ETAPE_FINALE),
    DEFIS_1(FamilleSucces.DEFIS, 1),
    DEFIS_10(FamilleSucces.DEFIS, 10),
    DEFIS_25(FamilleSucces.DEFIS, 25),
    DEFIS_50(FamilleSucces.DEFIS, 50),
    EQUIPE_COMPLETE(FamilleSucces.EQUIPE, 6),
}

/** Mesures d'une run à un instant donné, dont dépendent tous les succès. */
data class MesuresSucces(
    val captures: Int = 0,
    val maitrises: Int = 0,
    val evolutions: Int = 0,
    val badges: Int = 0,
    val etapesValidees: Set<Int> = emptySet(),
    val defisReussis: Int = 0,
    val tailleEquipe: Int = 0,
) {
    /** Valeur de la mesure suivie par [succes] (pour l'Aventure : l'étape validée la plus avancée). */
    fun valeur(succes: Succes): Int = when (succes.famille) {
        FamilleSucces.CAPTURES -> captures
        FamilleSucces.MAITRISES -> maitrises
        FamilleSucces.EVOLUTIONS -> evolutions
        FamilleSucces.BADGES -> badges
        FamilleSucces.AVENTURE -> (etapesValidees.maxOrNull() ?: 0).coerceAtMost(succes.seuil)
        FamilleSucces.DEFIS -> defisReussis
        FamilleSucces.EQUIPE -> tailleEquipe
    }

    fun atteint(succes: Succes): Boolean = when (succes.famille) {
        FamilleSucces.AVENTURE -> succes.seuil in etapesValidees
        else -> valeur(succes) >= succes.seuil
    }

    /** Tous les succès atteints par ces mesures. */
    fun atteints(): Set<Succes> = Succes.entries.filter(::atteint).toSet()

    companion object {
        /** Étape de la Ligue Pokémon (Pierre Rochard) : la valider débloque aussi le Pokédex National. */
        const val ETAPE_LIGUE = 10

        /** Dernière étape de l'aventure (revanche de Pierre Rochard). */
        const val ETAPE_FINALE = 12

        fun de(
            statuts: Map<Int, StatutEspece>,
            etapes: List<ProgressionEtape>,
            defis: List<Defi>,
            equipe: List<MembreEquipe>,
        ): MesuresSucces {
            val validees = etapes.filter { it.dateValidation != null }.map { it.etapeId }.toSet()
            return MesuresSucces(
                captures = statuts.values.count { it >= StatutEspece.CAPTUREE },
                maitrises = statuts.values.count { it == StatutEspece.MAITRISEE },
                evolutions = statuts.values.count { it == StatutEspece.EVOLUEE },
                badges = BadgeArene.obtenus(validees).size,
                etapesValidees = validees,
                defisReussis = defis.count { it.etat == EtatDefi.REUSSI },
                tailleEquipe = equipe.size,
            )
        }
    }
}

/** Un succès vu dans la vitrine : débloqué (avec sa date) ou en cours, avec l'avancement de sa mesure. */
data class EtatSucces(val succes: Succes, val dateDeblocage: Long?, val avancement: Int) {
    val debloque: Boolean get() = dateDeblocage != null
}
