package com.omegadex.app.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omegadex.app.ui.navigation.BarreNavigationOmega
import com.omegadex.app.ui.navigation.Destination
import com.omegadex.app.ui.navigation.OmegadexNavHost

/** Squelette de l'application : barre de navigation Oméga à 5 onglets et contenu de l'onglet courant. */
@Composable
fun OmegadexApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entreeCourante by navController.currentBackStackEntryAsState()
    val destinationCourante = Destination.entries.firstOrNull { destination ->
        entreeCourante?.destination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Les écrans de détail (fiche Pokémon, fiche de lieu, Mes runs…) prennent tout l'écran,
            // comme les maquettes ; ils ont leur propre bouton retour.
            if (destinationCourante != null) {
                BarreNavigationOmega(
                    destinationCourante = destinationCourante,
                    onDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        // Les marges du Scaffold couvrent déjà les barres système : elles sont consommées pour que les écrans
        // qui gèrent eux-mêmes les marges (création de run) ne les ajoutent pas une seconde fois.
        OmegadexNavHost(
            navController = navController,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}
