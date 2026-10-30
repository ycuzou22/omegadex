package com.omegadex.app.data.repository

import androidx.room.withTransaction
import com.omegadex.app.data.mapper.SEPARATEUR_REGLES
import com.omegadex.app.data.mapper.versDomaine
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import com.omegadex.app.data.progression.entity.PokemonPcEntity
import com.omegadex.app.data.progression.entity.ProgressionEspeceEntity
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.QueteCocheeEntity
import com.omegadex.app.data.progression.entity.RunEntity
import com.omegadex.app.domain.repository.SauvegardeRepository
import com.omegadex.app.domain.sauvegarde.DefiSauvegarde
import com.omegadex.app.domain.sauvegarde.EspeceSuivie
import com.omegadex.app.domain.sauvegarde.PokemonPcSauvegarde
import com.omegadex.app.domain.sauvegarde.QueteCochee
import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import javax.inject.Inject

/** Lit et restaure toute la progression d'un compte, chaque fois en une seule transaction. */
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
                pc = dao.pc(run.id).map {
                    PokemonPcSauvegarde(it.boite, it.especeId, it.surnom, it.niveau, it.nature, it.talent)
                },
            )
        }
    }

    override suspend fun restaurer(utilisateurId: Long, runs: List<RunSauvegardee>): List<Long> = base.withTransaction {
        runs.map { run -> restaurerRun(utilisateurId, run) }
    }

    private suspend fun restaurerRun(utilisateurId: Long, run: RunSauvegardee): Long {
        val runId = dao.creerRun(
            RunEntity(
                utilisateurId = utilisateurId,
                nom = run.nom,
                starterId = run.starterId,
                difficulte = run.difficulte,
                regles = run.regles.joinToString(SEPARATEUR_REGLES),
                dateDebut = run.dateDebut,
                dateFin = run.dateFin,
            ),
        )
        dao.ajouterEtapes(
            run.etapes.map {
                ProgressionEtapeEntity(runId, it.etapeId, it.deverrouillee, it.dateDeverrouillage, it.dateValidation)
            },
        )
        dao.ajouterQuetes(run.quetesCochees.map { QueteCocheeEntity(runId, it.queteId, it.dateCochage) })
        dao.ajouterEspeces(
            run.especes.map { ProgressionEspeceEntity(runId, it.especeId, it.statut, it.notes, it.dateMaj) },
        )
        dao.ajouterMembres(
            run.equipe.map {
                MembreEquipeEntity(runId, it.emplacement, it.especeId, it.surnom, it.niveau, it.nature, it.talent)
            },
        )
        dao.ajouterAuPc(
            run.pc.map {
                PokemonPcEntity(
                    runId = runId,
                    boite = it.boite,
                    especeId = it.especeId,
                    surnom = it.surnom,
                    niveau = it.niveau,
                    nature = it.nature,
                    talent = it.talent,
                )
            },
        )
        dao.ajouterDefis(
            run.defis.map {
                DefiEntity(
                    runId = runId,
                    modeleId = it.modeleId,
                    especeId = it.especeId,
                    etapeId = it.etapeId,
                    etat = it.etat,
                    dateDebut = it.dateDebut,
                    dateFin = it.dateFin,
                )
            },
        )
        return runId
    }
}
