package com.omegadex.app.feature.aventure

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.MethodeRencontre.HERBE
import com.omegadex.app.domain.model.MethodeRencontre.SURF
import com.omegadex.app.domain.rencontre.Debloqueur
import com.omegadex.app.ui.animations.AnimationCapture
import com.omegadex.app.ui.animations.AnimationFinAventure
import com.omegadex.app.ui.animations.CaptureAnimee
import com.omegadex.app.ui.components.CarteRubis
import com.omegadex.app.ui.components.GemCheckbox
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.Blanc
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.PlexMono
import com.omegadex.app.ui.theme.RubisPale

/** Aventure : seule l'étape en cours de la run active est visible, avec ses quêtes. */
@Composable
fun AventureScreen(
    onLieu: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AventureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var celebration by remember { mutableStateOf<EtapeValidee?>(null) }
    var derniereValidee by remember { mutableStateOf<EtapeValidee?>(null) }
    var capture by remember { mutableStateOf<CaptureAnimee?>(null) }
    // Centre du rubis de chaque ligne de rencontre : l'onde de la capture en part.
    val rubis = remember { mutableMapOf<Int, Offset>() }
    LaunchedEffect(viewModel) { viewModel.evenements.collect { celebration = it } }
    LaunchedEffect(viewModel) { viewModel.captures.collect { capture = it } }
    // Ambiance volcanique tant que l'Aventure est affichée (si elle est activée dans les réglages).
    val sons = LocalSons.current
    DisposableEffect(sons) {
        sons.ambiance(active = true)
        onDispose { sons.ambiance(active = false) }
    }
    Box(modifier) {
        AventureContenu(
            uiState = uiState,
            onCocher = viewModel::cocher,
            onLieu = onLieu,
            derniereValidee = derniereValidee,
            rencontres = ActionsRencontres(
                onAttraper = viewModel::attraper,
                onRubis = { id, centre -> rubis[id] = centre },
            ),
            modifier = Modifier.fillMaxSize(),
        )
        // « Capture → Pokédex » se joue par-dessus l'Aventure, la barre de navigation reste visible.
        capture?.let { enCours ->
            AnimationCapture(
                capture = enCours,
                origine = { rubis[enCours.especeId] },
                onFin = { capture = null },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
    // Après l'animation « Étape validée », l'étape validée se résume en haut et la nouvelle étape s'illumine.
    celebration?.let { evenement ->
        CelebrationEtape(evenement) {
            derniereValidee = evenement
            celebration = null
        }
    }
}

@Composable
private fun AventureContenu(
    uiState: AventureUiState,
    onCocher: (Int, Boolean) -> Unit,
    onLieu: (Int) -> Unit,
    modifier: Modifier = Modifier,
    derniereValidee: EtapeValidee? = null,
    rencontres: ActionsRencontres = ActionsRencontres(),
) {
    when (uiState) {
        AventureUiState.Chargement -> Box(modifier.fillMaxSize())
        AventureUiState.AucuneRun -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.aventure_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        AventureUiState.Terminee -> AnimationFinAventure(modifier)
        is AventureUiState.EtapeEnCours -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                derniereValidee?.let { validee ->
                    item(key = "validee-${validee.numero}") { ResumeEtapeValidee(validee) }
                }
                item { LueurDeblocage(derniereValidee) { CarteEtape(uiState, onCocher) } }
                if (uiState.lieux.isNotEmpty()) {
                    item { LieuxEtape(uiState.lieux, onLieu) }
                }
                if (uiState.rencontres.isNotEmpty()) {
                    item(key = "rencontres") { EnTeteRencontres(uiState.rencontres) }
                    uiState.rencontres.forEach { groupe ->
                        val cle = "${groupe.lieuId}-${groupe.debloqueur}"
                        item(key = "groupe-$cle") { EnTeteGroupe(groupe, onLieu) }
                        items(groupe.lignes, key = { "rencontre-$cle-${it.especeId}" }) { rencontre ->
                            LigneRencontre(
                                rencontre,
                                onAttraper = { rencontres.onAttraper(rencontre.especeId) },
                                onRubis = { rencontres.onRubis(rencontre.especeId, it) },
                            )
                        }
                    }
                }
                if (uiState.annexes.isNotEmpty()) {
                    item { EnTeteAnnexes(uiState.annexes) }
                    items(uiState.annexes, key = { it.id }) { quete ->
                        LigneQuete(quete) { onCocher(quete.id, it) }
                    }
                }
            }
        }
    }
}

/** Actions des lignes de rencontre : attraper une espèce, et position de son rubis pour l'animation. */
private data class ActionsRencontres(
    val onAttraper: (Int) -> Unit = {},
    val onRubis: (Int, Offset) -> Unit = { _, _ -> },
)

@Composable
internal fun CarteEtape(
    etape: AventureUiState.EtapeEnCours,
    onCocher: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CarteRubis(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aventure_etape, etape.numero, etape.total).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RubisPale,
                modifier = Modifier.weight(1f),
            )
            etape.typeDominant?.let { type ->
                TypeChip(type, nomDuType(type)?.let { stringResource(it) } ?: type)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                etape.titre,
                style = MaterialTheme.typography.displaySmall,
                color = Blanc,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.aventure_plafond, etape.lieuPrincipal))
                    append(" ")
                    withStyle(SpanStyle(fontFamily = PlexMono, color = Blanc)) {
                        append(stringResource(R.string.aventure_niveau, etape.plafond))
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = RubisPale,
            )
            if (etape.adversaire != etape.titre) {
                Text(
                    stringResource(R.string.aventure_adversaire, etape.adversaire),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RubisPale,
                )
            }
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.aventure_principales),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                etape.principales.forEach { quete ->
                    LigneQuete(quete) { onCocher(quete.id, it) }
                }
            }
        }
    }
}

