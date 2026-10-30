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

data class MembreEquipe(
    val emplacement: Int,
    val especeId: Int,
    val surnom: String?,
    val niveau: Int,
    val nature: Nature? = null,
    /** Nom du talent, choisi parmi ceux de l'espèce ; null tant que le joueur ne l'a pas renseigné. */
    val talent: String? = null,
)

/** Pokémon rangé dans une [boite] du PC (numérotée à partir de 1). */
data class PokemonPc(
    val id: Long,
    val boite: Int,
    val especeId: Int,
    val surnom: String?,
    val niveau: Int,
    val nature: Nature? = null,
    val talent: String? = null,
)

/** Où se trouve un Pokémon de la run : un emplacement de l'équipe ou une place du PC. */
sealed interface PlacePokemon {
    data class Equipe(val emplacement: Int) : PlacePokemon

    data class Pc(val id: Long) : PlacePokemon
}

/** Les 25 natures, dans l'ordre du jeu ; [nom] est le nom affiché. */
enum class Nature(val nom: String) {
    HARDI("Hardi"),
    SOLO("Solo"),
    BRAVE("Brave"),
    RIGIDE("Rigide"),
    MAUVAIS("Mauvais"),
    ASSURE("Assuré"),
    DOCILE("Docile"),
    RELAX("Relax"),
    MALIN("Malin"),
    LACHE("Lâche"),
    TIMIDE("Timide"),
    PRESSE("Pressé"),
    SERIEUX("Sérieux"),
    JOVIAL("Jovial"),
    NAIF("Naïf"),
    MODESTE("Modeste"),
    DOUX("Doux"),
    DISCRET("Discret"),
    PUDIQUE("Pudique"),
    FOUFOU("Foufou"),
    CALME("Calme"),
    GENTIL("Gentil"),
    MALPOLI("Malpoli"),
    PRUDENT("Prudent"),
    BIZARRE("Bizarre"),
}

data class Defi(
    val id: Long,
    val modeleId: Int,
    val especeId: Int?,
    val etapeId: Int?,
    val etat: EtatDefi,
    val dateDebut: Long,
    val dateFin: Long?,
)
