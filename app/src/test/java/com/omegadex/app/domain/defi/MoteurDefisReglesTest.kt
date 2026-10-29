package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.TypeDefi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles du moteur de défis (T11.4) : plafond, Hardcore, déblocage par étape et défi Évolution atteignable.
 * Un gabarit « {niveau} » sert de sonde : l'énoncé généré est alors le niveau fixé par le défi.
 */
class MoteurDefisReglesTest {
    private fun etape(id: Int, plafond: Int) = Etape(id, "Étape $id", "Champion $id", null, plafond, "Lieu $id")

    private fun modele(
        id: Int,
        type: TypeDefi,
        difficulte: Difficulte = Difficulte.NORMAL,
        etapeId: Int? = null,
        etapeMin: Int = 0,
        conditions: Set<ConditionDeblocage> = emptySet(),
        parametres: Map<String, String> = emptyMap(),
        gabarit: String = SONDE_NIVEAU,
    ) = ModeleDefi(id, type, etapeId, "Modèle $id", gabarit, parametres, difficulte, etapeMin, conditions)

    private fun espece(
        id: Int,
        evolutions: List<EvolutionPossible> = emptyList(),
        dansEquipe: Boolean = true,
        capturee: Boolean = true,
    ) = EspeceCandidate(
        id = id,
        nom = "Espèce $id",
        dansEquipe = dansEquipe,
        capturee = capturee,
        aUneMega = false,
        talentCache = false,
        evolutions = evolutions,
    )

    private fun contexte(
        etape: Etape,
        difficulte: Difficulte = Difficulte.NORMAL,
        especes: List<EspeceCandidate> = emptyList(),
        defis: List<Defi> = emptyList(),
        national: Boolean = false,
    ) = ContexteDefis(difficulte, etape, "Arcko", especes, defis, national)

    private fun niveaux(defis: List<DefiPropose>) = defis.map { it.enonce.toInt() }

    /** Défis proposés à l'étape donnée pour ces espèces, en Normal et sans défi existant. */
    private fun proposes(etape: Etape, especes: List<EspeceCandidate>, vararg modeles: ModeleDefi) =
        GenerateurDefis.generer(contexte(etape, especes = especes), modeles.toList())

    private val evolutionModele = modele(
        3001,
        TypeDefi.EVOLUTION,
        conditions = setOf(ConditionDeblocage.ESPECE_CAPTUREE, ConditionDeblocage.EVOLUTION_SOUS_PLAFOND),
    )

    // --- Plafond ---

    @Test
    fun `aucun defi d'etape ne fixe un niveau au-dessus du plafond, quel que soit l'ecart`() {
        val plafonds = listOf(5, 14, 36, 59)
        plafonds.forEachIndexed { index, plafond ->
            val courante = etape(index, plafond)
            val modeles = (0..plafond).map { ecart ->
                modele(1000 + ecart, TypeDefi.ROTATION, parametres = mapOf("ecart" to ecart.toString()))
            }
            val proposes = GenerateurDefis.generer(contexte(courante), modeles)
            assertEquals(plafond + 1, proposes.size)
            assertTrue("plafond $plafond", niveaux(proposes).all { it in 0..plafond })
            assertEquals((plafond downTo 0).toList(), niveaux(proposes))
        }
    }

    @Test
    fun `un ecart absent ou illisible vaut le plafond, un ecart negatif est refuse`() {
        val courante = etape(3, plafond = 24)
        val sansEcart = modele(1, TypeDefi.ROTATION)
        val illisible = modele(2, TypeDefi.ROTATION, parametres = mapOf("ecart" to "beaucoup"))
        assertEquals(listOf(24, 24), niveaux(GenerateurDefis.generer(contexte(courante), listOf(sansEcart, illisible))))

        val auDessus = modele(3, TypeDefi.ROTATION, parametres = mapOf("ecart" to "-3"))
        assertThrows(IllegalArgumentException::class.java) {
            GenerateurDefis.generer(contexte(courante), listOf(auDessus))
        }
    }

    // --- Hardcore ---

