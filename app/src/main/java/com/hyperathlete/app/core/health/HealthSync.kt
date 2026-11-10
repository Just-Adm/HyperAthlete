package com.hyperathlete.app.core.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** One weigh-in from another app (a smart scale, Honor Health…). */
data class OutsideWeight(val date: LocalDate, val kg: Double, val source: String)

/**
 * Two-way body-weight sync with Health Connect (built into Android 14+). Our weigh-ins are written with a
 * per-day client id (so editing a day replaces it), and weigh-ins from other apps can be imported.
 * Everything is optional: without Health Connect or its permission, nothing happens.
 */
@Singleton
class HealthSync @Inject constructor(@ApplicationContext private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    fun available(): Boolean =
        runCatching { HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE }.getOrDefault(false)

    private fun client(): HealthConnectClient? = if (available()) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    suspend fun granted(): Boolean {
        val c = client() ?: return false
        return runCatching { c.permissionController.getGrantedPermissions().containsAll(permissions) }.getOrDefault(false)
    }

    /** Writes (or replaces) the weigh-in of [date]. */
    suspend fun writeWeight(date: LocalDate, kg: Double): Boolean {
        val c = client() ?: return false
        if (!granted()) return false
        val zone = ZoneId.systemDefault()
        val time = date.atTime(LocalTime.of(8, 0)).atZone(zone)
        val record = WeightRecord(
            time = time.toInstant(), zoneOffset = time.offset, weight = Mass.kilograms(kg),
            metadata = Metadata.manualEntry(clientRecordId = clientId(date), clientRecordVersion = System.currentTimeMillis()),
        )
        return runCatching { c.insertRecords(listOf(record)); true }
            .onFailure { Log.w(TAG, "write weight", it) }.getOrDefault(false)
    }

    /** Weigh-ins other apps saved since [from], one per day (the latest of the day). */
    suspend fun outsideWeights(from: LocalDate): List<OutsideWeight> {
        val c = client() ?: return emptyList()
        if (!granted()) return emptyList()
        val zone = ZoneId.systemDefault()
        return runCatching {
            c.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.after(from.atStartOfDay(zone).toInstant()))).records
                .filter { it.metadata.dataOrigin.packageName != context.packageName }
                .groupBy { it.time.atZone(zone).toLocalDate() }
                .map { (day, list) ->
                    val r = list.maxBy { it.time }
                    OutsideWeight(day, r.weight.inKilograms, r.metadata.dataOrigin.packageName)
                }
                .sortedBy { it.date }
        }.onFailure { Log.w(TAG, "read weights", it) }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "HealthSync"
        fun clientId(date: LocalDate) = "hyper-weight-$date"

        /** The system screen that asks for Health Connect access. */
        fun requestContract() = PermissionController.createRequestPermissionResultContract()
    }
}
