package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.di.PorteeApplication
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Session persistante (Preferences DataStore) : le dernier joueur reste connecté après un redémarrage,
 * jusqu'à ce qu'il se déconnecte. Seul l'identifiant numérique du compte est enregistré ;
 * une session qui désigne un compte supprimé est ignorée.
 */
@Singleton
class SessionDataStore internal constructor(
    private val magasin: DataStore<Preferences>,
    comptes: CompteRepository,
    portee: CoroutineScope,
) : SessionRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        comptes: CompteRepository,
        @PorteeApplication portee: CoroutineScope,
    ) :
        this(context.sessionDataStore, comptes, portee)

    override val etat: StateFlow<EtatSession> =
        combine(magasin.data.map { it[CLE_UTILISATEUR] }, comptes.utilisateurs()) { id, utilisateurs ->
            if (id != null && utilisateurs.any { it.id == id }) EtatSession.Connecte(id) else EtatSession.Deconnecte
        }.stateIn(portee, SharingStarted.Eagerly, EtatSession.Chargement)

    override val utilisateurConnecte: StateFlow<Long?> =
        etat.map { (it as? EtatSession.Connecte)?.utilisateurId }.stateIn(portee, SharingStarted.Eagerly, null)

    override suspend fun ouvrir(utilisateurId: Long) {
        magasin.edit { it[CLE_UTILISATEUR] = utilisateurId }
    }

    override suspend fun fermer() {
        magasin.edit { it.remove(CLE_UTILISATEUR) }
    }

    private companion object {
        val CLE_UTILISATEUR = longPreferencesKey("utilisateur_connecte")
    }
}
