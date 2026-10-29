package com.omegadex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omegadex.app.feature.comptes.ConnexionScreen
import com.omegadex.app.feature.comptes.InscriptionScreen
import com.omegadex.app.feature.runs.CreationRunScreen
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son

private const val ROUTE_CONNEXION = "connexion"
private const val ROUTE_INSCRIPTION = "inscription"

/**
 * Point d'entrée de l'interface : écrans de compte tant que personne n'est connecté,
 * création de la première run si le compte n'en a pas, puis l'application.
 */
@Composable
fun OmegadexRacine(modifier: Modifier = Modifier, viewModel: RacineViewModel = hiltViewModel()) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    val sons = LocalSons.current
    var etaitDeconnecte by remember { mutableStateOf(false) }
    LaunchedEffect(etat) {
        // Connexion réussie : on quitte les écrans de compte.
        val connecte = etat != EtatRacine.Deconnecte && etat != EtatRacine.Chargement
        if (etaitDeconnecte && connecte) sons.jouer(Son.CONFIRMATION)
        if (etat != EtatRacine.Chargement) etaitDeconnecte = etat == EtatRacine.Deconnecte
    }
    when (etat) {
        // Le temps de relire la session enregistrée : simple fond, pour ne pas montrer la connexion à tort.
        EtatRacine.Chargement -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        EtatRacine.Deconnecte -> EcransDeCompte(modifier)
        // La run créée devient la run active : la racine passe d'elle-même à l'application.
        EtatRacine.SansRun -> CreationRunScreen(onCree = {}, modifier = modifier)
        EtatRacine.Pret -> OmegadexApp(modifier)
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
