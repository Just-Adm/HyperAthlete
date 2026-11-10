package com.hyperathlete.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.BuildConfig
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.core.update.UpdateChecker
import com.hyperathlete.app.core.update.UpdateManifest
import com.hyperathlete.app.core.update.UpdateResult
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.ui.components.HyperDialog
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.theme.Hyper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the Settings update row is. [dialog] = a newer version the user hasn't closed yet. */
data class UpdateUiState(
    val checking: Boolean = false,
    val result: UpdateResult? = null,
    val dialog: UpdateManifest? = null,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(private val checker: UpdateChecker) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var job: Job? = null

    /** [quiet]: the automatic check when Settings opens; it only speaks up when there is an update. */
    fun check(quiet: Boolean = false) {
        if (job?.isActive == true) return
        if (quiet && _state.value.result != null) return
        job = viewModelScope.launch {
            _state.value = _state.value.copy(checking = !quiet)
            val r = checker.check()
            _state.value = UpdateUiState(
                checking = false,
                result = r,
                dialog = (r as? UpdateResult.Available)?.manifest,
            )
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(dialog = null)
    }
}

/** What the update row and dialog need (plain values in screenshot tests). */
class UpdateDeps(
    val state: UpdateUiState = UpdateUiState(),
    val onCheck: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val autoCheck: () -> Unit = {},
)

@Composable
fun rememberUpdateDeps(): UpdateDeps {
    val vm: UpdateViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val st by vm.state.collectAsStateWithLifecycle()
    return UpdateDeps(st, onCheck = { vm.check() }, onDismiss = vm::dismiss, autoCheck = { vm.check(quiet = true) })
}

/** Settings rows: the installed version, and "Check for updates" with what the last check found. */
@Composable
fun UpdateRows(deps: UpdateDeps) {
    val c = Hyper.colors
    LaunchedEffect(Unit) { deps.autoCheck() }
    SettingsRow(
        R.drawable.ic_info, stringResource(R.string.settings_about),
        subtitle = stringResource(R.string.settings_version, Bidi.ltr(BuildConfig.VERSION_NAME)),
        iconTint = c.textSecondary, iconBg = c.surfaceAlt,
    )
    HyperDivider()
    val st = deps.state
    SettingsRow(
        R.drawable.ic_refresh, stringResource(R.string.update_check),
        subtitle = when {
            st.checking -> stringResource(R.string.update_checking)
            st.result is UpdateResult.Available -> stringResource(R.string.update_available_short, Bidi.ltr(st.result.manifest.versionName))
            st.result is UpdateResult.UpToDate -> stringResource(R.string.update_up_to_date)
            st.result is UpdateResult.Incompatible -> stringResource(R.string.update_incompatible)
            st.result is UpdateResult.Unavailable -> stringResource(R.string.update_unavailable)
            else -> stringResource(R.string.update_check_desc)
        },
        onClick = deps.onCheck,
        trailing = {
            if (st.checking) CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
        },
    )
    st.dialog?.let { UpdateDialog(it, deps.onDismiss) }
}

/** Glass dialog with the release notes; "Update" opens the APK link in the browser (nothing installs by itself). */
@Composable
fun UpdateDialog(m: UpdateManifest, onDismiss: () -> Unit) {
    val c = Hyper.colors
    val context = LocalContext.current
    val english = LocalAppLanguage.current == AppLanguage.ENGLISH
    val notes = (if (english) m.releaseNotes.en else m.releaseNotes.ar).ifBlank { m.releaseNotes.en.ifBlank { m.releaseNotes.ar } }
    HyperDialog(onDismiss) {
        Text(stringResource(R.string.update_title), style = Hyper.type.h2, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.update_versions, Bidi.ltr(BuildConfig.VERSION_NAME), Bidi.ltr(m.versionName)),
            style = Hyper.type.smallMedium, color = c.textSecondary,
        )
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                notes.lines().filter { it.isNotBlank() }.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•  ", style = Hyper.type.body, color = c.accent)
                        Text(line.trim().removePrefix("-").removePrefix("•").trim(), style = Hyper.type.body, color = c.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.update_install_hint), style = Hyper.type.small, color = c.textSecondary)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(R.string.update_action), {
            openInBrowser(context, m.apkUrl)
            onDismiss()
        })
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.update_later), onDismiss, content = c.textSecondary)
    }
}

private fun openInBrowser(context: android.content.Context, url: String) {
    if (!url.startsWith("https://")) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    } catch (_: SecurityException) {
    }
}
