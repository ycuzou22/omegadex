package com.omegadex.app.domain.espece

import org.junit.Assert.assertEquals
import org.junit.Test

class TableDesTypesTest {
    @Test
    fun `chaque type d'attaque a une ligne et 18 types sont connus`() {
        assertEquals(18, TableDesTypes.TYPES.size)
        assertEquals(0f, TableDesTypes.multiplicateur("normal", "spectre"))
        assertEquals(2f, TableDesTypes.multiplicateur("fee", "dragon"))
        assertEquals(1f, TableDesTypes.multiplicateur("feu", "normal"))
    }

    @Test
    fun `efficacites d'un Pokemon a deux types`() {
        val flobio = TableDesTypes.efficacites(listOf("eau", "sol"))
        assertEquals(4f, flobio.getValue("plante"))
        assertEquals(0f, flobio.getValue("electrik"))
        assertEquals(0.5f, flobio.getValue("feu"))
        val arcko = TableDesTypes.efficacites(listOf("plante"))
        assertEquals(
            setOf("feu", "glace", "poison", "vol", "insecte"),
            arcko.filterValues { it == 2f }.keys,
        )
        assertEquals(setOf("eau", "electrik", "plante", "sol"), arcko.filterValues { it == 0.5f }.keys)
    }
}
