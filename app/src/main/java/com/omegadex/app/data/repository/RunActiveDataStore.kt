package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.RunActiveRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.runsDataStore: DataStore<Preferences> by preferencesDataStore(name = "runs")

/** Run active de chaque compte, enregistrée dans un Preferences DataStore (seulement des identifiants). */
@Singleton
class RunActiveDataStore internal constructor(
    private val magasin: DataStore<Preferences>,
    private val comptes: CompteRepository,
) : RunActiveRepository {
    @Inject
    constructor(@ApplicationContext context: Context, comptes: CompteRepository) :
        this(context.runsDataStore, comptes)

    override fun runActive(utilisateurId: Long): Flow<Long?> =
        combine(magasin.data.map { it[cle(utilisateurId)] }, comptes.runs(utilisateurId)) { choisie, runs ->
            // Les runs sont triées de la plus récente à la plus ancienne.
            runs.firstOrNull { it.id == choisie }?.id ?: runs.firstOrNull()?.id
        }.distinctUntilChanged()

    override suspend fun choisir(utilisateurId: Long, runId: Long) {
        magasin.edit { it[cle(utilisateurId)] = runId }
    }

    private fun cle(utilisateurId: Long) = longPreferencesKey("run_active_$utilisateurId")
}
