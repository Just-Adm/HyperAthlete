package com.hyperathlete.app.ui.workout

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.core.workout.ActiveSession
import com.hyperathlete.app.core.workout.SessionController
import com.hyperathlete.app.core.workout.SessionEvent
import com.hyperathlete.app.core.workout.SessionSummary
import com.hyperathlete.app.core.workout.WorkoutService
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.streak.StreakResult
import com.hyperathlete.app.domain.training.SetValues
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Everything the workout screen can do; the work itself happens in [SessionController]. */
interface WorkoutActions {
    fun toggleWarmup()
    fun begin()
    fun log(values: SetValues)
    fun startHold(seconds: Int)
    fun cancelHold()
    fun skipRest()
    fun addRest(seconds: Int)
    fun togglePause()
    fun skipExercise(key: String)
    fun jumpTo(key: String)
    fun previous()
    fun swap(key: String, exerciseId: String)
    fun note(key: String, text: String)
    fun undo()
    fun answerKnee(pain: Int?, sharp: Boolean)
    fun finish()
    fun discard()
    fun share(bitmap: Bitmap)
    fun saveImage(bitmap: Bitmap)
    fun close()

    companion object {
        val None = object : WorkoutActions {
            override fun toggleWarmup() {}
            override fun begin() {}
            override fun log(values: SetValues) {}
            override fun startHold(seconds: Int) {}
            override fun cancelHold() {}
            override fun skipRest() {}
            override fun addRest(seconds: Int) {}
            override fun togglePause() {}
            override fun skipExercise(key: String) {}
            override fun jumpTo(key: String) {}
            override fun previous() {}
            override fun swap(key: String, exerciseId: String) {}
            override fun note(key: String, text: String) {}
            override fun undo() {}
            override fun answerKnee(pain: Int?, sharp: Boolean) {}
            override fun finish() {}
            override fun discard() {}
            override fun share(bitmap: Bitmap) {}
            override fun saveImage(bitmap: Bitmap) {}
            override fun close() {}
        }
    }
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: SessionController,
    private val repo: WorkoutRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel(), WorkoutActions {

    val active: StateFlow<ActiveSession?> = controller.active

    private val _summary = MutableStateFlow<SessionSummary?>(null)
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()

    private val _physio = MutableStateFlow(false)
    val physio: StateFlow<Boolean> = _physio.asStateFlow()

    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    val streak: StateFlow<StreakResult?> = combine(repo.days(LocalDate.now(clock).minusDays(400)), repo.firstCompletedDay(), settings.settings) { days, first, s ->
        val today = LocalDate.now(clock)
        StreakCalculator.compute(days, { WeekPlan.of(it).kind }, listOfNotNull(s.streakSince, first).minOrNull(), today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            controller.events.collect { e ->
                when (e) {
                    is SessionEvent.Finished -> _summary.value = e.summary
                    SessionEvent.SeePhysio -> _physio.value = true
                }
            }
        }
    }

    /** Opens the running session, or starts today's when arriving from a reminder's "Start". */
    fun ensure(startToday: Boolean) {
        viewModelScope.launch {
            val a = controller.restore()
            if (a == null && startToday && _summary.value == null) {
                val today = LocalDate.now(clock)
                controller.start(today.dayOfWeek, today)
            }
            if (controller.active.value != null) WorkoutService.start(context)
            else if (_summary.value == null) _closed.value = true
        }
    }

    fun dismissPhysio() { _physio.value = false }
    fun consumeMessage() { _message.value = null }

    override fun toggleWarmup() = controller.toggleWarmup()
    override fun begin() = controller.begin()
    override fun log(values: SetValues) = controller.log(values)
    override fun startHold(seconds: Int) = controller.startHold(seconds)
    override fun cancelHold() = controller.cancelHold()
    override fun skipRest() = controller.skipRest()
    override fun addRest(seconds: Int) = controller.addRest(seconds)
    override fun togglePause() = controller.togglePause()
    override fun skipExercise(key: String) = controller.skipExercise(key)
    override fun jumpTo(key: String) = controller.jumpTo(key)
    override fun previous() = controller.previous()
    override fun swap(key: String, exerciseId: String) = controller.swap(key, exerciseId)
    override fun note(key: String, text: String) = controller.note(key, text)
    override fun undo() = controller.undo()
    override fun answerKnee(pain: Int?, sharp: Boolean) = controller.answerKnee(pain, sharp)

    override fun finish() {
        viewModelScope.launch {
            val s = controller.finish()
            if (s == null) _closed.value = true else _summary.value = s
        }
    }

    override fun discard() {
        viewModelScope.launch { controller.discard(); _closed.value = true }
    }

    override fun close() { _closed.value = true }

    override fun share(bitmap: Bitmap) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { cacheImage(bitmap) } ?: return@launch
            val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun saveImage(bitmap: Bitmap) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { saveToGallery(bitmap) }
            _message.value = if (ok) com.hyperathlete.app.R.string.summary_saved else com.hyperathlete.app.R.string.summary_save_failed
        }
    }

    private fun cacheImage(bitmap: Bitmap): Uri? = runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "hyper-athlete-session.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }.getOrNull()

    private fun saveToGallery(bitmap: Bitmap): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "HyperAthlete_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hyper Athlete")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } != null
    }.getOrDefault(false)
}
