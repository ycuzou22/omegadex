package com.omegadex.app.domain.repository

import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.MethodeFiltre
import kotlinx.coroutines.flow.Flow

/** Filtres du Pokédex gardés pour un compte (la recherche texte, elle, n'est pas gardée). */
data class FiltresMemorises(
    val statuts: Set<StatutEspece> = emptySet(),
    val type: String? = null,
    val zoneId: Int? = null,
    val methode: MethodeFiltre? = null,
)

/** Mémorisation des filtres du Pokédex par compte : retrouvés au redémarrage de l'app (T19.2). */
interface FiltresPokedexRepository {
    /** Filtres du compte connecté ; aucun filtre sans compte. */
    val filtres: Flow<FiltresMemorises>

    suspend fun enregistrer(filtres: FiltresMemorises)
}
