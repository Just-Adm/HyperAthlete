package com.hyperathlete.app.ui.account

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.core.backup.BackupWork
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.domain.account.AccountState
import com.hyperathlete.app.domain.account.BackupPrefs
import com.hyperathlete.app.domain.account.LinkedAccount
import com.hyperathlete.app.domain.account.Provider
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTimePickerDialog
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.settings.SubScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** State + actions for the account / backup / data UI. Screens take this so screenshot tests can fake it. */
class AccountDeps(
    val state: AccountState,
    val busy: Provider? = null,
    val backupWork: BackupWork? = null,
    val dataWork: DataWork? = null,
    val latestBackup: Instant? = null,
    val connectGoogle: () -> Unit = {},
    val connectDiscord: () -> Unit = {},
    val unlink: (Provider) -> Unit = {},
    val setPhotoFrom: (Provider?) -> Unit = {},
    val setBackupPrefs: (BackupPrefs) -> Unit = {},
    val backupNow: () -> Unit = {},
    val restore: () -> Unit = {},
    val dismissRestore: () -> Unit = {},
)

/** Wires [AccountViewModel] to the UI: Google consent screen, Discord tab, one-line notices. */
@Composable
fun rememberAccountDeps(vm: AccountViewModel = hiltViewModel(), onNotice: (Notice) -> Unit = {}): AccountDeps {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val backupWork by vm.backupWork.collectAsStateWithLifecycle()
    val dataWork by vm.dataWork.collectAsStateWithLifecycle()
    val latest by vm.latestBackup.collectAsStateWithLifecycle()
    val toolbar = Hyper.colors.surface.toArgb()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        vm.googleResult(r.resultCode == android.app.Activity.RESULT_OK, r.data)
    }
    LaunchedEffect(vm) {
        vm.consent.collect { pi -> runCatching { consent.launch(IntentSenderRequest.Builder(pi).build()) }.onFailure { vm.googleResult(false, null) } }
    }
    LaunchedEffect(vm) {
        vm.notices.collect { n ->
            val text = resources.getString(noticeRes(n)) + (if (n == Notice.GOOGLE_SETUP) vm.lastGoogleCode?.let { " (${'$'}it)" }.orEmpty() else "")
            Toast.makeText(context, text, if (n.ordinal >= Notice.BACKED_UP.ordinal || n == Notice.CANCELLED) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            onNotice(n)
        }
    }
    LifecycleResumeEffect(vm) {
        vm.resumed()
        onPauseOrDispose { }
    }
    return AccountDeps(
        state = state, busy = busy, backupWork = backupWork, dataWork = dataWork, latestBackup = latest,
        connectGoogle = { vm.connectGoogle() },
        connectDiscord = { vm.connectDiscord(context, toolbar) },
        unlink = vm::unlink,
        setPhotoFrom = vm::setPhotoFrom,
        setBackupPrefs = vm::setBackupPrefs,
        backupNow = { vm.backupNow() },
        restore = { vm.restore() },
        dismissRestore = vm::dismissRestoreOffer,
    )
}

fun noticeRes(n: Notice): Int = when (n) {
    Notice.GOOGLE_LINKED -> R.string.notice_google_linked
    Notice.DISCORD_LINKED -> R.string.notice_discord_linked
    Notice.GOOGLE_SETUP -> R.string.notice_google_setup
    Notice.DISCORD_SETUP -> R.string.notice_discord_setup
    Notice.CANCELLED -> R.string.notice_cancelled
    Notice.OFFLINE -> R.string.notice_offline
    Notice.FAILED -> R.string.notice_failed
    Notice.BACKED_UP -> R.string.notice_backed_up
    Notice.NOTHING_NEW -> R.string.notice_nothing_new
    Notice.RESTORED -> R.string.notice_restored
    Notice.NO_BACKUP -> R.string.notice_no_backup
    Notice.SIGN_IN_AGAIN -> R.string.notice_sign_in_again
    Notice.DRIVE_SETUP -> R.string.notice_drive_setup
    Notice.NEWER_APP -> R.string.notice_newer_app
    Notice.NOT_OURS -> R.string.notice_not_ours
    Notice.EXPORTED -> R.string.notice_exported
    Notice.IMPORTED -> R.string.notice_imported
    Notice.CLEARED -> R.string.notice_cleared
}

