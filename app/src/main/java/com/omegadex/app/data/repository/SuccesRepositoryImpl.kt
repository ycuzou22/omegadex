package com.omegadex.app.data.repository

import androidx.room.withTransaction
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.progression.entity.SuccesEntity
import com.omegadex.app.domain.repository.SessionRepository
import com.omegadex.app.domain.repository.SuccesRepository
import com.omegadex.app.domain.succes.Succes
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Succès d'une run, filtrés comme toute la progression par le joueur connecté. */
@OptIn(ExperimentalCoroutinesApi::class)
class SuccesRepositoryImpl @Inject constructor(
    private val base: ProgressionDatabase,
    private val horloge: Horloge,
    private val session: SessionRepository,
) : SuccesRepository {
    private val comptes = base.compteDao()
    private val dao = base.succesDao()

    override fun succes(runId: Long): Flow<Map<Succes, Long>> = session.utilisateurConnecte.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            comptes.runAppartient(id, runId).flatMapLatest { ok -> if (ok) dao.succes(runId) else flowOf(emptyList()) }
        }
    }.map { liste ->
        // Un identifiant inconnu (succès retiré par une version plus récente) est ignoré.
        liste.mapNotNull { e ->
            Succes.entries.firstOrNull { it.name == e.succesId }?.let { it to e.dateDeblocage }
        }.toMap()
    }

    override suspend fun debloquer(runId: Long, atteints: Set<Succes>): List<Succes> = base.withTransaction {
        val id = session.utilisateurConnecte.value ?: throw AccesRunRefuse(runId)
        if (!comptes.runAppartient(id, runId).first()) throw AccesRunRefuse(runId)
        val maintenant = horloge.maintenant()
        atteints.filter { dao.debloquer(SuccesEntity(runId, it.name, maintenant)) != -1L }
    }
}
