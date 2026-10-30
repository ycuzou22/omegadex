package com.omegadex.app.feature.equipe

/** Actions de l'écran Équipe. */
class ActionsEquipe(
    val onEspece: (Int) -> Unit,
    val onNiveau: (emplacement: Int, delta: Int) -> Unit,
    /** Ouvre le choix d'une capture pour l'emplacement : ajout s'il est libre, échange sinon. */
    val onChoisir: (emplacement: Int) -> Unit,
    val onRetirer: (emplacement: Int) -> Unit,
    /** Ouvre le PC de stockage. */
    val onPc: () -> Unit = {},
)
