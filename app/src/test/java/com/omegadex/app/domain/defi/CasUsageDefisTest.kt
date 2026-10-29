package com.omegadex.app.domain.defi

import com.omegadex.app.domain.model.AttaqueApprise
import com.omegadex.app.domain.model.ConditionDeblocage
import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Etape
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ModeleDefi
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.Quete
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.model.TalentEspece
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.RunActiveRepository
import com.omegadex.app.domain.run.ObserverRunEnCoursUseCase
import com.omegadex.app.testutil.FausseSession
import com.omegadex.app.testutil.FauxComptes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cas d'usage du moteur de défis (T11.4) sur des dépôts en mémoire, sans base ni Robolectric :
 * proposer, accepter, suivre, valider et retenter les défis d'une run.
 */
class CasUsageDefisTest {
    private val etapes = listOf(
        Etape(0, "Départ", "Rival", null, 20, "Bourg-en-Vol"),
        Etape(1, "Roxanne", "Roxanne", "roche", 14, "Mérouville"),
    )
    private val especes = listOf(
        espece(252, "Arcko"),
        espece(253, "Massko"),
        espece(258, "Gobou"),
        espece(261, "Medhyèna"),
    )
    private val modeles = listOf(
        modele(1001, TypeDefi.ETAPE, etapeId = 0, gabarit = "Battre {adversaire} avec {starter}"),
        modele(2001, TypeDefi.ESPECE, gabarit = "Capturer avec {espece}", conditions = setOf(CAPTUREE)),
        modele(2002, TypeDefi.ESPECE, gabarit = "Garder {espece}", conditions = setOf(DANS_EQUIPE)),
        modele(
            3001,
            TypeDefi.EVOLUTION,
            gabarit = "Faire évoluer {espece} en {evolution} au niveau {niveau}",
            conditions = setOf(CAPTUREE, ConditionDeblocage.EVOLUTION_SOUS_PLAFOND),
        ),
        modele(9001, TypeDefi.ESPECE, conditions = setOf(ConditionDeblocage.POKEDEX_NATIONAL)),
    )
    private val evolutions = listOf(Evolution(252, 253, ConditionEvolution.NIVEAU, "16"))
    private val jeu = FauxJeu(etapes, especes, modeles, evolutions)
    private val session = FausseSession()
    private val progression = FausseProgression()
    private val comptes = ComptesAvecRun(Run(RUN, 1, "Run", 252, Difficulte.NORMAL, emptyList(), 0, null))
    private val runEnCours = ObserverRunEnCoursUseCase(session, RunActiveFixe(), comptes, progression, jeu)
    private val proposer = ProposerDefisUseCase(runEnCours, progression, jeu)
    private val observer = ObserverDefisUseCase(runEnCours, progression, jeu)
    private val accepter = AccepterDefiUseCase(progression)
    private val valider = ValiderDefiUseCase(progression)
    private val retenter = RetenterDefiUseCase(progression)

    private suspend fun demarrerRun() {
        session.ouvrir(1)
        progression.placerMembre(RUN, MembreEquipe(1, 252, null, 12))
        progression.definirStatut(RUN, 258, StatutEspece.CAPTUREE)
        progression.definirStatut(RUN, 261, StatutEspece.VUE)
    }

    @Test
    fun `sans joueur connecte, rien n'est propose ni suivi`() = runTest {
        assertTrue(proposer().first().isEmpty())
        assertNull(observer().first())
    }

    @Test
    fun `les defis proposes respectent l'etape, les captures, l'equipe et le plafond`() = runTest {
        demarrerRun()
        val proposes = proposer().first()
        val parModele = proposes.groupBy({ it.modele.id }, { it.especeId })
        assertEquals(listOf(null), parModele[1001])
        assertEquals("équipe et captures, pas une espèce seulement vue", listOf(252, 258), parModele[2001])
        assertEquals(listOf(252), parModele[2002])
        assertEquals("évolution d'Arcko au niveau 16 sous le plafond 20", listOf(252), parModele[3001])
        assertNull("national pas encore obtenu", parModele[9001])
        assertEquals("Battre Rival avec Arcko", proposes.first { it.modele.id == 1001 }.enonce)
        assertEquals("Faire évoluer Arcko en Massko au niveau 16", proposes.first { it.modele.id == 3001 }.enonce)
    }

    @Test
    fun `apres la derniere etape, plus aucune etape en cours donc plus de proposition`() = runTest {
        demarrerRun()
        etapes.forEach { progression.validerEtape(RUN, it.id) }
        assertTrue(proposer().first().isEmpty())
    }

