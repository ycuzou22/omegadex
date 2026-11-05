package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.MethodeFiltre
import com.omegadex.app.domain.repository.FiltresMemorises
import com.omegadex.app.domain.repository.FiltresPokedexRepository
import com.omegadex.app.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.filtresPokedexDataStore: DataStore<Preferences> by preferencesDataStore(name = "filtres_pokedex")

/** Filtres du Pokédex dans un Preferences DataStore, une série de clés par compte (« u12_type »…). */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FiltresPokedexDataStore internal constructor(
    private val magasin: DataStore<Preferences>,
    private val session: SessionRepository,
) : FiltresPokedexRepository {
    @Inject
    constructor(@ApplicationContext context: Context, session: SessionRepository) :
        this(context.filtresPokedexDataStore, session)

    override val filtres: Flow<FiltresMemorises> = session.utilisateurConnecte.flatMapLatest { id ->
        if (id == null) {
            flowOf(FiltresMemorises())
        } else {
            val cles = Cles(id)
            magasin.data.map { p ->
                FiltresMemorises(
                    statuts = p[cles.statuts].orEmpty()
                        .mapNotNull { nom -> StatutEspece.entries.firstOrNull { it.name == nom } }
                        .toSet(),
                    type = p[cles.type],
                    zoneId = p[cles.zone],
                    methode = p[cles.methode]?.let { nom -> MethodeFiltre.entries.firstOrNull { it.name == nom } },
                )
            }
        }
    }.distinctUntilChanged()

    override suspend fun enregistrer(filtres: FiltresMemorises) {
        val id = session.utilisateurConnecte.value ?: return
        val cles = Cles(id)
        magasin.edit { p ->
            p[cles.statuts] = filtres.statuts.map { it.name }.toSet()
            if (filtres.type == null) p.remove(cles.type) else p[cles.type] = filtres.type
            if (filtres.zoneId == null) p.remove(cles.zone) else p[cles.zone] = filtres.zoneId
            if (filtres.methode == null) p.remove(cles.methode) else p[cles.methode] = filtres.methode.name
        }
    }

    private class Cles(utilisateurId: Long) {
        val statuts = stringSetPreferencesKey("u${utilisateurId}_statuts")
        val type = stringPreferencesKey("u${utilisateurId}_type")
        val zone = intPreferencesKey("u${utilisateurId}_zone")
        val methode = stringPreferencesKey("u${utilisateurId}_methode")
    }
}
