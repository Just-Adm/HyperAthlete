package com.hyperathlete.app.core.workout

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.model.WorkoutPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Cue { TICK, READY, REST_END, SESSION_DONE }

/**
 * Timer sounds and vibration. Uses the alarm stream so the end of a rest is heard even with the
 * ringer on silent and the screen off (brief §6: "rings and vibrates at the exact time").
 */
@Singleton
class Feedback @Inject constructor(@ApplicationContext private val context: Context) {

    private val pool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    }
    private val ids = mutableMapOf<Cue, Int>()
    private val loaded = mutableSetOf<Int>()
    private val pending = mutableMapOf<Int, Float>()

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }

    /** Loads the sounds ahead of time so the first cue isn't late. */
    fun warmUp() {
        if (ids.isNotEmpty()) return
        pool.setOnLoadCompleteListener { p, id, status ->
            if (status == 0) {
                loaded += id
                pending.remove(id)?.let { vol -> p.play(id, vol, vol, 1, 0, 1f) }
            }
        }
        ids[Cue.TICK] = pool.load(context, R.raw.tick, 1)
        ids[Cue.READY] = pool.load(context, R.raw.ready, 1)
        ids[Cue.REST_END] = pool.load(context, R.raw.rest_end, 1)
        ids[Cue.SESSION_DONE] = pool.load(context, R.raw.session_done, 1)
    }

    fun play(cue: Cue, prefs: WorkoutPrefs) {
        if (cue == Cue.TICK && !prefs.countdownTicks) return
        if (prefs.sounds) {
            warmUp()
            val id = ids.getValue(cue)
            val vol = prefs.volume.coerceIn(0f, 1f)
            if (id in loaded) pool.play(id, vol, vol, 1, 0, 1f) else pending[id] = vol
        }
        if (prefs.vibration) vibrate(cue)
    }

    private fun vibrate(cue: Cue) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (cue) {
            Cue.TICK -> return
            Cue.READY -> VibrationEffect.createOneShot(45, 160)
            Cue.REST_END -> VibrationEffect.createWaveform(longArrayOf(0, 320, 160, 320, 160, 520), intArrayOf(0, 255, 0, 255, 0, 255), -1)
            Cue.SESSION_DONE -> VibrationEffect.createWaveform(longArrayOf(0, 90, 70, 90, 70, 260), intArrayOf(0, 200, 0, 220, 0, 255), -1)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
