package com.omegadex.app.feature.succes

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.omegadex.app.R
import com.omegadex.app.domain.succes.FamilleSucces
import com.omegadex.app.domain.succes.Succes

/** Nom affiché de chaque succès. */
@StringRes
fun titreSucces(succes: Succes): Int = TITRES.getValue(succes)

private val TITRES: Map<Succes, Int> = mapOf(
    Succes.CAPTURES_10 to R.string.succes_captures_10,
    Succes.CAPTURES_50 to R.string.succes_captures_50,
    Succes.CAPTURES_100 to R.string.succes_captures_100,
    Succes.CAPTURES_200 to R.string.succes_captures_200,
    Succes.MAITRISES_1 to R.string.succes_maitrises_1,
    Succes.MAITRISES_10 to R.string.succes_maitrises_10,
    Succes.MAITRISES_50 to R.string.succes_maitrises_50,
    Succes.EVOLUTIONS_10 to R.string.succes_evolutions_10,
    Succes.EVOLUTIONS_30 to R.string.succes_evolutions_30,
    Succes.BADGES_1 to R.string.succes_badges_1,
    Succes.BADGES_4 to R.string.succes_badges_4,
    Succes.BADGES_8 to R.string.succes_badges_8,
    Succes.LIGUE to R.string.succes_ligue,
    Succes.AVENTURE_TERMINEE to R.string.succes_aventure_terminee,
    Succes.DEFIS_1 to R.string.succes_defis_1,
    Succes.DEFIS_10 to R.string.succes_defis_10,
    Succes.DEFIS_25 to R.string.succes_defis_25,
    Succes.DEFIS_50 to R.string.succes_defis_50,
    Succes.EQUIPE_COMPLETE to R.string.succes_equipe_complete,
)

/** Condition du succès (ressource au pluriel, à formater avec le seuil : « Capturer 10 espèces »). */
@PluralsRes
fun conditionSucces(succes: Succes): Int = when (succes.famille) {
    FamilleSucces.CAPTURES -> R.plurals.succes_condition_captures
    FamilleSucces.MAITRISES -> R.plurals.succes_condition_maitrises
    FamilleSucces.EVOLUTIONS -> R.plurals.succes_condition_evolutions
    FamilleSucces.BADGES -> R.plurals.succes_condition_badges
    FamilleSucces.AVENTURE ->
        if (succes == Succes.LIGUE) R.plurals.succes_condition_ligue else R.plurals.succes_condition_fin
    FamilleSucces.DEFIS -> R.plurals.succes_condition_defis
    FamilleSucces.EQUIPE -> R.plurals.succes_condition_equipe
}
