package com.omegadex.app.feature.runs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.run.ErreurCreationRun
import com.omegadex.app.domain.run.LONGUEUR_MAX_NOM_RUN
import com.omegadex.app.domain.run.RegleOptionnelle
import com.omegadex.app.domain.run.Starter
import com.omegadex.app.feature.comptes.EnTeteCompte
import com.omegadex.app.ui.components.ChampTexte
import com.omegadex.app.ui.components.GemCheckbox
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.dessinerGemme
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.texteLisibleSur

/**
 * Création d'une run : nom, starter, difficulté et règles optionnelles.
 * [onAnnuler] est null pour la toute première run d'un compte (elle est obligatoire).
 */
@Composable
fun CreationRunScreen(
    onCree: () -> Unit,
    modifier: Modifier = Modifier,
    onAnnuler: (() -> Unit)? = null,
    viewModel: CreationRunViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nomParDefaut = stringResource(R.string.creation_run_nom_defaut)
    LaunchedEffect(uiState.creee) { if (uiState.creee) onCree() }
    CreationRunContenu(
        uiState = uiState,
        nomParDefaut = nomParDefaut,
        actions = ActionsCreationRun(
            onNom = viewModel::saisirNom,
            onStarter = viewModel::choisirStarter,
            onDifficulte = viewModel::choisirDifficulte,
            onRegle = viewModel::basculerRegle,
            onCreer = { viewModel.creer(nomParDefaut) },
            onAnnuler = onAnnuler,
        ),
        modifier = modifier,
    )
}

@Composable
private fun CreationRunContenu(
    uiState: CreationRunUiState,
    nomParDefaut: String,
    actions: ActionsCreationRun,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EnTeteCompte(stringResource(R.string.creation_run_sous_titre), Modifier.fillMaxWidth().padding(top = 8.dp))
            ChampTexte(
                valeur = uiState.nom ?: nomParDefaut,
                onValeur = actions.onNom,
                libelle = stringResource(R.string.creation_run_nom),
                actionClavier = ImeAction.Done,
                erreur = when {
                    ErreurCreationRun.NOM_VIDE in uiState.erreurs -> stringResource(
                        R.string.creation_run_erreur_nom_vide,
                    )
                    ErreurCreationRun.NOM_TROP_LONG in uiState.erreurs ->
                        stringResource(R.string.creation_run_erreur_nom_long, LONGUEUR_MAX_NOM_RUN)
                    else -> null
                },
            )
            TitreSection(R.string.creation_run_starter)
            ChoixStarter(uiState.starter, actions.onStarter)
            if (ErreurCreationRun.STARTER_MANQUANT in uiState.erreurs) {
                Text(
                    stringResource(R.string.creation_run_erreur_starter),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TitreSection(R.string.creation_run_difficulte)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulte.entries.forEach { difficulte ->
                    LigneDifficulte(difficulte, difficulte == uiState.difficulte) { actions.onDifficulte(difficulte) }
                }
            }
            TitreSection(R.string.creation_run_regles)
            RegleOptionnelle.entries.forEach { regle ->
                LigneRegle(regle, regle in uiState.regles) { actions.onRegle(regle, it) }
            }
            Text(
                stringResource(R.string.creation_run_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OmegaButton(
                texte = stringResource(
                    if (uiState.enCours) R.string.creation_run_en_cours else R.string.creation_run_bouton,
                ),
                onClick = actions.onCreer,
                avecOmega = true,
                actif = !uiState.enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            actions.onAnnuler?.let { annuler ->
                TextButton(onClick = annuler, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.creation_run_annuler))
                }
            }
        }
    }
}

@Composable
private fun TitreSection(@StringRes titre: Int) {
    Text(
        stringResource(titre),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ChoixStarter(choisi: Starter?, onStarter: (Starter) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Starter.entries.forEach { starter ->
            CarteStarter(starter, starter == choisi, Modifier.weight(1f)) { onStarter(starter) }
        }
    }
}

@Composable
private fun CarteStarter(starter: Starter, choisi: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val nom = stringResource(nomStarter(starter))
    val couleurType = CouleursTypes.de(starter.type)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = bordureChoix(choisi),
        modifier = modifier.selectable(selected = choisi, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(width = 44.dp, height = 42.dp)
                    .background(couleurType, FormeGemme),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    nom.take(1),
                    color = texteLisibleSur(couleurType),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(nom, style = MaterialTheme.typography.titleSmall)
            TypeChip(starter.type, stringResource(nomType(starter)))
        }
    }
}

