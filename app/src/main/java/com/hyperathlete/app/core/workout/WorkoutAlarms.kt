package com.hyperathlete.app.core.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exact alarms for the end of a rest / timed hold, so the sound and vibration come on time even when
 * the phone dozes with the screen off (brief §6).
 */
@Singleton
class WorkoutAlarms @Inject constructor(@ApplicationContext private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_TIMER,
        Intent(context, WorkoutAlarmReceiver::class.java).setAction(ACTION_TIMER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun scheduleTimer(atMillis: Long) {
        val pi = pending()
        runCatching {
            if (canExact(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancelTimer() {
        runCatching { am.cancel(pending()) }
    }

    companion object {
        const val ACTION_TIMER = "com.hyperathlete.app.action.TIMER"
        private const val REQ_TIMER = 41

        fun canExact(am: AlarmManager): Boolean = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }
}

@AndroidEntryPoint
class WorkoutAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: SessionController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WorkoutAlarms.ACTION_TIMER) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                controller.onAlarm()
                if (controller.active.value != null) WorkoutService.start(context)
                delay(2_500) // keep the CPU awake while the chime plays
            } finally {
                result.finish()
            }
        }
    }
}
