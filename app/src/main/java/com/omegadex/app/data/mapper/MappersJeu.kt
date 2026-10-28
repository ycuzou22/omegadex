package com.omegadex.app.data.mapper

import com.omegadex.app.data.jeu.dao.AttaqueAppriseLigne
import com.omegadex.app.data.jeu.dao.RencontreLigne
import com.omegadex.app.data.jeu.dao.TalentEspeceLigne
import com.omegadex.app.data.jeu.entity.AttaqueEntity
import com.omegadex.app.data.jeu.entity.EspeceEntity
import com.omegadex.app.data.jeu.entity.EtapeEntity
import com.omegadex.app.data.jeu.entity.EvolutionEntity
import com.omegadex.app.data.jeu.entity.LieuEntity
import com.omegadex.app.data.jeu.entity.QueteEntity
import com.omegadex.app.domain.model.Attaque
import com.omegadex.app.domain.model.AttaqueApprise
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.domain.model.TalentEspece

fun EtapeEntity.versDomaine() = Etape(id, titre, adversaire, typeDominant, plafond, lieuPrincipal)

fun LieuEntity.versDomaine() = Lieu(id, nom, type, etapeDeblocageId)

fun QueteEntity.versDomaine() = Quete(id, etapeId, lieuId, type, ordre, description, recompense)

fun EspeceEntity.versDomaine() = Espece(
    id = id,
    numeroHoenn = numeroHoenn,
    nom = nom,
    types = listOfNotNull(type1, type2),
    categorie = categorie,
    tailleM = tailleM,
    poidsKg = poidsKg,
    stats = Stats(pv, attaque, defense, attaqueSpeciale, defenseSpeciale, vitesse),
    evDonnes = evDonnes,
    tauxCapture = tauxCapture,
    ratioMale = ratioMale,
    groupesOeufs = groupesOeufs.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    aUneMega = aUneMega,
)

fun TalentEspeceLigne.versDomaine() = TalentEspece(nom, description, cache)

fun EvolutionEntity.versDomaine() = Evolution(especeSourceId, especeCibleId, condition, valeur)

fun AttaqueEntity.versDomaine() = Attaque(id, nom, type, categorie, puissance, precision, pp, description)

fun AttaqueAppriseLigne.versDomaine() = AttaqueApprise(attaque.versDomaine(), methode, niveau, numeroCt)

fun RencontreLigne.versDomaine() =
    Rencontre(lieu.versDomaine(), especeId, methode, niveauMin, niveauMax, taux, condition)
