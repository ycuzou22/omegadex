package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.LieuEntity
import com.omegadex.app.domain.model.MethodeRencontre
import kotlinx.coroutines.flow.Flow

/** Rencontres : où trouver une espèce (fiche) et qui trouver dans un lieu (fiche de lieu). */
@Dao
interface RencontreDao {
    @Query(
        """
        SELECT l.*, r.especeId AS especeId, r.methode AS methode, r.niveauMin AS niveauMin, r.niveauMax AS niveauMax
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE r.especeId = :especeId
        ORDER BY l.etapeDeblocageId, l.nom, r.methode
        """,
    )
    fun lieuxDeLEspece(especeId: Int): Flow<List<RencontreLigne>>

    @Query(
        """
        SELECT l.*, r.especeId AS especeId, r.methode AS methode, r.niveauMin AS niveauMin, r.niveauMax AS niveauMax
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE r.lieuId = :lieuId
        ORDER BY r.methode, r.especeId
        """,
    )
    fun especesDuLieu(lieuId: Int): Flow<List<RencontreLigne>>

    /** Espèces trouvables dans les lieux débloqués jusqu'à l'étape donnée (mises en avant dans le Pokédex). */
    @Query(
        """
        SELECT DISTINCT r.especeId FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE l.etapeDeblocageId <= :etapeId
        """,
    )
    fun especesAccessibles(etapeId: Int): Flow<List<Int>>
}

data class RencontreLigne(
    @Embedded val lieu: LieuEntity,
    val especeId: Int,
    val methode: MethodeRencontre,
    val niveauMin: Int?,
    val niveauMax: Int?,
)
