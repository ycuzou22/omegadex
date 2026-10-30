package com.omegadex.app.ui.sons

import androidx.compose.runtime.staticCompositionLocalOf

/** Joue les sons et vibrations de l'app ; les écrans l'obtiennent par [LocalSons]. */
interface Sons {
    fun jouer(son: Son)

    /** Démarre ou arrête l'ambiance de l'écran Aventure (si le réglage l'autorise). */
    fun ambiance(active: Boolean)
}

/** Aucun son : aperçus, tests et écrans hors de l'activité. */
object SonsMuets : Sons {
    override fun jouer(son: Son) = Unit

    override fun ambiance(active: Boolean) = Unit
}

val LocalSons = staticCompositionLocalOf<Sons> { SonsMuets }
