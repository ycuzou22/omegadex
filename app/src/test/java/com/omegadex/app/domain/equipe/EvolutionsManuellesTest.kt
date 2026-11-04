package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.ConditionEvolution
import com.omegadex.app.domain.model.ConditionEvolution.AUTRE
import com.omegadex.app.domain.model.ConditionEvolution.BONHEUR
import com.omegadex.app.domain.model.ConditionEvolution.ECHANGE
import com.omegadex.app.domain.model.ConditionEvolution.LIEU
import com.omegadex.app.domain.model.ConditionEvolution.NIVEAU
import com.omegadex.app.domain.model.ConditionEvolution.OBJET
import com.omegadex.app.domain.model.ConditionEvolution.PIERRE
import com.omegadex.app.domain.model.Evolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conditions recopiées de la base du jeu. */
class EvolutionsManuellesTest {
    private fun lire(condition: ConditionEvolution, valeur: String?) =
        EvolutionsManuelles.lire(Evolution(1, 2, condition, valeur))

    /** « méthode | détail | moment », pour comparer une ligne à la fois. */
    private fun resume(condition: ConditionEvolution, valeur: String?) =
        lire(condition, valeur)?.let { "${it.methode} | ${it.detail} | ${it.moment}" }

    @Test
    fun `pierre et echanges`() {
        assertEquals("PIERRE | Pierre Lune | null", resume(PIERRE, "Pierre Lune"))
        assertEquals("ECHANGE | null | null", resume(ECHANGE, null))
        assertEquals("ECHANGE_OBJET | Peau Métal | null", resume(ECHANGE, "en tenant Peau Métal"))
    }

    @Test
    fun `le bonheur sans precision d'heure n'est ni de jour ni de nuit`() {
        assertEquals(MethodeEvolution.BONHEUR, lire(BONHEUR, null)?.methode)
        assertEquals(MethodeEvolution.BONHEUR_JOUR, lire(BONHEUR, "de jour")?.methode)
        assertEquals(MethodeEvolution.BONHEUR_NUIT, lire(BONHEUR, "de nuit")?.methode)
        assertTrue(MethodeEvolution.BONHEUR.monteDUnNiveau)
    }

    @Test
    fun `objet tenu, champ magnetique et lieu`() {
        assertEquals("OBJET_TENU | Pierre Ovale | de jour", resume(OBJET, "Pierre Ovale tenu de jour"))
        assertEquals(MethodeEvolution.CHAMP_MAGNETIQUE, lire(LIEU, "New Lavandia")?.methode)
        assertEquals("LIEU | Bois Clémenti | null", resume(LIEU, "Bois Clémenti"))
    }

    @Test
    fun `beaute, attaque apprise et autres conditions`() {
        assertEquals(MethodeEvolution.BEAUTE, lire(AUTRE, "Beauté maximale")?.methode)
        assertEquals("ATTAQUE | Coup Double | null", resume(AUTRE, "connaître Coup Double"))
        assertEquals("ATTAQUE | Copie | null", resume(AUTRE, "connaître Copie"))
        val nymphali = lire(AUTRE, "connaître une attaque de type Fée et 2 cœurs d'affection (Poké Récré)")
        assertEquals(MethodeEvolution.AUTRE, nymphali?.methode)
        assertEquals(MethodeEvolution.AUTRE, lire(AUTRE, "avec Rémoraid dans l'équipe")?.methode)
    }

    @Test
    fun `ni le niveau ni Munja ne donnent de bouton`() {
        assertNull(lire(NIVEAU, "16"))
        assertNull(lire(AUTRE, "place libre et Poké Ball dans le sac"))
        assertFalse(MethodeEvolution.PIERRE.monteDUnNiveau)
        assertFalse(MethodeEvolution.ECHANGE_OBJET.monteDUnNiveau)
    }

    @Test
    fun `seules les evolutions de l'espece sont gardees`() {
        val famille = listOf(
            Evolution(133, 196, BONHEUR, "de jour"),
            Evolution(133, 136, PIERRE, "Pierre Feu"),
            Evolution(133, 134, PIERRE, "Pierre Eau"),
            Evolution(25, 26, PIERRE, "Pierre Foudre"),
        )
        assertEquals(listOf(196, 136, 134), EvolutionsManuelles.de(133, famille).map { it.evolution.especeCibleId })
    }
}
