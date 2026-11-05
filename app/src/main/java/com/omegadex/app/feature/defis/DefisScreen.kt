package com.omegadex.app.feature.defis

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.defi.BilanDefis
import com.omegadex.app.domain.defi.CategorieDefi
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.TypeDefi
import com.omegadex.app.feature.runs.titreDifficulte
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.ProgressGaugeRing
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Espace Défis : jauge de réussite, six roulettes (une par catégorie), défis en cours et terminés.
 * Avec [onRetour], c'est la page « Défis de … » d'un Pokémon, sans roulette.
 */
@Composable
fun DefisScreen(
    onEspece: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onRetour: (() -> Unit)? = null,
    viewModel: DefisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val annonces = remember { SnackbarHostState() }
    val ressources = LocalResources.current
    val sons = LocalSons.current
    var feuille by remember { mutableStateOf<FeuilleOuverte?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.refusEspece.collect { annonces.showSnackbar(ressources.getString(R.string.defis_refus_espece)) }
    }
    LaunchedEffect(viewModel) {
        viewModel.toursDeRoulette.collect { tour -> feuille = FeuilleOuverte(tour.categorie, tour) }
    }
    Box(modifier.fillMaxSize()) {
        DefisContenu(
            uiState = uiState,
            actions = ActionsDefis(
                onOnglet = viewModel::choisirOnglet,
                onFiltre = viewModel::filtrer,
                onValider = { defiId, reussi ->
                    sons.jouer(if (reussi) Son.DEFI_REUSSI else Son.DEFI_ECHOUE)
                    viewModel.valider(defiId, reussi)
                },
                onRetenter = viewModel::retenter,
                onEspece = onEspece,
                onRetour = onRetour,
            ),
            roulettes = ActionsRoulettes(
                onTourner = viewModel::tourner,
                onVoirTirage = { feuille = FeuilleOuverte(it, tour = null) },
                onVoirEnCours = { viewModel.choisirOnglet(OngletDefis.ACTIFS) },
            ),
        )
        SnackbarHost(annonces, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    feuille?.let { ouverte ->
        val case = (uiState as? DefisUiState.Espace)?.roulette?.cases?.firstOrNull { it.categorie == ouverte.categorie }
        // Une fois relevé, le défi passe « en cours » : la roulette se referme.
        LaunchedEffect(case?.etat) { if (case?.etat == EtatCase.EN_COURS) feuille = null }
        FeuilleRoulette(
            ouverte,
            case,
            ActionsTirage(
                onRelever = viewModel::releverTirage,
                onRelancer = viewModel::tourner,
                onFermer = { feuille = null },
            ),
        )
    }
}

@Composable
private fun DefisContenu(
    uiState: DefisUiState,
    actions: ActionsDefis,
    roulettes: ActionsRoulettes,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DefisUiState.Chargement -> Box(modifier.fillMaxSize())
        DefisUiState.AucuneRun -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.defis_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        is DefisUiState.Espace -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                actions.onRetour?.let { retour ->
                    item { TextButton(onClick = retour) { Text(stringResource(R.string.defis_retour)) } }
                }
                item { EnTete(uiState) }
                item { Onglets(uiState, actions.onOnglet) }
                val roulette = uiState.roulette
                if (uiState.onglet == OngletDefis.ROULETTE && roulette != null) {
                    item { GrilleRoulettes(roulette, roulettes) }
                } else {
                    item { Filtres(uiState.difficulteRun, uiState.filtre, actions.onFiltre) }
                    if (uiState.cartes.isEmpty()) {
                        item { Vide(uiState.onglet) }
                    }
                    items(uiState.cartes, key = { it.cle }) { carte ->
                        CarteDefiVue(carte, pageEspece = uiState.especeNom != null, actions = actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnTete(espace: DefisUiState.Espace) {
    val bilan = espace.bilan
    val pourcentage = (bilan.tauxReussite * POURCENT).toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            espace.roulette?.let { roulette ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.defis_entete_etape, roulette.numeroEtape, roulette.adversaire)
                            .uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PuceDifficulte(espace.difficulteRun)
                }
            }
            Text(
                espace.especeNom?.let { stringResource(R.string.defis_titre_espece, it) }
                    ?: stringResource(R.string.defis_titre),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            espace.roulette?.let { roulette ->
                Text(
                    pluralStringResource(R.plurals.defis_categories_en_cours, roulette.occupees, roulette.occupees),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                stringResource(R.string.defis_bilan, bilan.actifs, bilan.reussis, bilan.rates),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProgressGaugeRing(
                progression = bilan.tauxReussite,
                description = stringResource(R.string.defis_jauge, pourcentage),
                couleur = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.defis_reussite, bilan.reussis, bilan.termines),
                style = OmegadexTheme.chiffres.petit,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filtres(difficulteRun: Difficulte, filtre: Difficulte?, onFiltre: (Difficulte?) -> Unit) {
    val choix = listOf<Difficulte?>(null) + Difficulte.entries.filter { it.ordinal <= difficulteRun.ordinal }
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choix.forEach { difficulte ->
            Pastille(
                texte = stringResource(difficulte?.let { titreDifficulte(it) } ?: R.string.defis_toutes),
                choisie = filtre == difficulte,
                onClick = { onFiltre(difficulte) },
            )
        }
    }
}

@Composable
private fun Onglets(espace: DefisUiState.Espace, onOnglet: (OngletDefis) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        espace.onglets.forEach { onglet ->
            val (nom, nombre) = when (onglet) {
                OngletDefis.ROULETTE ->
                    R.string.defis_onglet_roulette to espace.roulette?.cases?.count { it.peutTourner }
                OngletDefis.ACTIFS -> R.string.defis_onglet_actifs to espace.actifs.size
                OngletDefis.TERMINES -> R.string.defis_onglet_termines to espace.termines.size
            }
            Pastille(
                texte = stringResource(nom),
                choisie = espace.onglet == onglet,
                modifier = Modifier.weight(1f),
                sombre = true,
                compteur = nombre,
            ) { onOnglet(onglet) }
        }
    }
}

/** Pastille sélectionnable : rubis (ou roche pour les onglets) quand elle est choisie. */
@Composable
private fun Pastille(
    texte: String,
    choisie: Boolean,
    modifier: Modifier = Modifier,
    sombre: Boolean = false,
    compteur: Int? = null,
    onClick: () -> Unit,
) {
    val schema = MaterialTheme.colorScheme
    val fond = when {
        !choisie -> schema.surface
        sombre -> schema.secondary
        else -> schema.primary
    }
    val texteCouleur = when {
        !choisie -> schema.onSurface
        sombre -> schema.onSecondary
        else -> schema.onPrimary
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = fond,
        contentColor = texteCouleur,
        border = if (choisie) null else BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = modifier.selectable(selected = choisie, role = Role.Tab, onClick = onClick),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(texte, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            compteur?.let { Text(it.toString(), style = OmegadexTheme.chiffres.petit) }
        }
    }
}

@Composable
private fun Vide(onglet: OngletDefis) {
    Text(
        stringResource(
            when (onglet) {
                OngletDefis.ACTIFS -> R.string.defis_vide_actifs
                OngletDefis.ROULETTE -> R.string.roulette_vide
                OngletDefis.TERMINES -> R.string.defis_vide_termines
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CarteDefiVue(carte: CarteDefi, pageEspece: Boolean, actions: ActionsDefis) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                listOf(stringResource(nomTypeDefi(carte.type)), stringResource(titreDifficulte(carte.difficulte)))
                    .joinToString(" · ").uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(carte.titre, style = MaterialTheme.typography.titleMedium)
            Text(carte.enonce, style = MaterialTheme.typography.bodyMedium)
            ActionsCarte(carte, actions)
            if (!pageEspece && carte.especeId != null && carte.especeNom != null) {
                TextButton(onClick = { actions.onEspece(carte.especeId) }) {
                    Text(stringResource(R.string.defis_voir_espece, carte.especeNom))
                }
            }
        }
    }
}

@Composable
private fun ActionsCarte(carte: CarteDefi, actions: ActionsDefis) {
    val defiId = carte.defiId
    when {
        defiId == null -> Unit
        carte.etat == EtatDefi.ACTIF -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OmegaButton(stringResource(R.string.defis_reussi), { actions.onValider(defiId, true) }, Modifier.weight(1f))
            OmegaButton(
                stringResource(R.string.defis_rate),
                { actions.onValider(defiId, false) },
                Modifier.weight(1f),
                variante = VarianteBouton.CONTOUR,
            )
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            val reussi = carte.etat == EtatDefi.REUSSI
            Text(
                stringResource(if (reussi) R.string.defis_etat_reussi else R.string.defis_etat_rate),
                style = MaterialTheme.typography.labelLarge,
                color = if (reussi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!reussi) {
                OmegaButton(
                    stringResource(R.string.defis_retenter),
                    { actions.onRetenter(defiId) },
                    variante = VarianteBouton.SOMBRE,
                )
            }
        }
    }
}

private val NOMS_TYPES_DEFIS = mapOf(
    TypeDefi.ETAPE to R.string.type_defi_etape,
    TypeDefi.ESPECE to R.string.type_defi_espece,
    TypeDefi.EVOLUTION to R.string.type_defi_evolution,
    TypeDefi.ROTATION to R.string.type_defi_rotation,
    TypeDefi.MONOTYPE to R.string.type_defi_monotype,
    TypeDefi.NAVI_DEX to R.string.type_defi_navi_dex,
    TypeDefi.MEGA to R.string.type_defi_mega,
    TypeDefi.CONCOURS to R.string.type_defi_concours,
)

@StringRes
internal fun nomTypeDefi(type: TypeDefi): Int = NOMS_TYPES_DEFIS.getValue(type)

private const val POURCENT = 100

@ApercusClairSombre
@Composable
private fun DefisPreview() {
    val carte = CarteDefi(
        "a",
        1,
        "Évolution sous le plafond",
        "Faire évoluer Gobou en Flobio avant Voltère, sans dépasser le niveau 21",
        TypeDefi.EVOLUTION,
        Difficulte.NORMAL,
        258,
        "Gobou",
        EtatDefi.ACTIF,
    )
    OmegadexTheme {
        DefisContenu(
            uiState = DefisUiState.Espace(
                especeNom = null,
                difficulteRun = Difficulte.DIFFICILE,
                onglet = OngletDefis.ROULETTE,
                filtre = null,
                actifs = listOf(carte),
                termines = listOf(carte.copy(cle = "b", etat = EtatDefi.RATE)),
                bilan = BilanDefis(1, 3, 1),
                roulette = RouletteUi(
                    numeroEtape = 3,
                    adversaire = "Voltère",
                    cases = CategorieDefi.entries.mapIndexed { i, categorie ->
                        CaseRouletteUi(
                            categorie = categorie,
                            etat = EtatCase.entries[i % EtatCase.entries.size],
                            nombre = i + 1,
                            defi = carte,
                            relancesRestantes = 1,
                            peutTourner = i % 2 == 0,
                            queteRequise = "Battre le rival sur la Route 110",
                        )
                    },
                ),
            ),
            actions = ActionsDefis({}, {}, { _, _ -> }, {}, {}, null),
            roulettes = ActionsRoulettes({}, {}, {}),
        )
    }
}
