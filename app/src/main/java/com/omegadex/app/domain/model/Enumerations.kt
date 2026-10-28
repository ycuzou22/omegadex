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
