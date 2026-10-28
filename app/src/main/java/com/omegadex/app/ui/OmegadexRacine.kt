package com.omegadex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.feature.comptes.ConnexionScreen
import com.omegadex.app.feature.comptes.InscriptionScreen

private const val ROUTE_CONNEXION = "connexion"
private const val ROUTE_INSCRIPTION = "inscription"

/** Point d'entrée de l'interface : écrans de compte tant que personne n'est connecté, puis l'application. */
@Composable
fun OmegadexRacine(modifier: Modifier = Modifier, viewModel: RacineViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (session) {
        // Le temps de relire la session enregistrée : simple fond, pour ne pas montrer la connexion à tort.
        EtatSession.Chargement -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        is EtatSession.Connecte -> OmegadexApp(modifier)
        EtatSession.Deconnecte -> EcransDeCompte(modifier)
    }
}

@Composable
private fun EcransDeCompte(modifier: Modifier) {
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
