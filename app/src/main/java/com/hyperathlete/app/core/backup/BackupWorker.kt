package com.hyperathlete.app.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hyperathlete.app.core.auth.NeedsSignInException
import com.hyperathlete.app.data.account.AccountStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Nightly Drive backup (23:30 by default) once there's internet; retried with back-off when it fails. */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: BackupEngine,
    private val accounts: AccountStore,
    private val settings: com.hyperathlete.app.data.settings.SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val s = accounts.current()
        if (s.google == null || !s.backup.auto) return Result.success()
        // Right after "clear data" or a reinstall the setup isn't finished: an empty backup must never
        // become the newest one (setup offers to restore the real one instead).
        if (!settings.current().onboardingDone) return Result.success()
        return try {
            engine.backup()
            Result.success()
        } catch (e: NeedsSignInException) {
            Result.success() // shown on the backup screen; retrying won't help until the user signs in again
        } catch (e: DriveAuthException) {
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 5) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val NAME = "drive-backup"

        /** Minutes from [now] until the next [at]. */
        fun delayUntil(now: LocalDateTime, at: LocalTime): Long {
            var next = now.toLocalDate().atTime(at)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes()
        }

        fun schedule(context: Context, at: LocalTime) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(LocalDateTime.now(), at), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
