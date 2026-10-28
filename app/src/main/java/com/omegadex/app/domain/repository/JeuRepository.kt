package com.omegadex.app.domain.repository

import com.omegadex.app.domain.model.AttaqueApprise
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.TalentEspece
import kotlinx.coroutines.flow.Flow

/** Données de Rubis Oméga (base jeu, lecture seule). */
interface JeuRepository {
    fun etapes(): Flow<List<Etape>>
    fun etape(id: Int): Flow<Etape?>
    fun quetes(etapeId: Int): Flow<List<Quete>>
    fun lieuxDebloques(etapeId: Int): Flow<List<Lieu>>
    fun lieu(id: Int): Flow<Lieu?>
    fun quetesDuLieu(lieuId: Int): Flow<List<Quete>>

    /** Modèles de défis de la base jeu (moteur de défis, T11). */
    fun modelesDefis(): Flow<List<ModeleDefi>>
    fun pokedexHoenn(): Flow<List<Espece>>
    fun pokedexNational(): Flow<List<Espece>>
    fun espece(id: Int): Flow<Espece?>
    fun talents(especeId: Int): Flow<List<TalentEspece>>
    fun famille(especeId: Int): Flow<List<Evolution>>
    fun attaquesApprises(especeId: Int): Flow<List<AttaqueApprise>>
    fun lieuxDeLEspece(especeId: Int): Flow<List<Rencontre>>
    fun especesDuLieu(lieuId: Int): Flow<List<Rencontre>>
    fun especesAccessibles(etapeId: Int): Flow<Set<Int>>

    /** Espèces qu'on ne peut obtenir que par échange ou transfert (mention « échange requis »). */
    fun especesEchangeRequis(): Flow<Set<Int>>
}
