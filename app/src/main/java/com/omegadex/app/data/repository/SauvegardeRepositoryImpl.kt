package com.omegadex.app.data.repository

import androidx.room.withTransaction
import com.omegadex.app.data.mapper.SEPARATEUR_REGLES
import com.omegadex.app.data.mapper.versDomaine
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.domain.repository.SauvegardeRepository
import com.omegadex.app.domain.sauvegarde.DefiSauvegarde
import com.omegadex.app.domain.sauvegarde.EspeceSuivie
import com.omegadex.app.domain.sauvegarde.QueteCochee
import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import javax.inject.Inject

/** Lit toute la progression d'un compte en une transaction, pour un export cohérent. */
class SauvegardeRepositoryImpl @Inject constructor(private val base: ProgressionDatabase) : SauvegardeRepository {
    private val dao = base.sauvegardeDao()

    override suspend fun runs(utilisateurId: Long): List<RunSauvegardee> = base.withTransaction {
        dao.runs(utilisateurId).map { run ->
            RunSauvegardee(
                nom = run.nom,
                starterId = run.starterId,
                difficulte = run.difficulte,
                regles = run.regles.split(SEPARATEUR_REGLES).filter { it.isNotBlank() },
                dateDebut = run.dateDebut,
                dateFin = run.dateFin,
                etapes = dao.etapes(run.id).map { it.versDomaine() },
                quetesCochees = dao.quetesCochees(run.id).map { QueteCochee(it.queteId, it.dateCochage) },
                especes = dao.especes(run.id).map { EspeceSuivie(it.especeId, it.statut, it.notes, it.dateMaj) },
                equipe = dao.equipe(run.id).map { it.versDomaine() },
                defis = dao.defis(run.id).map {
                    DefiSauvegarde(it.modeleId, it.especeId, it.etapeId, it.etat, it.dateDebut, it.dateFin)
                },
            )
        }
    }
}
