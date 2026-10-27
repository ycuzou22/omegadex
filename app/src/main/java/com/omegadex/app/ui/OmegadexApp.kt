package com.omegadex.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omegadex.app.ui.navigation.Destination
import com.omegadex.app.ui.navigation.OmegadexNavHost

/** Squelette de l'application : barre de navigation à 5 onglets et contenu de l'onglet courant. */
@Composable
fun OmegadexApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entreeCourante by navController.currentBackStackEntryAsState()
    val destinationCourante = entreeCourante?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selectionne = destinationCourante?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selectionne,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icone, contentDescription = null) },
                        label = { Text(stringResource(destination.libelle)) },
                    )
                }
            }
        },
    ) { padding ->
        OmegadexNavHost(navController = navController, modifier = Modifier.padding(padding))
    }
}
