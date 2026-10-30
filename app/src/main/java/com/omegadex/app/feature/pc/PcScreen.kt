// Proportions et instants de la maquette « 16 · Fonds en vignettes » du canevas : ce sont des données.
@file:Suppress("MagicNumber")

package com.omegadex.app.feature.pc

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omegadex.app.R
import com.omegadex.app.domain.equipe.NOMBRE_BOITES
import com.omegadex.app.domain.equipe.ResultatPc
import com.omegadex.app.domain.equipe.TAILLE_BOITE
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
import com.omegadex.app.ui.components.IconeType
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.sons.Son
import com.omegadex.app.ui.theme.OmegadexTheme

private const val DUREE_APPROCHE_MS = 1_400
private const val ECHELLE_DEPART = 0.12f
private const val PART_APPARITION = 0.2f
private const val VOILE_MAX = 0.88f
private const val COLONNES = 6
private val EASE_APPROCHE = CubicBezierEasing(0.55f, 0f, 0.2f, 1f)
private val CURSEUR = Color(0xFFFFD23F)
private val ENCRE = Color(0xFF1A1416)
private val FORME_BOITE = RoundedCornerShape(18.dp)
private val FORME_CASE = RoundedCornerShape(12.dp)

/**
 * PC de stockage en plein écran : l'écran s'assombrit et la vue s'approche du PC, puis ses boîtes s'affichent.
 * Avec les animations réduites, le PC s'affiche directement. Retour ou « Fermer » appelle [onFermer].
 */
