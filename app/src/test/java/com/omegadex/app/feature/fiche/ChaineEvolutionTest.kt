package com.omegadex.app.feature.fiche

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Evolution
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.domain.model.StatutEspece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaineEvolutionTest {
    private fun espece(id: Int, nom: String) = Espece(
        id = id,
        numeroHoenn = null,
        nom = nom,
        types = listOf("psy"),
        categorie = "",
        tailleM = 1.0,
        poidsKg = 1.0,
        stats = Stats(1, 1, 1, 1, 1, 1),
        evDonnes = "",
        tauxCapture = 45,
        ratioMale = 0.5,
        groupesOeufs = emptyList(),
        aUneMega = false,
    )

    private val tarsal = listOf(
        espece(280, "Tarsal"),
        espece(281, "Kirlia"),
        espece(282, "Gardevoir"),
        espece(475, "Gallame"),
    )
    private val familleTarsal = listOf(
        Evolution(280, 281, ConditionEvolution.NIVEAU, "20"),
        Evolution(281, 282, ConditionEvolution.NIVEAU, "30"),
        Evolution(281, 475, ConditionEvolution.PIERRE, "Pierre Aube (mâle)"),
    )

    @Test
    fun `une chaine a branches est rangee par stade avec ses conditions`() {
        val chaine = ChaineEvolution.construire(281, familleTarsal, tarsal, suivi = null)
        assertEquals(
            listOf(listOf("Tarsal"), listOf("Kirlia"), listOf("Gardevoir", "Gallame")),
            chaine.stades.map { s -> s.map { it.nom } },
        )
        assertFalse(chaine.lineaire)
        assertEquals("Pierre Aube (mâle)", chaine.palierVers(475)?.valeur)
        assertEquals(ConditionEvolution.NIVEAU to "20", chaine.palierVers(281)?.let { it.condition to it.valeur })
        assertNull(chaine.palierVers(280))
        assertNull(chaine.stadeActuel)
    }

    @Test
    fun `le stade actuel est celui de l'equipe, sinon la capture la plus evoluee`() {
        val statuts = mapOf(280 to StatutEspece.MAITRISEE, 281 to StatutEspece.CAPTUREE, 282 to StatutEspece.VUE)
        val captures = SuiviRun(statuts, emptyList())
        assertEquals(281, ChaineEvolution.construire(280, familleTarsal, tarsal, captures).stadeActuel)
        val equipe = captures.copy(equipe = listOf(MembreEquipe(1, 280, null, 12)))
        assertEquals(280, ChaineEvolution.construire(280, familleTarsal, tarsal, equipe).stadeActuel)
    }

    @Test
    fun `une espece jamais vue garde son sprite en ombre`() {
        val statuts = mapOf(280 to StatutEspece.CAPTUREE, 281 to StatutEspece.VUE, 282 to StatutEspece.NON_VUE)
        val chaine = ChaineEvolution.construire(280, familleTarsal, tarsal, SuiviRun(statuts, emptyList()))
        val vues = chaine.stades.flatten().associate { it.id to it.vue }
        assertEquals(mapOf(280 to true, 281 to true, 282 to false, 475 to false), vues)
        assertTrue(ChaineEvolution.construire(280, familleTarsal, tarsal, null).stades.flatten().all { it.vue })
    }

    @Test
    fun `une chaine simple de trois stades s'affiche en ligne, une espece seule n'a pas de palier`() {
        val gobou = listOf(espece(258, "Gobou"), espece(259, "Flobio"), espece(260, "Laggron"))
        val famille = listOf(
            Evolution(258, 259, ConditionEvolution.NIVEAU, "16"),
            Evolution(259, 260, ConditionEvolution.NIVEAU, "36"),
        )
        assertTrue(ChaineEvolution.construire(259, famille, gobou, null).lineaire)
        val seul = ChaineEvolution.construire(115, emptyList(), listOf(espece(115, "Kangourex")), null)
        assertEquals(listOf(listOf("Kangourex")), seul.stades.map { s -> s.map { it.nom } })
        assertTrue(seul.paliers.isEmpty())
    }
}
