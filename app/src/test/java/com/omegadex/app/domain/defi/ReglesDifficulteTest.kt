package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Difficulte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglesDifficulteTest {
    @Test
    fun `chaque difficulte ajoute des defis a la precedente`() {
        assertTrue(ReglesDifficulte.estProposable(Difficulte.NORMAL, Difficulte.NORMAL))
        assertFalse(ReglesDifficulte.estProposable(Difficulte.NORMAL, Difficulte.DIFFICILE))
        assertTrue(ReglesDifficulte.estProposable(Difficulte.DIFFICILE, Difficulte.NORMAL))
        assertTrue(ReglesDifficulte.estProposable(Difficulte.HARDCORE, Difficulte.DIFFICILE))
        assertFalse(ReglesDifficulte.estProposable(Difficulte.DIFFICILE, Difficulte.HARDCORE))
    }

    @Test
    fun `aucun pokemon au-dessus du plafond`() {
        assertTrue(ReglesDifficulte.respectePlafond(listOf(12, 14, 14), plafond = 14))
        assertFalse(ReglesDifficulte.respectePlafond(listOf(12, 15), plafond = 14))
        assertEquals(listOf(15, 30), ReglesDifficulte.auDessusDuPlafond(listOf(12, 15, 30), plafond = 14))
    }

    @Test
    fun `hardcore exige le niveau exact du plafond`() {
        assertTrue(ReglesDifficulte.respecteHardcore(listOf(14, 14, 14), plafond = 14))
        assertFalse(ReglesDifficulte.respecteHardcore(listOf(14, 13), plafond = 14))
        assertFalse(ReglesDifficulte.respecteHardcore(listOf(14, 15), plafond = 14))
        assertFalse(ReglesDifficulte.respecteHardcore(emptyList(), plafond = 14))
        assertEquals(3, ReglesDifficulte.niveauxManquantsHardcore(listOf(12, 13, 14), plafond = 14))
    }

    @Test
    fun `une evolution n'est proposee que sous le plafond`() {
        assertTrue(ReglesDifficulte.evolutionAtteignable(ConditionEvolution.NIVEAU, "16", plafond = 16))
        assertFalse(ReglesDifficulte.evolutionAtteignable(ConditionEvolution.NIVEAU, "36", plafond = 21))
        assertFalse(
            ReglesDifficulte.evolutionAtteignable(ConditionEvolution.NIVEAU, "20 (Attaque > Défense)", plafond = 16),
        )
        assertTrue(ReglesDifficulte.evolutionAtteignable(ConditionEvolution.PIERRE, "Pierre Feu", plafond = 5))
        assertNull(ReglesDifficulte.niveauRequis(ConditionEvolution.ECHANGE, null))
    }

    @Test
    fun `un defi peut viser sous le plafond mais jamais au-dessus`() {
        assertEquals(25, ReglesDifficulte.niveauMaximalDuDefi(plafond = 30, ecartSousPlafond = 5))
        assertEquals(30, ReglesDifficulte.niveauMaximalDuDefi(plafond = 30))
        assertThrows(IllegalArgumentException::class.java) {
            ReglesDifficulte.niveauMaximalDuDefi(30, ecartSousPlafond = -2)
        }
    }
}
