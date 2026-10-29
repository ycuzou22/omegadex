package com.omegadex.app.feature.fiche

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.Espece
import com.omegadex.app.domain.model.Stats
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.feature.pokedex.libelleStatut
import com.omegadex.app.ui.components.GemStatus
import com.omegadex.app.ui.components.NIVEAUX_STATUT
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.brosseGemmeEspece
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.FormeGemme
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.texteLisibleSur
import kotlinx.coroutines.launch

/** Part de noir mêlée à la couleur du type pour l'en-tête (texte lisible, comme le bleu de la maquette). */
private const val ASSOMBRISSEMENT_EN_TETE = 0.3f
private const val OPACITE_VERRE = 0.16f
private const val ASSOMBRISSEMENT_PASTILLE = 0.4f
private const val OPACITE_NUMEROS = 0.85f
private const val OPACITE_ECRIN = 0.18f
private const val DUREE_ETINCELLE_MS = 4_000
private const val DEBUT_ETINCELLE = 0.8f
private const val PIC_ETINCELLE = 0.88f

/**
 * Fiche Pokémon (maquette E · Fiche) : en-tête à la couleur du type, écrin en joyau, six onglets glissables,
 * puis le statut d'entraînement et l'accès aux défis du Pokémon.
 */
@Composable
fun FichePokemonScreen(
    onRetour: () -> Unit,
    onDefis: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FichePokemonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val etat = uiState) {
        FichePokemonUiState.Chargement -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        FichePokemonUiState.Introuvable -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.fiche_introuvable), style = MaterialTheme.typography.bodyLarge)
        }
        is FichePokemonUiState.Fiche -> FicheContenu(
            fiche = etat,
            actions = ActionsFiche(
                onRetour = onRetour,
                onDefis = { onDefis(etat.espece.id) },
                onAvancerStatut = viewModel::avancerStatut,
                onReculerStatut = viewModel::reculerStatut,
            ),
            modifier = modifier,
            onglet = { onglet -> ContenuOnglet(onglet) },
        )
    }
}

@Composable
private fun FicheContenu(
    fiche: FichePokemonUiState.Fiche,
    actions: ActionsFiche,
    modifier: Modifier = Modifier,
    onglet: @Composable (OngletFiche) -> Unit,
) {
    val type = fiche.espece.types.firstOrNull().orEmpty()
    val fond = lerp(CouleursTypes.de(type), Color.Black, ASSOMBRISSEMENT_EN_TETE)
    // Le fond de la colonne est celui du panneau : le chevauchement de 20 dp ne laisse pas de bande en bas.
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        EnTeteFiche(fiche.espece, fond, actions.onRetour)
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize().offset(y = (-20).dp),
        ) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OngletsEtPages(Modifier.weight(1f), onglet)
                PiedFiche(fiche, actions)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OngletsEtPages(modifier: Modifier, onglet: @Composable (OngletFiche) -> Unit) {
    val pages = rememberPagerState(initialPage = OngletFiche.STATS.ordinal) { OngletFiche.entries.size }
    val portee = rememberCoroutineScope()
    val reduites = OmegadexTheme.animationsReduites
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OngletFiche.entries.forEach { entree ->
                val choisi = pages.currentPage == entree.ordinal
                Box(
                    Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (choisi) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = choisi, role = Role.Tab) {
                            portee.launch {
                                if (reduites) {
                                    pages.scrollToPage(entree.ordinal)
                                } else {
                                    pages.animateScrollToPage(entree.ordinal)
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(libelleOnglet(entree)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (choisi) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
        HorizontalPager(pages, Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.Top) { page ->
            onglet(OngletFiche.entries[page])
        }
    }
}

@Composable
private fun EnTeteFiche(espece: Espece, fond: Color, onRetour: () -> Unit) {
    val texte = texteLisibleSur(fond)
    Box(Modifier.fillMaxWidth().background(fond)) {
        Canvas(Modifier.matchParentSize()) { dessinerOmegaFiligrane(texte) }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onRetour,
                    shape = MaterialTheme.shapes.small,
                    color = texte.copy(alpha = OPACITE_VERRE),
                    contentColor = texte,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_retour),
                            contentDescription = stringResource(R.string.fiche_retour),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    numeros(espece),
                    style = OmegadexTheme.chiffres.petit,
                    color = texte.copy(alpha = OPACITE_NUMEROS),
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    textAlign = TextAlign.End,
                )
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        espece.nom,
                        style = MaterialTheme.typography.displayLarge,
                        color = texte,
                        modifier = Modifier.semantics { heading() },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        espece.types.forEach { type -> PastilleType(type, texte) }
                    }
                    Pastille(
                        espece.categorie,
                        texte.copy(alpha = OPACITE_VERRE),
                        texte,
                    )
                }
                Ecrin(espece, texte)
            }
        }
    }
}

