package com.omegadex.app.feature.runs

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.run.RegleOptionnelle
import com.omegadex.app.domain.run.Starter

/** Actions du formulaire, regroupées pour garder des signatures courtes. */
class ActionsCreationRun(
    val onNom: (String) -> Unit,
    val onStarter: (Starter) -> Unit,
    val onDifficulte: (Difficulte) -> Unit,
    val onRegle: (RegleOptionnelle, Boolean) -> Unit,
    val onCreer: () -> Unit,
    val onAnnuler: (() -> Unit)?,
)
