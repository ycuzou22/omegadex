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
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateurDefisTest {
    private val roxanne = Etape(1, "Roxanne", "Roxanne", "roche", 14, "Mérouville")

    private fun modele(
        id: Int,
        type: TypeDefi,
        difficulte: Difficulte = Difficulte.NORMAL,
        etapeId: Int? = null,
        gabarit: String = "Défi {espece}",
        etapeMin: Int = 0,
        conditions: Set<ConditionDeblocage> = emptySet(),
        parametres: Map<String, String> = emptyMap(),
    ) = ModeleDefi(id, type, etapeId, "Modèle $id", gabarit, parametres, difficulte, etapeMin, conditions)

    private val arcko = EspeceCandidate(
        id = 252,
        nom = "Arcko",
        dansEquipe = true,
        capturee = true,
        aUneMega = false,
        talentCache = true,
        evolutions = listOf(EvolutionPossible("Massko", ConditionEvolution.NIVEAU, "16")),
    )
    private val massko = arcko.copy(
        id = 253,
        nom = "Massko",
        evolutions = listOf(EvolutionPossible("Jungko", ConditionEvolution.NIVEAU, "36")),
    )
    private val gobou = arcko.copy(id = 258, nom = "Gobou", dansEquipe = false, evolutions = emptyList())

    private fun contexte(
        difficulte: Difficulte = Difficulte.NORMAL,
        especes: List<EspeceCandidate> = listOf(arcko),
        defis: List<Defi> = emptyList(),
        etape: Etape = roxanne,
        national: Boolean = false,
    ) = ContexteDefis(difficulte, etape, "Arcko", especes, defis, national)

    private val modelesEtape = listOf(
        modele(1011, TypeDefi.ETAPE, etapeId = 1, gabarit = "Battre {adversaire} sans {starter}"),
        modele(
            1012,
            TypeDefi.ETAPE,
            Difficulte.DIFFICILE,
            etapeId = 1,
            gabarit = "Battre {adversaire} sous niv. {niveau}",
            parametres = mapOf("ecart" to "2"),
        ),
        modele(1013, TypeDefi.ETAPE, Difficulte.HARDCORE, etapeId = 1),
        modele(1021, TypeDefi.ETAPE, etapeId = 2),
    )

    private fun ids(defis: List<DefiPropose>) = defis.map { it.modele.id }

    @Test
    fun `chaque difficulte ajoute ses defis a ceux des difficultes inferieures`() {
        assertEquals(listOf(1011), ids(GenerateurDefis.generer(contexte(Difficulte.NORMAL), modelesEtape)))
        assertEquals(listOf(1011, 1012), ids(GenerateurDefis.generer(contexte(Difficulte.DIFFICILE), modelesEtape)))
        val hardcore = GenerateurDefis.generer(contexte(Difficulte.HARDCORE), modelesEtape)
        assertEquals(listOf(1011, 1012, 1013), ids(hardcore))
    }

    @Test
    fun `les defis d'etape visent l'etape en cours et remplissent les marqueurs sans depasser le plafond`() {
        val defis = GenerateurDefis.generer(contexte(Difficulte.DIFFICILE), modelesEtape)
        assertTrue(defis.all { it.etapeId == 1 && it.especeId == null })
        assertEquals("Battre Roxanne sans Arcko", defis[0].enonce)
        assertEquals("Battre Roxanne sous niv. 12", defis[1].enonce)
    }

    @Test
    fun `un modele n'est pas propose avant son etape de deblocage ni sans le national`() {
        val modeles = listOf(
            modele(4001, TypeDefi.ROTATION, etapeMin = 2, gabarit = "Rotation"),
            modele(9001, TypeDefi.ESPECE, conditions = setOf(ConditionDeblocage.POKEDEX_NATIONAL)),
        )
        assertTrue(GenerateurDefis.generer(contexte(), modeles).isEmpty())
        val ligue = Etape(10, "Ligue Pokémon", "Pierre Rochard", null, 59, "Éternara")
        val aLaLigue = GenerateurDefis.generer(contexte(etape = ligue, national = true), modeles)
        assertEquals(listOf(4001, 9001), ids(aLaLigue))
    }

    @Test
    fun `une evolution n'est proposee que si elle est atteignable sous le plafond`() {
        val evolution = modele(
            3001,
            TypeDefi.EVOLUTION,
            gabarit = "Faire évoluer {espece} en {evolution} au niveau {niveau} avant {adversaire}",
            conditions = setOf(ConditionDeblocage.ESPECE_CAPTUREE, ConditionDeblocage.EVOLUTION_SOUS_PLAFOND),
        )
        val bastien = Etape(2, "Bastien", "Bastien", "combat", 16, "Myokara")
        val especes = listOf(arcko, massko, gobou)
        assertTrue(GenerateurDefis.generer(contexte(especes = especes), listOf(evolution)).isEmpty())
        val defis = GenerateurDefis.generer(contexte(especes = especes, etape = bastien), listOf(evolution))
        assertEquals(listOf(252), defis.map { it.especeId })
        assertEquals("Faire évoluer Arcko en Massko au niveau 16 avant Bastien", defis.single().enonce)
    }

    @Test
    fun `les conditions d'espece filtrent les candidats`() {
        val equipe = modele(2001, TypeDefi.ESPECE, conditions = setOf(ConditionDeblocage.ESPECE_DANS_EQUIPE))
        val mega = modele(7001, TypeDefi.MEGA, conditions = setOf(ConditionDeblocage.MEGA_DISPONIBLE))
        val talent = modele(6001, TypeDefi.NAVI_DEX, conditions = setOf(ConditionDeblocage.TALENT_CACHE))
        val especes = listOf(arcko, gobou.copy(talentCache = false))
        val defis = GenerateurDefis.generer(contexte(especes = especes), listOf(equipe, mega, talent))
        assertEquals(listOf(2001 to 252, 6001 to 252), defis.map { it.modele.id to it.especeId })
    }

    @Test
    fun `un seul defi espece actif par pokemon et pas de defi deja actif ou reussi`() {
        val equipe = modele(2001, TypeDefi.ESPECE, conditions = setOf(ConditionDeblocage.ESPECE_DANS_EQUIPE))
        val autre = modele(2002, TypeDefi.ESPECE)
        val actif = Defi(1, 2001, 252, null, EtatDefi.ACTIF, 0, null)
        assertTrue(GenerateurDefis.generer(contexte(defis = listOf(actif)), listOf(equipe, autre)).isEmpty())

        val reussi = actif.copy(etat = EtatDefi.REUSSI)
        val apresReussite = GenerateurDefis.generer(contexte(defis = listOf(reussi)), listOf(equipe, autre))
        assertEquals(listOf(2002), ids(apresReussite))

        val rate = actif.copy(etat = EtatDefi.RATE)
        val apresEchec = GenerateurDefis.generer(contexte(defis = listOf(rate)), listOf(equipe, autre))
        assertEquals(listOf(2001, 2002), ids(apresEchec))

        val etapeActive = Defi(2, 1011, null, 1, EtatDefi.ACTIF, 0, null)
        assertTrue(GenerateurDefis.generer(contexte(defis = listOf(etapeActive)), modelesEtape).isEmpty())
    }

    @Test
    fun `le marqueur type liste les types en francais`() {
        val monotype = modele(
            5014,
            TypeDefi.MONOTYPE,
            gabarit = "Battre {adversaire} avec des Pokémon {type}",
            parametres = mapOf("type" to "eau,sol"),
        )
        val enonce = GenerateurDefis.generer(contexte(), listOf(monotype)).single().enonce
        assertEquals("Battre Roxanne avec des Pokémon Eau ou Sol", enonce)
        assertEquals("Électrik", NomsTypes.nom("electrik"))
    }
}
