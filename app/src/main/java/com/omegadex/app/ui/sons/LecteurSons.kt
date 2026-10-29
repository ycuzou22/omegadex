package com.omegadex.app.ui.sons

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.omegadex.app.R
import com.omegadex.app.domain.repository.Reglages
import com.omegadex.app.domain.repository.ReglagesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Sons courts par SoundPool (chargés une fois au démarrage) et vibrations, selon les réglages.
 * Rien ne sonne quand le téléphone est en silencieux ou en vibreur ; les vibrations suivent leur propre réglage.
 */
@Singleton
class LecteurSons @Inject constructor(
    @ApplicationContext private val context: Context,
    reglagesRepository: ReglagesRepository,
) : Sons {
    private val portee = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val reglages: StateFlow<Reglages> =
        reglagesRepository.reglages.stateIn(portee, SharingStarted.Eagerly, Reglages())

    private val attributs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(MAX_FLUX).setAudioAttributes(attributs).build()
    private val identifiants: Map<Son, Int> = Son.entries.associateWith { pool.load(context, it.ressource, 1) }
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibreur: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    private var lecteurAmbiance: MediaPlayer? = null

    override fun jouer(son: Son) {
        val actuels = reglages.value
        if (actuels.sons && sonore()) {
            identifiants[son]?.let { pool.play(it, VOLUME, VOLUME, 1, 0, 1f) }
        }
        if (actuels.vibrations && son.vibration != Vibration.AUCUNE) vibrer(son.vibration)
    }

    override fun ambiance(active: Boolean) {
        val autorisee = reglages.value.let { it.ambiance && it.sons }
        if (active && autorisee && sonore()) {
            if (lecteurAmbiance == null) {
                lecteurAmbiance = MediaPlayer.create(context, R.raw.ambiance_aventure)?.apply {
                    setAudioAttributes(attributs)
                    isLooping = true
                    setVolume(VOLUME_AMBIANCE, VOLUME_AMBIANCE)
                    start()
                }
            }
        } else {
            lecteurAmbiance?.release()
            lecteurAmbiance = null
        }
    }

    private fun sonore() = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    private fun vibrer(vibration: Vibration) {
        val v = vibreur?.takeIf { it.hasVibrator() } ?: return
        val amplitudes = IntArray(vibration.motif.size) { if (it % 2 == 1) vibration.amplitude else 0 }
        val effet = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(vibration.motif, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(vibration.motif, -1)
        }
        v.vibrate(effet)
    }

    private companion object {
        const val MAX_FLUX = 6
        const val VOLUME = 0.8f
        const val VOLUME_AMBIANCE = 0.35f
    }
}