@Composable
fun PcPleinEcran(onFermer: () -> Unit, viewModel: PcViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sons = LocalSons.current
    val actions = ActionsPc(
        onFermer = onFermer,
        onBoite = {
            sons.jouer(Son.CLIC)
            viewModel.choisirBoite(it)
        },
        onPrecedente = {
            sons.jouer(Son.CLIC)
            viewModel.boitePrecedente()
        },
        onSuivante = {
            sons.jouer(Son.CLIC)
            viewModel.boiteSuivante()
        },
        onChoisir = {
            sons.jouer(Son.CLIC)
            viewModel.selectionner(it)
        },
        onDeplacer = {
            sons.jouer(Son.CONFIRMATION)
            viewModel.deplacer()
        },
        onNature = viewModel::definirNature,
        onTalent = viewModel::definirTalent,
    )
    val refus = (uiState as? PcUiState.Pc)?.refus
    LaunchedEffect(refus) { if (refus != null) sons.jouer(Son.ERREUR) }
    Dialog(
        onDismissRequest = onFermer,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ApprochePc {
            when (val etat = uiState) {
                is PcUiState.Pc -> PcContenu(etat, actions)
                PcUiState.AucuneRun, PcUiState.Chargement -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

/** L'écran s'assombrit, puis le PC vient du fond jusqu'à remplir l'écran. */
@Composable
private fun ApprochePc(contenu: @Composable BoxScope.() -> Unit) {
    val reduites = OmegadexTheme.animationsReduites
    val approche = remember { Animatable(if (reduites) 1f else 0f) }
    LaunchedEffect(Unit) { approche.animateTo(1f, tween(DUREE_APPROCHE_MS, easing = EASE_APPROCHE)) }
    val p = approche.value
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = VOILE_MAX * (p / PART_APPARITION).coerceAtMost(1f))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val echelle = ECHELLE_DEPART + (1f - ECHELLE_DEPART) * p
                    scaleX = echelle
                    scaleY = echelle
                    translationY = (1f - p) * size.height * 0.12f
                    alpha = (p / PART_APPARITION).coerceAtMost(1f)
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter,
            content = contenu,
        )
    }
}

@Composable
internal fun PcContenu(etat: PcUiState.Pc, actions: ActionsPc, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.pc_titre),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = actions.onFermer) { Text(stringResource(R.string.pc_fermer)) }
        }
        Boite(etat, actions)
        Text(
            stringResource(R.string.pc_equipe),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            etat.equipe.forEach { pokemon ->
                CasePokemon(
                    pokemon,
                    choisie = pokemon != null && pokemon.place == etat.fiche?.pokemon?.place,
                    surFond = false,
                    onChoisir = actions.onChoisir,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val fiche = etat.fiche
        if (fiche == null) {
            Text(
                stringResource(R.string.pc_aide),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        } else {
            FichePokemonPc(fiche, etat, actions)
        }
    }
}

/** Boîte affichée : bandeau avec flèches, vignettes des fonds et grille de 30 places sur le fond de la boîte. */
@Composable
private fun Boite(etat: PcUiState.Pc, actions: ActionsPc) {
    val fond = fondBoite(etat.boite)
    val nomFond = stringResource(fond.nom)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(FORME_BOITE)
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Bandeau(etat, nomFond, actions)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..etat.nombreBoites).forEach { numero ->
                Vignette(
                    numero,
                    active = numero == etat.boite,
                    onClick = { actions.onBoite(numero) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(Modifier.fillMaxWidth().clip(FORME_BOITE)) {
            Canvas(Modifier.matchParentSize()) { fond.dessiner(this) }
            Column(Modifier.padding(8.dp)) {
                etat.cases.chunked(COLONNES).forEach { rangee ->
                    Row(Modifier.fillMaxWidth()) {
                        rangee.forEach { pokemon ->
                            CasePokemon(
                                pokemon,
                                choisie = pokemon != null && pokemon.place == etat.fiche?.pokemon?.place,
                                surFond = true,
                                onChoisir = actions.onChoisir,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bandeau(etat: PcUiState.Pc, nomFond: String, actions: ActionsPc) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Fleche("‹", stringResource(R.string.pc_boite_precedente), actions.onPrecedente)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.pc_nom_boite, etat.boite, nomFond),
                style = MaterialTheme.typography.titleSmall,
                color = ENCRE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.pc_compte_boite, etat.occupees, TAILLE_BOITE),
                style = OmegadexTheme.chiffres.petit,
                color = ENCRE.copy(alpha = 0.7f),
            )
        }
        Fleche("›", stringResource(R.string.pc_boite_suivante), actions.onSuivante)
    }
}

@Composable
private fun Fleche(symbole: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ENCRE)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbole, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Vignette(numero: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fond = fondBoite(numero)
    val description = stringResource(R.string.pc_nom_boite, numero, stringResource(fond.nom))
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                if (active) 3.dp else 1.dp,
                if (active) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp),
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = active
            },
    ) {
        Canvas(Modifier.matchParentSize()) { fond.dessiner(this) }
    }
}

/** Une place de boîte ou d'équipe : le sprite du Pokémon, ou un point discret si elle est libre. */
@Composable
private fun CasePokemon(
    pokemon: PokemonVue?,
    choisie: Boolean,
    surFond: Boolean,
    onChoisir: (PlacePokemon) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = pokemon?.let {
        stringResource(R.string.pc_case, it.surnom ?: it.nom, it.niveau)
    } ?: stringResource(R.string.pc_case_libre)
    val fondCase = when {
        choisie -> Color.White.copy(alpha = 0.32f)
        surFond -> Color.Transparent
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(FORME_CASE)
            .background(fondCase)
            .then(
                if (pokemon != null) {
                    Modifier.clickable(role = Role.Button) { onChoisir(pokemon.place) }
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = description
                selected = choisie
            },
        contentAlignment = Alignment.Center,
    ) {
        if (pokemon == null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = if (surFond) 0.45f else 0f)))
        } else {
            SpriteEspece(pokemon.especeId, decouverte = 1f, taille = 44.dp)
        }
        if (choisie) Curseur()
    }
}

private val FORME_CURSEUR = GenericShape { taille, _ ->
    moveTo(0f, 0f)
    lineTo(taille.width, 0f)
    lineTo(taille.width / 2f, taille.height)
    close()
}

/** Petit triangle qui rebondit au-dessus du Pokémon choisi. */
@Composable
private fun BoxScope.Curseur() {
    val reduites = OmegadexTheme.animationsReduites
    val rebond by rememberInfiniteTransition(label = "curseur").animateFloat(
        initialValue = 0f,
        targetValue = if (reduites) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "rebond",
    )
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .graphicsLayer { translationY = rebond * 4.dp.toPx() }
            .size(width = 14.dp, height = 11.dp)
            .background(CURSEUR, FORME_CURSEUR),
    )
}

