package com.omegadex.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.testutil.FauxComptes
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionDataStoreTest {
    @get:Rule
    val dossier = TemporaryFolder()

    private val comptes = FauxComptes()

    /**
     * Ouvre la session sur [fichier], exécute [bloc], puis libère le fichier comme à l'arrêt de l'app.
     * On attend la fin de la portée : DataStore refuse deux magasins actifs sur le même fichier.
     */
    private fun <T> demarrer(fichier: File, bloc: suspend (SessionDataStore) -> T): T {
        val portee = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val magasin = PreferenceDataStoreFactory.create(scope = portee) { fichier }
        return try {
            runBlocking { withTimeout(DELAI_MAX_MS) { bloc(SessionDataStore(magasin, comptes, portee)) } }
        } finally {
            runBlocking { portee.coroutineContext.job.cancelAndJoin() }
        }
    }

    private suspend fun SessionDataStore.etatStable() = etat.first { it != EtatSession.Chargement }

    @Test
    fun `apres un redemarrage le dernier joueur reste connecte`() {
        val fichier = File(dossier.root, "session.preferences_pb")
        val id = runBlocking { requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")) }

        demarrer(fichier) { session ->
            assertEquals(EtatSession.Deconnecte, session.etatStable())
            session.ouvrir(id)
            assertEquals(EtatSession.Connecte(id), session.etat.first { it is EtatSession.Connecte })
        }
        demarrer(fichier) { session ->
            assertEquals(EtatSession.Connecte(id), session.etatStable())
            assertEquals(id, session.utilisateurConnecte.first { it != null })
        }
    }

    @Test
    fun `la deconnexion est conservee au redemarrage`() {
        val fichier = File(dossier.root, "session.preferences_pb")
        val id = runBlocking { requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")) }
        demarrer(fichier) { session ->
            session.ouvrir(id)
            session.etat.first { it is EtatSession.Connecte }
            session.fermer()
            session.etat.first { it == EtatSession.Deconnecte }
        }
        demarrer(fichier) { session -> assertEquals(EtatSession.Deconnecte, session.etatStable()) }
    }

    @Test
    fun `une session vers un compte supprime est ignoree`() {
        val fichier = File(dossier.root, "session.preferences_pb")
        val id = runBlocking { requireNotNull(comptes.creerUtilisateur("Yamao", "h", "s")) }
        demarrer(fichier) { session -> session.ouvrir(id) }
        runBlocking { comptes.supprimerUtilisateur(id) }
        demarrer(fichier) { session -> assertEquals(EtatSession.Deconnecte, session.etatStable()) }
    }

    private companion object {
        const val DELAI_MAX_MS = 10_000L
    }
}
