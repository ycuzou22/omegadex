package com.omegadex.app.domain.model

data class Utilisateur(val id: Long, val identifiant: String, val dateCreation: Long)

/** Empreinte stockée d'un compte, utilisée uniquement pour vérifier le mot de passe. */
data class Identifiants(val utilisateurId: Long, val hashMotDePasse: String, val sel: String)

data class Run(
    val id: Long,
    val utilisateurId: Long,
    val nom: String,
    val starterId: Int,
    val difficulte: Difficulte,
    val regles: List<String>,
    val dateDebut: Long,
    val dateFin: Long?,
)

data class ProgressionEtape(
    val etapeId: Int,
    val deverrouillee: Boolean,
    val dateDeverrouillage: Long?,
    val dateValidation: Long?,
)

data class MembreEquipe(val emplacement: Int, val especeId: Int, val surnom: String?, val niveau: Int)

data class Defi(
    val id: Long,
    val modeleId: Int,
    val especeId: Int?,
    val etapeId: Int?,
    val etat: EtatDefi,
    val dateDebut: Long,
    val dateFin: Long?,
)
