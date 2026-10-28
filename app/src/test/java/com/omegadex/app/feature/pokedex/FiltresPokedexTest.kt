package com.omegadex.app.feature.pokedex

import com.omegadex.app.domain.model.StatutEspece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltresPokedexTest {
    private val especes = listOf(
        espece(252, 1, "Arcko", listOf("plante"), StatutEspece.CAPTUREE),
        espece(255, 4, "Poussifeu", listOf("feu"), StatutEspece.VUE),
        espece(256, 5, "Galifeu", listOf("feu", "combat"), StatutEspece.NON_VUE),
        espece(282, 10, "Gardevoir", listOf("psy", "fee"), StatutEspece.MAITRISEE),
        espece(286, 25, "Chapignon", listOf("plante", "combat"), StatutEspece.NON_VUE),
        espece(330, 104, "Libégon", listOf("sol", "dragon"), StatutEspece.ENTRAINEE),
    )

    private fun espece(id: Int, numero: Int, nom: String, types: List<String>, statut: StatutEspece) =
        EspeceAffichee(id, numero, nom, types, statut, echangeRequis = false, aPortee = false)

    private fun ids(filtres: FiltresPokedex) = filtrer(especes, filtres).map { it.id }

    @Test
    fun `sans filtre la liste est rendue telle quelle`() {
        assertFalse(FiltresPokedex().actifs)
        assertSame(especes, filtrer(especes, FiltresPokedex()))
        assertFalse(FiltresPokedex(recherche = "   ").actifs)
    }

    @Test
    fun `la recherche par nom ignore la casse et les accents`() {
        assertEquals(listOf(255, 256), ids(FiltresPokedex(recherche = "FEU")))
        assertEquals(listOf(330), ids(FiltresPokedex(recherche = "libego")))
        assertEquals(listOf(330), ids(FiltresPokedex(recherche = " Libégon ")))
    }

    @Test
    fun `la recherche par numero accepte les zeros et les prefixes`() {
        assertEquals(listOf(282, 330), ids(FiltresPokedex(recherche = "10")))
        assertEquals(listOf(286), ids(FiltresPokedex(recherche = "#025")))
        assertEquals(listOf(286), ids(FiltresPokedex(recherche = "n°25")))
        assertEquals(emptyList<Int>(), ids(FiltresPokedex(recherche = "0")))
    }

    @Test
    fun `le filtre par statut garde un ou plusieurs statuts`() {
        assertEquals(listOf(256, 286), ids(FiltresPokedex(statuts = setOf(StatutEspece.NON_VUE))))
        val avances = setOf(StatutEspece.ENTRAINEE, StatutEspece.MAITRISEE)
        assertEquals(listOf(282, 330), ids(FiltresPokedex(statuts = avances)))
    }

    @Test
    fun `le filtre par type tient compte des deux types et se combine aux autres`() {
        assertEquals(listOf(256, 286), ids(FiltresPokedex(type = "combat")))
        assertEquals(listOf(282), ids(FiltresPokedex(type = "Fée")))
        val combine = FiltresPokedex(recherche = "feu", statuts = setOf(StatutEspece.NON_VUE), type = "combat")
        assertTrue(combine.actifs)
        assertEquals(listOf(256), ids(combine))
    }

    @Test
    fun `les types proposes sont ceux de la grille, sans doublon et tries`() {
        assertEquals(listOf("combat", "dragon", "fee", "feu", "plante", "psy", "sol"), typesPresents(especes))
    }
}
