package com.omegadex.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** Réglages de l'appareil, communs à tous les comptes : sons, vibrations et ambiance sonore. */
data class Reglages(val sons: Boolean = true, val vibrations: Boolean = true, val ambiance: Boolean = false)

interface ReglagesRepository {
    val reglages: Flow<Reglages>

    suspend fun activerSons(actifs: Boolean)

    suspend fun activerVibrations(actives: Boolean)

    suspend fun activerAmbiance(active: Boolean)
}
