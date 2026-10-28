package com.omegadex.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.omegadex.app.R

/** Onglets de la barre de navigation principale, dans l'ordre d'affichage. */
enum class Destination(val route: String, @param:StringRes val libelle: Int, @param:DrawableRes val icone: Int) {
    ACCUEIL("accueil", R.string.nav_accueil, R.drawable.ic_nav_accueil),
    AVENTURE("aventure", R.string.nav_aventure, R.drawable.ic_nav_aventure),
    POKEDEX("pokedex", R.string.nav_pokedex, R.drawable.ic_gem),
    DEFIS("defis", R.string.nav_defis, R.drawable.ic_nav_defis),
    EQUIPE("equipe", R.string.nav_equipe, R.drawable.ic_nav_equipe),
}
