package com.hyperathlete.app.data.workout

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One training or mobility session. A row exists from the moment the session starts. */
@Entity(tableName = "workout_session", indices = [Index("epochDay")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** java.time.DayOfWeek value (1 = Monday … 7 = Sunday) of the program day that was trained. */
    val programDay: Int,
    val week: Int,
    val cycle: Int,
    val phase: String,
    val mobility: Boolean,
    val startedAt: Long,
    val endedAt: Long? = null,
    val activeSeconds: Int = 0,
    val completed: Boolean = false,
    val volumeKg: Double = 0.0,
    val sets: Int = 0,
    val avgRir: Double? = null,
    val records: Int = 0,
)

@Entity(
    tableName = "set_log",
    foreignKeys = [ForeignKey(WorkoutSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val slotKey: String,
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val meters: Int? = null,
    val rir: Int? = null,
    val deload: Boolean = false,
    val loggedAt: Long,
)

@Entity(tableName = "knee_pain", indices = [Index("epochDay")])
data class KneePainEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val sessionId: Long? = null,
    val exerciseId: String? = null,
    val during: Int? = null,
    val morning: Int? = null,
    val sharp: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "exercise_note", indices = [Index("exerciseId")])
data class ExerciseNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val text: String,
    val createdAt: Long,
)

/** Snapshot of the running session (JSON), written after every change so a killed app resumes exactly. */
@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long,
)

/** Calendar events this app created, so updates and "stop syncing" only touch our own events. */
@Entity(tableName = "calendar_event")
data class CalendarEventEntity(
    @PrimaryKey val key: String,
    val eventId: Long,
    val calendarId: Long,
    val hash: Int,
)

data class DayDone(val epochDay: Long, val mobility: Boolean)

/** A logged set with its session day (progress charts). */
data class SetRow(
    val sessionId: Long,
    val epochDay: Long,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val seconds: Int?,
    val meters: Int?,
    val rir: Int?,
    val deload: Boolean,
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(s: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(s: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun session(id: Long): WorkoutSessionEntity?

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertSet(s: SetLogEntity): Long

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY loggedAt")
    suspend fun sets(sessionId: Long): List<SetLogEntity>

    /** Most recent earlier session (other than [excludeSession]) that has sets for this exercise. */
    @Query(
        """SELECT * FROM set_log WHERE exerciseId = :exerciseId AND sessionId = (
             SELECT sessionId FROM set_log WHERE exerciseId = :exerciseId AND sessionId != :excludeSession
             ORDER BY loggedAt DESC LIMIT 1) ORDER BY setIndex, loggedAt"""
    )
    suspend fun lastSetsFor(exerciseId: String, excludeSession: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId AND sessionId != :excludeSession")
    suspend fun allSetsFor(exerciseId: String, excludeSession: Long): List<SetLogEntity>

    @Query("SELECT epochDay, mobility FROM workout_session WHERE completed = 1 AND epochDay >= :fromDay")
    fun completedSince(fromDay: Long): Flow<List<DayDone>>

    @Query("SELECT COUNT(*) FROM workout_session WHERE completed = 1 AND epochDay = :epochDay")
    suspend fun completedCount(epochDay: Long): Int

    @Query("SELECT MIN(epochDay) FROM workout_session WHERE completed = 1")
    fun firstCompletedDay(): Flow<Long?>

    /** Slot keys with at least one logged set today (feeds the read-only checklist on Home). */
    @Query(
        """SELECT DISTINCT l.slotKey FROM set_log l JOIN workout_session s ON s.id = l.sessionId
           WHERE s.epochDay = :epochDay"""
    )
    fun slotsLoggedOn(epochDay: Long): Flow<List<String>>

    @Query(
        """SELECT l.sessionId, s.epochDay, l.exerciseId, l.setIndex, l.weightKg, l.reps, l.seconds, l.meters, l.rir, l.deload
           FROM set_log l JOIN workout_session s ON s.id = l.sessionId WHERE s.completed = 1 ORDER BY s.epochDay, l.loggedAt"""
    )
    fun allSets(): Flow<List<SetRow>>

    @Query("SELECT * FROM workout_session WHERE completed = 1 ORDER BY epochDay")
    fun completedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM knee_pain ORDER BY epochDay, createdAt")
    fun kneeAll(): Flow<List<KneePainEntity>>

    @Insert
    suspend fun insertKnee(k: KneePainEntity): Long

    @Update
    suspend fun updateKnee(k: KneePainEntity)

    @Query("SELECT * FROM knee_pain ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastKnee(): KneePainEntity?

    @Query("SELECT * FROM knee_pain ORDER BY createdAt DESC LIMIT 1")
    fun lastKneeFlow(): Flow<KneePainEntity?>

    @Query("SELECT MAX(during) FROM knee_pain WHERE sessionId = :sessionId")
    suspend fun maxKneeDuring(sessionId: Long): Int?

    @Insert
    suspend fun insertNote(n: ExerciseNoteEntity): Long

    @Query("SELECT * FROM exercise_note WHERE exerciseId = :exerciseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastNote(exerciseId: String): ExerciseNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActive(a: ActiveSessionEntity)

    @Query("SELECT * FROM active_session WHERE id = 1")
    suspend fun active(): ActiveSessionEntity?

    @Query("DELETE FROM active_session")
    suspend fun clearActive()

    @Query("SELECT * FROM calendar_event")
    suspend fun calendarEvents(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCalendarEvent(e: CalendarEventEntity)

    @Query("DELETE FROM calendar_event WHERE `key` = :key")
    suspend fun deleteCalendarEvent(key: String)

    @Query("DELETE FROM calendar_event")
    suspend fun clearCalendarEvents()
}