    @Test
    fun `accepter un second defi espece pour le meme pokemon est refuse`() = runTest {
        demarrerRun()
        val proposes = proposer().first()
        assertTrue(accepter(RUN, proposes.first { it.modele.id == 1001 }).isSuccess)
        assertTrue(accepter(RUN, proposes.first { it.modele.id == 2001 && it.especeId == 252 }).isSuccess)
        val refus = accepter(RUN, proposes.first { it.modele.id == 2002 })
        assertEquals(RefusDefi.ESPECE_DEJA_ENGAGEE.name, refus.exceptionOrNull()?.message)
        assertTrue(accepter(RUN, proposes.first { it.especeId == 258 }).isSuccess)
        assertEquals(3, progression.defis(RUN).first().size)
        assertTrue("Arcko est occupé", proposer().first().none { it.especeId == 252 })
    }

    @Test
    fun `le suivi affiche les enonces, trie actifs et historique et compte le bilan`() = runTest {
        demarrerRun()
        val etape = progression.ajouterDefi(RUN, 1001, null, 0)
        val evolution = progression.ajouterDefi(RUN, 3001, 252, null)
        val capture = progression.ajouterDefi(RUN, 2001, 258, null)
        progression.ajouterDefi(RUN, 4242, null, 0)
        assertTrue(valider(RUN, capture, reussi = false))
        assertTrue(valider(RUN, etape, reussi = true))

        val suivi = requireNotNull(observer().first())
        assertEquals(Difficulte.NORMAL, suivi.difficulte)
        assertEquals("un modèle inconnu n'est pas affiché", listOf(evolution), suivi.actifs.map { it.defi.id })
        assertEquals("le plus récent d'abord", listOf(etape, capture), suivi.historique.map { it.defi.id })
        val actif = suivi.actifs.single()
        assertEquals("Arcko", actif.especeNom)
        assertEquals("Faire évoluer Arcko en Massko au niveau 16", actif.enonce)
        assertEquals("Battre Rival avec Arcko", suivi.historique.first().enonce)
        assertEquals(BilanDefis(actifs = 2, reussis = 1, rates = 1), suivi.bilan)
    }

    @Test
    fun `valider ne change qu'un defi actif`() = runTest {
        demarrerRun()
        val defi = progression.ajouterDefi(RUN, 1001, null, 0)
        assertTrue(valider(RUN, defi, reussi = true))
        assertFalse(valider(RUN, defi, reussi = false))
        assertFalse(valider(RUN, defiId = 99, reussi = true))
        assertEquals(EtatDefi.REUSSI, progression.defis(RUN).first().single().etat)
    }

    @Test
    fun `retenter un defi rate une seule fois et seulement si le pokemon est libre`() = runTest {
        demarrerRun()
        val defi = progression.ajouterDefi(RUN, 2001, 252, null)
        assertNull("actif", retenter(RUN, defi))
        valider(RUN, defi, reussi = false)

        val autre = progression.ajouterDefi(RUN, 2002, 252, null)
        assertNull("Arcko porte un autre défi actif", retenter(RUN, defi))
        valider(RUN, autre, reussi = true)

        val nouveau = requireNotNull(retenter(RUN, defi))
        assertNull("déjà retenté", retenter(RUN, defi))
        val repris = progression.defis(RUN).first().single { it.id == nouveau }
        assertEquals(EtatDefi.ACTIF, repris.etat)
        assertEquals(2001, repris.modeleId)

        val etape = progression.ajouterDefi(RUN, 1001, null, 0)
        valider(RUN, etape, reussi = false)
        assertNotNull("un défi d'étape raté se retente", retenter(RUN, etape))
    }

    private companion object {
        val CAPTUREE = ConditionDeblocage.ESPECE_CAPTUREE
        val DANS_EQUIPE = ConditionDeblocage.ESPECE_DANS_EQUIPE

        fun espece(id: Int, nom: String) = Espece(
            id = id,
            numeroHoenn = id,
            nom = nom,
            types = listOf("plante"),
            categorie = "",
            tailleM = 0.0,
            poidsKg = 0.0,
            stats = Stats(1, 1, 1, 1, 1, 1),
            evDonnes = "",
            tauxCapture = 0,
            ratioMale = null,
            groupesOeufs = emptyList(),
            aUneMega = false,
        )

        fun modele(
            id: Int,
            type: TypeDefi,
            etapeId: Int? = null,
            gabarit: String = "Défi {espece}",
            conditions: Set<ConditionDeblocage> = emptySet(),
        ) = ModeleDefi(id, type, etapeId, "Modèle $id", gabarit, emptyMap(), Difficulte.NORMAL, 0, conditions)
    }
}

private const val RUN = 7L

