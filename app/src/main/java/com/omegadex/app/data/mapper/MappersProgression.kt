package com.omegadex.app.data.mapper

import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import com.omegadex.app.data.progression.entity.PokemonPcEntity
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.RunEntity
import com.omegadex.app.data.progression.entity.UtilisateurEntity
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.PokemonPc
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.model.Utilisateur

/** Séparateur des règles optionnelles d'une run dans la colonne `regles`. */
const val SEPARATEUR_REGLES = ","

fun UtilisateurEntity.versDomaine() = Utilisateur(id, identifiant, dateCreation)

fun RunEntity.versDomaine() = Run(
    id = id,
    utilisateurId = utilisateurId,
    nom = nom,
    starterId = starterId,
    difficulte = difficulte,
    regles = regles.split(SEPARATEUR_REGLES).filter { it.isNotBlank() },
    dateDebut = dateDebut,
    dateFin = dateFin,
)

fun ProgressionEtapeEntity.versDomaine() = ProgressionEtape(etapeId, deverrouillee, dateDeverrouillage, dateValidation)

fun MembreEquipeEntity.versDomaine() = MembreEquipe(emplacement, especeId, surnom, niveau, nature, talent)

fun PokemonPcEntity.versDomaine() = PokemonPc(id, boite, especeId, surnom, niveau, nature, talent)

fun DefiEntity.versDomaine() = Defi(id, modeleId, especeId, etapeId, etat, dateDebut, dateFin)
