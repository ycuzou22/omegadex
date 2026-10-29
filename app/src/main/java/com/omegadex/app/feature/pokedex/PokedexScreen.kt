package com.omegadex.app.feature.pokedex

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.ui.components.ChampTexte
import com.omegadex.app.ui.components.IconeType
import com.omegadex.app.ui.components.IconesTypes
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.cleType
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme
import com.omegadex.app.ui.theme.texteLisibleSur

/**
 * Pokédex de la run active : la grille de toutes les espèces, colorée selon leur statut.
 * Toucher une case ouvre sa fiche ([onEspece], fiche Pokémon de T10) ; toucher la bande des rubis
 * fait avancer le statut d'un cran, un appui long sur cette bande le fait reculer.
 * [onStatutModifie] reçoit chaque changement : c'est le point d'accroche de l'animation
 * « Capture → Pokédex » (T14.2), à brancher quand [StatutModifie.capture] est vrai.
 */
@Composable
fun PokedexScreen(
    modifier: Modifier = Modifier,
    onEspece: (Int) -> Unit = {},
    onStatutModifie: (StatutModifie) -> Unit = {},
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val surChangement by rememberUpdatedState(onStatutModifie)
    LaunchedEffect(viewModel) {
        viewModel.evenements.collect { surChangement(it) }
    }
    PokedexContenu(
        uiState = uiState,
        actions = ActionsPokedex(
            onOuvrir = onEspece,
            onAvancer = viewModel::avancerStatut,
            onReculer = viewModel::reculerStatut,
            onRecherche = viewModel::rechercher,
            onStatut = viewModel::basculerStatut,
            onType = viewModel::choisirType,
            onEffacer = viewModel::effacerFiltres,
        ),
        modifier = modifier,
    )
}

/** Réactions de l'écran aux gestes du joueur, regroupées pour alléger les signatures. */
private class ActionsPokedex(
    val onOuvrir: (Int) -> Unit = {},
    val onAvancer: (Int) -> Unit = {},
    val onReculer: (Int) -> Unit = {},
    val onRecherche: (String) -> Unit = {},
    val onStatut: (StatutEspece) -> Unit = {},
    val onType: (String) -> Unit = {},
    val onEffacer: () -> Unit = {},
)