/** Fiche du Pokémon choisi : identité, niveau, nature et talent modifiables, et déplacement. */
@Composable
private fun FichePokemonPc(fiche: FichePc, etat: PcUiState.Pc, actions: ActionsPc) {
    val pokemon = fiche.pokemon
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = pokemon.surnom ?: pokemon.nom },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpriteEspece(pokemon.especeId, decouverte = 1f, taille = 64.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(pokemon.surnom ?: pokemon.nom, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (fiche.dansEquipe) {
                            stringResource(R.string.pc_dans_equipe, pokemon.nom)
                        } else {
                            stringResource(R.string.pc_dans_boite, pokemon.nom, etat.boite)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        pokemon.types.forEach { type ->
                            IconeType(type, taille = 24.dp, description = nomDuType(type)?.let { stringResource(it) })
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Info(stringResource(R.string.pc_niveau), pokemon.niveau.toString(), Modifier.weight(1f))
                Choix(
                    titre = stringResource(R.string.pc_nature),
                    valeur = pokemon.nature?.nom,
                    options = Nature.entries.map { it.nom },
                    onChoix = { index -> actions.onNature(index?.let { Nature.entries[it] }) },
                    modifier = Modifier.weight(1f),
                )
            }
            Choix(
                titre = stringResource(R.string.pc_talent),
                valeur = pokemon.talent,
                options = fiche.talents,
                onChoix = { index -> actions.onTalent(index?.let { fiche.talents[it] }) },
            )
            etat.refus?.let { refus ->
                Text(
                    stringResource(texteRefus(refus)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OmegaButton(
                texte = stringResource(if (fiche.dansEquipe) R.string.pc_deposer else R.string.pc_reprendre),
                onClick = actions.onDeplacer,
                modifier = Modifier.fillMaxWidth(),
                actif = peutDeplacer(fiche, etat),
            )
        }
    }
}

private fun peutDeplacer(fiche: FichePc, etat: PcUiState.Pc): Boolean {
    val dansEquipe = etat.equipe.count { it != null }
    return if (fiche.dansEquipe) dansEquipe > 1 && etat.occupees < TAILLE_BOITE else dansEquipe < etat.equipe.size
}

private fun texteRefus(refus: ResultatPc): Int = when (refus) {
    ResultatPc.DernierMembre -> R.string.pc_refus_dernier
    ResultatPc.BoitePleine -> R.string.pc_refus_boite_pleine
    ResultatPc.EquipePleine -> R.string.pc_refus_equipe_pleine
    ResultatPc.Introuvable, ResultatPc.Fait -> R.string.pc_refus_introuvable
}

@Composable
private fun Info(titre: String, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(titre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valeur, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** Valeur choisie dans une liste ; « Effacer » remet la valeur à vide. */
@Composable
private fun Choix(
    titre: String,
    valeur: String?,
    options: List<String>,
    onChoix: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ouvert by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(titre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            TextButton(onClick = { ouvert = true }, enabled = options.isNotEmpty() || valeur != null) {
                Text(valeur ?: stringResource(R.string.pc_a_renseigner))
            }
            DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        ouvert = false
                        onChoix(index)
                    })
                }
                if (valeur != null) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.pc_effacer)) }, onClick = {
                        ouvert = false
                        onChoix(null)
                    })
                }
            }
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun PcContenuPreview() {
    val gecko = PokemonVue(PlacePokemon.Equipe(1), 253, "Massko", "Gecko", listOf("plante"), 18, Nature.RIGIDE, null)
    val etat = PcUiState.Pc(
        boite = 1,
        nombreBoites = NOMBRE_BOITES,
        cases = List(TAILLE_BOITE) { i ->
            if (i < 7) gecko.copy(place = PlacePokemon.Pc(i.toLong()), especeId = 261 + i, surnom = null) else null
        },
        equipe = List(6) { if (it < 4) gecko.copy(place = PlacePokemon.Equipe(it + 1)) else null },
        fiche = FichePc(gecko, listOf("Engrais", "Délestage")),
        refus = null,
    )
    OmegadexTheme { PcContenu(etat, ActionsPc({}, {}, {}, {}, {}, {}, {}, {})) }
}
