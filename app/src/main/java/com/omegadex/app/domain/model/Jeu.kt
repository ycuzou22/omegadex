package com.omegadex.app.domain.model

/** Étape de l'aventure. [plafond] : niveau du Pokémon le plus fort du champion. */
data class Etape(
    val id: Int,
    val titre: String,
    val adversaire: String,
    val typeDominant: String?,
    val plafond: Int,
    val lieuPrincipal: String,
)

data class Lieu(val id: Int, val nom: String, val type: TypeLieu, val etapeDeblocageId: Int)

data class Quete(
    val id: Int,
    val etapeId: Int,
    val lieuId: Int?,
    val type: TypeQuete,
    val ordre: Int,
    val description: String,
    val recompense: String?,
)

data class Stats(
    val pv: Int,
    val attaque: Int,
    val defense: Int,
    val attaqueSpeciale: Int,
    val defenseSpeciale: Int,
    val vitesse: Int,
) {
    val total: Int get() = pv + attaque + defense + attaqueSpeciale + defenseSpeciale + vitesse
}

data class Espece(
    val id: Int,
    val numeroHoenn: Int?,
    val nom: String,
    val types: List<String>,
    val categorie: String,
    val tailleM: Double,
    val poidsKg: Double,
    val stats: Stats,
    val evDonnes: String,
    val tauxCapture: Int,
    val ratioMale: Double?,
    val groupesOeufs: List<String>,
    val aUneMega: Boolean,
)

data class TalentEspece(val nom: String, val description: String, val cache: Boolean)

data class Evolution(
    val especeSourceId: Int,
    val especeCibleId: Int,
    val condition: ConditionEvolution,
    val valeur: String?,
)

data class Attaque(
    val id: Int,
    val nom: String,
    val type: String,
    val categorie: CategorieAttaque,
    val puissance: Int?,
    val precision: Int?,
    val pp: Int,
    val description: String,
)

data class AttaqueApprise(
    val attaque: Attaque,
    val methode: MethodeApprentissage,
    val niveau: Int,
    val numeroCt: String?,
)

data class Rencontre(
    val lieu: Lieu,
    val especeId: Int,
    val methode: MethodeRencontre,
    val niveauMin: Int?,
    val niveauMax: Int?,
    val taux: Int?,
    val condition: String?,
)