/** Base jeu en mémoire : seules les données utiles au moteur de défis sont fournies. */
private class FauxJeu(
    private val etapes: List<Etape>,
    private val especes: List<Espece>,
    private val modeles: List<ModeleDefi>,
    private val evolutions: List<Evolution>,
) : JeuRepository {
    override fun etapes() = flowOf(etapes)

    override fun etape(id: Int) = flowOf(etapes.firstOrNull { it.id == id })

    override fun quetes(etapeId: Int) = flowOf(emptyList<Quete>())

    override fun lieuxDebloques(etapeId: Int) = flowOf(emptyList<Lieu>())

    override fun lieu(id: Int) = flowOf<Lieu?>(null)

    override fun quetesDuLieu(lieuId: Int) = flowOf(emptyList<Quete>())

    override fun modelesDefis() = flowOf(modeles)

    override fun pokedexHoenn() = flowOf(especes)

    override fun pokedexNational() = flowOf(especes)

    override fun espece(id: Int) = flowOf(especes.firstOrNull { it.id == id })

    override fun talents(especeId: Int) = flowOf(emptyList<TalentEspece>())

    override fun famille(especeId: Int) =
        flowOf(evolutions.filter { it.especeSourceId == especeId || it.especeCibleId == especeId })

    override fun attaquesApprises(especeId: Int) = flowOf(emptyList<AttaqueApprise>())

    override fun lieuxDeLEspece(especeId: Int) = flowOf(emptyList<Rencontre>())

    override fun especesDuLieu(lieuId: Int) = flowOf(emptyList<Rencontre>())

    override fun especesAccessibles(etapeId: Int) = flowOf(emptySet<Int>())

    override fun especesEchangeRequis() = flowOf(emptySet<Int>())
}

/** Progression d'une run en mémoire ; les dates avancent d'une unité à chaque écriture. */
private class FausseProgression : ProgressionRepository {
    private var horloge = 0L
    private val suivi = MutableStateFlow(listOf<ProgressionEtape>())
    private val statutsFlux = MutableStateFlow(mapOf<Int, StatutEspece>())
    private val equipeFlux = MutableStateFlow(listOf<MembreEquipe>())
    private val defisFlux = MutableStateFlow(listOf<Defi>())

    override fun etapes(runId: Long): Flow<List<ProgressionEtape>> = suivi

    override suspend fun deverrouillerEtape(runId: Long, etapeId: Int) {
        suivi.value = suivi.value.filterNot { it.etapeId == etapeId } + ProgressionEtape(etapeId, true, ++horloge, null)
    }

    override suspend fun validerEtape(runId: Long, etapeId: Int) {
        suivi.value = suivi.value.filterNot { it.etapeId == etapeId } +
            ProgressionEtape(etapeId, true, ++horloge, horloge)
    }

    override fun quetesCochees(runId: Long) = flowOf(emptySet<Int>())

    override suspend fun cocherQuete(runId: Long, queteId: Int, cochee: Boolean) = Unit

    override fun statuts(runId: Long): Flow<Map<Int, StatutEspece>> = statutsFlux

    override suspend fun definirStatut(runId: Long, especeId: Int, statut: StatutEspece) {
        statutsFlux.value = statutsFlux.value + (especeId to statut)
    }

    override fun equipe(runId: Long): Flow<List<MembreEquipe>> = equipeFlux

    override suspend fun placerMembre(runId: Long, membre: MembreEquipe) {
        equipeFlux.value = equipeFlux.value.filterNot { it.emplacement == membre.emplacement } + membre
    }

    override suspend fun retirerMembre(runId: Long, emplacement: Int) {
        equipeFlux.value = equipeFlux.value.filterNot { it.emplacement == emplacement }
    }

    override fun defis(runId: Long): Flow<List<Defi>> = defisFlux

    override suspend fun ajouterDefi(runId: Long, modeleId: Int, especeId: Int?, etapeId: Int?): Long {
        val id = defisFlux.value.size + 1L
        defisFlux.value = defisFlux.value + Defi(id, modeleId, especeId, etapeId, EtatDefi.ACTIF, ++horloge, null)
        return id
    }

    override suspend fun changerEtatDefi(runId: Long, defiId: Long, etat: EtatDefi) {
        defisFlux.value = defisFlux.value.map { if (it.id == defiId) it.copy(etat = etat, dateFin = ++horloge) else it }
    }
}

/** Le compte connecté possède une seule run. */
private class ComptesAvecRun(private val run: Run) : CompteRepository by FauxComptes() {
    override fun runs(utilisateurId: Long) = flowOf(listOf(run))

    override fun run(utilisateurId: Long, runId: Long) = flowOf(run.takeIf { it.id == runId })
}

private class RunActiveFixe : RunActiveRepository {
    override fun runActive(utilisateurId: Long): Flow<Long?> = flowOf(RUN)

    override suspend fun choisir(utilisateurId: Long, runId: Long) = Unit
}
