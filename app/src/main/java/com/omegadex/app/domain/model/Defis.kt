package com.omegadex.app.domain.model

/** Conditions de déblocage d'un modèle de défi (voir `docs/modeles-defis.md`). */
enum class ConditionDeblocage {
    ESPECE_CAPTUREE,
    ESPECE_DANS_EQUIPE,
    EVOLUTION_SOUS_PLAFOND,
    MEGA_DISPONIBLE,
    TALENT_CACHE,
    CONCOURS_OUVERTS,
    POKEDEX_NATIONAL,
}

/** Modèle de défi saisi à la main (`tools/import/donnees/defis.json`), instancié par le moteur pour chaque run. */
data class ModeleDefi(
    val id: Int,
    val type: TypeDefi,
    val etapeId: Int?,
    val titre: String,
    val gabarit: String,
    val parametres: Map<String, String>,
    val difficulteMin: Difficulte,
    val etapeMin: Int,
    val conditions: Set<ConditionDeblocage>,
)
