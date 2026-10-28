package com.omegadex.app.core

import javax.inject.Inject

/** Source de l'heure courante (millisecondes depuis l'époque Unix), remplaçable dans les tests. */
fun interface Horloge {
    fun maintenant(): Long
}

class HorlogeSysteme @Inject constructor() : Horloge {
    override fun maintenant(): Long = System.currentTimeMillis()
}
