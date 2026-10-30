package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.espece.TableDesTypes
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.EntreePokedex
import com.omegadex.app.domain.pokedex.ObserverPokedexUseCase
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Face au type dominant de l'adversaire : l'espèce le frappe fort, lui résiste, ou le craint. */
enum class Affinite { AVANTAGE, RESISTANCE, NEUTRE, FAIBLESSE }

/**
 * Espèce capturée mais jamais entraînée, proposée pour entrer dans l'équipe, au niveau [niveau]
 * (jamais au-dessus du plafond de l'étape).
 */
data class SuggestionRotation(val espece: Espece, val affinite: Affinite, val niveau: Int)

/**
 * Suggestions de rotation (F10) : faire jouer les espèces capturées qui n'ont encore jamais servi.
 *
 * Une espèce « jamais utilisée » est au statut [StatutEspece.CAPTUREE] (pas encore entraînée, évoluée
 * ni maîtrisée) et n'est pas dans l'équipe. Les espèces qui ont l'avantage sur le type dominant de
 * l'adversaire passent en premier, puis celles qui lui résistent ; celles qui le craignent passent en dernier.
 * À affinité égale, la plus robuste (total des statistiques de base) d'abord.
 */
object SuggestionsRotation {
    const val LIMITE = 3

    private val ORDRE = compareBy<SuggestionRotation> { it.affinite.ordinal }.thenByDescending { it.espece.stats.total }

    fun calculer(
        entrees: List<EntreePokedex>,
        equipe: List<MembreEquipe>,
        etape: Etape,
        limite: Int = LIMITE,
    ): List<SuggestionRotation> {
        val dansLEquipe = equipe.map { it.especeId }.toSet()
        return entrees
            .filter { it.statut == StatutEspece.CAPTUREE && it.espece.id !in dansLEquipe }
            .map { SuggestionRotation(it.espece, affinite(it.espece.types, etape.typeDominant), etape.plafond) }
            .sortedWith(ORDRE)
            .take(limite)
    }

    /** Affinité d'une espèce de types [types] face au type [adverse] (null : pas de type dominant). */
    fun affinite(types: List<String>, adverse: String?): Affinite {
        if (adverse == null) return Affinite.NEUTRE
        val frappe = types.maxOfOrNull { TableDesTypes.multiplicateur(it, adverse) } ?: 1f
        val recoit = TableDesTypes.efficacites(types)[adverse] ?: 1f
        return when {
            frappe > 1f && recoit <= 1f -> Affinite.AVANTAGE
            recoit > 1f -> Affinite.FAIBLESSE
            recoit < 1f -> Affinite.RESISTANCE
            else -> Affinite.NEUTRE
        }
    }
}

/** Suggestions de rotation de la run pour l'étape en cours, mises à jour à chaque capture ou changement d'équipe. */
class ObserverSuggestionsRotationUseCase @Inject constructor(
    private val pokedex: ObserverPokedexUseCase,
    private val progression: ProgressionRepository,
) {
    operator fun invoke(runId: Long, etape: Etape): Flow<List<SuggestionRotation>> =
        combine(pokedex(runId), progression.equipe(runId)) { dex, equipe ->
            SuggestionsRotation.calculer(dex.entrees, equipe, etape)
        }
}
