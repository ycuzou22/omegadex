package com.omegadex.app.domain.repository

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Identifiants
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.model.Utilisateur
import kotlinx.coroutines.flow.Flow

/** Comptes locaux et runs. Le calcul de l'empreinte du mot de passe se fait dans le domaine (T06). */
interface CompteRepository {
    fun utilisateurs(): Flow<List<Utilisateur>>
    suspend fun identifiants(identifiant: String): Identifiants?

    /** Retourne l'id du compte créé, ou null si l'identifiant est déjà pris. */
    suspend fun creerUtilisateur(identifiant: String, hashMotDePasse: String, sel: String): Long?
    suspend fun supprimerUtilisateur(utilisateurId: Long)
    fun runs(utilisateurId: Long): Flow<List<Run>>
    fun run(utilisateurId: Long, runId: Long): Flow<Run?>

    /** Supprime la run du compte et toute sa progression (quêtes, statuts, équipe, défis). */
    suspend fun supprimerRun(utilisateurId: Long, runId: Long)
    suspend fun creerRun(
        utilisateurId: Long,
        nom: String,
        starterId: Int,
        difficulte: Difficulte,
        regles: List<String>,
    ): Long
}
