package com.omegadex.app.domain.dresseur

import com.omegadex.app.domain.model.Etape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DresseursEtapeTest {
    private fun dresseur(cle: String, lieu: String, starter: Int? = null) = Dresseur(
        cle = cle,
        etapeId = 0,
        nom = cle,
        titre = "Gamin",
        lieu = lieu,
        type = "normal",
        queteId = null,
        equipe = listOf(PokemonDresseur(263, 4, emptyList())),
        attention = null,
        starter = starter,
    )

    private fun fiche(cle: String, lieu: String, battu: Boolean = false) =
        FicheDresseur(dresseur(cle, lieu), emptyList(), Strategie(emptyList(), emptyList(), null), battu)

    private val etape = Etape(0, "Départ", "Flora", null, 5, "Bourg-en-Vol")

    @Test
    fun `l'equipe de Flora ne vise que le starter de la run, les autres dresseurs toujours`() {
        assertTrue(dresseur("flora", "Route 103", starter = 252).affronte(252))
        assertFalse(dresseur("flora", "Route 103", starter = 255).affronte(252))
        assertTrue(dresseur("gamin", "Route 102").affronte(252))
    }

    @Test
    fun `les fiches se regroupent par lieu sans perdre l'ordre de rencontre`() {
        val etapeDresseurs = DresseursEtape(
            etape,
            listOf(
                fiche("a", "Route 102", battu = true),
                fiche("b", "Route 102"),
                fiche("c", "Route 103"),
                fiche("d", "Route 102"),
            ),
        )
        assertEquals(listOf("Route 102", "Route 103", "Route 102"), etapeDresseurs.parLieu.map { it.lieu })
        assertEquals(listOf("a", "b"), etapeDresseurs.parLieu.first().fiches.map { it.dresseur.cle })
        assertEquals("d", etapeDresseurs.chef?.dresseur?.cle)
        assertEquals(1, etapeDresseurs.battus)
    }
}
