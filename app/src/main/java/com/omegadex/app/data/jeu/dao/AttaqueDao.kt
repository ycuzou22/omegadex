package com.omegadex.app.data.jeu.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import com.omegadex.app.data.jeu.entity.AttaqueEntity
import com.omegadex.app.domain.model.MethodeApprentissage
import kotlinx.coroutines.flow.Flow

/** Attaques apprises (onglet Attaques de la fiche). */
@Dao
interface AttaqueDao {
    @Query(
        """
        SELECT a.*, aa.methode AS methode, aa.niveau AS niveau, aa.numeroCt AS numeroCt
        FROM attaque_apprise aa JOIN attaque a ON a.id = aa.attaqueId
        WHERE aa.especeId = :especeId
        ORDER BY aa.methode, aa.niveau, a.nom
        """,
    )
    fun attaquesApprises(especeId: Int): Flow<List<AttaqueAppriseLigne>>
}

data class AttaqueAppriseLigne(
    @Embedded val attaque: AttaqueEntity,
    val methode: MethodeApprentissage,
    val niveau: Int,
    val numeroCt: String?,
)
