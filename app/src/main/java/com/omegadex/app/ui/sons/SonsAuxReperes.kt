package com.omegadex.app.ui.sons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/** Écart au-delà duquel un repère franchi d'un coup (animation passée d'un toucher) reste silencieux. */
private const val RETARD_MAX_MS = 400f

/**
 * Joue chaque son de [reperes] (instant en ms, son) quand le temps [t] d'une animation l'atteint, une seule fois.
 * Quand l'animation est passée d'un toucher, les repères sautés ne sonnent pas tous à la fois.
 */
@Composable
fun SonsAuxReperes(t: Float, reperes: List<Pair<Float, Son>>) {
    val sons = LocalSons.current
    val joues = remember { mutableSetOf<Int>() }
    LaunchedEffect(t) {
        reperes.forEachIndexed { index, (instant, son) ->
            if (t >= instant && joues.add(index) && t - instant <= RETARD_MAX_MS) sons.jouer(son)
        }
    }
}
