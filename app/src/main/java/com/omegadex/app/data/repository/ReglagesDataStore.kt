package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.domain.repository.Reglages
import com.omegadex.app.domain.repository.ReglagesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.reglagesDataStore: DataStore<Preferences> by preferencesDataStore(name = "reglages")

/** Réglages sons, vibrations et ambiance, dans un Preferences DataStore. */
@Singleton
class ReglagesDataStore internal constructor(private val magasin: DataStore<Preferences>) : ReglagesRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.reglagesDataStore)

    private val parDefaut = Reglages()

    override val reglages: Flow<Reglages> = magasin.data.map {
        Reglages(
            sons = it[SONS] ?: parDefaut.sons,
            vibrations = it[VIBRATIONS] ?: parDefaut.vibrations,
            ambiance = it[AMBIANCE] ?: parDefaut.ambiance,
        )
    }.distinctUntilChanged()

    override suspend fun activerSons(actifs: Boolean) {
        magasin.edit { it[SONS] = actifs }
    }

    override suspend fun activerVibrations(actives: Boolean) {
        magasin.edit { it[VIBRATIONS] = actives }
    }

    override suspend fun activerAmbiance(active: Boolean) {
        magasin.edit { it[AMBIANCE] = active }
    }

    private companion object {
        val SONS = booleanPreferencesKey("sons")
        val VIBRATIONS = booleanPreferencesKey("vibrations")
        val AMBIANCE = booleanPreferencesKey("ambiance")
    }
}
