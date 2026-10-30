package com.omegadex.app.domain.pokedex

import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Pokédex d'une run : régional de Hoenn, puis national une fois la Ligue validée. */
data class Pokedex(val national: Boolean, val entrees: List<EntreePokedex>)

/**
 * Une espèce du Pokédex et son statut dans la run ([StatutEspece.NON_VUE] si elle n'a jamais été vue :
 * l'écran l'affiche en silhouette). [echangeRequis] : absente de Rubis Oméga, à obtenir par échange.
 */
data class EntreePokedex(val espece: Espece, val statut: StatutEspece, val echangeRequis: Boolean)

/** Règle de déblocage du Pokédex national. */
object RegleNational {
    /** Étape « Ligue Pokémon » (données `tools/import/donnees/etapes_rosa.json`). */
    const val ETAPE_LIGUE = 10

    /** Le national n'apparaît qu'une fois l'étape Ligue validée, jamais avant. */
    fun debloque(etapes: List<ProgressionEtape>) = etapes.any { it.etapeId == ETAPE_LIGUE && it.dateValidation != null }
}

/** Suit le Pokédex d'une run : il bascule sur le national dès la validation de la Ligue. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserverPokedexUseCase @Inject constructor(
    private val jeu: JeuRepository,
    private val progression: ProgressionRepository,
) {
    operator fun invoke(runId: Long): Flow<Pokedex> = nationalDebloque(runId).flatMapLatest { national ->
        val especes = if (national) jeu.pokedexNational() else jeu.pokedexHoenn()
        combine(especes, progression.statuts(runId), jeu.especesEchangeRequis()) { liste, statuts, echanges ->
            Pokedex(
                national = national,
                entrees = liste.map { espece ->
                    EntreePokedex(
                        espece = espece,
                        statut = statuts[espece.id] ?: StatutEspece.NON_VUE,
                        echangeRequis = espece.id in echanges,
                    )
                },
            )
        }
    }

    private fun nationalDebloque(runId: Long) =
        progression.etapes(runId).map(RegleNational::debloque).distinctUntilChanged()
}
