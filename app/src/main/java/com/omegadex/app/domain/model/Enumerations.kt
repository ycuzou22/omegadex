package com.omegadex.app.domain.model

/** Types de lieux de Hoenn. */
/** HORS_HOENN regroupe les espèces à obtenir par échange ou transfert depuis un autre jeu. */
enum class TypeLieu { ROUTE, VILLE, GROTTE, MER, ZONE_ANNEXE, LIEU_MIRAGE, BATIMENT, HORS_HOENN }

/** Une quête principale débloque l'étape suivante ; une quête annexe est facultative. */
enum class TypeQuete { PRINCIPALE, ANNEXE }

/** Façon d'obtenir une espèce dans un lieu. */
enum class MethodeRencontre {
    HERBE,
    HORDE,
    SURF,
    PLONGEE,
    PECHE,
    NAVI_DEX,
    ROCHE_FRACASSEE,
    CADEAU,
    ECHANGE_EN_JEU,
    FIXE,
    OEUF,

    /** Espèce absente de Rubis Oméga : à obtenir par échange ou transfert. */
    ECHANGE_REQUIS,
}

/** Condition d'une évolution (la valeur précise le niveau, la pierre, l'objet ou le lieu). */
enum class ConditionEvolution { NIVEAU, PIERRE, BONHEUR, ECHANGE, OBJET, LIEU, AUTRE }

/** Catégorie d'une attaque. */
enum class CategorieAttaque { PHYSIQUE, SPECIALE, STATUT }

/** Façon dont une espèce apprend une attaque dans Rubis Oméga. */
enum class MethodeApprentissage { NIVEAU, CT_CS, REPRODUCTION, DONNEUR }

/** Difficulté choisie à la création d'une run ; chaque niveau ajoute des défis au précédent. */
enum class Difficulte { NORMAL, DIFFICILE, HARDCORE }

/**
 * Statut d'entraînement d'une espèce, dans l'ordre de progression.
 * Le rang (0 à 5) sert aux jauges et aux 5 rubis de statut.
 */
enum class StatutEspece { NON_VUE, VUE, CAPTUREE, ENTRAINEE, EVOLUEE, MAITRISEE }

/** État d'un défi d'une run. */
enum class EtatDefi { ACTIF, REUSSI, RATE }

/**
 * Types de défis du cahier des charges. [ETAPE] regroupe les défis propres à une étape
 * (défi de base et défi Difficile de chaque arène).
 */
enum class TypeDefi { ETAPE, ESPECE, EVOLUTION, ROTATION, MONOTYPE, NAVI_DEX, MEGA, CONCOURS }
