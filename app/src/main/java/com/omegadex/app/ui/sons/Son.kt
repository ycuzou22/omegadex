@file:Suppress("MagicNumber")

package com.omegadex.app.ui.sons

import androidx.annotation.RawRes
import com.omegadex.app.R

/** Vibration qui accompagne un son : motif en millisecondes (pause, vibration, pause, vibration…). */
enum class Vibration(val motif: LongArray, val amplitude: Int) {
    AUCUNE(longArrayOf(), 0),
    TIC_LEGER(longArrayOf(0, 8), 60),
    TIC(longArrayOf(0, 12), 110),
    DOUBLE_TIC(longArrayOf(0, 12, 70, 14), 140),
    GRESILLEMENT(longArrayOf(0, 10, 40, 10, 40, 10), 90),
    IMPACT(longArrayOf(0, 35), 230),
    SECOUSSE(longArrayOf(0, 60), 180),
    DOUBLE_SECOUSSE(longArrayOf(0, 45, 90, 45), 170),
    MONTEE(longArrayOf(0, 20, 60, 30, 60, 45), 160),
}

/**
 * Sons de l'app, synthétisés pour le projet (cristal, roche, lave, métal), sans aucun son repris du jeu.
 * L'ambiance se joue en boucle à part, voir [Sons.ambiance].
 */
enum class Son(@RawRes val ressource: Int, val vibration: Vibration = Vibration.AUCUNE) {
    COCHER(R.raw.cocher, Vibration.TIC_LEGER),
    DECOCHER(R.raw.decocher),
    STATUT_1(R.raw.statut_1, Vibration.TIC),
    STATUT_2(R.raw.statut_2, Vibration.TIC),
    STATUT_3(R.raw.statut_3, Vibration.TIC),
    STATUT_4(R.raw.statut_4, Vibration.TIC),
    STATUT_5(R.raw.statut_5, Vibration.DOUBLE_TIC),
    STATUT_RECULE(R.raw.statut_recule),
    CLIC(R.raw.clic),
    NIVEAU_CRAN(R.raw.niveau_cran, Vibration.TIC_LEGER),
    PLAFOND_ALERTE(R.raw.plafond_alerte, Vibration.SECOUSSE),
    CAPTURE_ECLOSION(R.raw.capture_eclosion, Vibration.TIC),
    CAPTURE_PULSE(R.raw.capture_pulse),
    CAPTURE_FISSURE(R.raw.capture_fissure, Vibration.GRESILLEMENT),
    CAPTURE_FENTE(R.raw.capture_fente, Vibration.IMPACT),
    CAPTURE_DECOUVERTE(R.raw.capture_decouverte),
    ETAPE_VALIDEE(R.raw.etape_validee, Vibration.MONTEE),
    BADGE_COUVERCLE(R.raw.badge_couvercle),
    BADGE_IMPACT(R.raw.badge_impact, Vibration.IMPACT),
    BADGE_VAPEUR(R.raw.badge_vapeur),
    EVOLUTION(R.raw.evolution, Vibration.MONTEE),
    DEFI_REUSSI(R.raw.defi_reussi, Vibration.DOUBLE_TIC),
    DEFI_ECHOUE(R.raw.defi_echoue),
    NAVIGATION(R.raw.navigation),
    CONFIRMATION(R.raw.confirmation, Vibration.TIC),
    ERREUR(R.raw.erreur, Vibration.DOUBLE_SECOUSSE),
    NATIONAL(R.raw.national, Vibration.MONTEE),
    ;

    companion object {
        /** Son du statut atteint en avançant : un rubis de plus, une note plus aiguë. */
        fun statut(rang: Int): Son = when (rang) {
            1 -> STATUT_1
            2 -> STATUT_2
            3 -> STATUT_3
            4 -> STATUT_4
            else -> STATUT_5
        }
    }
}