/* ------------------------------------------------------------ Shared bits */

/** Profile picture from the linked account (local file), or the first letter on the accent colour. */
@Composable
fun ProfileAvatar(photo: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val bitmap by produceState<android.graphics.Bitmap?>(null, photo) {
        value = photo?.substringBefore('?')?.let { p ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(p) }.getOrNull() }
        }
    }
    Box(modifier.size(size).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size), contentScale = ContentScale.Crop)
        else Text(name.trim().take(1).uppercase(), style = if (size >= 50.dp) Hyper.type.h1 else Hyper.type.title, color = c.onAccent)
    }
}

@Composable
private fun providerName(p: Provider) = stringResource(if (p == Provider.GOOGLE) R.string.account_google else R.string.account_discord)

private fun providerIcon(p: Provider) = if (p == Provider.GOOGLE) R.drawable.ic_account_circle else R.drawable.ic_waving_hand

/** "16 Oct · 23:30" in the app language, LTR-safe. */
@Composable
fun backupTime(i: Instant): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.US
    val t = i.atZone(ZoneId.systemDefault())
    return t.format(DateTimeFormatter.ofPattern("d MMM", locale)) + " · " + Bidi.ltr(t.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)))
}

fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024L * 1024 -> String.format(Locale.US, "%.0f KB", b / 1024.0)
    b < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", b / 1024.0 / 1024)
    else -> String.format(Locale.US, "%.2f GB", b / 1024.0 / 1024 / 1024)
}

@Composable
private fun BusyDot() = CircularProgressIndicator(Modifier.size(22.dp), color = Hyper.colors.accent, strokeWidth = 2.5.dp)

/* ------------------------------------------------------------ Account tab section */

/** "Accounts" card: Google and Discord rows, and which picture the profile uses. */
@Composable
fun AccountsCard(deps: AccountDeps, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val s = deps.state
    var confirm by rememberSaveable { mutableStateOf<Provider?>(null) }
    HyperCard(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
        Provider.entries.forEachIndexed { i, p ->
            if (i > 0) HyperDivider()
            AccountRow(p, s.of(p), deps.busy == p, onConnect = if (p == Provider.GOOGLE) deps.connectGoogle else deps.connectDiscord, onDisconnect = { confirm = p })
        }
        val withPhoto = Provider.entries.filter { s.of(it)?.avatarUrl != null }
        if (withPhoto.size == 2) {
            HyperDivider()
            Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.account_photo_from), style = Hyper.type.smallMedium, color = c.textSecondary)
                Spacer(Modifier.height(8.dp))
                val options = listOf<Provider?>(Provider.GOOGLE, Provider.DISCORD, null)
                Segmented(
                    listOf(stringResource(R.string.account_google), stringResource(R.string.account_discord), stringResource(R.string.account_photo_none)),
                    selected = options.indexOf(s.photoFrom).coerceAtLeast(0),
                    onSelect = { deps.setPhotoFrom(options[it]) },
                )
            }
        }
    }
    confirm?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.account_unlink_title, providerName(p)),
            message = stringResource(if (p == Provider.GOOGLE) R.string.account_unlink_google_msg else R.string.account_unlink_discord_msg),
            confirm = stringResource(R.string.account_disconnect),
            destructive = true,
            onDismiss = { confirm = null },
            onConfirm = { deps.unlink(p); confirm = null },
        )
    }
}

@Composable
private fun AccountRow(p: Provider, account: LinkedAccount?, busy: Boolean, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val c = Hyper.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(providerIcon(p), size = 40.dp, iconSize = 20.dp, tint = if (account != null) c.accent else c.textSecondary, background = if (account != null) c.accentSoft else c.surfaceAlt)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                providerName(p),
                style = Hyper.type.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                account?.handle?.let { Bidi.ltr(it) } ?: stringResource(if (p == Provider.GOOGLE) R.string.account_google_hint else R.string.account_discord_hint),
                style = Hyper.type.small, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            busy -> BusyDot()
            account != null -> SmallPillButton(stringResource(R.string.account_disconnect), onDisconnect, filled = false)
            else -> SmallPillButton(stringResource(R.string.account_connect), onConnect)
        }
    }
}

