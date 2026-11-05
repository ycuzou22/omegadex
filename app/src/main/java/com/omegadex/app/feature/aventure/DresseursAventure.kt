package com.omegadex.app.feature.aventure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omegadex.app.R
import com.omegadex.app.domain.dresseur.DresseursEtape
import com.omegadex.app.domain.dresseur.FicheDresseur
import com.omegadex.app.domain.equipe.Affinite
import com.omegadex.app.ui.components.OmegaButton
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.TypeChip
import com.omegadex.app.ui.components.VarianteBouton
import com.omegadex.app.ui.components.nomDuType
import com.omegadex.app.ui.components.rememberImageDresseur
import com.omegadex.app.ui.theme.CouleursTypes
import com.omegadex.app.ui.theme.OmegadexTheme

/** Parcours des dresseurs de l'étape, dans l'ordre de rencontre, jusqu'au champion. */
@Composable
internal fun ParcoursDresseurs(
    dresseurs: DresseursEtape?,
    onStrategie: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fiches = dresseurs?.fiches.orEmpty()
    if (fiches.isEmpty()) {
        Text(
            stringResource(R.string.dresseurs_aucun),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            fiches.forEachIndexed { i, fiche ->
                EtapeParcours(fiche, dernier = i == fiches.lastIndex, onStrategie = { onStrategie(fiche.dresseur.cle) })
            }
        }
    }
}

@Composable
private fun EtapeParcours(fiche: FicheDresseur, dernier: Boolean, onStrategie: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val vert = Color(COULEUR_BATTU)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val point = when {
                fiche.battu -> vert
                dernier -> couleurs.magma
                else -> MaterialTheme.colorScheme.surface
            }
            Box(
                Modifier.size(if (dernier) 18.dp else 14.dp)
                    .background(point, CircleShape)
                    .border(3.dp, if (fiche.battu) vert else MaterialTheme.colorScheme.primary, CircleShape),
            )
            if (!dernier) {
                Box(Modifier.width(3.dp).height(56.dp).background(if (fiche.battu) vert else couleurs.trait))
            }
        }
        Row(
            Modifier.weight(1f).padding(bottom = if (dernier) 0.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    fiche.dresseur.lieu,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    fiche.dresseur.nom,
                    style = if (dernier) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    fiche.equipe.forEach { SpriteEspece(it.espece.id, decouverte = 1f, taille = 24.dp) }
                }
            }
            if (fiche.battu) {
                Text(
                    stringResource(R.string.dresseurs_battu),
                    style = MaterialTheme.typography.labelMedium,
                    color = vert,
                )
            } else {
                OmegaButton(
                    stringResource(R.string.dresseurs_strategie),
                    onClick = onStrategie,
                    variante = if (dernier) VarianteBouton.PRINCIPAL else VarianteBouton.CONTOUR,
                )
            }
        }
    }
}

/**
 * Fiche plein cadre d'un dresseur : son image (ou un fond aux couleurs de son type) et son nom en grand,
 * puis, en faisant défiler, la stratégie pour le battre.
 */
