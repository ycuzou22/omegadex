package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Evolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionEvolutionTest {
    private val arcko = listOf(
        Evolution(252, 253, ConditionEvolution.NIVEAU, "16"),
        Evolution(253, 254, ConditionEvolution.NIVEAU, "36"),
    )

    /** Chenipotte évolue au niveau 7 en Armulys ou en Blindalys (au hasard en jeu). */
    private val chenipotte = listOf(
        Evolution(265, 266, ConditionEvolution.NIVEAU, "7"),
        Evolution(265, 268, ConditionEvolution.NIVEAU, "7"),
        Evolution(266, 267, ConditionEvolution.NIVEAU, "10"),
    )

    @Test
    fun `rien sous le niveau requis, l'evolution au niveau requis et au-dela`() {
        assertTrue(DetectionEvolution.disponibles(252, 15, arcko).isEmpty())
        assertEquals(listOf(253), DetectionEvolution.disponibles(252, 16, arcko).map { it.evolution.especeCibleId })
        assertEquals(listOf(253), DetectionEvolution.disponibles(252, 40, arcko).map { it.evolution.especeCibleId })
    }

    @Test
    fun `seules les evolutions de l'espece elle-meme sont proposees`() {
        val disponibles = DetectionEvolution.disponibles(253, 36, arcko)
        assertEquals(listOf(254), disponibles.map { it.evolution.especeCibleId })
        assertEquals(36, disponibles.single().niveauRequis)
    }

    @Test
    fun `plusieurs evolutions possibles au meme niveau`() {
        assertEquals(
            listOf(266, 268),
            DetectionEvolution.disponibles(265, 7, chenipotte).map { it.evolution.especeCibleId },
        )
    }

    @Test
    fun `les evolutions sans niveau ne sont pas detectees a la saisie d'un niveau`() {
        val evoli = listOf(
            Evolution(133, 134, ConditionEvolution.PIERRE, "Pierre Eau"),
            Evolution(133, 196, ConditionEvolution.BONHEUR, "jour"),
        )
        assertTrue(DetectionEvolution.disponibles(133, 100, evoli).isEmpty())
    }

    @Test
    fun `la precision garde la condition que le joueur doit confirmer`() {
        assertNull(DetectionEvolution.precision("16"))
        assertNull(DetectionEvolution.precision(null))
        assertEquals("femelle", DetectionEvolution.precision("20 (femelle)"))
        assertEquals("Attaque > Défense", DetectionEvolution.precision("20 (Attaque > Défense)"))
        assertEquals(
            "avec un Pokémon Ténèbres dans l'équipe",
            DetectionEvolution.precision("32 avec un Pokémon Ténèbres dans l'équipe"),
        )
        val mysdibule = listOf(Evolution(1, 2, ConditionEvolution.NIVEAU, "20 (mâle)"))
        assertEquals("mâle", DetectionEvolution.disponibles(1, 20, mysdibule).single().precision)
    }
}
