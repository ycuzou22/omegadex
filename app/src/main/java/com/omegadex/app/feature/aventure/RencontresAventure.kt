package com.omegadex.app.feature.aventure

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.rencontre.Debloqueur
import com.omegadex.app.domain.rencontre.Echange
import com.omegadex.app.feature.lieu.nomMethodeRencontre
import com.omegadex.app.ui.components.IconesTypes
import com.omegadex.app.ui.components.SpriteEspece
import com.omegadex.app.ui.components.dessinerGemme
import com.omegadex.app.ui.theme.OmegadexTheme

private const val ECLOSION_MS = 520
private const val DECOUVERTE_MS = 600
private const val ROTATION_ECLOSION = -30f
private val EASE_REBOND = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)

/** Titre de la section des rencontres de l'étape, avec le compte des espèces capturées. */
@Composable
internal fun EnTeteRencontres(groupes: List<GroupeRencontresAffiche>) {
    val lignes = groupes.flatMap { it.lignes }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.aventure_rencontres),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            stringResource(R.string.aventure_compteur, lignes.count { it.capturee }, lignes.size),
            style = OmegadexTheme.chiffres.moyen,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Lieu d'un groupe de rencontres, qui ouvre sa fiche ; « Nouveau · Surf » quand une capacité ou un objet
 * débloque ces rencontres.
 */
