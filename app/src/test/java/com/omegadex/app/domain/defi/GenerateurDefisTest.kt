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
    fun `un seul defi de combat par adversaire, libere par un echec`() {
        val plante = modele(5012, TypeDefi.MONOTYPE, parametres = mapOf("type" to "plante"))
        val tenebres = modele(5017, TypeDefi.MONOTYPE, parametres = mapOf("type" to "tenebres"))
        val rotation = modele(4001, TypeDefi.ROTATION)
        val espece = modele(2002, TypeDefi.ESPECE)
        val modeles = modelesEtape + listOf(plante, tenebres, rotation, espece)
        val accepte = Defi(1, 5012, null, 1, EtatDefi.ACTIF, 0, null)

        // Monotype Plante accepté contre Roxanne : plus aucun autre combat, les défis espèce restent.
        assertEquals(listOf(2002), ids(GenerateurDefis.generer(contexte(defis = listOf(accepte)), modeles)))
        val reussi = accepte.copy(etat = EtatDefi.REUSSI)
        assertEquals(listOf(2002), ids(GenerateurDefis.generer(contexte(defis = listOf(reussi)), modeles)))

        // Raté : les défis de combat reviennent.
        val rate = accepte.copy(etat = EtatDefi.RATE)
        assertTrue(5017 in ids(GenerateurDefis.generer(contexte(defis = listOf(rate)), modeles)))

        // Un défi de combat réussi contre un autre adversaire ne bloque pas Roxanne.
        val ailleurs = accepte.copy(etapeId = 0, etat = EtatDefi.REUSSI)
        assertTrue(1011 in ids(GenerateurDefis.generer(contexte(defis = listOf(ailleurs)), modeles)))
    }

    @Test
    fun `a la validation de l'etape, seuls ses defis de combat actifs sont a clore`() {
        fun affiche(id: Long, modele: ModeleDefi, etapeId: Int?, etat: EtatDefi = EtatDefi.ACTIF) =
            DefiAffiche(Defi(id, modele.id, null, etapeId, etat, 0, null), modele, "Défi $id", null)
        val combat = affiche(1, modele(5012, TypeDefi.MONOTYPE), etapeId = 1)
        val autreEtape = affiche(2, modele(1021, TypeDefi.ETAPE), etapeId = 2)
        val espece = affiche(3, modele(2002, TypeDefi.ESPECE), etapeId = null)
        val run =
            DefisDeLaRun(Difficulte.HARDCORE, listOf(combat, autreEtape, espece), emptyList(), BilanDefis(3, 0, 0))

        assertEquals(listOf(combat), defisAClore(run, etapeId = 1))
        assertTrue(defisAClore(null, etapeId = 1).isEmpty())
    }

    @Test
    fun `en partie 0, les evolutions visent la prochaine arene`() {
        val depart = Etape(0, "Départ", "Rival", null, 5, "Bourg-en-Vol")
        val armulys = arcko.copy(
            id = 266,
            nom = "Armulys",
            evolutions = listOf(EvolutionPossible("Charmillon", ConditionEvolution.NIVEAU, "10")),
        )
        val evolution = modele(
            3001,
            TypeDefi.EVOLUTION,
            gabarit = "Faire évoluer {espece} en {evolution} avant {adversaire}, sans dépasser le niveau {niveau}",
            conditions = setOf(ConditionDeblocage.ESPECE_CAPTUREE, ConditionDeblocage.EVOLUTION_SOUS_PLAFOND),
        )
        val contexte = contexte(especes = listOf(armulys), etape = depart)
        assertTrue("plafond du rival : 5", GenerateurDefis.generer(contexte, listOf(evolution)).isEmpty())

        val arene = ProchaineArene.de(depart, listOf(depart, roxanne))
        assertEquals(roxanne, arene)
        val propose = GenerateurDefis.generer(contexte.copy(arene = arene), listOf(evolution)).single()
        assertEquals("Faire évoluer Armulys en Charmillon avant Roxanne, sans dépasser le niveau 10", propose.enonce)
        assertEquals(roxanne, ProchaineArene.de(roxanne, listOf(depart, roxanne)))
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
