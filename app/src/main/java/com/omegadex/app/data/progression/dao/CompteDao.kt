package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omegadex.app.data.progression.entity.RunEntity
import com.omegadex.app.data.progression.entity.UtilisateurEntity
import kotlinx.coroutines.flow.Flow

/** Comptes locaux et runs (écrans Connexion, Inscription, création et choix de run). */
@Dao
interface CompteDao {
    @Query("SELECT * FROM utilisateur ORDER BY identifiant COLLATE NOCASE")
    fun utilisateurs(): Flow<List<UtilisateurEntity>>

    @Query("SELECT * FROM utilisateur WHERE identifiant = :identifiant COLLATE NOCASE")
    suspend fun utilisateurParIdentifiant(identifiant: String): UtilisateurEntity?

    /** Échoue si l'identifiant existe déjà (index unique). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun creerUtilisateur(utilisateur: UtilisateurEntity): Long

    @Query("DELETE FROM utilisateur WHERE id = :id")
    suspend fun supprimerUtilisateur(id: Long)

    @Query("SELECT * FROM run WHERE utilisateurId = :utilisateurId ORDER BY dateDebut DESC")
    fun runs(utilisateurId: Long): Flow<List<RunEntity>>

    /** Une run n'est lue que pour son propriétaire : l'utilisateur fait partie du filtre. */
    @Query("SELECT * FROM run WHERE id = :runId AND utilisateurId = :utilisateurId")
    fun run(utilisateurId: Long, runId: Long): Flow<RunEntity?>

    @Insert
    suspend fun creerRun(run: RunEntity): Long

    @Query("DELETE FROM run WHERE id = :runId AND utilisateurId = :utilisateurId")
    suspend fun supprimerRun(utilisateurId: Long, runId: Long)
}
