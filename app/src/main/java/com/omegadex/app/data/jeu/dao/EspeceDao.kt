package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.EspeceEntity
import com.omegadex.app.data.jeu.entity.EvolutionEntity
import kotlinx.coroutines.flow.Flow

/** Espèces, talents et évolutions (écrans Pokédex et Fiche Pokémon). */
@Dao
interface EspeceDao {
    /** Pokédex régional de Hoenn, dans l'ordre du Pokédex. */
    @Query("SELECT * FROM espece WHERE numeroHoenn IS NOT NULL ORDER BY numeroHoenn")
    fun pokedexHoenn(): Flow<List<EspeceEntity>>

    /** Pokédex national, dans l'ordre national. */
    @Query("SELECT * FROM espece ORDER BY id")
    fun pokedexNational(): Flow<List<EspeceEntity>>

    @Query("SELECT * FROM espece WHERE id = :id")
    fun espece(id: Int): Flow<EspeceEntity?>

    @Query(
        """
        SELECT t.nom AS nom, t.description AS description, et.cache AS cache
        FROM espece_talent et JOIN talent t ON t.id = et.talentId
        WHERE et.especeId = :especeId ORDER BY et.cache, t.nom
        """,
    )
    fun talents(especeId: Int): Flow<List<TalentEspeceLigne>>

    /** Toutes les évolutions de la famille de l'espèce (ascendants et descendants). */
    @Query(
        """
        WITH RECURSIVE
        ancetres(id) AS (
            SELECT :especeId
            UNION SELECT e.especeSourceId FROM evolution e JOIN ancetres a ON e.especeCibleId = a.id
        ),
        racines(id) AS (
            SELECT id FROM ancetres WHERE id NOT IN (SELECT especeCibleId FROM evolution)
        ),
        descendants(id) AS (
            SELECT id FROM racines
            UNION SELECT e.especeCibleId FROM evolution e JOIN descendants d ON e.especeSourceId = d.id
        )
        SELECT * FROM evolution WHERE especeSourceId IN (SELECT id FROM descendants) ORDER BY id
        """,
    )
    fun famille(especeId: Int): Flow<List<EvolutionEntity>>
}

/** Talent d'une espèce, aplati pour la fiche. */
data class TalentEspeceLigne(val nom: String, val description: String, val cache: Boolean)
