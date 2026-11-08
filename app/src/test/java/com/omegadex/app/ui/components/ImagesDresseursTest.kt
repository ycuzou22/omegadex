package com.omegadex.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagesDresseursTest {
    @Test
    fun `un nom avec tirets, soulignes, majuscules ou accents designe la meme image`() {
        val noms = listOf("pierre-rochard", "Pierre_Rochard", "pierre rochard", "pierre_rochard")
        noms.forEach { assertEquals("pierre_rochard", ImagesDresseurs.normaliser(it)) }
        assertEquals("soeur_parasol", ImagesDresseurs.normaliser("Sœur Parasol"))
        assertEquals("levy_et_tatia", ImagesDresseurs.normaliser("Lévy & Tatia"))
    }

    @Test
    fun `la cle passe avant l'image de repli, quel que soit le separateur du fichier`() {
        val fichiers = listOf("sbire_de_la_team_magma.JPG", "max.png", "max_mont_chimnee.webp", "LISEZMOI.md")
        assertEquals("max_mont_chimnee.webp", ImagesDresseurs.trouverFichier(fichiers, "max-mont-chimnee", "max"))
        assertEquals("max.png", ImagesDresseurs.trouverFichier(fichiers, "max-caverne-fondmer", "max"))
        assertEquals(
            "sbire_de_la_team_magma.JPG",
            ImagesDresseurs.trouverFichier(fichiers, "bois-clementi-sbire", "sbire-de-la-team-magma"),
        )
        assertNull(ImagesDresseurs.trouverFichier(fichiers, "gamin", "gamin"))
        assertNull(ImagesDresseurs.trouverFichier(fichiers, "lisezmoi"))
    }
}
