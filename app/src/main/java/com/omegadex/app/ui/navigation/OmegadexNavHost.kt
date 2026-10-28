package com.omegadex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omegadex.app.R
import com.omegadex.app.feature.accueil.AccueilScreen
import com.omegadex.app.feature.aventure.AventureScreen
import com.omegadex.app.feature.defis.DefisScreen
import com.omegadex.app.feature.equipe.EquipeScreen
import com.omegadex.app.feature.lieu.FicheLieuScreen
import com.omegadex.app.feature.lieu.FicheLieuViewModel
import com.omegadex.app.feature.pokedex.PokedexScreen
import com.omegadex.app.feature.runs.CreationRunScreen
import com.omegadex.app.feature.runs.MesRunsScreen
import com.omegadex.app.ui.components.EcranEnConstruction

/** Écrans hors onglets, ouverts depuis l'accueil. */
private const val ROUTE_MES_RUNS = "mes-runs"
private const val ROUTE_NOUVELLE_RUN = "nouvelle-run"
private const val ROUTE_LIEU = "lieu"
private const val ROUTE_ESPECE = "espece"

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
        composable(Destination.AVENTURE.route) {
            AventureScreen(onLieu = { navController.navigate("$ROUTE_LIEU/$it") })
        }
        composable(Destination.POKEDEX.route) { PokedexScreen() }
        composable(Destination.DEFIS.route) { DefisScreen() }
        composable(Destination.EQUIPE.route) { EquipeScreen() }
        composable(
            "$ROUTE_LIEU/{${FicheLieuViewModel.ARGUMENT_LIEU}}",
            arguments = listOf(navArgument(FicheLieuViewModel.ARGUMENT_LIEU) { type = NavType.IntType }),
        ) {
            FicheLieuScreen(
                onRetour = { navController.popBackStack() },
                onEspece = { navController.navigate("$ROUTE_ESPECE/$it") },
            )
        }
        // Fiche Pokémon : écran provisoire jusqu'à T10.
        composable("$ROUTE_ESPECE/{especeId}") {
            EcranEnConstruction(
                titre = stringResource(R.string.espece_fiche_titre),
                detail = stringResource(R.string.ecran_bientot_disponible),
            )
        }
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
