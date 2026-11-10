package com.hyperathlete.app.data.workout

import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.domain.training.PastExercise
import com.hyperathlete.app.domain.training.SetValues
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Training history: sessions, sets, knee reports and the running-session snapshot. */
@Singleton
class WorkoutRepository @Inject constructor(private val dao: WorkoutDao) {

    suspend fun createSession(s: WorkoutSessionEntity): Long = dao.insertSession(s)
    suspend fun session(id: Long): WorkoutSessionEntity? = dao.session(id)
    suspend fun updateSession(s: WorkoutSessionEntity) = dao.updateSession(s)
    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    suspend fun logSet(
        sessionId: Long, exerciseId: String, slotKey: String, setIndex: Int, v: SetValues, deload: Boolean, at: Long,
    ): Long = dao.insertSet(
        SetLogEntity(
            sessionId = sessionId, exerciseId = exerciseId, slotKey = slotKey, setIndex = setIndex,
            weightKg = v.weightKg, reps = v.reps, seconds = v.seconds, meters = v.meters, rir = v.rir,
            deload = deload, loggedAt = at,
        ),
    )

    suspend fun deleteSet(id: Long) = dao.deleteSet(id)

    suspend fun setsOf(sessionId: Long): List<SetLogEntity> = dao.sets(sessionId)

    /** The last earlier time this exercise was trained. */
    suspend fun lastTime(exerciseId: String, excludeSession: Long): PastExercise? {
        val rows = dao.lastSetsFor(exerciseId, excludeSession)
        if (rows.isEmpty()) return null
        val day = dao.session(rows.first().sessionId)?.epochDay ?: 0
        return PastExercise(day, rows.map { it.values() }, rows.any { it.deload })
    }

    suspend fun everythingBefore(exerciseId: String, excludeSession: Long): List<SetValues> =
        dao.allSetsFor(exerciseId, excludeSession).map { it.values() }

    /** Finished sessions per day, for the streak. */
    fun days(from: LocalDate): Flow<Map<LocalDate, DayRecord>> = dao.completedSince(from.toEpochDay()).map { rows ->
        rows.groupBy { it.epochDay }.map { (day, list) ->
            LocalDate.ofEpochDay(day) to DayRecord(sessionDone = list.any { !it.mobility }, mobilityDone = list.any { it.mobility })
        }.toMap()
    }

    suspend fun isDone(date: LocalDate): Boolean = dao.completedCount(date.toEpochDay()) > 0

    fun firstCompletedDay(): Flow<LocalDate?> = dao.firstCompletedDay().map { it?.let(LocalDate::ofEpochDay) }

    fun slotsLoggedOn(date: LocalDate): Flow<Set<String>> = dao.slotsLoggedOn(date.toEpochDay()).map { it.toSet() }

    /* ---- progress page ---- */
    val allSets: Flow<List<SetRow>> = dao.allSets()
    val completedSessions: Flow<List<WorkoutSessionEntity>> = dao.completedSessions()
    val kneeReports: Flow<List<KneePainEntity>> = dao.kneeAll()

    suspend fun saveKnee(k: KneePainEntity): Long = dao.insertKnee(k)
    suspend fun updateKnee(k: KneePainEntity) = dao.updateKnee(k)
    suspend fun lastKnee(): KneePainEntity? = dao.lastKnee()
    fun lastKneeFlow(): Flow<KneePainEntity?> = dao.lastKneeFlow()
    suspend fun maxKneeDuring(sessionId: Long): Int? = dao.maxKneeDuring(sessionId)

    suspend fun addNote(sessionId: Long, exerciseId: String, text: String, at: Long) =
        dao.insertNote(ExerciseNoteEntity(sessionId = sessionId, exerciseId = exerciseId, text = text, createdAt = at))

    suspend fun lastNote(exerciseId: String): String? = dao.lastNote(exerciseId)?.text

    suspend fun saveSnapshot(json: String, at: Long) = dao.saveActive(ActiveSessionEntity(json = json, updatedAt = at))
    suspend fun snapshot(): String? = dao.active()?.json
    suspend fun clearSnapshot() = dao.clearActive()

    private fun SetLogEntity.values() = SetValues(weightKg, reps, seconds, meters, rir)
}
