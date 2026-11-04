package com.omegadex.app.feature.succes

import com.omegadex.app.domain.succes.Succes
import org.junit.Assert.assertEquals
import org.junit.Test

class TextesSuccesTest {
    @Test
    fun `chaque succes a un titre distinct`() {
        assertEquals(Succes.entries.size, Succes.entries.map(::titreSucces).toSet().size)
    }
}
