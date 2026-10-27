package com.omegadex.app.feature.aventure

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.omegadex.app.R
import com.omegadex.app.ui.components.EcranEnConstruction

@Composable
fun AventureScreen(modifier: Modifier = Modifier) {
    EcranEnConstruction(
        titre = stringResource(R.string.nav_aventure),
        detail = stringResource(R.string.ecran_bientot_disponible),
        modifier = modifier,
    )
}
