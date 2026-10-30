package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.TypeDefi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BanqueDefisTest {
    private val roxanne = Etape(1, "Roxanne", "Roxanne", "roche", 14, "Mérouville")

    private fun defiBanque(
        n: Int,
        cible: Int = 252,
        difficulte: Difficulte = Difficulte.NORMAL,
        etapeMin: Int = 0,
        conditions: Set<ConditionDeblocage> = emptySet(),
        gabarit: String = "Battre {adversaire} avec Arcko (défi $n)",
    ) = ModeleDefi(
        id = 352_000 + n,
        type = TypeDefi.ESPECE,
        etapeId = null,
        titre = "Défi $n",
        gabarit = gabarit,
        parametres = mapOf("famille" to "252", "cible" to cible.toString(), "categorie" to "combat"),
        difficulteMin = difficulte,
        etapeMin = etapeMin,
        conditions = conditions,
    )

    private val arcko = EspeceCandidate(
        id = 252,
        nom = "Arcko",
        dansEquipe = true,
        capturee = true,
        aUneMega = false,
        talentCache = false,
        evolutions = listOf(EvolutionPossible("Massko", ConditionEvolution.NIVEAU, "16")),
    )

    private fun contexte(defis: List<Defi> = emptyList(), difficulte: Difficulte = Difficulte.NORMAL) =
        ContexteDefis(difficulte, roxanne, "Arcko", listOf(arcko), defis, nationalObtenu = false)

    private fun defi(id: Long, modeleId: Int, etat: EtatDefi, especeId: Int? = 252) =
        Defi(id, modeleId, especeId, null, etat, dateDebut = id, dateFin = null)

    @Test
    fun `la racine d'une famille est sa plus petite espece qui n'est l'evolution d'aucune autre`() {
        val lignee = listOf(
            Evolution(252, 253, ConditionEvolution.NIVEAU, "16"),
            Evolution(253, 254, ConditionEvolution.NIVEAU, "36"),
        )
        assertEquals(252, BanqueDefis.racine(254, lignee))
        assertEquals(252, BanqueDefis.racine(252, lignee))
        assertEquals("une espèce sans évolution est sa propre racine", 359, BanqueDefis.racine(359, emptyList()))
        val pichu = listOf(
            Evolution(172, 25, ConditionEvolution.BONHEUR, null),
            Evolution(25, 26, ConditionEvolution.PIERRE, "Pierre Foudre"),
        )
        assertEquals("le bébé est la racine", 172, BanqueDefis.racine(26, pichu))
    }

    @Test
    fun `les identifiants a partir de 100000 sont ceux de la banque`() {
        assertTrue(BanqueDefis.estDeLaBanque(352_001))
        assertFalse(BanqueDefis.estDeLaBanque(8_002))
        assertEquals(252, BanqueDefis.cible(defiBanque(1)))
    }

    @Test
    fun `le tirage ne garde que les defis de l'espece, debloques et de la bonne difficulte`() {
        val banque = listOf(
            defiBanque(1),
            defiBanque(2, cible = 253),
            defiBanque(3, etapeMin = 5),
            defiBanque(4, difficulte = Difficulte.HARDCORE),
            defiBanque(5, conditions = setOf(ConditionDeblocage.MEGA_DISPONIBLE)),
        )
        assertEquals(listOf(352_001), BanqueDefis.eligibles(banque, arcko, contexte()).map { it.id })
        val hardcore = BanqueDefis.eligibles(banque, arcko, contexte(difficulte = Difficulte.HARDCORE))
        assertEquals(listOf(352_001, 352_004), hardcore.map { it.id })
    }

    @Test
    fun `le tirage est stable pour une meme graine et remplit l'enonce`() {
        val banque = (1..20).map { defiBanque(it) }
        val graine = BanqueDefis.graine(runId = 1, especeId = 252, etapeId = 1, defisTermines = 0)
        val tire = requireNotNull(BanqueDefis.tirer(banque, arcko, contexte(), graine))
        assertEquals(tire, BanqueDefis.tirer(banque, arcko, contexte(), graine))
        assertEquals(252, tire.especeId)
        assertNull(tire.etapeId)
        assertEquals("Arcko", tire.especeNom)
        assertTrue(tire.enonce, tire.enonce.startsWith("Battre Roxanne avec Arcko"))
        val tirages = (0..30).map {
            BanqueDefis.tirer(banque, arcko, contexte(), BanqueDefis.graine(1, 252, 1, it))?.modele?.id
        }
        assertTrue("la graine fait varier le défi tiré", tirages.toSet().size > 1)
    }

    @Test
    fun `aucun tirage pour un pokemon qui releve deja un defi espece`() {
        val banque = (1..5).map { defiBanque(it) }
        val occupe = contexte(defis = listOf(defi(1, 2001, EtatDefi.ACTIF)))
        assertNull(BanqueDefis.tirer(banque, arcko, occupe, graine = 7))
    }

    @Test
    fun `un defi deja releve n'est plus tire, meme rate`() {
        val banque = (1..3).map { defiBanque(it) }
        val historique = listOf(defi(1, 352_001, EtatDefi.REUSSI), defi(2, 352_002, EtatDefi.RATE))
        val restant = BanqueDefis.eligibles(banque, arcko, contexte(defis = historique))
        assertEquals(listOf(352_003), restant.map { it.id })
        assertNotNull(BanqueDefis.tirer(banque, arcko, contexte(defis = historique), graine = 3))
        val tout = historique + defi(3, 352_003, EtatDefi.REUSSI)
        assertNull("banque épuisée", BanqueDefis.tirer(banque, arcko, contexte(defis = tout), graine = 3))
    }

    @Test
    fun `les conditions de la banque suivent l'espece`() {
        val mega = defiBanque(1, conditions = setOf(ConditionDeblocage.MEGA_DISPONIBLE))
        val horsEquipe = defiBanque(2, conditions = setOf(ConditionDeblocage.ESPECE_DANS_EQUIPE))
        val concours = defiBanque(3, conditions = setOf(ConditionDeblocage.CONCOURS_OUVERTS))
        val banque = listOf(mega, horsEquipe, concours)
        assertEquals(listOf(352_002, 352_003), BanqueDefis.eligibles(banque, arcko, contexte()).map { it.id })
        val megaArcko = arcko.copy(aUneMega = true, dansEquipe = false)
        assertEquals(
            listOf(352_001, 352_003),
            BanqueDefis.eligibles(banque, megaArcko, contexte()).map { it.id },
        )
    }
}
