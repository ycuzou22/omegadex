package com.omegadex.app.feature.equipe

/** Actions de l'écran Équipe. */
class ActionsEquipe(
    val onEspece: (Int) -> Unit,
    val onNiveau: (emplacement: Int, delta: Int) -> Unit,
    /** Ouvre le choix d'une capture pour l'emplacement : ajout s'il est libre, échange sinon. */
    val onChoisir: (emplacement: Int) -> Unit,
    val onRetirer: (emplacement: Int) -> Unit,
    /** Fait entrer une suggestion de rotation : dans l'[emplacement] donné (échange) ou le premier libre. */
    val onAjouterSuggestion: (suggestion: SuggestionVue, emplacement: Int?) -> Unit = { _, _ -> },
    /** Ouvre le PC de stockage. */
    val onPc: () -> Unit = {},
    /** Lance une évolution manuelle (pierre, bonheur, échange…) confirmée depuis le ruban de la carte. */
    val onEvoluer: (emplacement: Int, evolution: EvolutionVue) -> Unit,
)
