package com.omegadex.app.feature.pokedex

import com.omegadex.app.domain.model.StatutEspece
import java.text.Normalizer

/**
 * Recherche et filtres du Pokédex. Un ensemble [statuts] vide et un [type] nul ne filtrent rien.
 * [recherche] porte sur le nom (sans tenir compte des accents ni de la casse) ou sur le numéro affiché.
 */
data class FiltresPokedex(
    val recherche: String = "",
    val statuts: Set<StatutEspece> = emptySet(),
    val type: String? = null,
) {
    val actifs: Boolean get() = recherche.isNotBlank() || statuts.isNotEmpty() || type != null
}

private val ACCENTS = Regex("\\p{Mn}+")
private val PREFIXE_NUMERO = Regex("^(#|n°|no|n)\\s*")

/** Texte comparable : minuscules et sans accents (« Électrik » → « electrik »). */
fun normaliser(texte: String): String = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD)
    .replace(ACCENTS, "")
    .lowercase()

/**
 * Filtre la grille en mémoire : quelques centaines d'espèces au plus, le résultat est instantané.
 * Une recherche faite de chiffres (« 25 », « #025 », « n°25 ») cherche les numéros qui commencent ainsi ;
 * sinon elle cherche les noms qui contiennent le texte.
 */
fun filtrer(especes: List<EspeceAffichee>, filtres: FiltresPokedex): List<EspeceAffichee> {
    if (!filtres.actifs) return especes
    val texte = normaliser(filtres.recherche)
    val chiffres = texte.replace(PREFIXE_NUMERO, "")
    // « 007 » cherche le n° 7 ; « 0 » seul ne correspond à aucun numéro.
    val numero = chiffres.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.trimStart('0')
    val type = filtres.type?.let(::normaliser)
    return especes.filter { espece ->
        val trouvee = when {
            texte.isEmpty() -> true
            numero != null -> numero.isNotEmpty() && espece.numero.toString().startsWith(numero)
            else -> texte in normaliser(espece.nom)
        }
        trouvee && filtres.garde(espece.statut) && espece.estDuType(type)
    }
}

private fun FiltresPokedex.garde(statut: StatutEspece) = statuts.isEmpty() || statut in statuts

private fun EspeceAffichee.estDuType(type: String?) = type == null || types.any { normaliser(it) == type }

/** Types présents dans la grille, normalisés et sans doublon, pour proposer le filtre par type. */
fun typesPresents(especes: List<EspeceAffichee>): List<String> =
    especes.flatMap { it.types }.map(::normaliser).distinct().sorted()
