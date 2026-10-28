package com.omegadex.app.domain.pokedex

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.di.DatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** La règle du national s'appuie sur la base jeu embarquée : on vérifie qu'elle y correspond. */
@RunWith(RobolectricTestRunner::class)
class DonneesPokedexNationalTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var base: JeuDatabase

    @Before
    fun ouvrir() {
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
        base = DatabaseModule.jeuDatabase(context)
    }

    @After
    fun fermer() {
        base.close()
        context.deleteDatabase(JeuDatabase.NOM_FICHIER)
    }

    @Test
    fun `l'etape de deblocage du national est bien la Ligue`() = runTest {
        val etape = JeuRepositoryImpl(base).etape(RegleNational.ETAPE_LIGUE).first()
        assertEquals("Ligue Pokémon", etape?.titre)
    }

    @Test
    fun `les especes a echanger existent et sont hors du Pokedex de Hoenn pour l'essentiel`() = runTest {
        val repository = JeuRepositoryImpl(base)
        val echanges = repository.especesEchangeRequis().first()
        val hoenn = repository.pokedexHoenn().first().map { it.id }.toSet()

        assertTrue(echanges.isNotEmpty())
        assertTrue((echanges - hoenn).size > echanges.size / 2)
    }
}
