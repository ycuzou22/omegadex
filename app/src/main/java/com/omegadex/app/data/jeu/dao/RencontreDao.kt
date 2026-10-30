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
        SELECT l.*, r.especeId AS especeId, r.methode AS methode, r.niveauMin AS niveauMin, r.niveauMax AS niveauMax,
            r.taux AS taux, r.condition AS condition
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE r.especeId = :especeId
        ORDER BY l.etapeDeblocageId, l.nom, r.methode
        """,
    )
    fun lieuxDeLEspece(especeId: Int): Flow<List<RencontreLigne>>

    @Query(
        """
        SELECT l.*, r.especeId AS especeId, r.methode AS methode, r.niveauMin AS niveauMin, r.niveauMax AS niveauMax,
            r.taux AS taux, r.condition AS condition
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE r.lieuId = :lieuId
        ORDER BY r.methode, r.especeId
        """,
    )
    fun especesDuLieu(lieuId: Int): Flow<List<RencontreLigne>>

    /** Toutes les rencontres, avec leur lieu : la disponibilité selon l'avancement se calcule dans le domaine. */
    @Query(
        """
        SELECT l.*, r.especeId AS especeId, r.methode AS methode, r.niveauMin AS niveauMin, r.niveauMax AS niveauMax,
            r.taux AS taux, r.condition AS condition
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        ORDER BY l.etapeDeblocageId, l.nom, r.methode, r.especeId
        """,
    )
    fun toutesLesRencontres(): Flow<List<RencontreLigne>>

    /** Espèces absentes de Rubis Oméga, à obtenir par échange ou transfert depuis un autre jeu. */
    @Query("SELECT DISTINCT especeId FROM rencontre WHERE methode = 'ECHANGE_REQUIS' ORDER BY especeId")
    fun especesEchangeRequis(): Flow<List<Int>>
}

data class RencontreLigne(
    @Embedded val lieu: LieuEntity,
    val especeId: Int,
    val methode: MethodeRencontre,
    val niveauMin: Int?,
    val niveauMax: Int?,
    val taux: Int?,
    val condition: String?,
)
