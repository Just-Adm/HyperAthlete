package com.hyperathlete.app.ui.account

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.core.auth.AccountRepository
import com.hyperathlete.app.core.auth.AuthMessage
import com.hyperathlete.app.core.auth.GoogleStep
import com.hyperathlete.app.core.auth.NeedsSignInException
import com.hyperathlete.app.core.backup.BackupEngine
import com.hyperathlete.app.core.backup.BackupWork
import com.hyperathlete.app.core.backup.DataTransfer
import com.hyperathlete.app.core.backup.NoBackupException
import com.hyperathlete.app.core.backup.SnapshotException
import com.hyperathlete.app.data.account.AccountStore
import com.hyperathlete.app.domain.account.AccountState
import com.hyperathlete.app.domain.account.BackupPrefs
import com.hyperathlete.app.domain.account.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Everything the account, backup and data screens show as a one-line message. */
enum class Notice {
    GOOGLE_LINKED, DISCORD_LINKED, GOOGLE_SETUP, DISCORD_SETUP, CANCELLED, OFFLINE, FAILED,
    BACKED_UP, NOTHING_NEW, RESTORED, NO_BACKUP, SIGN_IN_AGAIN, DRIVE_SETUP, NEWER_APP, NOT_OURS,
    EXPORTED, IMPORTED, CLEARED,
}

/** Busy state for the data screen buttons. */
enum class DataWork { EXPORT_JSON, EXPORT_CSV, IMPORT, CLEAR }

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val store: AccountStore,
    private val engine: BackupEngine,
    private val transfer: DataTransfer,
) : ViewModel() {

    val state: StateFlow<AccountState> = accounts.state.stateIn(viewModelScope, SharingStarted.Eagerly, AccountState())
    val busy: StateFlow<Provider?> = accounts.busy
    val backupWork: StateFlow<BackupWork?> = engine.running

    private val _data = MutableStateFlow<DataWork?>(null)
    val dataWork: StateFlow<DataWork?> = _data

    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 4)
    val notices: SharedFlow<Notice> = _notices

    /** Google consent screen to show (the screen launches it). */
    private val _consent = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    val consent: SharedFlow<PendingIntent> = _consent

    /** Newest backup on Drive, checked after Google is linked during setup (offer to restore). */
    private val _latestBackup = MutableStateFlow<Instant?>(null)
    val latestBackup: StateFlow<Instant?> = _latestBackup

    init {
        viewModelScope.launch {
            accounts.messages.collect { m ->
                _notices.tryEmit(
                    when (m) {
                        AuthMessage.GOOGLE_LINKED -> Notice.GOOGLE_LINKED
                        AuthMessage.DISCORD_LINKED -> Notice.DISCORD_LINKED
                        AuthMessage.GOOGLE_SETUP -> Notice.GOOGLE_SETUP
                        AuthMessage.DISCORD_SETUP -> Notice.DISCORD_SETUP
                        AuthMessage.CANCELLED -> Notice.CANCELLED
                        AuthMessage.OFFLINE -> Notice.OFFLINE
                        AuthMessage.FAILED -> Notice.FAILED
                    },
                )
                if (m == AuthMessage.GOOGLE_LINKED) checkLatestBackup()
            }
        }
    }

    fun connectGoogle() = viewModelScope.launch {
        when (val step = accounts.startGoogle()) {
            is GoogleStep.NeedsConsent -> _consent.tryEmit(step.intent)
            else -> Unit
        }
    }

    fun googleResult(ok: Boolean, data: Intent?) = viewModelScope.launch {
        if (ok || data != null) accounts.completeGoogle(data) else accounts.cancelGoogle()
    }

    fun connectDiscord(context: Context, toolbarColor: Int) = accounts.startDiscord(context, toolbarColor)

    /** The app came back to the front: a Discord tab closed without an answer stops spinning. */
    fun resumed() = accounts.discordAbandoned()

    fun unlink(p: Provider) = viewModelScope.launch { accounts.unlink(p) }
    fun setPhotoFrom(p: Provider?) = viewModelScope.launch { accounts.setPhotoFrom(p) }
    fun setBackupPrefs(b: BackupPrefs) = viewModelScope.launch { store.setBackupPrefs(b) }

    fun checkLatestBackup() = viewModelScope.launch {
        _latestBackup.value = runCatching { engine.latest() }.getOrNull()
    }

    fun backupNow() = viewModelScope.launch {
        _notices.tryEmit(
            runCatching { engine.backup() }.fold(
                onSuccess = { if (it.newBackup) Notice.BACKED_UP else Notice.NOTHING_NEW },
                onFailure = ::failure,
            ),
        )
    }

    fun restore() = viewModelScope.launch {
        _notices.tryEmit(runCatching { engine.restoreLatest() }.fold(onSuccess = { Notice.RESTORED }, onFailure = ::failure))
        _latestBackup.value = null
    }

    fun dismissRestoreOffer() { _latestBackup.value = null }

    fun exportJson(uri: Uri) = data(DataWork.EXPORT_JSON, Notice.EXPORTED) { transfer.exportJson(uri) }
    fun exportCsv(uri: Uri) = data(DataWork.EXPORT_CSV, Notice.EXPORTED) { transfer.exportCsv(uri) }
    fun import(uri: Uri) = data(DataWork.IMPORT, Notice.IMPORTED) { transfer.importJson(uri) }
    fun clearAll() = data(DataWork.CLEAR, Notice.CLEARED) { transfer.clearAll() }

    /** Google status code for the last failure, e.g. 10 = app not registered in Google Cloud. */
    val lastGoogleCode: Int? get() = accounts.lastGoogleCode

    fun fileName(ext: String) = transfer.suggestedName(ext)

    private fun data(work: DataWork, ok: Notice, block: suspend () -> Unit) = viewModelScope.launch {
        _data.value = work
        _notices.tryEmit(runCatching { block() }.fold(onSuccess = { ok }, onFailure = ::failure))
        _data.value = null
    }

    private fun failure(e: Throwable): Notice = when (e) {
        is NeedsSignInException -> Notice.SIGN_IN_AGAIN
        is NoBackupException -> Notice.NO_BACKUP
        is SnapshotException -> if (e.reason == SnapshotException.Reason.NEWER_APP) Notice.NEWER_APP else Notice.NOT_OURS
        else -> when (BackupEngine.errorCode(e)) {
            "SIGN_IN" -> Notice.SIGN_IN_AGAIN
            "SETUP" -> Notice.DRIVE_SETUP
            "OFFLINE" -> Notice.OFFLINE
            else -> Notice.FAILED
        }
    }
}
