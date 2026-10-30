package com.omegadex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.omegadex.app.ui.OmegadexRacine
import com.omegadex.app.ui.sons.LecteurSons
import com.omegadex.app.ui.sons.LocalSons
import com.omegadex.app.ui.theme.OmegadexTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sons: LecteurSons

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalSons provides sons) {
                OmegadexTheme {
                    OmegadexRacine()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // L'ambiance ne joue jamais en arrière-plan.
        sons.ambiance(active = false)
    }
}