/** Rows that open the backup and data screens. */
@Composable
fun BackupAndDataCard(deps: AccountDeps, onBackup: () -> Unit, onData: () -> Unit, modifier: Modifier = Modifier) {
    val s = deps.state
    HyperCard(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
        SettingsRow(
            R.drawable.ic_cloud_upload, stringResource(R.string.backup_title),
            subtitle = when {
                s.google == null -> stringResource(R.string.backup_row_off)
                s.status.lastAt != null -> stringResource(R.string.backup_row_last, backupTime(s.status.lastAt))
                else -> stringResource(R.string.backup_never)
            },
            onClick = onBackup,
        )
        HyperDivider()
        SettingsRow(R.drawable.ic_layers, stringResource(R.string.data_title), subtitle = stringResource(R.string.data_desc), onClick = onData)
    }
}

/* ------------------------------------------------------------ Backup screen */

@Composable
fun BackupScreen(deps: AccountDeps, onBack: () -> Unit) {
    val c = Hyper.colors
    val s = deps.state
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val linked = s.google != null
    SubScreen(
        stringResource(R.string.backup_title), onBack,
        bottom = if (!linked) null else ({
            PrimaryButton(
                stringResource(if (deps.backupWork == BackupWork.BACKING_UP) R.string.backup_running else R.string.backup_now),
                deps.backupNow, loading = deps.backupWork == BackupWork.BACKING_UP, enabled = deps.backupWork == null, icon = R.drawable.ic_cloud_upload,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                stringResource(if (deps.backupWork == BackupWork.RESTORING) R.string.restore_running else R.string.backup_restore),
                { confirmRestore = true }, enabled = deps.backupWork == null,
                icon = R.drawable.ic_history,
            )
        }),
    ) {
        if (!linked) {
            HyperCard(Modifier.fillMaxWidth().enterAnimation(0), padding = PaddingValues(20.dp)) {
                IconTile(R.drawable.ic_cloud_upload)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.backup_connect_title), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.backup_connect_desc), style = Hyper.type.small, color = c.textSecondary)
                Spacer(Modifier.height(18.dp))
                PrimaryButton(stringResource(R.string.ob_account_google), deps.connectGoogle, loading = deps.busy == Provider.GOOGLE, icon = R.drawable.ic_account_circle)
            }
        } else {
            StatusCard(deps, Modifier.enterAnimation(0))
            Spacer(Modifier.height(12.dp))
            HyperCard(Modifier.fillMaxWidth().enterAnimation(1), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                SettingsRow(
                    R.drawable.ic_sync, stringResource(R.string.backup_auto), subtitle = stringResource(R.string.backup_auto_desc),
                    trailing = { HyperSwitch(s.backup.auto, { deps.setBackupPrefs(s.backup.copy(auto = it)) }) },
                )
                HyperDivider()
                SettingsRow(
                    R.drawable.ic_schedule, stringResource(R.string.backup_time), enabled = s.backup.auto,
                    onClick = { pickTime = true },
                    trailing = {
                        Text(Bidi.ltr(s.backup.time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))), style = Hyper.type.title, color = if (s.backup.auto) c.accent else c.textMuted)
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().enterAnimation(2).padding(horizontal = 4.dp)) {
            Icon(painterResource(R.drawable.ic_info), null, tint = c.textMuted, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.backup_rules), style = Hyper.type.small, color = c.textMuted)
        }
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore),
            message = stringResource(R.string.backup_restore_confirm),
            confirm = stringResource(R.string.backup_restore),
            destructive = true,
            onDismiss = { confirmRestore = false },
            onConfirm = { confirmRestore = false; deps.restore() },
        )
    }
    if (pickTime) {
        HyperTimePickerDialog(
            title = stringResource(R.string.backup_time), initial = s.backup.time,
            onDismiss = { pickTime = false },
            onConfirm = { deps.setBackupPrefs(s.backup.copy(time = it)); pickTime = false },
        )
    }
}

