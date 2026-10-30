package com.omegadex.app.data.progression.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import com.omegadex.app.data.progression.entity.ProgressionEspeceEntity
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.QueteCocheeEntity
import com.omegadex.app.data.progression.entity.RunEntity

/** Lectures et écritures en bloc de toute la progression d'un compte (export et import de sauvegarde). */
@Dao
interface SauvegardeDao {
    @Query("SELECT * FROM run WHERE utilisateurId = :utilisateurId ORDER BY dateDebut, id")
    suspend fun runs(utilisateurId: Long): List<RunEntity>

    @Query("SELECT * FROM progression_etape WHERE runId = :runId ORDER BY etapeId")
    suspend fun etapes(runId: Long): List<ProgressionEtapeEntity>

    @Query("SELECT * FROM quete_cochee WHERE runId = :runId ORDER BY queteId")
    suspend fun quetesCochees(runId: Long): List<QueteCocheeEntity>

    @Query("SELECT * FROM progression_espece WHERE runId = :runId ORDER BY especeId")
    suspend fun especes(runId: Long): List<ProgressionEspeceEntity>

    @Query("SELECT * FROM membre_equipe WHERE runId = :runId ORDER BY emplacement")
    suspend fun equipe(runId: Long): List<MembreEquipeEntity>

    @Query("SELECT * FROM defi WHERE runId = :runId ORDER BY id")
    suspend fun defis(runId: Long): List<DefiEntity>

    @Insert
    suspend fun creerRun(run: RunEntity): Long

    /** Les insertions d'import échouent sur un doublon plutôt que de remplacer une ligne en silence. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun ajouterEtapes(etapes: List<ProgressionEtapeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun ajouterQuetes(quetes: List<QueteCocheeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun ajouterEspeces(especes: List<ProgressionEspeceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun ajouterMembres(membres: List<MembreEquipeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun ajouterDefis(defis: List<DefiEntity>)
}
