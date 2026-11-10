package com.hyperathlete.app.data.progress

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Weight, test results and tape measurements: one value per metric per day. */
@Entity(tableName = "metric_entry", indices = [Index(value = ["metric", "epochDay"])])
data class MetricEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: String,
    val epochDay: Long,
    val value: Double,
    val week: Int? = null,
    val cycle: Int? = null,
    val test: Boolean = false,
    val createdAt: Long,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM metric_entry ORDER BY epochDay, createdAt")
    fun all(): Flow<List<MetricEntryEntity>>

    @Query("SELECT * FROM metric_entry WHERE metric = :metric ORDER BY epochDay DESC, createdAt DESC LIMIT 1")
    suspend fun latest(metric: String): MetricEntryEntity?

    @Query("SELECT COUNT(*) FROM metric_entry WHERE test = 1 AND epochDay BETWEEN :from AND :to")
    suspend fun testsBetween(from: Long, to: Long): Int

    @Query("DELETE FROM metric_entry WHERE metric = :metric AND epochDay = :epochDay")
    suspend fun deleteOn(metric: String, epochDay: Long)

    @Query("SELECT * FROM metric_entry WHERE metric = :metric ORDER BY epochDay")
    suspend fun allOnce(metric: String): List<MetricEntryEntity>

    @Query("DELETE FROM metric_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insert(e: MetricEntryEntity): Long

    @Transaction
    suspend fun replace(e: MetricEntryEntity): Long {
        deleteOn(e.metric, e.epochDay)
        return insert(e)
    }
}

@Singleton
class ProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val profiles: ProfileRepository,
    private val clock: Clock,
    private val health: com.hyperathlete.app.core.health.HealthSync,
    private val settings: com.hyperathlete.app.data.settings.SettingsRepository,
) {
    val metrics: Flow<List<MetricEntry>> = dao.all().map { rows -> rows.mapNotNull { it.toDomain() } }

    /** Saving a weight also updates the profile when it is the newest one, so nutrition targets follow (brief §16). */
    suspend fun saveWeight(date: LocalDate, kg: Double) {
        save(MetricEntry(metric = Metric.WEIGHT, date = date, value = kg))
        if (settings.current().progress.healthSync) health.writeWeight(date, kg)
    }

    /**
     * Health Connect sync: weigh-ins other apps saved (a smart scale…) fill the days that have none here,
     * and our recent weigh-ins are written there. Returns how many were imported.
     */
    suspend fun syncHealth(pushMine: Boolean = false, days: Long = 60): Int {
        if (!settings.current().progress.healthSync || !health.granted()) return 0
        val from = LocalDate.now(clock).minusDays(days)
        val mine = dao.allOnce(Metric.WEIGHT.name).associateBy { it.epochDay }
        var imported = 0
        health.outsideWeights(from).forEach { w ->
            if (w.date.toEpochDay() !in mine) {
                save(MetricEntry(metric = Metric.WEIGHT, date = w.date, value = w.kg)); imported++
            }
        }
        if (pushMine) mine.values.filter { it.epochDay >= from.toEpochDay() }.forEach { health.writeWeight(LocalDate.ofEpochDay(it.epochDay), it.value) }
        return imported
    }

    suspend fun save(e: MetricEntry): Long {
        val id = dao.replace(
            MetricEntryEntity(
                metric = e.metric.name, epochDay = e.date.toEpochDay(), value = e.value,
                week = e.week, cycle = e.cycle, test = e.test, createdAt = clock.millis(),
            ),
        )
        if (e.metric == Metric.WEIGHT) syncProfileWeight()
        return id
    }

    /** A whole test-week form in one go. */
    suspend fun saveTests(date: LocalDate, week: Int, cycle: Int, values: Map<Metric, Double>) {
        values.forEach { (m, v) -> save(MetricEntry(metric = m, date = date, value = v, week = week, cycle = cycle, test = true)) }
    }

    suspend fun delete(e: MetricEntry) {
        dao.delete(e.id)
        if (e.metric == Metric.WEIGHT) syncProfileWeight()
    }

    suspend fun testsRecorded(from: LocalDate, to: LocalDate): Boolean = dao.testsBetween(from.toEpochDay(), to.toEpochDay()) > 0

    private suspend fun syncProfileWeight() {
        val latest = dao.latest(Metric.WEIGHT.name) ?: return
        profiles.update { it.copy(weightKg = latest.value) }
    }

    private fun MetricEntryEntity.toDomain(): MetricEntry? {
        val m = Metric.entries.firstOrNull { it.name == metric } ?: return null
        return MetricEntry(id, m, LocalDate.ofEpochDay(epochDay), value, week, cycle, test)
    }
}