@Composable
private fun PokedexContenu(uiState: PokedexUiState, actions: ActionsPokedex, modifier: Modifier = Modifier) {
    when (uiState) {
        PokedexUiState.Chargement -> Box(modifier.fillMaxSize())
        PokedexUiState.AucuneRun -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pokedex_aucune_run), style = MaterialTheme.typography.bodyLarge)
        }
        is PokedexUiState.Pret -> Column(modifier.fillMaxSize()) {
            // La barre reste fixe au-dessus de la grille : le champ garde le focus pendant le défilement.
            BarreFiltres(etat = uiState, actions = actions)
            GrillePokedex(uiState, actions, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GrillePokedex(etat: PokedexUiState.Pret, actions: ActionsPokedex, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(LARGEUR_CASE),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "en-tete", span = { GridItemSpan(maxLineSpan) }, contentType = "en-tete") {
            EnTetePokedex(etat)
        }
        if (etat.resultats.isEmpty()) {
            item(key = "aucun-resultat", span = { GridItemSpan(maxLineSpan) }, contentType = "vide") {
                Text(
                    stringResource(R.string.pokedex_aucun_resultat),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(etat.resultats, key = { it.id }, contentType = { "espece" }) { espece ->
            CaseEspece(
                espece = espece,
                onOuvrir = { actions.onOuvrir(espece.id) },
                onAvancer = { actions.onAvancer(espece.id) },
                onReculer = { actions.onReculer(espece.id) },
            )
        }
    }
}

@Composable
private fun EnTetePokedex(etat: PokedexUiState.Pret) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(if (etat.national) R.string.pokedex_titre_national else R.string.pokedex_titre_hoenn),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.pokedex_compteur, etat.vues, etat.capturees, etat.especes.size),
            style = OmegadexTheme.chiffres.petit,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (etat.filtres.actifs) {
            Text(
                stringResource(R.string.pokedex_resultats, etat.resultats.size),
                style = OmegadexTheme.chiffres.petit,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Text(
            stringResource(R.string.pokedex_legende_portee),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Case d'une espèce. Une espèce jamais vue apparaît en silhouette : fond éteint, nom estompé, sans types ;
 * une espèce à portée et pas encore capturée a une bordure rubis ; « Échange requis » signale une espèce
 * que Rubis Oméga n'obtient pas seul (exclusivité de Saphir Alpha, espèce d'une autre région).
 *
 * Deux gestes, pour laisser la touche simple à la fiche Pokémon (T10) :
 * - toucher le haut de la case ouvre la fiche ;
 * - toucher la bande des rubis fait avancer le statut, un appui long sur elle le fait reculer.
 * Les deux zones sont voisines et non imbriquées : une zone cliquable dans une autre perdait la touche
 * au profit de la case sous Android 8 (#201).
 * TalkBack lit la case d'un bloc ; il l'ouvre par un double appui et propose les deux changements
 * de statut dans ses actions.
 */
@Composable
private fun CaseEspece(espece: EspeceAffichee, onOuvrir: () -> Unit, onAvancer: () -> Unit, onReculer: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val silhouette = espece.statut == StatutEspece.NON_VUE
    val numero = stringResource(R.string.pokedex_numero, espece.numero)
    val statut = stringResource(libelleStatut(espece.statut))
    val portee = stringResource(R.string.pokedex_a_portee)
    val echange = stringResource(R.string.pokedex_echange)
    val actionOuvrir = stringResource(R.string.pokedex_action_ouvrir)
    val actionAvancer = stringResource(R.string.pokedex_action_avancer)
    val actionReculer = stringResource(R.string.pokedex_action_reculer)
    val forme = RoundedCornerShape(14.dp)
    val nomsTypes = espece.types.map { type -> nomDuType(cleType(type))?.let { stringResource(it) } ?: type }
    val description = buildList {
        add("$numero ${espece.nom}")
        if (!silhouette) addAll(nomsTypes)
        add(statut)
        if (espece.aPortee) add(portee)
        if (espece.echangeRequis) add(echange)
    }.joinToString(", ")
    Surface(
        shape = forme,
        color = if (silhouette) couleurs.piste else MaterialTheme.colorScheme.surface,
        border = if (espece.aPortee) BorderStroke(2.dp, couleurs.gemme) else BorderStroke(1.dp, couleurs.trait),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HAUTEUR_CASE)
            .clip(forme)
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick(label = actionOuvrir) {
                    onOuvrir()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(actionAvancer) {
                        onAvancer()
                        true
                    },
                    CustomAccessibilityAction(actionReculer) {
                        onReculer()
                        true
                    },
                )
            },
    ) {
        // Les textes de la case sont déjà lus d'un bloc par la description de la case.
        Column(Modifier.clearAndSetSemantics {}) {
            // La partie haute prend la hauteur de son contenu (image, types) : pondérée, elle était rognée.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOuvrir)
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(numero, style = OmegadexTheme.chiffres.petit, color = couleurs.texteEteint)
                Text(
                    espece.nom,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (silhouette) couleurs.texteEteint else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SpriteEspece(
                    especeId = espece.id,
                    decouverte = if (silhouette) 0f else 1f,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                if (!silhouette) IconesTypes(espece.types)
                if (espece.echangeRequis) {
                    Text(echange, style = MaterialTheme.typography.labelSmall, color = couleurs.texteEteint)
                }
            }
            BandeStatut(espece.statut, onAvancer = onAvancer, onReculer = onReculer)
        }
    }
}

/**
 * Bas de la case, cible tactile de 48 dp : les rubis du statut et un « + » qui invite à toucher.
 * Une espèce jamais vue n'a pas encore de rubis : la bande propose seulement de la marquer vue.
 */
@Composable
private fun BandeStatut(statut: StatutEspece, onAvancer: () -> Unit, onReculer: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .combinedClickable(onLongClick = onReculer, onClick = onAvancer)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (statut == StatutEspece.NON_VUE) {
            Text(
                stringResource(R.string.pokedex_marquer_vu),
                style = MaterialTheme.typography.labelSmall,
                color = couleurs.texteEteint,
            )
        } else {
            RubisStatut(statut)
        }
        if (statut != StatutEspece.MAITRISEE) {
            Text("+", style = MaterialTheme.typography.labelLarge, color = couleurs.gemme)
        }
    }
}

/** Rang du statut en 5 petits rubis : 0 pour « non vu », 5 pour « maîtrisé ». */
@Composable
private fun RubisStatut(statut: StatutEspece) {
    val couleurs = OmegadexTheme.couleurs
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(StatutEspece.MAITRISEE.ordinal) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (index < statut.ordinal) couleurs.gemme else couleurs.gemmeEteinte,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/** Champ de recherche, puis une ligne de filtres par statut et une ligne de filtres par type. */
@Composable
private fun BarreFiltres(etat: PokedexUiState.Pret, actions: ActionsPokedex) {
    val clavier = LocalSoftwareKeyboardController.current
    val filtres = etat.filtres
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChampTexte(
            valeur = filtres.recherche,
            onValeur = actions.onRecherche,
            libelle = stringResource(R.string.pokedex_recherche),
            actionClavier = ImeAction.Search,
            onValider = { clavier?.hide() },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LigneFiltres {
            StatutEspece.entries.forEach { statut ->
                PuceFiltre(
                    libelle = stringResource(libelleStatut(statut)),
                    choisie = statut in filtres.statuts,
                    onBascule = { actions.onStatut(statut) },
                )
            }
        }
        LigneFiltres {
            etat.typesDisponibles.forEach { type ->
                PuceType(
                    type = type,
                    nom = nomDuType(cleType(type))?.let { stringResource(it) } ?: type,
                    choisie = type == filtres.type,
                    onBascule = { actions.onType(type) },
                )
            }
            if (filtres.actifs) {
                TextButton(onClick = actions.onEffacer) { Text(stringResource(R.string.pokedex_effacer_filtres)) }
            }
        }
    }
}

@Composable
private fun LigneFiltres(contenu: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        contenu()
    }
}

/**
 * Puce de filtre à cocher. Choisie, elle prend la [couleur] donnée (celle du type, rubis par défaut) ;
 * TalkBack l'annonce comme une case cochée ou non.
 */
@Composable
private fun PuceFiltre(libelle: String, choisie: Boolean, onBascule: () -> Unit, couleur: Color = Color.Unspecified) {
    val fond = if (couleur == Color.Unspecified) MaterialTheme.colorScheme.primary else couleur
    Text(
        text = libelle,
        style = MaterialTheme.typography.labelLarge,
        color = if (choisie) texteLisibleSur(fond) else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (choisie) fond else MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, if (choisie) fond else OmegadexTheme.couleurs.trait, CircleShape)
            .toggleable(value = choisie, role = Role.Checkbox, onValueChange = { onBascule() })
            .heightIn(min = HAUTEUR_PUCE)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Filtre par type : le logo du type seul ; TalkBack lit son nom et l'annonce comme une case cochée ou non. */
@Composable
private fun PuceType(type: String, nom: String, choisie: Boolean, onBascule: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val forme = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(TAILLE_PUCE_TYPE)
            .clip(forme)
            .background(
                if (choisie) couleurs.gemme.copy(alpha = OPACITE_PUCE_CHOISIE) else MaterialTheme.colorScheme.surface,
            )
            .border(if (choisie) 2.dp else 1.dp, if (choisie) couleurs.gemme else couleurs.trait, forme)
            .toggleable(value = choisie, role = Role.Checkbox, onValueChange = { onBascule() })
            .semantics { contentDescription = nom },
        contentAlignment = Alignment.Center,
    ) {
        IconeType(type, taille = 28.dp)
    }
}

/** Libellé d'un statut, partagé avec la fiche Pokémon (T10) : chaînes `statut_*` de `strings_pokedex.xml`. */
@StringRes
fun libelleStatut(statut: StatutEspece): Int = when (statut) {
    StatutEspece.NON_VUE -> R.string.statut_non_vue
    StatutEspece.VUE -> R.string.statut_vue
    StatutEspece.CAPTUREE -> R.string.statut_capturee
    StatutEspece.ENTRAINEE -> R.string.statut_entrainee
    StatutEspece.EVOLUEE -> R.string.statut_evoluee
    StatutEspece.MAITRISEE -> R.string.statut_maitrisee
}

private val LARGEUR_CASE = 104.dp
private val HAUTEUR_CASE = 100.dp

/** Cible tactile minimale d'une puce de filtre (48 dp moins le texte, centré par le padding). */
private val HAUTEUR_PUCE = 32.dp

/** Puce de filtre par type : cible tactile de 48 dp. */
private val TAILLE_PUCE_TYPE = 48.dp
private const val OPACITE_PUCE_CHOISIE = 0.12f

@ApercusClairSombre
@Composable
private fun PokedexPreview() {
    OmegadexTheme {
        PokedexContenu(
            PokedexUiState.Pret(
                runId = 1,
                national = false,
                especes = listOf(
                    EspeceAffichee(252, 1, "Arcko", listOf("plante"), StatutEspece.ENTRAINEE, false, false),
                    EspeceAffichee(253, 2, "Massko", listOf("plante"), StatutEspece.VUE, false, true),
                    EspeceAffichee(254, 3, "Jungko", listOf("plante"), StatutEspece.NON_VUE, false, false),
                    EspeceAffichee(263, 15, "Zigzaton", listOf("normal"), StatutEspece.NON_VUE, false, true),
                    EspeceAffichee(280, 30, "Tarsal", listOf("psy", "fee"), StatutEspece.CAPTUREE, false, false),
                ),
                vues = 3,
                capturees = 2,
            ),
            actions = ActionsPokedex(),
        )
    }
}
