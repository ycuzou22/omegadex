package com.omegadex.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.omegadex.app.R

/**
 * Onglets de la barre de navigation principale, dans l'ordre d'affichage.
 * Les icônes Material sont provisoires : les icônes Ω et gemme arrivent avec T02.3.
 */
enum class Destination(val route: String, @param:StringRes val libelle: Int, val icone: ImageVector) {
    ACCUEIL("accueil", R.string.nav_accueil, Icons.Filled.Home),
    AVENTURE("aventure", R.string.nav_aventure, Icons.Filled.Place),
    POKEDEX("pokedex", R.string.nav_pokedex, Icons.AutoMirrored.Filled.List),
    DEFIS("defis", R.string.nav_defis, Icons.Filled.Star),
    EQUIPE("equipe", R.string.nav_equipe, Icons.Filled.Person),
}
