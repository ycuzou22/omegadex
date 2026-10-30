package com.omegadex.app.domain.rencontre

import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.repository.JeuRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Rencontres débloquées à une étape, groupées par lieu et par ce qui les débloque : un nouveau lieu donne
 * un groupe sans [debloqueur] ; une méthode qui devient utilisable sur un lieu déjà atteint (Surf, canne…)
 * donne un groupe à part, annoncé par son [debloqueur]. Une espèce figure une fois par groupe.
 */
data class GroupeRencontres(val lieu: Lieu, val debloqueur: Debloqueur?, val rencontres: List<RencontreDisponible>)

data class RencontreDisponible(val rencontre: Rencontre, val echange: Echange?)

class RencontresDeLEtapeUseCase @Inject constructor(private val jeu: JeuRepository) {
    /** [starterId] : starter choisi pour la run ; les autres « Pokémon de départ » ne sont pas proposés. */
    operator fun invoke(etape: Int, starterId: Int? = null): Flow<List<GroupeRencontres>> =
        jeu.toutesLesRencontres().map { rencontres -> grouper(rencontres, etape, starterId) }

    companion object {
        fun grouper(rencontres: List<Rencontre>, etape: Int, starterId: Int? = null): List<GroupeRencontres> =
            rencontres
                .filterNot { DisponibiliteRencontre.estUnAutreStarter(it, starterId) }
                .map { it to DisponibiliteRencontre.de(it) }
                .filter { (_, dispo) -> dispo.etape == etape }
                .groupBy { (rencontre, dispo) -> rencontre.lieu to dispo.debloqueur }
                .map { (cle, liste) ->
                    GroupeRencontres(
                        lieu = cle.first,
                        debloqueur = cle.second,
                        rencontres = liste
                            .distinctBy { (rencontre, _) -> rencontre.especeId }
                            .map { (rencontre, dispo) -> RencontreDisponible(rencontre, dispo.echange) },
                    )
                }
                .sortedWith(compareBy({ it.debloqueur != null }, { it.debloqueur?.ordinal ?: -1 }, { it.lieu.nom }))
    }
}
