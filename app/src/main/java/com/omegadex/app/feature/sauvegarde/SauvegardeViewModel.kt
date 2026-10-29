package com.omegadex.app.feature.sauvegarde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omegadex.app.core.Horloge
import com.omegadex.app.domain.sauvegarde.ExporterProgressionUseCase
import com.omegadex.app.domain.sauvegarde.ImporterSauvegardeUseCase
import com.omegadex.app.domain.sauvegarde.ResultatExport
import com.omegadex.app.domain.sauvegarde.ResultatImport
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Message affiché après un export ou un import. */
sealed interface MessageSauvegarde {
    data class Exportee(val nombreRuns: Int) : MessageSauvegarde
    data class Importee(val runsImportees: Int, val runsIgnorees: Int) : MessageSauvegarde

    /** Le fichier choisi n'est pas une sauvegarde Omégadex valide : rien n'a été importé. */
    data class FichierInvalide(val raison: String) : MessageSauvegarde
    data object EchecEcriture : MessageSauvegarde
    data object EchecLecture : MessageSauvegarde
    data object NonConnecte : MessageSauvegarde
}

/** [enCours] : un export ou un import est en train de s'exécuter, les boutons sont désactivés. */
data class SauvegardeUiState(val enCours: Boolean = false, val message: MessageSauvegarde? = null)

/**
 * Export et import de la progression du compte connecté (T15), dans un fichier JSON choisi avec le
 * sélecteur de fichiers Android. L'écran ne transmet que l'adresse du fichier choisi.
 */
@HiltViewModel
class SauvegardeViewModel @Inject constructor(
    private val exporterProgression: ExporterProgressionUseCase,
    private val importerSauvegarde: ImporterSauvegardeUseCase,
    private val horloge: Horloge,
) : ViewModel() {
    private val etat = MutableStateFlow(SauvegardeUiState())
    val uiState: StateFlow<SauvegardeUiState> = etat.asStateFlow()

    /** Nom proposé dans le sélecteur de fichiers : `omegadex-sauvegarde-2026-10-29.json`. */
    fun nomFichierPropose(): String {
        val jour = DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.ofEpochMilli(horloge.maintenant()).atZone(ZoneId.systemDefault()),
        )
        return "omegadex-sauvegarde-$jour.json"
    }

    fun exporter(destination: String) = executer {
        when (val resultat = exporterProgression(destination)) {
            is ResultatExport.Reussi -> MessageSauvegarde.Exportee(resultat.nombreRuns)
            ResultatExport.EchecEcriture -> MessageSauvegarde.EchecEcriture
            ResultatExport.NonConnecte -> MessageSauvegarde.NonConnecte
        }
    }

    fun importer(source: String) = executer {
        when (val resultat = importerSauvegarde(source)) {
            is ResultatImport.Reussi -> MessageSauvegarde.Importee(resultat.runsImportees, resultat.runsIgnorees)
            is ResultatImport.FichierInvalide -> MessageSauvegarde.FichierInvalide(resultat.raison)
            ResultatImport.EchecLecture -> MessageSauvegarde.EchecLecture
            ResultatImport.NonConnecte -> MessageSauvegarde.NonConnecte
        }
    }

    fun effacerMessage() = etat.update { it.copy(message = null) }

    private fun executer(action: suspend () -> MessageSauvegarde) {
        if (etat.value.enCours) return
        etat.update { SauvegardeUiState(enCours = true) }
        viewModelScope.launch {
            val message = action()
            etat.update { SauvegardeUiState(enCours = false, message = message) }
        }
    }
}
