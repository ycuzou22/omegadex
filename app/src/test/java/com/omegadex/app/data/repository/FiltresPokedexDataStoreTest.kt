package com.omegadex.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.pokedex.MethodeFiltre
import com.omegadex.app.domain.repository.FiltresMemorises
import com.omegadex.app.testutil.FausseSession
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FiltresPokedexDataStoreTest {
    @get:Rule
    val dossier = TemporaryFolder()

    @Test
    fun `les filtres sont gardes par compte et relus apres redemarrage`() = runTest {
        val portee = TestScope(UnconfinedTestDispatcher(testScheduler))
        val fichier = File(dossier.root, "filtres.preferences_pb")
        val magasin = PreferenceDataStoreFactory.create(scope = portee) { fichier }
        val session = FausseSession()
        val filtres = FiltresPokedexDataStore(magasin, session)
        val choisis = FiltresMemorises(setOf(StatutEspece.VUE), "eau", 12, MethodeFiltre.LIEU_MIRAGE)

        session.ouvrir(1L)
        assertEquals(FiltresMemorises(), filtres.filtres.first())
        filtres.enregistrer(choisis)
        assertEquals(choisis, filtres.filtres.first())

        session.ouvrir(2L)
        assertEquals("un autre compte part sans filtre", FiltresMemorises(), filtres.filtres.first())

        session.ouvrir(1L)
        assertEquals(choisis, FiltresPokedexDataStore(magasin, session).filtres.first())
        filtres.enregistrer(FiltresMemorises())
        assertEquals(FiltresMemorises(), filtres.filtres.first())
    }
}
