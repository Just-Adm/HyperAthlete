package com.hyperathlete.app.data.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hyperathlete.app.data.settings.SettingsRepository.Companion.enumOr
import com.hyperathlete.app.domain.account.AccountState
import com.hyperathlete.app.domain.account.BackupPrefs
import com.hyperathlete.app.domain.account.BackupStatus
import com.hyperathlete.app.domain.account.LinkedAccount
import com.hyperathlete.app.domain.account.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Linked accounts + backup status. A separate store from the settings on purpose: settings are part of
 * every backup, while "who is signed in on this phone" and "when did this phone last back up" are not.
 */
private val Context.accountStore: DataStore<Preferences> by preferencesDataStore(name = "account")

@Singleton
class AccountStore @Inject constructor(@ApplicationContext private val context: Context) {

    private fun keys(p: Provider) = p.name.lowercase().let { n ->
        AccountKeys(
            stringPreferencesKey("${n}_id"), stringPreferencesKey("${n}_name"), stringPreferencesKey("${n}_handle"),
            stringPreferencesKey("${n}_avatar"), longPreferencesKey("${n}_linked_at"),
        )
    }

    private data class AccountKeys(
        val id: Preferences.Key<String>, val name: Preferences.Key<String>, val handle: Preferences.Key<String>,
        val avatar: Preferences.Key<String>, val linkedAt: Preferences.Key<Long>,
    )

    private object K {
        val photoFrom = stringPreferencesKey("photo_from")
        val auto = booleanPreferencesKey("backup_auto")
        val time = stringPreferencesKey("backup_time")
        val lastAt = longPreferencesKey("backup_last_at")
        val lastBytes = longPreferencesKey("backup_last_bytes")
        val used = longPreferencesKey("backup_used_bytes")
        val count = intPreferencesKey("backup_count")
        val error = stringPreferencesKey("backup_error")
    }

    val state: Flow<AccountState> = context.accountStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            AccountState(
                google = p.account(Provider.GOOGLE),
                discord = p.account(Provider.DISCORD),
                photoFrom = p[K.photoFrom]?.let { enumOr<Provider>(it, Provider.GOOGLE) },
                backup = BackupPrefs(
                    auto = p[K.auto] ?: BackupPrefs().auto,
                    time = p[K.time]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: BackupPrefs().time,
                ),
                status = BackupStatus(
                    lastAt = p[K.lastAt]?.let(Instant::ofEpochMilli),
                    lastUploadedBytes = p[K.lastBytes] ?: 0,
                    usedBytes = p[K.used] ?: 0,
                    backups = p[K.count] ?: 0,
                    lastError = p[K.error],
                ),
            )
        }

    suspend fun current(): AccountState = state.first()

    private fun Preferences.account(provider: Provider): LinkedAccount? {
        val k = keys(provider)
        val id = this[k.id] ?: return null
        return LinkedAccount(provider, id, this[k.name].orEmpty(), this[k.handle].orEmpty(), this[k.avatar], Instant.ofEpochMilli(this[k.linkedAt] ?: 0))
    }

    suspend fun link(a: LinkedAccount) = context.accountStore.edit {
        val k = keys(a.provider)
        it[k.id] = a.id; it[k.name] = a.name; it[k.handle] = a.handle; it[k.linkedAt] = a.linkedAt.toEpochMilli()
        if (a.avatarUrl != null) it[k.avatar] = a.avatarUrl else it.remove(k.avatar)
        // First linked account with a picture becomes the profile photo.
        if (it[K.photoFrom] == null && a.avatarUrl != null) it[K.photoFrom] = a.provider.name
    }

    suspend fun unlink(provider: Provider) = context.accountStore.edit {
        val k = keys(provider)
        listOf(k.id, k.name, k.handle, k.avatar).forEach { key -> it.remove(key) }
        it.remove(k.linkedAt)
        if (it[K.photoFrom] == provider.name) it.remove(K.photoFrom)
        if (provider == Provider.GOOGLE) { it.remove(K.error) }
    }

    suspend fun setPhotoFrom(p: Provider?) = context.accountStore.edit { if (p == null) it.remove(K.photoFrom) else it[K.photoFrom] = p.name }

    suspend fun setBackupPrefs(b: BackupPrefs) = context.accountStore.edit { it[K.auto] = b.auto; it[K.time] = b.time.toString() }

    suspend fun setStatus(s: BackupStatus) = context.accountStore.edit {
        if (s.lastAt != null) it[K.lastAt] = s.lastAt.toEpochMilli() else it.remove(K.lastAt)
        it[K.lastBytes] = s.lastUploadedBytes; it[K.used] = s.usedBytes; it[K.count] = s.backups
        if (s.lastError != null) it[K.error] = s.lastError else it.remove(K.error)
    }
}
