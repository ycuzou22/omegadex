package com.omegadex.app.feature.reglages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.omegadex.app.R
import com.omegadex.app.domain.repository.Reglages
import com.omegadex.app.domain.repository.ReglagesRepository
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReglagesSonsViewModel @Inject constructor(private val repository: ReglagesRepository) : ViewModel() {
    val reglages: StateFlow<Reglages> =
        repository.reglages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ARRET_MS), Reglages())

    fun activerSons(actifs: Boolean) = viewModelScope.launch { repository.activerSons(actifs) }

    fun activerVibrations(actives: Boolean) = viewModelScope.launch { repository.activerVibrations(actives) }

    fun activerAmbiance(active: Boolean) = viewModelScope.launch { repository.activerAmbiance(active) }

    private companion object {
        const val ARRET_MS = 5_000L
    }
}

/** Carte « Sons et vibrations » de Mes runs : trois interrupteurs, l'ambiance désactivée par défaut. */
@Composable
fun CarteReglagesSons(modifier: Modifier = Modifier, viewModel: ReglagesSonsViewModel = hiltViewModel()) {
    val reglages by viewModel.reglages.collectAsStateWithLifecycle()
    CarteReglagesSonsContenu(
        reglages = reglages,
        onSons = { viewModel.activerSons(it) },
        onVibrations = { viewModel.activerVibrations(it) },
        onAmbiance = { viewModel.activerAmbiance(it) },
        modifier = modifier,
    )
}

@Composable
private fun CarteReglagesSonsContenu(
    reglages: Reglages,
    onSons: (Boolean) -> Unit,
    onVibrations: (Boolean) -> Unit,
    onAmbiance: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.reglages_sons_titre),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Interrupteur(stringResource(R.string.reglages_sons), null, reglages.sons, onSons)
            Interrupteur(stringResource(R.string.reglages_vibrations), null, reglages.vibrations, onVibrations)
            Interrupteur(
                stringResource(R.string.reglages_ambiance),
                stringResource(R.string.reglages_ambiance_aide),
                reglages.ambiance,
                onAmbiance,
                actif = reglages.sons,
            )
        }
    }
}

/** Ligne entière cliquable (cible de 48 dp), lue par TalkBack comme un interrupteur. */
@Composable
private fun Interrupteur(
    libelle: String,
    aide: String?,
    coche: Boolean,
    onChange: (Boolean) -> Unit,
    actif: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = coche, enabled = actif, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(libelle, style = MaterialTheme.typography.bodyLarge)
            aide?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = coche, onCheckedChange = null, enabled = actif)
    }
}

@ApercusClairSombre
@Composable
private fun CarteReglagesSonsPreview() {
    OmegadexTheme {
        CarteReglagesSonsContenu(Reglages(), onSons = {}, onVibrations = {}, onAmbiance = {})
    }
}
