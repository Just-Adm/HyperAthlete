package com.hyperathlete.app.core.workout

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the workout alive with the screen off or the app in the background (type "health"), and owns
 * the ongoing notification / Live Update. Stops itself when the session ends.
 */
@AndroidEntryPoint
class WorkoutService : Service() {

    @Inject lateinit var controller: SessionController
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            // startForeground must happen right away; the real content follows a moment later.
            WorkoutNotifier.ensureChannels(this, Localizer(this, com.hyperathlete.app.domain.model.AppLanguage.MIXED))
            promote(WorkoutNotifier.placeholder(this))
            scope.launch {
                val s = settings.current()
                WorkoutNotifier.ensureChannels(this@WorkoutService, Localizer(this@WorkoutService, s.language))
                val first = controller.restore()
                if (first == null) {
                    ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf(); return@launch
                }
                notify(WorkoutNotifier.build(this@WorkoutService, first, s, controller.now()))
                combine(controller.active, settings.settings) { a, st -> a to st }
                    .distinctUntilChanged()
                    .collect { (a, st) ->
                        if (a == null) {
                            ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } else {
                            notify(WorkoutNotifier.build(this@WorkoutService, a, st, controller.now()))
                        }
                    }
            }
        }
        return START_STICKY
    }

    private fun promote(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        runCatching { ServiceCompat.startForeground(this, WorkoutNotifier.ID_WORKOUT, n, type) }
            .onFailure { stopSelf() }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notify(n: Notification) {
        runCatching { NotificationManagerCompat.from(this).notify(WorkoutNotifier.ID_WORKOUT, n) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Starts (or pokes) the service. Safe from the foreground, an exact alarm or a notification action. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java)) }
        }
    }
}

/** Buttons on the workout notification / lock screen. */
@AndroidEntryPoint
class WorkoutActionReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: SessionController

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                controller.restore()
                when (intent.action) {
                    SET_DONE -> controller.quickLog()
                    ADD_30 -> controller.addRest(30)
                    SKIP_REST -> controller.skipRest()
                    PAUSE -> controller.pause()
                    RESUME -> controller.resume()
                    BEGIN -> controller.begin()
                }
                if (controller.active.value != null) WorkoutService.start(context)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val SET_DONE = "com.hyperathlete.app.action.SET_DONE"
        const val ADD_30 = "com.hyperathlete.app.action.ADD_30"
        const val SKIP_REST = "com.hyperathlete.app.action.SKIP_REST"
        const val PAUSE = "com.hyperathlete.app.action.PAUSE"
        const val RESUME = "com.hyperathlete.app.action.RESUME"
        const val BEGIN = "com.hyperathlete.app.action.BEGIN"
    }
}
