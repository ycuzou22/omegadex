package com.omegadex.app.feature.runs

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.run.ErreurCreationRun
import com.omegadex.app.domain.run.RegleOptionnelle
import com.omegadex.app.domain.run.Starter

/** Formulaire de création de run ; [nom] null tant que le joueur n'a rien saisi (nom proposé par défaut). */
data class CreationRunUiState(
    val nom: String? = null,
    val starter: Starter? = null,
    val difficulte: Difficulte = Difficulte.NORMAL,
    val regles: Set<RegleOptionnelle> = emptySet(),
    val erreurs: Set<ErreurCreationRun> = emptySet(),
    val enCours: Boolean = false,
    val creee: Boolean = false,
)