@Composable
private fun StatusCard(deps: AccountDeps, modifier: Modifier) {
    val c = Hyper.colors
    val st = deps.state.status
    val error = st.lastError
    HyperCard(modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                if (error != null) R.drawable.ic_warning else R.drawable.ic_cloud_done,
                tint = if (error != null) c.warn else c.success, background = if (error != null) c.warnSoft else c.successSoft,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.backup_last), style = Hyper.type.small, color = c.textSecondary)
                Text(st.lastAt?.let { backupTime(it) } ?: stringResource(R.string.backup_never), style = Hyper.type.title, color = c.text)
            }
            if (deps.backupWork != null) BusyDot()
        }
        deps.state.google?.let {
            Spacer(Modifier.height(10.dp))
            Text(Bidi.ltr(it.handle), style = Hyper.type.small, color = c.textMuted)
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(
                    when (error) {
                        "SIGN_IN" -> R.string.backup_err_sign_in
                        "SETUP" -> R.string.backup_err_setup
                        "OFFLINE" -> R.string.backup_err_offline
                        else -> R.string.backup_err_failed
                    },
                ),
                style = Hyper.type.smallMedium, color = c.warn,
            )
        }
        Spacer(Modifier.height(16.dp))
        val fraction = (st.usedBytes.toDouble() / com.hyperathlete.app.core.backup.BackupPlan.CAP_BYTES).toFloat().coerceIn(0f, 1f)
        val shown by animateFloatAsState(fraction, tween(700), label = "used")
        Box(Modifier.fillMaxWidth().height(8.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
            // At least a sliver, so a few MB still read as "something is there".
            Box(Modifier.fillMaxWidth(if (st.usedBytes > 0) shown.coerceAtLeast(0.015f) else 0f).height(8.dp).clip(HyperShapes.pill).background(c.accent))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(stringResource(R.string.backup_used, Bidi.ltr(formatBytes(st.usedBytes))), style = Hyper.type.small, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.backup_count, st.backups), style = Hyper.type.small, color = c.textSecondary)
        }
    }
}

/* ------------------------------------------------------------ Data screen */

class DataActions(
    val exportJson: () -> Unit = {},
    val exportCsv: () -> Unit = {},
    val import: () -> Unit = {},
    val clear: () -> Unit = {},
)

@Composable
fun DataRoute(onBack: () -> Unit, vm: AccountViewModel = hiltViewModel()) {
    val work by vm.dataWork.collectAsStateWithLifecycle()
    rememberAccountDeps(vm)
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportJson) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(vm::exportCsv) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::import) }
    DataScreen(
        work,
        DataActions(
            exportJson = { json.launch(vm.fileName("json")) },
            exportCsv = { csv.launch(vm.fileName("csv.zip")) },
            import = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            clear = vm::clearAll,
        ),
        onBack,
    )
}

@Composable
fun DataScreen(work: DataWork?, actions: DataActions, onBack: () -> Unit) {
    val c = Hyper.colors
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.data_title), onBack) {
        HyperCard(Modifier.fillMaxWidth().enterAnimation(0), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            DataRow(R.drawable.ic_download, R.string.data_export_json, R.string.data_export_json_desc, work == DataWork.EXPORT_JSON, work == null, actions.exportJson)
            HyperDivider()
            DataRow(R.drawable.ic_format_list_bulleted, R.string.data_export_csv, R.string.data_export_csv_desc, work == DataWork.EXPORT_CSV, work == null, actions.exportCsv)
            HyperDivider()
            DataRow(R.drawable.ic_history, R.string.data_import, R.string.data_import_desc, work == DataWork.IMPORT, work == null) { confirmImport = true }
        }
        Spacer(Modifier.height(12.dp))
        HyperCard(Modifier.fillMaxWidth().enterAnimation(1), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_delete, stringResource(R.string.data_clear), subtitle = stringResource(R.string.data_clear_desc),
                iconTint = c.danger, iconBg = c.dangerSoft, enabled = work == null,
                onClick = { confirmClear = true },
                trailing = { if (work == DataWork.CLEAR) BusyDot() },
            )
        }
    }
    if (confirmImport) {
        ConfirmDialog(
            title = stringResource(R.string.data_import), message = stringResource(R.string.data_import_confirm),
            confirm = stringResource(R.string.data_import), destructive = true,
            onDismiss = { confirmImport = false }, onConfirm = { confirmImport = false; actions.import() },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.data_clear), message = stringResource(R.string.data_clear_confirm),
            confirm = stringResource(R.string.data_clear_action), destructive = true,
            onDismiss = { confirmClear = false }, onConfirm = { confirmClear = false; actions.clear() },
        )
    }
}