    @Test
    fun `les defis hardcore ne sont proposes qu'en hardcore et s'ajoutent aux autres`() {
        val courante = etape(2, plafond = 18)
        val candidat = espece(252)
        val modeles = listOf(
            modele(1, TypeDefi.ETAPE, etapeId = 2),
            modele(2, TypeDefi.ETAPE, Difficulte.DIFFICILE, etapeId = 2),
            modele(3, TypeDefi.ETAPE, Difficulte.HARDCORE, etapeId = 2),
            modele(4, TypeDefi.ESPECE, Difficulte.HARDCORE),
            modele(5, TypeDefi.MONOTYPE, Difficulte.DIFFICILE),
        )
        val parDifficulte = Difficulte.entries.associateWith { difficulte ->
            GenerateurDefis.generer(contexte(courante, difficulte, listOf(candidat)), modeles)
                .map { it.modele.id }
                .toSet()
        }
        assertEquals(setOf(1), parDifficulte.getValue(Difficulte.NORMAL))
        assertEquals(setOf(1, 2, 5), parDifficulte.getValue(Difficulte.DIFFICILE))
        assertEquals(setOf(1, 2, 3, 4, 5), parDifficulte.getValue(Difficulte.HARDCORE))
    }

    @Test
    fun `la regle hardcore compte les niveaux manquants sans recompenser un depassement`() {
        assertEquals(0, ReglesDifficulte.niveauxManquantsHardcore(listOf(14, 14), plafond = 14))
        assertEquals(0, ReglesDifficulte.niveauxManquantsHardcore(emptyList(), plafond = 14))
        assertEquals(2, ReglesDifficulte.niveauxManquantsHardcore(listOf(12, 20), plafond = 14))
        assertFalse(ReglesDifficulte.respecteHardcore(listOf(12, 20), plafond = 14))
        assertEquals(listOf(20), ReglesDifficulte.auDessusDuPlafond(listOf(12, 20), plafond = 14))
    }

    // --- Déblocage par étape ---

    @Test
    fun `un modele se debloque exactement a son etape minimale`() {
        val rotation = modele(4001, TypeDefi.ROTATION, etapeMin = 4)
        assertTrue(GenerateurDefis.generer(contexte(etape(3, 30)), listOf(rotation)).isEmpty())
        assertEquals(1, GenerateurDefis.generer(contexte(etape(4, 30)), listOf(rotation)).size)
        assertEquals(1, GenerateurDefis.generer(contexte(etape(9, 50)), listOf(rotation)).size)
    }

    @Test
    fun `un defi d'etape ne vise que son etape et porte son identifiant`() {
        val modeles = listOf(
            modele(1011, TypeDefi.ETAPE, etapeId = 1),
            modele(1021, TypeDefi.ETAPE, etapeId = 2),
            modele(1099, TypeDefi.ETAPE, etapeId = null),
        )
        val proposes = GenerateurDefis.generer(contexte(etape(2, 18)), modeles)
        assertEquals(listOf(1021), proposes.map { it.modele.id })
        assertEquals(2, proposes.single().etapeId)
        assertNull(proposes.single().especeId)
    }

    @Test
    fun `un defi d'etape deja reussi n'est plus propose, un defi rate peut l'etre`() {
        val modeleEtape = modele(1021, TypeDefi.ETAPE, etapeId = 2)
        val courante = etape(2, 18)
        val reussi = Defi(1, 1021, null, 2, EtatDefi.REUSSI, 0, 1)
        val rate = reussi.copy(etat = EtatDefi.RATE)
        val ailleurs = reussi.copy(etapeId = 1)
        assertTrue(GenerateurDefis.generer(contexte(courante, defis = listOf(reussi)), listOf(modeleEtape)).isEmpty())
        assertEquals(1, GenerateurDefis.generer(contexte(courante, defis = listOf(rate)), listOf(modeleEtape)).size)
        assertEquals(1, GenerateurDefis.generer(contexte(courante, defis = listOf(ailleurs)), listOf(modeleEtape)).size)
    }

    @Test
    fun `les modeles du pokedex national attendent la ligue`() {
        val national = modele(9001, TypeDefi.ESPECE, conditions = setOf(ConditionDeblocage.POKEDEX_NATIONAL))
        val courante = etape(10, 59)
        val especes = listOf(espece(1))
        assertTrue(GenerateurDefis.generer(contexte(courante, especes = especes), listOf(national)).isEmpty())
        val apresLigue = GenerateurDefis.generer(
            contexte(courante, especes = especes, national = true),
            listOf(national),
        )
        assertEquals(listOf(1), apresLigue.map { it.especeId })
    }

    // --- Défi Évolution atteignable ---

    @Test
    fun `une evolution pile au plafond est proposee au niveau requis`() {
        val arcko = espece(252, listOf(EvolutionPossible("Massko", ConditionEvolution.NIVEAU, "16")))
        assertTrue(proposes(etape(1, 15), listOf(arcko), evolutionModele).isEmpty())
        assertEquals(listOf(16), niveaux(proposes(etape(2, 16), listOf(arcko), evolutionModele)))
        val auDela = proposes(etape(5, 40), listOf(arcko), evolutionModele)
        assertEquals("le défi garde le niveau d'évolution, pas le plafond", listOf(16), niveaux(auDela))
    }

