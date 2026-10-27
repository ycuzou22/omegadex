package com.omegadex.app.feature.accueil

import com.omegadex.app.domain.repository.InfosAppRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class AccueilViewModelTest {
    private val repositoryFactice = object : InfosAppRepository {
        override fun version() = "9.9.9"
    }

    @Test
    fun `l'etat initial expose la version de l'application`() {
        val viewModel = AccueilViewModel(repositoryFactice)

        assertEquals(AccueilUiState(version = "9.9.9"), viewModel.uiState.value)
    }
}
