package com.omegadex.app.feature.fiche

import com.omegadex.app.domain.defi.DefiAffiche
import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.defi.DefisDeLaRun
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre

/** Un lieu où trouver l'espèce, avec l'étape qui le débloque ; [accessible] est null sans run. */
data class LieuFiche(val rencontre: Rencontre, val etape: Etape?, val accessible: Boolean?)

/** Un défi de l'espèce : enregistré (avec son [etat]) ou seulement proposé ([etat] null). */
data class DefiFiche(val titre: String, val enonce: String, val difficulte: Difficulte, val etat: EtatDefi?)

/** Les défis de l'espèce dans la run active, rangés comme dans l'espace Défis. */
data class DefisFiche(
    val difficulteRun: Difficulte,
    val actifs: List<DefiFiche>,
    val disponibles: List<DefiFiche>,
    val termines: List<DefiFiche>,
) {
    val vide: Boolean get() = actifs.isEmpty() && disponibles.isEmpty() && termines.isEmpty()

    companion object {
        fun de(especeId: Int, run: DefisDeLaRun, proposes: List<DefiPropose>): DefisFiche {
            fun DefiAffiche.versFiche() = DefiFiche(modele.titre, enonce, modele.difficulteMin, defi.etat)
            return DefisFiche(
                difficulteRun = run.difficulte,
                actifs = run.actifs.filter { it.defi.especeId == especeId }.map { it.versFiche() },
                disponibles = proposes.filter { it.especeId == especeId }
                    .map { DefiFiche(it.modele.titre, it.enonce, it.modele.difficulteMin, null) },
                termines = run.historique.filter { it.defi.especeId == especeId }.map { it.versFiche() },
            )
        }
    }
}

/**
 * Lieux de l'espèce triés par étape de déblocage, puis par nom ; un lieu est accessible quand son étape
 * est atteinte dans la run active ([etapeEnCours] null : pas de run, l'accessibilité reste inconnue).
 * L'échange requis n'est pas un lieu : la fiche l'annonce à part.
 */
internal fun lieuxDeLaFiche(rencontres: List<Rencontre>, etapes: List<Etape>, etapeEnCours: Int?): List<LieuFiche> {
    val parId = etapes.associateBy { it.id }
    return rencontres
        .filter { it.methode != MethodeRencontre.ECHANGE_REQUIS }
        .sortedWith(compareBy({ it.lieu.etapeDeblocageId }, { it.lieu.nom }, { it.methode }))
        .map { r ->
            LieuFiche(r, parId[r.lieu.etapeDeblocageId], etapeEnCours?.let { r.lieu.etapeDeblocageId <= it })
        }
}