@Composable
internal fun FicheDresseurPleinCadre(fiche: FicheDresseur, plafond: Int, onBattu: () -> Unit, onFermer: () -> Unit) {
    Dialog(
        onDismissRequest = onFermer,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val hauteurHero = maxHeight * PART_HERO
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                HeroDresseur(fiche, plafond, Modifier.fillMaxWidth().height(hauteurHero))
                Surface(
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().offset(y = (-26).dp),
                ) {
                    StrategieDresseur(
                        fiche,
                        plafond,
                        onBattu,
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 40.dp),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onFermer, modifier = Modifier.background(Color(VOILE), CircleShape)) {
                    Icon(
                        painterResource(R.drawable.ic_retour),
                        stringResource(R.string.dresseurs_retour),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    fiche.dresseur.lieu.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.background(Color(VOILE), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroDresseur(fiche: FicheDresseur, plafond: Int, modifier: Modifier = Modifier) {
    val dresseur = fiche.dresseur
    val image = rememberImageDresseur(dresseur.cle)
    val typeCouleur = CouleursTypes.de(dresseur.type)
    val roche = OmegadexTheme.couleurs.roche
    Box(modifier.background(Brush.verticalGradient(listOf(lerp(roche, typeCouleur, 0.35f), roche)))) {
        image?.let {
            Image(
                it,
                contentDescription = stringResource(R.string.dresseurs_image, dresseur.nom),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxWidth().fillMaxSize(PART_FONDU).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, roche.copy(alpha = 0.8f), roche))),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NomEnGrand(dresseur.nom)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(dresseur.type, nomDuType(dresseur.type)?.let { stringResource(it) } ?: dresseur.type)
                Text(
                    stringResource(R.string.dresseurs_sous_titre, dresseur.titre, plafond),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(TEXTE_HERO),
                )
            }
            Text(
                stringResource(R.string.dresseurs_defiler),
                style = MaterialTheme.typography.labelMedium,
                color = Color(TEXTE_HERO),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

/** Le nom sur deux lignes (« ROXA / NNE »), la seconde décalée à droite et en lueur de magma. */
@Composable
private fun NomEnGrand(nom: String) {
    val (haut, bas) = decouperNom(nom)
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = TAILLE_NOM.sp,
        lineHeight = (TAILLE_NOM * 0.9f).sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-2).sp,
    )
    Column(Modifier.fillMaxWidth().semantics { heading() }) {
        Text(haut, style = style, color = Color.White, maxLines = 1)
        if (bas.isNotEmpty()) {
            Text(
                bas,
                style = style,
                color = OmegadexTheme.couleurs.lueurMagma,
                maxLines = 1,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** « Roxanne » → « ROXA », « NNE » ; « Lévy et Tatia » → « LÉVY », « TATIA » ; un nom court tient sur une ligne. */
internal fun decouperNom(nom: String): Pair<String, String> {
    val majuscules = nom.uppercase()
    val duo = majuscules.split(" ET ")
    if (duo.size == 2) return duo[0] to duo[1]
    if (majuscules.length <= NOM_COURT) return majuscules to ""
    val milieu = (majuscules.length + 1) / 2
    return majuscules.substring(0, milieu) to majuscules.substring(milieu)
}

@Composable
private fun StrategieDresseur(fiche: FicheDresseur, plafond: Int, onBattu: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.dresseurs_strategie),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, OmegadexTheme.couleurs.trait),
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Section(stringResource(R.string.dresseurs_son_equipe))
                fiche.equipe.forEach { p ->
                    LignePokemon(p.espece.id, p.espece.nom, p.niveau) {
                        p.espece.types.forEach { type ->
                            TypeChip(type, nomDuType(type)?.let { stringResource(it) } ?: type)
                        }
                    }
                }
                if (fiche.strategie.faiblesses.isNotEmpty()) {
                    Section(stringResource(R.string.dresseurs_faiblesses))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fiche.strategie.faiblesses.forEach { type ->
                            TypeChip(type, nomDuType(type)?.let { stringResource(it) } ?: type)
                        }
                    }
                }
                if (fiche.strategie.atouts.isNotEmpty()) {
                    Section(stringResource(R.string.dresseurs_atouts))
                    fiche.strategie.atouts.take(MAX_ATOUTS).forEach { atout ->
                        LignePokemon(atout.pokemon.espece.id, atout.pokemon.espece.nom, atout.pokemon.niveau) {
                            Text(
                                stringResource(libelleAffinite(atout.affinite)),
                                style = MaterialTheme.typography.labelMedium,
                                color = couleurAffinite(atout.affinite),
                            )
                        }
                    }
                }
                Section(stringResource(R.string.dresseurs_plan))
                planDeCombat(fiche, plafond).forEachIndexed { i, etape -> EtapePlan(i + 1, etape) }
                fiche.dresseur.attention?.let { Attention(it) }
            }
        }
        val nom = fiche.dresseur.nom
        val (marquer, deja) = when (fiche.dresseur.titre) {
            "Championne" -> R.string.dresseurs_marquer_battue to R.string.dresseurs_deja_battue
            "Champions" -> R.string.dresseurs_marquer_battus to R.string.dresseurs_deja_battus
            else -> R.string.dresseurs_marquer_battu to R.string.dresseurs_deja_battu
        }
        if (fiche.battu) {
            Text(
                stringResource(deja, nom),
                style = MaterialTheme.typography.titleSmall,
                color = Color(COULEUR_BATTU),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OmegaButton(
                stringResource(marquer, nom),
                onClick = onBattu,
                avecOmega = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Plan de combat généré à partir de la stratégie : plafond, atout, Pokémon le plus fort, membres à éviter. */
@Composable
private fun planDeCombat(fiche: FicheDresseur, plafond: Int): List<String> {
    val strategie = fiche.strategie
    val types = strategie.faiblesses.mapNotNull { nomDuType(it) }.map { stringResource(it) }.joinToString(", ")
    return buildList {
        if (fiche.dresseur.duo) add(stringResource(R.string.dresseurs_plan_duo))
        add(stringResource(R.string.dresseurs_plan_plafond, plafond))
        val atout = strategie.meilleurAtout
        if (atout != null) {
            add(stringResource(R.string.dresseurs_plan_atout, atout.pokemon.espece.nom))
        } else if (types.isNotEmpty()) {
            add(stringResource(R.string.dresseurs_plan_types, types))
        }
        strategie.plusFort?.let { add(stringResource(R.string.dresseurs_plan_plus_fort, it.espece.nom, it.niveau)) }
        strategie.aEviter.firstOrNull()?.let {
            add(stringResource(R.string.dresseurs_plan_eviter, it.pokemon.espece.nom))
        }
    }
}

@Composable
private fun Section(titre: String) {
    Text(
        titre.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun LignePokemon(especeId: Int, nom: String, niveau: Int, fin: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpriteEspece(especeId, decouverte = 1f, taille = 34.dp)
        Text(nom, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.aventure_niveau, niveau),
            style = OmegadexTheme.chiffres.petit,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        fin()
    }
}

@Composable
private fun EtapePlan(numero: Int, texte: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                numero.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(texte, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Attention(texte: String) {
    Text(
        stringResource(R.string.dresseurs_attention, texte),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private fun libelleAffinite(affinite: Affinite) = when (affinite) {
    Affinite.AVANTAGE -> R.string.affinite_avantage
    Affinite.RESISTANCE -> R.string.affinite_resistance
    Affinite.NEUTRE -> R.string.affinite_neutre
    Affinite.FAIBLESSE -> R.string.affinite_faiblesse
}

@Composable
private fun couleurAffinite(affinite: Affinite): Color = when (affinite) {
    Affinite.AVANTAGE -> Color(COULEUR_BATTU)
    Affinite.FAIBLESSE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private const val PART_HERO = 0.92f
private const val PART_FONDU = 0.45f
private const val TAILLE_NOM = 84
private const val NOM_COURT = 5
private const val MAX_ATOUTS = 3
private const val COULEUR_BATTU = 0xFF1F6B45
private const val VOILE = 0x33FFFFFF
private const val TEXTE_HERO = 0xFFFFD7DD