@Composable
private fun numeros(espece: Espece): String = espece.numeroHoenn?.let {
    stringResource(R.string.fiche_numeros, it, espece.id)
} ?: stringResource(R.string.fiche_numero_national, espece.id)

/** Type en pastille pleine, contrastée sur l'en-tête (maquette : « Eau » blanc sur bleu). */
@Composable
private fun PastilleType(type: String, fond: Color) {
    val couleur = if (fond == Color.White) {
        lerp(CouleursTypes.de(type), Color.Black, ASSOMBRISSEMENT_PASTILLE)
    } else {
        Color.White
    }
    Pastille(nomDuType(type)?.let { stringResource(it) } ?: type, fond, couleur)
}

@Composable
private fun Pastille(texte: String, fond: Color, couleur: Color) {
    Text(
        texte,
        style = MaterialTheme.typography.labelLarge,
        color = couleur,
        modifier = Modifier.background(fond, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/** Écrin en joyau : la gemme aux couleurs de l'espèce, et une étincelle qui brille de temps en temps. */
@Composable
private fun Ecrin(espece: Espece, texte: Color) {
    val reduites = OmegadexTheme.animationsReduites
    val brillance by rememberInfiniteTransition(label = "etincelle").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = DUREE_ETINCELLE_MS
                0f at (DUREE_ETINCELLE_MS * DEBUT_ETINCELLE).toInt()
                1f at (DUREE_ETINCELLE_MS * PIC_ETINCELLE).toInt()
                0f at DUREE_ETINCELLE_MS
            },
            RepeatMode.Restart,
        ),
        label = "brillance",
    )
    Box(Modifier.size(width = 120.dp, height = 112.dp).clearAndSetSemantics {}) {
        Box(Modifier.fillMaxSize().clip(FormeGemme).background(texte.copy(alpha = OPACITE_ECRIN)))
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .size(width = 64.dp, height = 58.dp)
                .clip(FormeGemme)
                .background(brosseGemmeEspece(espece.types)),
        )
        Canvas(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 22.dp)
                .size(18.dp)
                .graphicsLayer { alpha = if (reduites) 0f else brillance },
        ) { dessinerEtincelle() }
    }
}

@Composable
private fun PiedFiche(fiche: FichePokemonUiState.Fiche, actions: ActionsFiche) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fiche.statut?.let { statut ->
            val libelle = stringResource(libelleStatut(statut))
            val description = stringResource(R.string.fiche_statut_description, statut.ordinal, NIVEAUX_STATUT, libelle)
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClickLabel = stringResource(R.string.pokedex_action_avancer),
                        onLongClickLabel = stringResource(R.string.pokedex_action_reculer),
                        onLongClick = actions.onReculerStatut,
                        onClick = actions.onAvancerStatut,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.fiche_entrainement, libelle).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                GemStatus(statut.ordinal, description)
            }
        }
        OmegaButton(
            stringResource(R.string.fiche_voir_defis, fiche.espece.nom),
            onClick = actions.onDefis,
            avecOmega = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Contenu d'un onglet ; chaque rubrique est remplie par sa sous-tâche (T10.2 à T10.5). */
@Composable
private fun ContenuOnglet(onglet: OngletFiche) {
    Text(
        stringResource(libelleOnglet(onglet)) + " · " + stringResource(R.string.ecran_bientot_disponible),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@StringRes
private fun libelleOnglet(onglet: OngletFiche) = when (onglet) {
    OngletFiche.INFOS -> R.string.fiche_onglet_infos
    OngletFiche.STATS -> R.string.fiche_onglet_stats
    OngletFiche.EVOLUTION -> R.string.fiche_onglet_evolution
    OngletFiche.ATTAQUES -> R.string.fiche_onglet_attaques
    OngletFiche.LIEUX -> R.string.fiche_onglet_lieux
    OngletFiche.DEFIS -> R.string.fiche_onglet_defis
}

@ApercusClairSombre
@Composable
private fun FichePreview() {
    val gobou = Espece(
        id = 258,
        numeroHoenn = 7,
        nom = "Gobou",
        types = listOf("eau"),
        categorie = "Poissonboue",
        tailleM = 0.4,
        poidsKg = 7.6,
        stats = Stats(50, 70, 50, 50, 50, 40),
        evDonnes = "1 Atq",
        tauxCapture = 45,
        ratioMale = 0.875,
        groupesOeufs = listOf("Monstrueux", "Aquatique 1"),
        aUneMega = false,
    )
    OmegadexTheme {
        FicheContenu(
            FichePokemonUiState.Fiche(gobou, StatutEspece.CAPTUREE),
            ActionsFiche({}, {}, {}, {}),
        ) { ContenuOnglet(it) }
    }
}
