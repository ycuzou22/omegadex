package com.omegadex.app.feature.equipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.domain.equipe.MethodeEvolution
import com.omegadex.app.ui.theme.OmegadexTheme

/** Texte sombre lisible sur le dégradé magma du ruban et des boutons. */
private val TexteSurMagma = Color(0xFF2A0D05)

/** Ruban « Peut évoluer » : vers l'espèce s'il n'y a qu'une évolution, sinon le nombre de possibilités. */
@Composable
internal fun RubanEvolution(membre: MembreVue, modifier: Modifier = Modifier) {
    val couleurs = OmegadexTheme.couleurs
    val texte = membre.evolutions.singleOrNull()
        ?.let { stringResource(R.string.equipe_ruban_evolution, it.cible.nom) }
        ?: stringResource(R.string.equipe_ruban_evolutions, membre.evolutions.size)
    Text(
        texte,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = TexteSurMagma,
        modifier = modifier
            .background(Brush.verticalGradient(listOf(couleurs.lueurMagma, couleurs.magma)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/**
 * Un bouton par évolution manuelle, qui dit quoi faire en jeu (« Donner Pierre Lune », « Bonheur max · +1 niveau
 * de nuit »…). Chaque bouton demande une confirmation avant de faire évoluer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BoutonsEvolution(membre: MembreVue, onEvoluer: (EvolutionVue) -> Unit) {
    var aConfirmer by remember(membre.especeId) { mutableStateOf<EvolutionVue?>(null) }
    val plusieurs = membre.evolutions.size > 1
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 10.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        membre.evolutions.forEach { evolution ->
            BoutonMagma(
                libelleEvolution(evolution).let {
                    if (plusieurs) stringResource(R.string.equipe_evolution_vers, it, evolution.cible.nom) else it
                },
            ) { aConfirmer = evolution }
        }
    }
    aConfirmer?.let { evolution ->
        AlertDialog(
            onDismissRequest = { aConfirmer = null },
            title = {
                val nom = membre.surnom ?: membre.nom
                Text(stringResource(R.string.equipe_evolution_manuelle_titre, nom, evolution.cible.nom))
            },
            text = {
                val libelle = libelleEvolution(evolution)
                Text(
                    if (evolution.methode.monteDUnNiveau) {
                        stringResource(R.string.equipe_evolution_manuelle_niveau, libelle, membre.niveau + 1)
                    } else {
                        stringResource(R.string.equipe_evolution_manuelle_detail, libelle)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aConfirmer = null
                    onEvoluer(evolution)
                }) { Text(stringResource(R.string.equipe_evoluer_en, evolution.cible.nom)) }
            },
            dismissButton = {
                TextButton(onClick = { aConfirmer = null }) { Text(stringResource(R.string.equipe_annuler)) }
            },
        )
    }
}

@Composable
private fun BoutonMagma(texte: String, onClick: () -> Unit) {
    val couleurs = OmegadexTheme.couleurs
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TexteSurMagma),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.background(
            Brush.verticalGradient(listOf(couleurs.lueurMagma, couleurs.magma)),
            RoundedCornerShape(50),
        ),
    ) {
        Text(texte, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
    }
}

/** Ce que le joueur fait en jeu pour déclencher l'évolution. */
@Composable
internal fun libelleEvolution(evolution: EvolutionVue): String {
    val detail = evolution.detail.orEmpty()
    return when (evolution.methode) {
        MethodeEvolution.PIERRE -> stringResource(R.string.equipe_evo_pierre, detail)
        MethodeEvolution.BONHEUR -> stringResource(R.string.equipe_evo_bonheur)
        MethodeEvolution.BONHEUR_JOUR -> stringResource(R.string.equipe_evo_bonheur_jour)
        MethodeEvolution.BONHEUR_NUIT -> stringResource(R.string.equipe_evo_bonheur_nuit)
        MethodeEvolution.OBJET_TENU -> evolution.moment
            ?.let { stringResource(R.string.equipe_evo_objet_tenu_moment, detail, it) }
            ?: stringResource(R.string.equipe_evo_objet_tenu, detail)
        MethodeEvolution.CHAMP_MAGNETIQUE -> stringResource(R.string.equipe_evo_champ_magnetique)
        MethodeEvolution.LIEU -> stringResource(R.string.equipe_evo_lieu, detail)
        MethodeEvolution.ECHANGE -> stringResource(R.string.equipe_evo_echange)
        MethodeEvolution.ECHANGE_OBJET -> stringResource(R.string.equipe_evo_echange_objet, detail)
        MethodeEvolution.BEAUTE -> stringResource(R.string.equipe_evo_beaute)
        MethodeEvolution.ATTAQUE -> stringResource(R.string.equipe_evo_attaque, detail)
        MethodeEvolution.AUTRE -> stringResource(R.string.equipe_evo_autre, detail)
    }
}
