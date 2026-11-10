package com.hyperathlete.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.core.AppEntryPoint
import com.hyperathlete.app.core.backup.BackupPlan
import com.hyperathlete.app.core.backup.Snapshot
import com.hyperathlete.app.core.backup.SnapshotException
import com.hyperathlete.app.core.backup.SnapshotStore
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.UserProfile
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Real Room + DataStore: snapshot → wipe → restore gives everything back (what Drive restore and import do). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupFlowTest {

    @Test fun `snapshot, clear and restore round trip`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
        app.profiles().save(UserProfile("آدم", LocalDate.of(2009, 3, 20), 67.5, 176.0))
        app.progress().saveWeight(LocalDate.of(2026, 10, 15), 67.4)
        app.progress().saveWeight(LocalDate.of(2026, 10, 16), 67.2)
        app.settings().setLanguage(AppLanguage.ENGLISH)
        app.settings().setOnboardingDone(true)

        val snap = app.snapshots().take()
        assertEquals(SnapshotStore.DB_VERSION, snap.dbVersion)
        assertTrue(snap.tables.none { it.table in Snapshot.SkipTables })
        // Through the Drive encoding, like a real backup.
        val e = BackupPlan.encode(snap)
        val back = BackupPlan.decode(e.manifest, e.files)

        app.snapshots().clearAll()
        assertEquals("", app.profiles().current().name)
        assertFalse(app.settings().current().onboardingDone)
        assertTrue(app.progress().metrics.first().isEmpty())

        app.snapshots().apply(back)
        assertEquals("آدم", app.profiles().current().name)
        assertEquals(LocalDate.of(2009, 3, 20), app.profiles().current().birthDate)
        assertEquals(67.2, app.profiles().current().weightKg, 0.001)
        assertEquals(2, app.progress().metrics.first().size)
        assertEquals(AppLanguage.ENGLISH, app.settings().current().language)
        assertTrue(app.settings().current().onboardingDone)
    }

    @Test fun `room version and snapshot version agree`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.hyperathlete.app.data.db.HyperDatabase::class.java).build()
        assertEquals(SnapshotStore.DB_VERSION, db.openHelper.readableDatabase.version)
        db.close()
    }

    @Test fun `a newer or foreign file is refused and nothing changes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
        app.profiles().save(UserProfile("Adam"))
        val s = app.snapshots().take()
        for (bad in listOf(s.copy(dbVersion = 99), s.copy(app = "other"))) {
            try { app.snapshots().apply(bad.copy(tables = emptyList())); fail() } catch (_: SnapshotException) { }
        }
        assertEquals("Adam", app.profiles().current().name)
    }
}
