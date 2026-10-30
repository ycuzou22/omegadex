package com.omegadex.app.domain.accueil

import com.omegadex.app.domain.defi.DefiPropose
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.TypeDefi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefiDuJourTest {
    private fun propose(id: Int, type: TypeDefi, especeId: Int? = null) = DefiPropose(
        ModeleDefi(id, type, null, "M$id", "g", emptyMap(), Difficulte.NORMAL, 0, emptySet()),
        especeId,
        if (especeId == null) 1 else null,
        "Énoncé $id",
    )

    private val etape = propose(1011, TypeDefi.ETAPE)
    private val especes = listOf(propose(2001, TypeDefi.ESPECE, 252), propose(3001, TypeDefi.EVOLUTION, 252))

    @Test
    fun `le defi du jour est stable dans la journee et change le lendemain`() {
        val tous = listOf(etape) + especes
        assertEquals(DefiDuJourUseCase.choisir(tous, 100, 1), DefiDuJourUseCase.choisir(tous.reversed(), 100, 1))
        val aujourdhui = DefiDuJourUseCase.choisir(tous, 100, 1)
        val demain = DefiDuJourUseCase.choisir(tous, 101, 1)
        assertEquals(setOf(2001, 3001), setOf(aujourdhui?.modele?.id, demain?.modele?.id))
    }

    @Test
    fun `les defis espece sont privilegies, sinon le defi d'etape, sinon rien`() {
        repeat(5) { jour ->
            val choisi = DefiDuJourUseCase.choisir(listOf(etape) + especes, jour.toLong(), 0)
            assertEquals(252, choisi?.especeId)
        }
        assertEquals(etape, DefiDuJourUseCase.choisir(listOf(etape), 7, 3))
        assertNull(DefiDuJourUseCase.choisir(emptyList(), 7, 3))
    }
}
