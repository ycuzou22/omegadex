package com.omegadex.app.domain.compte

import com.omegadex.app.core.Horloge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Limite les essais de mot de passe : après [ESSAIS_LIBRES] échecs sur un identifiant,
 * chaque nouvel échec impose une attente qui double (30 s, 60 s, 120 s… jusqu'à 15 min).
 */
@Singleton
class LimiteurTentatives @Inject constructor(private val horloge: Horloge) {
    private data class Etat(val echecs: Int, val bloqueJusqua: Long)

    private val etats = mutableMapOf<String, Etat>()

    /** Secondes d'attente restantes avant un nouvel essai (0 si autorisé). */
    @Synchronized
    fun attenteRestante(identifiant: String): Long {
        val etat = etats[cle(identifiant)] ?: return 0
        return ((etat.bloqueJusqua - horloge.maintenant()).coerceAtLeast(0) + MS_PAR_S - 1) / MS_PAR_S
    }

    @Synchronized
    fun enregistrerEchec(identifiant: String) {
        val echecs = (etats[cle(identifiant)]?.echecs ?: 0) + 1
        val attente = if (echecs <= ESSAIS_LIBRES) {
            0L
        } else {
            (
                ATTENTE_INITIALE_MS shl (echecs - ESSAIS_LIBRES - 1).coerceAtMost(
                    DOUBLEMENTS_MAX,
                )
                ).coerceAtMost(ATTENTE_MAX_MS)
        }
        etats[cle(identifiant)] = Etat(echecs, horloge.maintenant() + attente)
    }

    @Synchronized
    fun enregistrerSucces(identifiant: String) {
        etats.remove(cle(identifiant))
    }

    private fun cle(identifiant: String) = identifiant.trim().lowercase()

    companion object {
        const val ESSAIS_LIBRES = 5
        private const val MS_PAR_S = 1_000L
        private const val ATTENTE_INITIALE_MS = 30_000L
        private const val ATTENTE_MAX_MS = 15 * 60_000L
        private const val DOUBLEMENTS_MAX = 5
    }
}
