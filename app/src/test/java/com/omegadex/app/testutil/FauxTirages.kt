package com.omegadex.app.testutil

import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.defi.Tirage
import com.omegadex.app.domain.repository.TiragesDefisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Tirages de roulette en mémoire, pour les tests. */
class FauxTirages : TiragesDefisRepository {
    private val etat = MutableStateFlow<Map<Pair<Long, CategorieDefi>, Tirage>>(emptyMap())

    override fun tirages(runId: Long): Flow<Map<CategorieDefi, Tirage>> =
        etat.map { tous -> tous.filterKeys { it.first == runId }.mapKeys { it.key.second } }

    override suspend fun enregistrer(runId: Long, categorie: CategorieDefi, tirage: Tirage) {
        etat.update { it + ((runId to categorie) to tirage) }
    }

    override suspend fun effacer(runId: Long, categorie: CategorieDefi) {
        etat.update { it - (runId to categorie) }
    }
}
