package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.model.StatutEspece.CAPTUREE
import com.omegadex.app.domain.model.StatutEspece.ENTRAINEE
import com.omegadex.app.domain.pokedex.EntreePokedex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsRotationTest {
    private fun espece(id: Int, nom: String, vararg types: String, total: Int = 300) = Espece(
        id = id,
        numeroHoenn = id,
        nom = nom,
        types = types.toList(),
        categorie = "",
        tailleM = 1.0,
        poidsKg = 1.0,
        stats = Stats(total, 0, 0, 0, 0, 0),
        evDonnes = "",
        tauxCapture = 45,
        ratioMale = 0.5,
        groupesOeufs = emptyList(),
        aUneMega = false,
    )

    private fun entree(espece: Espece, statut: StatutEspece = CAPTUREE) = EntreePokedex(espece, statut, false)

    // Voltère : type Électrik, plafond 21.
    private val voltere = Etape(3, "Voltère", "Voltère", "electrik", 21, "Lavandia")

    private val racaillou = espece(74, "Racaillou", "roche", "sol", total = 300)
    private val tarsal = espece(280, "Tarsal", "psy", "fee", total = 198)
    private val goelise = espece(278, "Goélise", "eau", "vol", total = 270)
    private val zigzaton = espece(263, "Zigzaton", "normal", total = 240)
    private val chenipotte = espece(265, "Chenipotte", "insecte", total = 195)

    @Test
    fun `l'avantage passe d'abord, la faiblesse en dernier`() {
        val suggestions = SuggestionsRotation.calculer(
            listOf(entree(goelise), entree(zigzaton), entree(racaillou), entree(chenipotte)),
            equipe = emptyList(),
            etape = voltere,
            limite = 4,
        )

        assertEquals(listOf("Racaillou", "Zigzaton", "Chenipotte", "Goélise"), suggestions.map { it.espece.nom })
        assertEquals(
            listOf(Affinite.AVANTAGE, Affinite.NEUTRE, Affinite.NEUTRE, Affinite.FAIBLESSE),
            suggestions.map { it.affinite },
        )
    }

    @Test
    fun `seules les especes capturees jamais entrainees et hors equipe sont proposees`() {
        val suggestions = SuggestionsRotation.calculer(
            listOf(
                entree(racaillou),
                entree(zigzaton, ENTRAINEE),
                entree(tarsal),
                entree(goelise, StatutEspece.VUE),
            ),
            equipe = listOf(MembreEquipe(1, racaillou.id, null, 18)),
            etape = voltere,
        )

        assertEquals(listOf("Tarsal"), suggestions.map { it.espece.nom })
    }

    @Test
    fun `les suggestions respectent le plafond et la limite`() {
        val entrees = (1..10).map { entree(espece(1_000 + it, "Espèce $it", "normal", total = 200 + it)) }

        val suggestions = SuggestionsRotation.calculer(entrees, emptyList(), voltere)

        assertEquals(SuggestionsRotation.LIMITE, suggestions.size)
        assertTrue(suggestions.all { it.niveau <= voltere.plafond })
        assertEquals(listOf("Espèce 10", "Espèce 9", "Espèce 8"), suggestions.map { it.espece.nom })
    }

    @Test
    fun `sans type dominant, toutes les especes sont neutres`() {
        val rival = Etape(0, "Rival", "Brice", null, 5, "Route 103")
        assertEquals(Affinite.NEUTRE, SuggestionsRotation.affinite(listOf("eau"), rival.typeDominant))
    }

    @Test
    fun `affinites face a un type`() {
        assertEquals(Affinite.AVANTAGE, SuggestionsRotation.affinite(listOf("sol"), "electrik"))
        assertEquals(Affinite.RESISTANCE, SuggestionsRotation.affinite(listOf("plante"), "electrik"))
        assertEquals(Affinite.FAIBLESSE, SuggestionsRotation.affinite(listOf("eau", "vol"), "electrik"))
        // Frappe fort mais craint le type : la faiblesse l'emporte.
        assertEquals(Affinite.FAIBLESSE, SuggestionsRotation.affinite(listOf("dragon"), "dragon"))
    }
}
