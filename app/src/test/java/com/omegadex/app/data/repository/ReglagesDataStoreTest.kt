package com.omegadex.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.omegadex.app.domain.repository.Reglages
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReglagesDataStoreTest {
    @get:Rule
    val dossier = TemporaryFolder()

    @Test
    fun `sons et vibrations actifs, ambiance coupee par defaut, puis chaque reglage est garde`() = runTest {
        val portee = TestScope(UnconfinedTestDispatcher(testScheduler))
        val magasin = PreferenceDataStoreFactory.create(scope = portee) { File(dossier.root, "reglages.preferences_pb") }
        val reglages = ReglagesDataStore(magasin)

        assertEquals(Reglages(sons = true, vibrations = true, ambiance = false), reglages.reglages.first())

        reglages.activerSons(false)
        reglages.activerVibrations(false)
        reglages.activerAmbiance(true)

        assertEquals(Reglages(sons = false, vibrations = false, ambiance = true), reglages.reglages.first())
    }
}
