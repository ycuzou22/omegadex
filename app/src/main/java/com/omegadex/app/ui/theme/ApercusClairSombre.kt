package com.omegadex.app.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Aperçu d'un composant dans le thème clair et dans le thème sombre. */
@Preview(name = "Clair", showBackground = true, backgroundColor = 0xFFF6F3F3)
@Preview(
    name = "Sombre",
    showBackground = true,
    backgroundColor = 0xFF1A1416,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ApercusClairSombre