@Composable
private fun DataRow(icon: Int, title: Int, desc: Int, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    SettingsRow(
        icon, stringResource(title), subtitle = stringResource(desc), enabled = enabled || busy, onClick = if (enabled) onClick else null,
        trailing = { if (busy) BusyDot() else Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Hyper.colors.textMuted, modifier = Modifier.size(20.dp)) },
    )
}

/* ------------------------------------------------------------ Setup (onboarding) */

/** Connect buttons, daily-backup switch and "found a backup — restore it?" for the setup's account step. */
@Composable
fun OnboardingAccountContent(deps: AccountDeps) {
    val c = Hyper.colors
    val s = deps.state
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Provider.entries.forEachIndexed { i, p ->
            ConnectButton(p, s.of(p), deps.busy == p, if (p == Provider.GOOGLE) deps.connectGoogle else deps.connectDiscord, Modifier.enterAnimation(1 + i))
        }
    }
    Spacer(Modifier.height(20.dp))
    HyperCard(Modifier.fillMaxWidth().enterAnimation(3), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val on = s.google != null
            IconTile(R.drawable.ic_cloud_upload, tint = if (on) c.accent else c.lock, background = if (on) c.accentSoft else c.lockSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ob_account_backup), style = Hyper.type.bodyMedium, color = c.text)
                Text(stringResource(R.string.ob_account_backup_desc), style = Hyper.type.small, color = c.textMuted)
            }
            HyperSwitch(on && s.backup.auto, { deps.setBackupPrefs(s.backup.copy(auto = it)) }, enabled = on)
        }
    }
    val latest = deps.latestBackup
    if (latest != null) {
        Spacer(Modifier.height(12.dp))
        HyperCard(Modifier.fillMaxWidth(), borderColor = c.accent.copy(alpha = 0.6f), padding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_history)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ob_restore_title), style = Hyper.type.title, color = c.text)
                    Text(stringResource(R.string.ob_restore_desc, backupTime(latest)), style = Hyper.type.small, color = c.textSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(stringResource(R.string.ob_restore_action), deps.restore, loading = deps.backupWork == BackupWork.RESTORING, icon = R.drawable.ic_history)
            Spacer(Modifier.height(8.dp))
            com.hyperathlete.app.ui.components.TextAction(stringResource(R.string.ob_restore_skip), deps.dismissRestore)
        }
    }
}

@Composable
private fun ConnectButton(p: Provider, account: LinkedAccount?, busy: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = Hyper.colors
    val linked = account != null
    Row(
        modifier.fillMaxWidth().clip(HyperShapes.pill)
            .background(if (linked) c.successSoft else c.surface)
            .border(1.4.dp, if (linked) c.success.copy(alpha = 0.4f) else c.border, HyperShapes.pill)
            .then(if (!linked && !busy) Modifier.hyperClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (busy) BusyDot()
        else Icon(painterResource(if (linked) R.drawable.ic_check_circle_fill else providerIcon(p)), null, tint = if (linked) c.success else c.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (linked) providerName(p) + " · " + account!!.name else stringResource(if (p == Provider.GOOGLE) R.string.ob_account_google else R.string.ob_account_discord),
            style = Hyper.type.button.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = if (linked) c.success else c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
