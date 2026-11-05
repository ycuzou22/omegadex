package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.domain.repository.DresseursBattusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dresseursBattusDataStore: DataStore<Preferences> by preferencesDataStore(name = "dresseurs_battus")

/** Dresseurs battus dans un Preferences DataStore : un ensemble de clés par run (« r3_battus »). */
@Singleton
class DresseursBattusDataStore internal constructor(private val magasin: DataStore<Preferences>) :
    DresseursBattusRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.dresseursBattusDataStore)

    override fun battus(runId: Long): Flow<Set<String>> =
        magasin.data.map { it[cle(runId)].orEmpty() }.distinctUntilChanged()

    override suspend fun marquer(runId: Long, cle: String, battu: Boolean) {
        magasin.edit { p ->
            val actuels = p[cle(runId)].orEmpty()
            p[cle(runId)] = if (battu) actuels + cle else actuels - cle
        }
    }

    private fun cle(runId: Long) = stringSetPreferencesKey("r${runId}_battus")
}
