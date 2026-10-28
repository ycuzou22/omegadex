package com.omegadex.app.data.jeu.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.TypeDefi

/**
 * Modèle de défi, seule donnée saisie à la main (tools/import/donnees/defis.json).
 * [gabarit] contient des marqueurs ({espece}, {type}, {adversaire}, {plafond}…) remplacés par le moteur de défis ;
 * [parametres] et [conditions] sont des objets JSON décrits dans docs/modeles-defis.md.
 */
@Entity(tableName = "modele_defi", indices = [Index("etapeId"), Index("type")])
data class ModeleDefiEntity(
    @PrimaryKey val id: Int,
    val type: TypeDefi,
    val etapeId: Int?,
    val titre: String,
    val gabarit: String,
    val parametres: String,
    val difficulteMin: Difficulte,
    val etapeMin: Int,
    val conditions: String,
)
