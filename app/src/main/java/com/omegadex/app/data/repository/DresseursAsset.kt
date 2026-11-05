package com.omegadex.app.data.repository

import android.content.Context
import com.omegadex.app.domain.dresseur.Dresseur
import com.omegadex.app.domain.dresseur.Genre
import com.omegadex.app.domain.dresseur.PokemonDresseur
import com.omegadex.app.domain.repository.DresseursRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject

/** Dresseurs lus une seule fois dans `assets/donnees/dresseurs.json`. */
@Singleton
class DresseursAsset @Inject constructor(@ApplicationContext private val context: Context) : DresseursRepository {
    @Volatile
    private var cache: List<Dresseur>? = null

    override fun dresseurs(): Flow<List<Dresseur>> = flow {
        emit(cache ?: lire().also { cache = it })
    }.flowOn(Dispatchers.IO)

    private fun lire(): List<Dresseur> {
        val texte = context.assets.open(FICHIER).bufferedReader().use { it.readText() }
        return lireDresseurs(texte)
    }

    companion object {
        const val FICHIER = "donnees/dresseurs.json"

        fun lireDresseurs(texte: String): List<Dresseur> {
            val liste = JSONObject(texte).getJSONArray("dresseurs")
            return List(liste.length()) { i -> dresseur(liste.getJSONObject(i)) }
        }

        private fun dresseur(o: JSONObject) = Dresseur(
            cle = o.getString("cle"),
            etapeId = o.getInt("etapeId"),
            nom = o.getString("nom"),
            titre = o.getString("titre"),
            lieu = o.getString("lieu"),
            type = o.getString("type"),
            queteId = if (o.has("queteId")) o.getInt("queteId") else null,
            equipe = o.getJSONArray("equipe").objets().map { p ->
                PokemonDresseur(p.getInt("especeId"), p.getInt("niveau"), p.optJSONArray("attaques").textes())
            },
            attention = o.optString("attention").takeIf { it.isNotBlank() },
            duo = o.optBoolean("duo", false),
            starter = if (o.has("starter")) o.getInt("starter") else null,
            image = o.optString("image").ifBlank { o.getString("cle") },
            genre = GENRES[o.optString("genre")] ?: Genre.MASCULIN,
        )

        private val GENRES = mapOf("f" to Genre.FEMININ, "p" to Genre.PLURIEL)

        private fun JSONArray.objets(): List<JSONObject> = List(length()) { getJSONObject(it) }

        private fun JSONArray?.textes(): List<String> =
            this?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
    }
}