/** Carte « Étape n · Titre — Validée · Badge obtenu » qui monte en place (maquette : `unlock`). */
@Composable
private fun ResumeEtapeValidee(validee: EtapeValidee) {
    val apparition = remember(validee) { Animatable(0f) }
    val reduites = OmegadexTheme.animationsReduites
    LaunchedEffect(validee) {
        if (reduites) {
            apparition.snapTo(1f)
        } else {
            apparition.animateTo(1f, tween(DUREE_MONTEE_MS, easing = EASE_MONTEE))
        }
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = apparition.value
                translationY = (1f - apparition.value) * 12.dp.toPx()
            },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 34.dp, height = 30.dp).background(MaterialTheme.colorScheme.primary, FormeGemme))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.aventure_resume_titre, validee.numero, validee.titre),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    validee.badge?.let { stringResource(R.string.aventure_resume_badge, it) }
                        ?: stringResource(R.string.aventure_resume_valide),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Halo magma qui pulse deux fois autour de l'étape qui vient d'être débloquée (maquette : `glow`). */
@Composable
private fun LueurDeblocage(cle: EtapeValidee?, contenu: @Composable () -> Unit) {
    val reduites = OmegadexTheme.animationsReduites
    val intensite = remember { Animatable(0f) }
    LaunchedEffect(cle) {
        if (cle != null && !reduites) {
            repeat(PULSATIONS) {
                intensite.animateTo(1f, tween(DUREE_PULSATION_MS / 2))
                intensite.animateTo(0f, tween(DUREE_PULSATION_MS / 2))
            }
        }
    }
    val magma = OmegadexTheme.couleurs.magma
    Box(
        Modifier.drawBehind {
            if (intensite.value > 0f) {
                val marge = 6.dp.toPx()
                drawRoundRect(
                    magma.copy(alpha = OPACITE_LUEUR * intensite.value),
                    topLeft = Offset(-marge, -marge),
                    size = Size(size.width + 2 * marge, size.height + 2 * marge),
                    cornerRadius = CornerRadius(26.dp.toPx() + marge),
                )
            }
        },
    ) { contenu() }
}

private const val DUREE_MONTEE_MS = 600
private val EASE_MONTEE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private const val PULSATIONS = 2
private const val DUREE_PULSATION_MS = 1_600
private const val OPACITE_LUEUR = 0.25f

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LieuxEtape(lieux: List<LieuAffiche>, onLieu: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.aventure_lieux),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lieux.forEach { lieu ->
                Surface(
                    onClick = { onLieu(lieu.id) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
                ) {
                    Text(
                        lieu.nom,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnTeteAnnexes(annexes: List<QueteAffichee>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aventure_annexes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                stringResource(R.string.aventure_compteur, annexes.count { it.cochee }, annexes.size),
                style = OmegadexTheme.chiffres.moyen,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.aventure_annexes_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LigneQuete(quete: QueteAffichee, onCocher: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        GemCheckbox(coche = quete.cochee, onCocheChange = onCocher, libelle = quete.description)
        quete.recompense?.let {
            Text(
                stringResource(R.string.aventure_recompense, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp),
            )
        }
    }
}

@ApercusClairSombre
@Composable
private fun AventurePreview() {
    OmegadexTheme {
        AventureContenu(
            uiState = AventureUiState.EtapeEnCours(
                runId = 1,
                numero = 3,
                total = 13,
                titre = "Voltère",
                adversaire = "Voltère",
                typeDominant = "electrik",
                plafond = 21,
                lieuPrincipal = "Lavandia",
                principales = listOf(
                    QueteAffichee(1, "Rejoindre Lavandia par la Route 110", null, true),
                    QueteAffichee(2, "Battre le rival sur la Route 110", null, false),
                    QueteAffichee(3, "Vaincre Voltère", "Badge Dynamo", false),
                ),
                annexes = listOf(QueteAffichee(4, "Capturer Dynavolt (Route 110)", null, false)),
                lieux = listOf(LieuAffiche(1, "Lavandia"), LieuAffiche(2, "Route 110")),
                rencontres = listOf(
                    GroupeRencontresAffiche(
                        2,
                        "Route 110",
                        null,
                        listOf(
                            RencontreAffichee(309, "Dynavolt", listOf("electrik"), HERBE, 12, 13, 15, false),
                            RencontreAffichee(316, "Gloupti", listOf("poison"), HERBE, 12, 13, 15, true),
                        ),
                    ),
                    GroupeRencontresAffiche(
                        3,
                        "Route 104",
                        Debloqueur.SURF,
                        listOf(RencontreAffichee(279, "Bekipan", listOf("eau", "vol"), SURF, 25, 25, 5, false)),
                    ),
                ),
            ),
            onCocher = { _, _ -> },
            onLieu = {},
        )
    }
}
