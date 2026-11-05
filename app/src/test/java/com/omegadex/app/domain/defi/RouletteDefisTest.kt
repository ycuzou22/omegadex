package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.domain.model.TypeQuete
import com.omegadex.app.testutil.FauxTirages
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouletteDefisTest {
    private val roxanne = Etape(1, "Roxanne", "Roxanne", "roche", 14, "Mérouville")

    private fun modele(id: Int, type: TypeDefi, etapeId: Int? = null) =
        ModeleDefi(id, type, etapeId, "Modèle $id", "Défi {adversaire}", emptyMap(), Difficulte.NORMAL, 0, emptySet())

    private fun propose(id: Int, type: TypeDefi = TypeDefi.MONOTYPE) =
        DefiPropose(modele(id, type), null, 1, "Défi $id")

    private val pool = listOf(propose(5001), propose(5002), propose(5003))

    private fun case(
        tirage: DefiPropose? = null,
        faites: Int = 0,
        difficulte: Difficulte = Difficulte.DIFFICILE,
        quete: Quete? = null,
    ) = CaseRoulette(
        categorie = CategorieDefi.CHAMPION,
        pool = pool,
        actif = null,
        tirage = tirage,
        relancesFaites = faites,
        relancesRestantes = RouletteDefis.relancesRestantes(difficulte, faites),
        queteRequise = quete,
    )

    @Test
    fun `chaque type de defi a sa categorie`() {
        assertEquals(CategorieDefi.CHAMPION, CategorieDefi.de(TypeDefi.ETAPE))
        assertEquals(CategorieDefi.CHAMPION, CategorieDefi.de(TypeDefi.MONOTYPE))
        assertEquals(CategorieDefi.CHAMPION, CategorieDefi.de(TypeDefi.ROTATION))
        assertEquals(CategorieDefi.NAVI_DEX, CategorieDefi.de(TypeDefi.NAVI_DEX))
        assertEquals(TypeDefi.entries.toSet(), CategorieDefi.entries.flatMap { it.types }.toSet())
    }

    @Test
    fun `relances selon la difficulte`() {
        assertNull(RouletteDefis.relancesRestantes(Difficulte.NORMAL, faites = 5))
        assertEquals(1, RouletteDefis.relancesRestantes(Difficulte.DIFFICILE, faites = 0))
        assertEquals(0, RouletteDefis.relancesRestantes(Difficulte.DIFFICILE, faites = 1))
        assertEquals(0, RouletteDefis.relancesRestantes(Difficulte.HARDCORE, faites = 0))
    }

    @Test
    fun `le rouleau s'arrete sur le gagnant sans doublon voisin`() {
        pool.forEach { gagnant ->
            val rouleau = RouletteDefis.rouleau(pool, gagnant)
            assertEquals(RouletteDefis.LONGUEUR_ROULEAU, rouleau.size)
            assertEquals(gagnant, rouleau[RouletteDefis.ARRET_ROULEAU])
            assertTrue(rouleau.zipWithNext().none { (a, b) -> a == b })
        }
    }

    @Test
    fun `une relance ne retombe pas sur le meme defi`() {
        repeat(20) { graine ->
            assertNotEquals(pool[0], RouletteDefis.tirer(pool, pool[0], Random(graine)))
        }
        assertEquals(pool[0], RouletteDefis.tirer(listOf(pool[0]), pool[0], Random(1)))
        assertNull(RouletteDefis.tirer(emptyList(), null, Random(1)))
    }

    @Test
    fun `le champion s'ouvre a l'avant-derniere quete principale avant le badge`() {
        val quetes = listOf(
            Quete(5, 1, null, TypeQuete.PRINCIPALE, 1, "Traverser le Bois Clémenti", null),
            Quete(290, 1, null, TypeQuete.ANNEXE, 1, "Recevoir CS01 Coupe", null),
            Quete(6, 1, null, TypeQuete.PRINCIPALE, 2, "Atteindre Mérouville", null),
            Quete(7, 1, null, TypeQuete.PRINCIPALE, 3, "Battre Roxanne", null),
        )
        assertEquals(6, VerrouChampion.queteRequise(1, quetes, cochees = setOf(5))?.id)
        assertNull(VerrouChampion.queteRequise(1, quetes, cochees = setOf(6)))
        assertNull("étape sans badge", VerrouChampion.queteRequise(0, quetes, cochees = emptySet()))
        assertNull("une seule quête principale", VerrouChampion.queteRequise(1, quetes.takeLast(1), emptySet()))
    }

    @Test
    fun `tourner garde le tirage et une relance coute une relance`() = runTest {
        val tirages = FauxTirages()
        val tourner = TournerRouletteUseCase(tirages)

        val premier = tourner(1L, roxanne.id, case(), Random(3))!!
        val garde = tirages.tirages(1L).first().getValue(CategorieDefi.CHAMPION)
        assertEquals(Tirage(1, CleDefi.de(premier), relances = 0), garde)

        val second = tourner(1L, roxanne.id, case(tirage = premier), Random(3))!!
        assertNotEquals(premier, second)
        assertEquals(1, tirages.tirages(1L).first().getValue(CategorieDefi.CHAMPION).relances)

        assertNull("Difficile : une seule relance", tourner(1L, roxanne.id, case(tirage = second, faites = 1)))
        assertNull("Hardcore : aucune relance", tourner(1L, 1, case(tirage = second, difficulte = Difficulte.HARDCORE)))
        assertFalse(case(quete = Quete(6, 1, null, TypeQuete.PRINCIPALE, 2, "Atteindre Mérouville", null)).peutTourner)
    }

    @Test
    fun `le generateur ne propose rien dans une categorie occupee ni le champion verrouille`() {
        val modeles = listOf(
            modele(5001, TypeDefi.MONOTYPE),
            modele(4001, TypeDefi.ROTATION),
            modele(2001, TypeDefi.ESPECE),
        )
        val arcko = EspeceCandidate(252, "Arcko", true, true, false, false, emptyList())
        val contexte = ContexteDefis(Difficulte.NORMAL, roxanne, "Arcko", listOf(arcko), emptyList(), false)
        assertEquals(listOf(5001, 4001, 2001), GenerateurDefis.generer(contexte, modeles).map { it.modele.id })

        val verrouille = contexte.copy(championOuvert = false)
        assertEquals(listOf(2001), GenerateurDefis.generer(verrouille, modeles).map { it.modele.id })

        val especeEnCours = contexte.copy(defisExistants = listOf(Defi(1, 2001, 999, null, EtatDefi.ACTIF, 0, null)))
        assertEquals(listOf(5001, 4001), GenerateurDefis.generer(especeEnCours, modeles).map { it.modele.id })
    }
}
