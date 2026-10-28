package com.omegadex.app.domain.model

/** Types de lieux de Hoenn. */
enum class TypeLieu { ROUTE, VILLE, GROTTE, MER, ZONE_ANNEXE, LIEU_MIRAGE, BATIMENT }

/** Une quête principale débloque l'étape suivante ; une quête annexe est facultative. */
enum class TypeQuete { PRINCIPALE, ANNEXE }

/** Façon d'obtenir une espèce dans un lieu. */
enum class MethodeRencontre { HERBE, SURF, PECHE, NAVI_DEX, CADEAU, FIXE, ECHANGE_REQUIS, ROCHE_FRACASSEE, OEUF }

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
