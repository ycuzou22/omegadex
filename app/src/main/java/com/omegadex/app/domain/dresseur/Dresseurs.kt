package com.omegadex.app.domain.dresseur

import com.omegadex.app.domain.equipe.Affinite
import com.omegadex.app.domain.equipe.SuggestionsRotation
import com.omegadex.app.domain.espece.TableDesTypes
import com.omegadex.app.domain.model.Espece

/** Pokémon de l'équipe d'un dresseur, avec ses attaques connues. */
data class PokemonDresseur(val especeId: Int, val niveau: Int, val attaques: List<String>)

/**
 * Dresseur à affronter pendant une étape (`assets/donnees/dresseurs.json`). [queteId] : la quête principale
 * « Battre … » de l'étape ; le dresseur est battu quand elle est cochée. [duo] : combat en duo.
 */
data class Dresseur(
    val cle: String,
    val etapeId: Int,
    val nom: String,
    val titre: String,
    val lieu: String,
    val type: String,
    val queteId: Int,
    val equipe: List<PokemonDresseur>,
    val attention: String?,
    val duo: Boolean = false,
)

/** Un Pokémon (de l'adversaire ou du joueur) avec son espèce et son niveau. */
data class PokemonEnCombat(val espece: Espece, val niveau: Int, val attaques: List<String> = emptyList())

/** Un membre de l'équipe du joueur et son affinité face au type de l'arène. */
data class AtoutCombat(val pokemon: PokemonEnCombat, val affinite: Affinite)

/**
 * Stratégie contre un dresseur : les types qui touchent fort au moins la moitié de son équipe ([faiblesses]),
 * l'équipe du joueur classée de l'avantage à la faiblesse ([atouts]) et son Pokémon le plus fort.
 */
data class Strategie(val faiblesses: List<String>, val atouts: List<AtoutCombat>, val plusFort: PokemonEnCombat?) {
    val meilleurAtout: AtoutCombat? get() = atouts.firstOrNull { it.affinite == Affinite.AVANTAGE }
    val aEviter: List<AtoutCombat> get() = atouts.filter { it.affinite == Affinite.FAIBLESSE }
}

/** Calcule la stratégie à partir des types : rien n'est saisi à la main, sauf le point d'attention. */
object StrategieCombat {
    private const val MAX_FAIBLESSES = 5

    /** Le type qui touche fort le plus de Pokémon d'abord, puis le plus fort multiplicateur. */
    private val PAR_FREQUENCE = compareByDescending<Map.Entry<String, List<Float>>> { it.value.size }
        .thenByDescending { it.value.max() }

    fun calculer(adversaire: List<PokemonEnCombat>, typeArene: String, equipe: List<PokemonEnCombat>): Strategie {
        val efficacites = adversaire.map { TableDesTypes.efficacites(it.espece.types.map(String::lowercase)) }
        val faiblesses = efficacites.flatMap { it.entries }
            .filter { it.value > 1f }
            .groupBy({ it.key }, { it.value })
            .filter { (_, valeurs) -> valeurs.size * 2 >= adversaire.size }
            .entries
            .sortedWith(PAR_FREQUENCE)
            .map { it.key }
            .take(MAX_FAIBLESSES)
        val atouts = equipe
            .map { AtoutCombat(it, SuggestionsRotation.affinite(it.espece.types, typeArene)) }
            .sortedWith(compareBy<AtoutCombat> { it.affinite.ordinal }.thenByDescending { it.pokemon.niveau })
        return Strategie(faiblesses, atouts, adversaire.maxByOrNull { it.niveau })
    }
}
