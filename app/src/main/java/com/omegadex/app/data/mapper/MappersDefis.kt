package com.omegadex.app.data.mapper

import com.omegadex.app.data.jeu.entity.ModeleDefiEntity
import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ModeleDefi
import org.json.JSONArray
import org.json.JSONObject

/** Les colonnes `parametres` (objet) et `conditions` (liste) sont stockées en JSON par l'import. */
fun ModeleDefiEntity.versDomaine() = ModeleDefi(
    id = id,
    type = type,
    etapeId = etapeId,
    titre = titre,
    gabarit = gabarit,
    parametres = JSONObject(parametres).let { objet ->
        objet.keys().asSequence().associateWith { objet.get(it).toString() }
    },
    difficulteMin = difficulteMin,
    etapeMin = etapeMin,
    conditions = JSONArray(conditions).let { liste ->
        (0 until liste.length()).map { ConditionDeblocage.valueOf(liste.getString(it)) }.toSet()
    },
)
