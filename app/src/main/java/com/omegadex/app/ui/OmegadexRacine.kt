package com.omegadex.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omegadex.app.feature.comptes.ConnexionScreen
import com.omegadex.app.feature.comptes.InscriptionScreen

private const val ROUTE_CONNEXION = "connexion"
private const val ROUTE_INSCRIPTION = "inscription"

/** Point d'entrée de l'interface : écrans de compte tant que personne n'est connecté, puis l'application. */
@Composable
fun OmegadexRacine(modifier: Modifier = Modifier, viewModel: RacineViewModel = hiltViewModel()) {
    val utilisateur by viewModel.utilisateurConnecte.collectAsStateWithLifecycle()
    if (utilisateur != null) {
        OmegadexApp(modifier)
    } else {
        val navController = rememberNavController()
        NavHost(navController, startDestination = ROUTE_CONNEXION, modifier = modifier) {
            composable(ROUTE_CONNEXION) {
                ConnexionScreen(onInscription = { navController.navigate(ROUTE_INSCRIPTION) })
            }
            composable(ROUTE_INSCRIPTION) {
                InscriptionScreen(onConnexion = { navController.popBackStack() })
            }
        }
    }
}
