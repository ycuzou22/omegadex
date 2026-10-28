package com.omegadex.app.feature.defis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.omegadex.app.R
import com.omegadex.app.ui.components.EcranEnConstruction

@Composable
fun DefisScreen(modifier: Modifier = Modifier) {
    EcranEnConstruction(
        titre = stringResource(R.string.nav_defis),
        detail = stringResource(R.string.ecran_bientot_disponible),
        modifier = modifier,
    )
}
