package com.omegadex.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.mapper.SEPARATEUR_REGLES
import com.omegadex.app.data.mapper.versDomaine
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.progression.entity.RunEntity
import com.omegadex.app.data.progression.entity.UtilisateurEntity
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Identifiants
import com.omegadex.app.domain.repository.CompteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class CompteRepositoryImpl @Inject constructor(base: ProgressionDatabase, private val horloge: Horloge) :
    CompteRepository {
    private val dao = base.compteDao()

    override fun utilisateurs() = dao.utilisateurs().map { liste -> liste.map { it.versDomaine() } }

    override suspend fun identifiants(identifiant: String) = dao.utilisateurParIdentifiant(identifiant.trim())?.let {
        Identifiants(it.id, it.hashMotDePasse, it.sel)
    }

    override suspend fun creerUtilisateur(identifiant: String, hashMotDePasse: String, sel: String): Long? = try {
        dao.creerUtilisateur(
            UtilisateurEntity(
                identifiant = identifiant.trim(),
                hashMotDePasse = hashMotDePasse,
                sel = sel,
                dateCreation = horloge.maintenant(),
            ),
        )
    } catch (_: SQLiteConstraintException) {
        null
    }

    override suspend fun supprimerUtilisateur(utilisateurId: Long) = dao.supprimerUtilisateur(utilisateurId)

    override fun runs(utilisateurId: Long) = dao.runs(utilisateurId).map { liste -> liste.map { it.versDomaine() } }

    override fun run(utilisateurId: Long, runId: Long) = dao.run(utilisateurId, runId).map { it?.versDomaine() }

    // Les tables de progression de la run sont supprimées en cascade (clés étrangères ON DELETE CASCADE).
    override suspend fun supprimerRun(utilisateurId: Long, runId: Long) = dao.supprimerRun(utilisateurId, runId)

    override suspend fun creerRun(
        utilisateurId: Long,
        nom: String,
        starterId: Int,
        difficulte: Difficulte,
        regles: List<String>,
    ) = dao.creerRun(
        RunEntity(
            utilisateurId = utilisateurId,
            nom = nom.trim(),
            starterId = starterId,
            difficulte = difficulte,
            regles = regles.joinToString(SEPARATEUR_REGLES),
            dateDebut = horloge.maintenant(),
        ),
    )
}
