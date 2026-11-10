package com.hyperathlete.app

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.core.AppEntryPoint
import com.hyperathlete.app.core.workout.WorkoutNotifier
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.session.SessionState
import com.hyperathlete.app.domain.training.SetValues
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The real session machinery (Hilt graph, Room, DataStore): start → warm-up → log every set →
 * snapshot → notification → finish → streak day recorded. One flow, same reason as [AppSmokeTest].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WorkoutFlowTest {

    @Test fun `a whole session is saved, survives as a snapshot and counts for the streak`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
        val sessions = app.sessions()
        val date = LocalDate.of(2026, 10, 18) // a Sunday
        app.settings().setProgramStart(LocalDate.of(2026, 10, 4))

        val a = sessions.start(DayOfWeek.SUNDAY, date)
        assertEquals(16, a.plan.totalSets)
        sessions.begin()
        withTimeout(5_000) { sessions.active.first { it?.state?.stage == com.hyperathlete.app.domain.session.Stage.WORK } }

        // First set, then the notification for the rest that follows
        sessions.log(SetValues(60.0, 8, rir = 2))
        val afterOne = withTimeout(5_000) { sessions.active.first { (it?.state?.logs?.size ?: 0) == 1 } }!!
        assertNotNull(afterOne.state.rest)
        val n: Notification = WorkoutNotifier.build(context, afterOne, AppSettings(), sessions.now())
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(n.extras.getCharSequence(Notification.EXTRA_TITLE).toString().isNotBlank())

        // The snapshot on disk brings the same state back
        val snap = SessionState.decode(app.workouts().snapshot()!!)!!
        assertEquals(1, snap.logs.size)

        // Rest of the session
        for (i in 2..16) {
            sessions.skipRest()
            sessions.log(SetValues(20.0, 10, rir = 1))
            withTimeout(5_000) { sessions.active.first { (it?.state?.logs?.size ?: 0) == i } }
        }
        assertTrue(sessions.active.value!!.done)

        val summary = sessions.finish()!!
        assertEquals(16, summary.sets)
        assertTrue(summary.volumeKg > 0)
        assertNull(sessions.active.value)
        assertNull(app.workouts().snapshot())
        assertTrue(app.workouts().isDone(date))
        val days = app.workouts().days(date.minusDays(3)).first()
        assertTrue(days[date]!!.sessionDone)
    }
}
