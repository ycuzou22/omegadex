package com.omegadex.app.domain.dresseur

import com.omegadex.app.domain.equipe.Affinite
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.feature.aventure.decouperNom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategieCombatTest {
    private fun espece(id: Int, nom: String, vararg types: String) = Espece(
        id = id,
        numeroHoenn = id,
        nom = nom,
        types = types.toList(),
        categorie = "",
        tailleM = 1.0,
        poidsKg = 1.0,
        stats = Stats(300, 0, 0, 0, 0, 0),
        evDonnes = "",
        tauxCapture = 45,
        ratioMale = 0.5,
        groupesOeufs = emptyList(),
        aUneMega = false,
    )

    private val racaillou = PokemonEnCombat(espece(74, "Racaillou", "roche", "sol"), 12)
    private val tarinor = PokemonEnCombat(espece(299, "Tarinor", "roche"), 14)

    @Test
    fun `contre Roxanne, Eau et Plante d'abord, l'equipe classee de l'avantage a la faiblesse`() {
        val equipe = listOf(
            PokemonEnCombat(espece(277, "Nirondelle", "normal", "vol"), 13),
            PokemonEnCombat(espece(252, "Arcko", "plante"), 12),
            PokemonEnCombat(espece(263, "Zigzaton", "normal"), 11),
        )
        val strategie = StrategieCombat.calculer(listOf(racaillou, tarinor), "roche", equipe)

        assertEquals(listOf("eau", "plante"), strategie.faiblesses.take(2))
        assertTrue(strategie.faiblesses.containsAll(listOf("combat", "sol", "acier")))
        assertEquals("Arcko", strategie.meilleurAtout?.pokemon?.espece?.nom)
        assertEquals(Affinite.FAIBLESSE, strategie.atouts.last().affinite)
        assertEquals(listOf("Nirondelle"), strategie.aEviter.map { it.pokemon.espece.nom })
        assertEquals(tarinor, strategie.plusFort)
    }

    @Test
    fun `sans equipe, pas d'atout mais toujours les faiblesses`() {
        val strategie = StrategieCombat.calculer(listOf(tarinor), "roche", emptyList())
        assertTrue(strategie.atouts.isEmpty())
        assertTrue(strategie.faiblesses.isNotEmpty())
    }

    @Test
    fun `le nom s'ecrit sur deux lignes`() {
        assertEquals("ROXA" to "NNE", decouperNom("Roxanne"))
        assertEquals("LÉVY" to "TATIA", decouperNom("Lévy et Tatia"))
        assertEquals("MARC" to "", decouperNom("Marc"))
    }
}
