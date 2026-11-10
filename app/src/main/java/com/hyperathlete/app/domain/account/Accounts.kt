package com.hyperathlete.app.domain.account

import java.time.Instant
import java.time.LocalTime

enum class Provider { GOOGLE, DISCORD }

/** A signed-in account linked to the (single) local profile. Tokens are not here: they live encrypted in SecretStore. */
data class LinkedAccount(
    val provider: Provider,
    val id: String,
    val name: String,
    /** Google: e-mail. Discord: @username. */
    val handle: String,
    val avatarUrl: String? = null,
    val linkedAt: Instant = Instant.EPOCH,
)

data class BackupPrefs(
    val auto: Boolean = true,
    val time: LocalTime = LocalTime.of(23, 30),
)

/** What the backup screen shows. Sizes are what is stored in Drive (compressed). */
data class BackupStatus(
    val lastAt: Instant? = null,
    val lastUploadedBytes: Long = 0,
    val usedBytes: Long = 0,
    val backups: Int = 0,
    val lastError: String? = null,
)

data class AccountState(
    val google: LinkedAccount? = null,
    val discord: LinkedAccount? = null,
    /** Which account's picture is the profile photo (null = none / initial letter). */
    val photoFrom: Provider? = null,
    val backup: BackupPrefs = BackupPrefs(),
    val status: BackupStatus = BackupStatus(),
) {
    val any: Boolean get() = google != null || discord != null
    fun of(p: Provider): LinkedAccount? = if (p == Provider.GOOGLE) google else discord
}
