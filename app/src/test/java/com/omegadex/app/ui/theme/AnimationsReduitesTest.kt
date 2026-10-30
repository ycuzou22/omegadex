package com.omegadex.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AnimationsReduitesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)

    private fun regler(echelle: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, echelle)
        context.contentResolver.notifyChange(uri, null)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun `le reglage systeme coupe les animations`() {
        regler(1f)
        assertFalse(animationsReduites(context))
        regler(0f)
        assertTrue(animationsReduites(context))
    }

    @Test
    fun `le changement de reglage est suivi en direct`() = runTest {
        regler(1f)
        assertFalse(context.fluxAnimationsReduites().first())
        val valeurs = mutableListOf<Boolean>()
        val collecte = launch { context.fluxAnimationsReduites().take(3).toList(valeurs) }
        testScheduler.runCurrent()
        regler(0f)
        testScheduler.runCurrent()
        regler(1f)
        testScheduler.runCurrent()
        collecte.join()
        assertEquals(listOf(false, true, false), valeurs)
    }
}
