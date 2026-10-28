package com.omegadex.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.omegadex.app.R
import com.omegadex.app.ui.theme.ApercusClairSombre
import com.omegadex.app.ui.theme.OmegadexTheme

/**
 * Champ de saisie de la DA. Avec [secret], le texte est masqué et un bouton permet de l'afficher.
 * [erreur] s'affiche sous le champ et le signale à TalkBack.
 */
@Composable
fun ChampTexte(
    valeur: String,
    onValeur: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier,
    erreur: String? = null,
    secret: Boolean = false,
    actionClavier: ImeAction = ImeAction.Next,
    onValider: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        OutlinedTextField(
            value = valeur,
            onValueChange = onValeur,
            label = { Text(libelle) },
            singleLine = true,
            isError = erreur != null,
            shape = MaterialTheme.shapes.medium,
            visualTransformation = if (secret &&
                !visible
            ) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
                imeAction = actionClavier,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onAny = { onValider() }),
            trailingIcon = if (secret) {
                {
                    TextButton(onClick = { visible = !visible }) {
                        Text(stringResource(if (visible) R.string.champ_masquer else R.string.champ_afficher))
                    }
                }
            } else {
                null
            },
            supportingText = erreur?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ApercusClairSombre
@Composable
private fun ChampTextePreview() {
    OmegadexTheme {
        Column(Modifier.padding(16.dp)) {
            ChampTexte(valeur = "yamao", onValeur = {}, libelle = "Identifiant")
            ChampTexte(valeur = "secret", onValeur = {}, libelle = "Mot de passe", secret = true, erreur = "Trop court")
        }
    }
}
