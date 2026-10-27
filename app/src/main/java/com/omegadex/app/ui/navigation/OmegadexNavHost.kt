package com.omegadex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.omegadex.app.feature.accueil.AccueilScreen
import com.omegadex.app.feature.aventure.AventureScreen
import com.omegadex.app.feature.defis.DefisScreen
import com.omegadex.app.feature.equipe.EquipeScreen
import com.omegadex.app.feature.pokedex.PokedexScreen

@Composable
fun OmegadexNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.ACCUEIL.route,
        modifier = modifier,
    ) {
        composable(Destination.ACCUEIL.route) { AccueilScreen() }
        composable(Destination.AVENTURE.route) { AventureScreen() }
        composable(Destination.POKEDEX.route) { PokedexScreen() }
        composable(Destination.DEFIS.route) { DefisScreen() }
        composable(Destination.EQUIPE.route) { EquipeScreen() }
    }
}
