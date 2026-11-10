package com.hyperathlete.app.core.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hyperathlete.app.HomeActivity
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.program.BlockType
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.session.Stage

/**
 * The ongoing workout notification. On Android 16 it is a Live Update (ProgressStyle + promoted
 * ongoing) so it shows as the capsule next to the camera and on the lock screen; older versions get the
 * same content as a regular ongoing notification (brief §6).
 */
object WorkoutNotifier {
    const val CHANNEL_WORKOUT = "workout"
    const val CHANNEL_REMINDERS = "reminders"
    const val ID_WORKOUT = 7001

    const val EXTRA_OPEN = "open"
    const val OPEN_WORKOUT = "workout"
    const val OPEN_TODAY = "today"
    const val OPEN_START = "start"
    const val OPEN_KNEE = "knee"
    const val OPEN_NUTRITION = "nutrition"
    const val OPEN_WEIGHT = "weight"
    const val OPEN_TESTS = "tests"

    fun ensureChannels(context: Context, l: Localizer) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WORKOUT, l.string(R.string.notif_channel_workout), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, l.string(R.string.notif_channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    fun openApp(context: Context, what: String = OPEN_WORKOUT): PendingIntent = PendingIntent.getActivity(
        context, what.hashCode(),
        Intent(context, HomeActivity::class.java).putExtra(EXTRA_OPEN, what).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, WorkoutActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun placeholder(context: Context): Notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
        .setSmallIcon(R.drawable.ic_fitness_center)
        .setContentTitle(context.getString(R.string.app_name))
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(openApp(context))
        .build()

    fun build(context: Context, a: ActiveSession, settings: AppSettings, now: Long): Notification {
        val l = Localizer(context, settings.language)
        val p = ProgramStrings(l, settings.weightUnit)
        val s = a.state
        val plan = a.plan
        val accent = settings.accentArgb.toInt()
        val title = p.sessionTitle(plan.template)
        val b = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_fitness_center)
            .setColor(accent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context))
            .setRequestPromotedOngoing(settings.workout.liveUpdates)

        val (done, total) = SessionEngine.progress(plan, s)
        val style = NotificationCompat.ProgressStyle()
            .setStyledByProgress(false)
            .setProgressTrackerIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, R.drawable.ic_fitness_center))
        // one segment per block, alternating accent / secondary so supersets and circuits read as chunks
        plan.blocks.forEachIndexed { i, block ->
            val steps = plan.steps.count { it.block == i }.coerceAtLeast(1)
            val c = if (i % 2 == 0) accent else settings.secondaryArgb.toInt()
            style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(steps).setColor(c))
        }
        style.setProgress(done.coerceAtMost(plan.steps.size))

        when {
            s.paused -> {
                b.setContentTitle(l.string(R.string.notif_paused)).setContentText(title)
                    .setShortCriticalText(l.string(R.string.notif_paused_short))
                    .addAction(R.drawable.ic_play_arrow_fill, l.string(R.string.notif_resume), action(context, WorkoutActionReceiver.RESUME))
            }
            s.stage == Stage.WARMUP -> {
                b.setContentTitle(title).setContentText(l.string(R.string.notif_warmup))
                    .setShortCriticalText(l.term(R.string.term_warmup))
                s.warmupStartedAt?.let { b.setWhen(it - s.warmupMs).setUsesChronometer(true).setShowWhen(true) }
                b.addAction(R.drawable.ic_play_arrow_fill, l.string(R.string.notif_begin), action(context, WorkoutActionReceiver.BEGIN))
            }
            a.done -> {
                b.setContentTitle(l.string(R.string.notif_all_done)).setContentText(title)
                    .setShortCriticalText(l.string(R.string.notif_done_short))
                style.setProgress(plan.steps.size)
            }
            s.rest != null -> {
                val next = a.step?.let(plan::slotOf)
                b.setContentTitle(l.string(R.string.notif_rest) + (next?.let { " · " + l.string(R.string.notif_next, p.exercise(it)) } ?: ""))
                    .setContentText(a.step?.let { st -> setLabel(l, plan.blockOf(st).type, st.set, st.of) + (next?.let { " · " + p.planned(it, controllerPrefill(a)) } ?: "") })
                    .setWhen(s.rest.endsAt).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
                    .addAction(R.drawable.ic_add, com.hyperathlete.app.core.locale.Bidi.ltr("+30s"), action(context, WorkoutActionReceiver.ADD_30))
                    .addAction(R.drawable.ic_skip_next_fill, l.string(R.string.notif_skip), action(context, WorkoutActionReceiver.SKIP_REST))
            }
            s.hold != null -> {
                val slot = a.slot
                val h = s.hold
                val switching = h.switchUntil != null && now < h.switchUntil
                b.setContentTitle((slot?.let(p::exercise) ?: title) + if (h.sides > 1) " · " + l.string(R.string.notif_side, h.side, h.sides) else "")
                    .setContentText(if (switching) l.string(R.string.notif_switch) else l.string(R.string.notif_hold))
                    .setWhen(if (switching) h.switchUntil!! else h.endsAt).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
                    .addAction(R.drawable.ic_pause_fill, l.string(R.string.notif_pause), action(context, WorkoutActionReceiver.PAUSE))
            }
            else -> {
                val st = a.step
                val slot = a.slot
                if (st != null && slot != null) {
                    val block = plan.blockOf(st)
                    val label = setLabel(l, block.type, st.set, st.of)
                    if (block.type == BlockType.CIRCUIT) {
                        val after = plan.blocks[st.block].slots.getOrNull(st.slot + 1)
                        b.setContentTitle(label + " · " + p.exercise(slot))
                            .setContentText(after?.let { l.string(R.string.notif_next, p.exercise(it)) } ?: p.planned(slot, controllerPrefill(a)))
                        s.roundStartedAt?.let { b.setWhen(it).setUsesChronometer(true).setShowWhen(true) }
                    } else {
                        b.setContentTitle(p.exercise(slot)).setContentText(label + " · " + p.planned(slot, controllerPrefill(a)))
                    }
                    b.setShortCriticalText(shortLabel(st.set, st.of))
                    val doneLabel = if (slot.exercise.measure == Measure.TIME) R.string.notif_start_hold else R.string.notif_done_set
                    b.addAction(R.drawable.ic_check_fill, l.string(doneLabel), action(context, WorkoutActionReceiver.SET_DONE))
                        .addAction(R.drawable.ic_pause_fill, l.string(R.string.notif_pause), action(context, WorkoutActionReceiver.PAUSE))
                } else {
                    b.setContentTitle(title)
                }
            }
        }
        b.setStyle(style)
        return b.build()
    }

    private fun setLabel(l: Localizer, type: BlockType, set: Int, of: Int): String =
        if (type == BlockType.STRAIGHT) l.term(R.string.term_set_of, set, of) else l.term(R.string.term_round_of, set, of)

    /** Chip text: the capsule only has room for a few characters. */
    private fun shortLabel(set: Int, of: Int) = "$set/$of"

    private fun controllerPrefill(a: ActiveSession) = a.prefill()
}