@Composable
private fun LigneDifficulte(difficulte: Difficulte, choisie: Boolean, onClick: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = bordureChoix(choisie),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = choisie, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(width = 18.dp, height = 16.dp)) {
                dessinerGemme(allumee = choisie, couleurs = couleurs)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(titreDifficulte(difficulte)), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(descriptionDifficulte(difficulte)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LigneRegle(regle: RegleOptionnelle, active: Boolean, onActive: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        GemCheckbox(
            coche = active,
            onCocheChange = onActive,
            libelle = stringResource(titreRegle(regle)),
            barrerSiCoche = false,
        )
        Text(
            stringResource(descriptionRegle(regle)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 40.dp),
        )
    }
}

@Composable
private fun bordureChoix(choisi: Boolean) = if (choisi) {
    BorderStroke(2.dp, OmegadexTheme.couleurs.magma)
} else {
    BorderStroke(1.dp, OmegadexTheme.couleurs.trait)
}

@StringRes
private fun nomStarter(starter: Starter) = when (starter) {
    Starter.ARCKO -> R.string.starter_arcko
    Starter.POUSSIFEU -> R.string.starter_poussifeu
    Starter.GOBOU -> R.string.starter_gobou
}

@StringRes
private fun nomType(starter: Starter) = when (starter) {
    Starter.ARCKO -> R.string.type_plante
    Starter.POUSSIFEU -> R.string.type_feu
    Starter.GOBOU -> R.string.type_eau
}

@StringRes
fun titreDifficulte(difficulte: Difficulte) = when (difficulte) {
    Difficulte.NORMAL -> R.string.difficulte_normal
    Difficulte.DIFFICILE -> R.string.difficulte_difficile
    Difficulte.HARDCORE -> R.string.difficulte_hardcore
}

@StringRes
private fun descriptionDifficulte(difficulte: Difficulte) = when (difficulte) {
    Difficulte.NORMAL -> R.string.difficulte_normal_description
    Difficulte.DIFFICILE -> R.string.difficulte_difficile_description
    Difficulte.HARDCORE -> R.string.difficulte_hardcore_description
}

@StringRes
private fun titreRegle(regle: RegleOptionnelle) = when (regle) {
    RegleOptionnelle.NUZLOCKE -> R.string.regle_nuzlocke
    RegleOptionnelle.MONOTYPE -> R.string.regle_monotype
    RegleOptionnelle.SANS_OBJETS -> R.string.regle_sans_objets
    RegleOptionnelle.SANS_MULTI_EXP -> R.string.regle_sans_multi_exp
    RegleOptionnelle.SURNOMS -> R.string.regle_surnoms
}

@StringRes
private fun descriptionRegle(regle: RegleOptionnelle) = when (regle) {
    RegleOptionnelle.NUZLOCKE -> R.string.regle_nuzlocke_description
    RegleOptionnelle.MONOTYPE -> R.string.regle_monotype_description
    RegleOptionnelle.SANS_OBJETS -> R.string.regle_sans_objets_description
    RegleOptionnelle.SANS_MULTI_EXP -> R.string.regle_sans_multi_exp_description
    RegleOptionnelle.SURNOMS -> R.string.regle_surnoms_description
}

@ApercusClairSombre
@Composable
private fun CreationRunPreview() {
    OmegadexTheme {
        CreationRunContenu(
            uiState = CreationRunUiState(
                starter = Starter.GOBOU,
                difficulte = Difficulte.DIFFICILE,
                regles = setOf(RegleOptionnelle.NUZLOCKE),
            ),
            nomParDefaut = "Rubis Oméga",
            actions = ActionsCreationRun({}, {}, {}, { _, _ -> }, {}, {}),
        )
    }
}
