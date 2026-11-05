package com.omegadex.app.domain.repository

import com.omegadex.app.domain.dresseur.Dresseur
import kotlinx.coroutines.flow.Flow

/** Dresseurs du jeu, étape par étape (données embarquées, en lecture seule). */
interface DresseursRepository {
    fun dresseurs(): Flow<List<Dresseur>>
}
