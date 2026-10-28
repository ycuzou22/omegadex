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
import com.omegadex.app.feature.runs.CreationRunScreen
import com.omegadex.app.feature.runs.MesRunsScreen

/** Écrans hors onglets, ouverts depuis l'accueil. */
private const val ROUTE_MES_RUNS = "mes-runs"
private const val ROUTE_NOUVELLE_RUN = "nouvelle-run"

@Composable
fun OmegadexNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Destination.ACCUEIL.route,
        modifier = modifier,
    ) {
        composable(Destination.ACCUEIL.route) {
            AccueilScreen(onMesRuns = { navController.navigate(ROUTE_MES_RUNS) })
        }
        composable(Destination.AVENTURE.route) { AventureScreen() }
        composable(Destination.POKEDEX.route) { PokedexScreen() }
        composable(Destination.DEFIS.route) { DefisScreen() }
        composable(Destination.EQUIPE.route) { EquipeScreen() }
        composable(ROUTE_MES_RUNS) {
            MesRunsScreen(
                onRetour = { navController.popBackStack() },
                onNouvelleRun = { navController.navigate(ROUTE_NOUVELLE_RUN) },
            )
        }
        composable(ROUTE_NOUVELLE_RUN) {
            // La nouvelle run devient la run active : on revient directement à l'accueil.
            CreationRunScreen(
                onCree = { navController.popBackStack(Destination.ACCUEIL.route, inclusive = false) },
                onAnnuler = { navController.popBackStack() },
            )
        }
    }
}
