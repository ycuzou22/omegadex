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
import kotlinx.coroutines.flow.flowOf

/** Données de Rubis Oméga (base jeu, lecture seule). */
interface JeuRepository {
    fun etapes(): Flow<List<Etape>>
    fun etape(id: Int): Flow<Etape?>
    fun quetes(etapeId: Int): Flow<List<Quete>>
    fun lieuxDebloques(etapeId: Int): Flow<List<Lieu>>
    fun lieu(id: Int): Flow<Lieu?>
    fun quetesDuLieu(lieuId: Int): Flow<List<Quete>>

    /** Modèles de défis saisis à la main (moteur de défis, T11), sans la banque par famille. */
    fun modelesDefis(): Flow<List<ModeleDefi>>

    /** Banque de défis de la famille de racine [racine] (voir `BanqueDefis`) ; vide si la famille n'en a pas. */
    fun banqueDefis(racine: Int): Flow<List<ModeleDefi>> = flowOf(emptyList())

    /** Modèles de défis par identifiant, banque comprise (pour réafficher les défis enregistrés). */
    fun modelesDefisParIds(ids: Collection<Int>): Flow<List<ModeleDefi>> = flowOf(emptyList())
    fun pokedexHoenn(): Flow<List<Espece>>
    fun pokedexNational(): Flow<List<Espece>>
    fun espece(id: Int): Flow<Espece?>
    fun talents(especeId: Int): Flow<List<TalentEspece>>
    fun famille(especeId: Int): Flow<List<Evolution>>
    fun attaquesApprises(especeId: Int): Flow<List<AttaqueApprise>>
    fun lieuxDeLEspece(especeId: Int): Flow<List<Rencontre>>
    fun especesDuLieu(lieuId: Int): Flow<List<Rencontre>>

    /** Toutes les rencontres du jeu, avec leur lieu. */
    fun toutesLesRencontres(): Flow<List<Rencontre>>

    /** Espèces attrapables dans Rubis Oméga à l'étape donnée : lieux atteints et méthodes utilisables. */
    fun especesAccessibles(etapeId: Int): Flow<Set<Int>>

    /** Espèces qu'on ne peut obtenir que par échange ou transfert (mention « échange requis »). */
    fun especesEchangeRequis(): Flow<Set<Int>>
}