    @Test
    fun `une evolution sans niveau est toujours atteignable et le defi vise le plafond`() {
        val pierre = espece(270, listOf(EvolutionPossible("Lombre", ConditionEvolution.PIERRE, "Pierre Eau")))
        val echange = espece(64, listOf(EvolutionPossible("Alakazam", ConditionEvolution.ECHANGE, null)))
        val defis = proposes(etape(0, 5), listOf(pierre, echange), evolutionModele)
        assertEquals(listOf(270, 64), defis.map { it.especeId })
        assertEquals(listOf(5, 5), niveaux(defis))
    }

    @Test
    fun `parmi plusieurs evolutions, la premiere atteignable sous le plafond est retenue`() {
        val evoli = espece(
            133,
            listOf(
                EvolutionPossible("Lointaine", ConditionEvolution.NIVEAU, "40"),
                EvolutionPossible("Aquali", ConditionEvolution.PIERRE, "Pierre Eau"),
                EvolutionPossible("Pyroli", ConditionEvolution.PIERRE, "Pierre Feu"),
            ),
        )
        assertEquals("Aquali", GenerateurDefis.evolutionAtteignable(evoli, plafond = 20)?.cibleNom)
        assertEquals("Lointaine", GenerateurDefis.evolutionAtteignable(evoli, plafond = 40)?.cibleNom)
        val gabaritCible = evolutionModele.copy(gabarit = "{espece} en {evolution}")
        assertEquals("Espèce 133 en Aquali", proposes(etape(3, 20), listOf(evoli), gabaritCible).single().enonce)
    }

    @Test
    fun `un niveau suivi d'une condition est lu et compare au plafond`() {
        val condition = "20 (Attaque > Défense)"
        val debugant = espece(236, listOf(EvolutionPossible("Kicklee", ConditionEvolution.NIVEAU, condition)))
        assertTrue(proposes(etape(1, 19), listOf(debugant), evolutionModele).isEmpty())
        assertEquals(listOf(20), niveaux(proposes(etape(2, 20), listOf(debugant), evolutionModele)))
    }

    @Test
    fun `une espece sans evolution ou non capturee ne recoit pas de defi evolution`() {
        val finale = espece(254)
        val vue = espece(
            255,
            listOf(EvolutionPossible("Galifeu", ConditionEvolution.NIVEAU, "16")),
            dansEquipe = false,
            capturee = false,
        )
        val sansCondition = evolutionModele.copy(conditions = emptySet())
        val contexte = contexte(etape(4, 30), especes = listOf(finale, vue))
        assertTrue(GenerateurDefis.generer(contexte, listOf(evolutionModele)).isEmpty())
        assertEquals(
            "le type Évolution exige une évolution même sans condition explicite",
            listOf(255),
            GenerateurDefis.generer(contexte, listOf(sansCondition)).map { it.especeId },
        )
    }

    // --- Un défi espèce actif par Pokémon ---

    @Test
    fun `seuls les defis espece actifs occupent un pokemon`() {
        val defis = listOf(
            Defi(1, 2001, 252, null, EtatDefi.ACTIF, 0, null),
            Defi(2, 2001, 255, null, EtatDefi.REUSSI, 0, 1),
            Defi(3, 2002, 258, null, EtatDefi.RATE, 0, 1),
            Defi(4, 1011, null, 1, EtatDefi.ACTIF, 0, null),
        )
        assertEquals(setOf(252), GenerateurDefis.especesOccupees(defis))
    }

    @Test
    fun `le bilan des defis compte les etats et le taux de reussite`() {
        assertEquals(0f, BilanDefis.de(emptyList()).tauxReussite)
        val defis = listOf(
            Defi(1, 1, null, 1, EtatDefi.ACTIF, 0, null),
            Defi(2, 2, null, 1, EtatDefi.REUSSI, 0, 1),
            Defi(3, 3, null, 1, EtatDefi.REUSSI, 0, 1),
            Defi(4, 4, null, 1, EtatDefi.RATE, 0, 1),
        )
        val bilan = BilanDefis.de(defis)
        assertEquals(BilanDefis(actifs = 1, reussis = 2, rates = 1), bilan)
        assertEquals(3, bilan.termines)
        assertEquals(2f / 3, bilan.tauxReussite)
    }

    private companion object {
        const val SONDE_NIVEAU = "{niveau}"
    }
}
