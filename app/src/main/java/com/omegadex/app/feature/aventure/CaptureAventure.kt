package com.omegadex.app.feature.aventure

import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.Pokedex
import com.omegadex.app.ui.animations.CaptureAnimee
import com.omegadex.app.ui.animations.CaseCapture

/** Colonnes de la grille du Pokédex sur un téléphone, et lignes montrées par l'animation de capture. */
private const val COLONNES = 3
private const val LIGNES = 3

/**
 * Prépare l'animation de capture de [especeId] à partir du Pokédex lu juste avant la capture : la ligne de la
 * grille où l'espèce s'insère, entourée de la ligne d'avant et de celle d'après quand elles existent.
 * Null si l'espèce n'est pas dans le Pokédex affiché (espèce hors de Hoenn avant le national).
 */
internal fun captureAnimee(pokedex: Pokedex, especeId: Int): CaptureAnimee? {
    val index = pokedex.entrees.indexOfFirst { it.espece.id == especeId }
    if (index < 0) return null
    val derniereLigne = (pokedex.entrees.size - 1) / COLONNES
    val premiereLigne = (index / COLONNES - 1).coerceIn(0, (derniereLigne - LIGNES + 1).coerceAtLeast(0))
    val debut = premiereLigne * COLONNES
    val fin = minOf(debut + COLONNES * LIGNES, pokedex.entrees.size)
    val cases = pokedex.entrees.subList(debut, fin).map { entree ->
        val espece = entree.espece
        CaseCapture(
            especeId = espece.id,
            numero = if (pokedex.national) espece.id else espece.numeroHoenn ?: espece.id,
            nom = espece.nom,
            types = espece.types,
            avant = entree.statut,
            apres = if (espece.id == especeId) StatutEspece.CAPTUREE else entree.statut,
        )
    }
    return CaptureAnimee(
        especeId = especeId,
        nom = pokedex.entrees[index].espece.nom,
        national = pokedex.national,
        cases = cases,
        capturees = pokedex.entrees.count { it.statut >= StatutEspece.CAPTUREE },
        total = pokedex.entrees.size,
    )
}