@Composable
internal fun EnTeteGroupe(groupe: GroupeRencontresAffiche, onLieu: (Int) -> Unit) {
    val ouvrir = stringResource(R.string.aventure_ouvrir_lieu, groupe.lieu)
    Row(
        Modifier
            .padding(top = 4.dp)
            .heightIn(min = 40.dp)
            .clickable(onClickLabel = ouvrir, role = Role.Button) { onLieu(groupe.lieuId) }
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(groupe.lieu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        groupe.debloqueur?.let { debloqueur ->
            val couleurs = OmegadexTheme.couleurs
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = couleurs.magma,
                border = BorderStroke(1.dp, couleurs.lueurMagma),
            ) {
                Text(
                    stringResource(R.string.rencontre_nouveau, stringResource(nomDebloqueur(debloqueur))),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/**
 * Une espèce qu'on peut rencontrer : son image, en ombre tant qu'elle n'est pas capturée (comme dans le
 * Pokédex), ses types en logos devant le nom, niveaux, taux et méthode, puis « Attraper » (ou « Reçu »
 * pour un échange). Une fois capturée, l'image se découvre et un rubis éclot à la place du bouton ;
 * [onRubis] reçoit le centre de ce rubis (repère de la fenêtre), d'où part l'animation « Capture → Pokédex ».
 */
@Composable
internal fun LigneRencontre(
    rencontre: RencontreAffichee,
    onAttraper: () -> Unit,
    onRubis: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleurs = OmegadexTheme.couleurs
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, couleurs.trait),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageRencontre(rencontre)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconesTypes(rencontre.types, taille = 18.dp)
                    Text(
                        rencontre.nom,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (rencontre.capturee) MaterialTheme.colorScheme.onSurface else couleurs.texteEteint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    infoRencontre(rencontre),
                    style = OmegadexTheme.chiffres.petit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                rencontre.echange?.let {
                    Text(
                        stringResource(libelleEchange(it)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = couleurs.gemme,
                    )
                }
            }
            RubisCapture(rencontre.capturee, onRubis)
            ActionRencontre(rencontre, onAttraper)
        }
    }
}

/** Image de l'espèce : ombre unie tant qu'elle n'est pas capturée, puis elle se découvre en couleurs. */
@Composable
private fun ImageRencontre(rencontre: RencontreAffichee) {
    val reduites = OmegadexTheme.animationsReduites
    val decouverte = remember(rencontre.especeId) { Animatable(if (rencontre.capturee) 1f else 0f) }
    LaunchedEffect(rencontre.capturee) {
        val cible = if (rencontre.capturee) 1f else 0f
        if (reduites || !rencontre.capturee) {
            decouverte.snapTo(cible)
        } else {
            decouverte.animateTo(cible, tween(DECOUVERTE_MS))
        }
    }
    SpriteEspece(rencontre.especeId, decouverte = decouverte.value, taille = 48.dp)
}

@Composable
private fun ActionRencontre(rencontre: RencontreAffichee, onAttraper: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val echange = rencontre.echange != null
    if (rencontre.capturee) {
        Text(
            stringResource(if (echange) R.string.rencontre_recu else R.string.aventure_capturee),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = couleurs.gemme,
        )
        return
    }
    val description = stringResource(
        if (echange) R.string.rencontre_recu_description else R.string.aventure_attraper_description,
        rencontre.nom,
    )
    val modifier = Modifier.semantics { contentDescription = description }
    if (echange) {
        OutlinedButton(
            onClick = onAttraper,
            contentPadding = ButtonDefaults.TextButtonContentPadding,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            modifier = modifier,
        ) { Text(stringResource(R.string.rencontre_recu)) }
    } else {
        Button(onClick = onAttraper, contentPadding = ButtonDefaults.TextButtonContentPadding, modifier = modifier) {
            Text(stringResource(R.string.aventure_attraper))
        }
    }
}

/** Emplacement du rubis : vide, puis le rubis éclot quand l'espèce devient capturée. */
@Composable
private fun RubisCapture(capturee: Boolean, onRubis: (Offset) -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    val reduites = OmegadexTheme.animationsReduites
    val eclosion = remember { Animatable(if (capturee) 1f else 0f) }
    LaunchedEffect(capturee) {
        when {
            !capturee -> eclosion.snapTo(0f)
            reduites -> eclosion.snapTo(1f)
            eclosion.value < 1f -> eclosion.animateTo(1f, tween(ECLOSION_MS, easing = EASE_REBOND))
        }
    }
    Canvas(
        Modifier
            .size(width = 22.dp, height = 20.dp)
            .onGloballyPositioned { onRubis(it.boundsInWindow().center) }
            .graphicsLayer {
                val v = eclosion.value
                alpha = v.coerceIn(0f, 1f)
                scaleX = v
                scaleY = v
                rotationZ = ROTATION_ECLOSION * (1f - v)
            },
    ) {
        dessinerGemme(allumee = true, couleurs = couleurs)
    }
}

@Composable
private fun infoRencontre(rencontre: RencontreAffichee): String = buildList {
    rencontre.niveauMin?.let { min ->
        val max = rencontre.niveauMax ?: min
        val texte = if (min == max) {
            stringResource(R.string.lieu_niveau, min)
        } else {
            stringResource(R.string.lieu_niveaux, min, max)
        }
        add(texte)
    }
    rencontre.taux?.let { add(stringResource(R.string.lieu_taux, it)) }
    if (rencontre.echange == null) add(stringResource(nomMethodeRencontre(rencontre.methode)))
}.joinToString(" · ")

@StringRes
internal fun nomDebloqueur(debloqueur: Debloqueur): Int = when (debloqueur) {
    Debloqueur.CANNE -> R.string.debloqueur_canne
    Debloqueur.ECLATE_ROC -> R.string.debloqueur_eclate_roc
    Debloqueur.LUNETTES_SABLE -> R.string.debloqueur_lunettes_sable
    Debloqueur.DEVON_SCOPE -> R.string.debloqueur_devon_scope
    Debloqueur.APRES_LIGUE -> R.string.debloqueur_apres_ligue
    Debloqueur.APRES_DELTA -> R.string.debloqueur_apres_delta
    Debloqueur.SURF -> R.string.debloqueur_surf
    Debloqueur.SUPER_CANNE -> R.string.debloqueur_super_canne
    Debloqueur.MEGA_CANNE -> R.string.debloqueur_mega_canne
    Debloqueur.PLONGEE -> R.string.debloqueur_plongee
    Debloqueur.APRES_GROUDON -> R.string.debloqueur_apres_groudon
    Debloqueur.POKEDEX_NATIONAL -> R.string.debloqueur_pokedex_national
}

@StringRes
private fun libelleEchange(echange: Echange): Int = when (echange) {
    Echange.SAPHIR_ALPHA -> R.string.rencontre_echange_saphir
    Echange.HORS_HOENN -> R.string.rencontre_echange_hors_hoenn
}
