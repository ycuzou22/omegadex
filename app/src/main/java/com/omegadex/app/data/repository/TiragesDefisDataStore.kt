package com.omegadex.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.CleDefi
import com.omegadex.app.domain.defi.Tirage
import com.omegadex.app.domain.repository.TiragesDefisRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.tiragesDefisDataStore: DataStore<Preferences> by preferencesDataStore(name = "tirages_defis")

/**
 * Tirages de la roulette dans un Preferences DataStore : une clé par run et par catégorie (« r3_ESPECE »),
 * valeur « étape;relances;modèle;espèce;étape visée » (champ vide pour une valeur absente).
 */
@Singleton
class TiragesDefisDataStore internal constructor(private val magasin: DataStore<Preferences>) :
    TiragesDefisRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.tiragesDefisDataStore)

    override fun tirages(runId: Long): Flow<Map<CategorieDefi, Tirage>> = magasin.data.map { p ->
        CategorieDefi.entries.mapNotNull { categorie -> p[cle(runId, categorie)]?.let(::lire)?.let { categorie to it } }
            .toMap()
    }.distinctUntilChanged()

    override suspend fun enregistrer(runId: Long, categorie: CategorieDefi, tirage: Tirage) {
        magasin.edit { it[cle(runId, categorie)] = ecrire(tirage) }
    }

    override suspend fun effacer(runId: Long, categorie: CategorieDefi) {
        magasin.edit { it.remove(cle(runId, categorie)) }
    }

    private fun cle(runId: Long, categorie: CategorieDefi) = stringPreferencesKey("r${runId}_${categorie.name}")

    private fun ecrire(t: Tirage) = listOf(t.etapeId, t.relances, t.defi?.modeleId, t.defi?.especeId, t.defi?.etapeId)
        .joinToString(SEPARATEUR) { it?.toString().orEmpty() }

    private fun lire(valeur: String): Tirage? {
        val champs = valeur.split(SEPARATEUR)
        if (champs.size != NOMBRE_CHAMPS) return null
        val (etape, relances, modele, espece, etapeVisee) = champs.map { it.toIntOrNull() }
        return Tirage(
            etapeId = etape ?: return null,
            defi = modele?.let { CleDefi(it, espece, etapeVisee) },
            relances = relances ?: 0,
        )
    }

    private companion object {
        const val SEPARATEUR = ";"
        const val NOMBRE_CHAMPS = 5
    }
}
