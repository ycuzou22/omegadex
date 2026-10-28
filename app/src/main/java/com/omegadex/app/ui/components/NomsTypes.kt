package com.omegadex.app.ui.components

import androidx.annotation.StringRes
import com.omegadex.app.R

private val NOMS_TYPES = mapOf(
    "normal" to R.string.type_normal,
    "feu" to R.string.type_feu,
    "eau" to R.string.type_eau,
    "plante" to R.string.type_plante,
    "electrik" to R.string.type_electrik,
    "glace" to R.string.type_glace,
    "combat" to R.string.type_combat,
    "poison" to R.string.type_poison,
    "sol" to R.string.type_sol,
    "vol" to R.string.type_vol,
    "psy" to R.string.type_psy,
    "insecte" to R.string.type_insecte,
    "roche" to R.string.type_roche,
    "spectre" to R.string.type_spectre,
    "dragon" to R.string.type_dragon,
    "tenebres" to R.string.type_tenebres,
    "acier" to R.string.type_acier,
    "fee" to R.string.type_fee,
)

/** Nom français d'un type à partir de sa clé (« electrik » → « Électrik »), ou null si la clé est inconnue. */
@StringRes
fun nomDuType(type: String): Int? = NOMS_TYPES[type.lowercase()]
