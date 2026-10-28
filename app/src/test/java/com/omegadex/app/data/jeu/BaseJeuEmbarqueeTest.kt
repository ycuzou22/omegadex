package com.omegadex.app.data.jeu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.omegadex.app.di.DatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * La base jeu embarquée s'ouvre hors ligne au premier lancement
 * et remplace la copie locale à chaque nouvelle version.
 */
@RunWith(RobolectricTestRunner::class)
class BaseJeuEmbarqueeTest {
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
    fun `la base embarquee est copiee et porte une version de donnees`() = runTest {
        val version = base.versionDonneesDao().version().first()
        assertNotNull(version)
        assertEquals(1, version?.id)
    }

    @Test
    fun `une nouvelle version de la base remplace la copie locale par la base embarquee`() = runTest {
        base.versionDonneesDao().version().first()
        base.openHelper.writableDatabase.execSQL("UPDATE version_donnees SET version = 99")
        base.openHelper.writableDatabase.version = JeuDatabase.VERSION - 1
        base.close()

        base = DatabaseModule.jeuDatabase(context)
        assertEquals(VERSION_EMBARQUEE, base.versionDonneesDao().version().first()?.version)
    }

    private companion object {
        const val VERSION_EMBARQUEE = 1
    }
}
