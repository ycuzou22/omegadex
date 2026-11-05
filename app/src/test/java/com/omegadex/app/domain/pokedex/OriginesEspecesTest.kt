package com.omegadex.app.domain.pokedex

import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.TypeLieu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginesEspecesTest {
    private val route101 = Lieu(1, "Route 101", TypeLieu.ROUTE, 0)
    private val mirage = Lieu(2, "Île Mirage (ouest de Mérouville)", TypeLieu.LIEU_MIRAGE, 12)
    private val grotte = Lieu(3, "Grotte Origine", TypeLieu.GROTTE, 8)

    private fun rencontre(lieu: Lieu, espece: Int, methode: MethodeRencontre, condition: String? = null) =
        Rencontre(lieu, espece, methode, 2, 4, 20, condition)

    private val rencontres = listOf(
        rencontre(route101, 263, MethodeRencontre.HERBE),
        rencontre(route101, 263, MethodeRencontre.NAVI_DEX),
        rencontre(mirage, 285, MethodeRencontre.HERBE),
        rencontre(grotte, 383, MethodeRencontre.FIXE),
    )

    @Test
    fun `chaque espece porte ses lieux et ses methodes`() {
        val origines = OriginesEspeces.de(rencontres)

        assertEquals(setOf(1), origines.getValue(263).lieux)
        assertEquals(setOf(MethodeFiltre.HERBE, MethodeFiltre.NAVI_DEX), origines.getValue(263).methodes)
        assertEquals(setOf(MethodeFiltre.HERBE, MethodeFiltre.LIEU_MIRAGE), origines.getValue(285).methodes)
        assertEquals(setOf(MethodeFiltre.FIXE_OU_CADEAU, MethodeFiltre.LEGENDAIRE), origines.getValue(383).methodes)
    }

    @Test
    fun `un legendaire sans rencontre reste legendaire et les filtres se combinent`() {
        val origines = OriginesEspeces.de(rencontres, especes = listOf(384))
        assertEquals(OriginesEspece(emptySet(), setOf(MethodeFiltre.LEGENDAIRE)), origines.getValue(384))

        val zigzaton = origines.getValue(263)
        assertTrue(zigzaton.correspond(1, MethodeFiltre.NAVI_DEX))
        assertTrue(zigzaton.correspond(null, null))
        assertFalse(zigzaton.correspond(2, null))
        assertFalse(zigzaton.correspond(1, MethodeFiltre.SURF))
    }
}
