package com.omegadex.app.domain.statistiques

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ProgressionEtape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatistiquesRunTest {
    private val heure = 3_600_000L
    private val titres = mapOf(0 to "Départ", 1 to "Roxanne", 2 to "Bastien")
    private val noms = mapOf(252 to "Arcko", 280 to "Tarsal", 263 to "Zigzaton")

    private fun defi(id: Long, espece: Int?, etat: EtatDefi) = Defi(id, 1, espece, null, etat, 0L, null)

    @Test
    fun `la duree de chaque etape validee va du deverrouillage a la validation`() {
        val stats = StatistiquesRun.de(
            listOf(
                ProgressionEtape(1, true, 2 * heure, 5 * heure),
                ProgressionEtape(0, true, 0, 2 * heure),
                ProgressionEtape(2, true, 5 * heure, null),
            ),
            emptyList(),
            titres,
            noms,
        )

        assertEquals(listOf(DureeEtape(0, "Départ", 2 * heure), DureeEtape(1, "Roxanne", 3 * heure)), stats.durees)
        assertEquals(5 * heure, stats.dureeTotaleMs)
        assertEquals("Roxanne", stats.etapeLaPlusLongue?.titre)
    }

    @Test
    fun `le taux de reussite ne compte que les defis termines`() {
        val defis = listOf(
            defi(1, 252, EtatDefi.REUSSI),
            defi(2, 252, EtatDefi.REUSSI),
            defi(3, 252, EtatDefi.RATE),
            defi(4, 280, EtatDefi.ACTIF),
            defi(5, null, EtatDefi.REUSSI),
        )
        val bilan = StatistiquesRun.de(emptyList(), defis, titres, noms).bilan

        assertEquals(BilanDefis(reussis = 3, rates = 1, actifs = 1), bilan)
        assertEquals(0.75f, bilan.tauxReussite)
        assertNull(BilanDefis(actifs = 2).tauxReussite)
    }

    @Test
    fun `les especes les plus sollicitees passent en premier`() {
        val defis = listOf(
            defi(1, 263, EtatDefi.RATE),
            defi(2, 252, EtatDefi.REUSSI),
            defi(3, 252, EtatDefi.ACTIF),
            defi(4, 280, EtatDefi.REUSSI),
            defi(5, 263, EtatDefi.REUSSI),
        )
        val especes = StatistiquesRun.de(emptyList(), defis, titres, noms).especes

        // Arcko et Zigzaton ont 2 défis chacun : à égalité de réussites, l'ordre alphabétique départage.
        assertEquals(listOf("Arcko", "Zigzaton", "Tarsal"), especes.map { it.nom })
        assertEquals(DefisEspece(252, "Arcko", reussis = 1, rates = 0, actifs = 1), especes.first())
    }
}
