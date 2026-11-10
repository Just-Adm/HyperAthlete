package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyperathlete.app.core.backup.BackupWork
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.domain.account.AccountState
import com.hyperathlete.app.domain.account.BackupPrefs
import com.hyperathlete.app.domain.account.BackupStatus
import com.hyperathlete.app.domain.account.LinkedAccount
import com.hyperathlete.app.domain.account.Provider
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.account.AccountDeps
import com.hyperathlete.app.ui.account.BackupScreen
import com.hyperathlete.app.ui.account.DataActions
import com.hyperathlete.app.ui.account.DataScreen
import com.hyperathlete.app.ui.account.DataWork
import com.hyperathlete.app.ui.main.AccountScreen
import com.hyperathlete.app.ui.onboarding.OnboardingActions
import com.hyperathlete.app.ui.onboarding.OnboardingScreen
import com.hyperathlete.app.ui.onboarding.OnboardingState
import com.hyperathlete.app.ui.onboarding.OnboardingStep
import com.hyperathlete.app.ui.permissions.PermissionController
import com.hyperathlete.app.ui.settings.AppearanceScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import com.hyperathlete.app.ui.theme.ThemePreset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class AccountScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 16)
    private val profile = UserProfile("آدم", LocalDate.of(2009, 3, 20), 67.2, 176.0)
    private fun at(h: Int, m: Int, daysAgo: Long = 0): Instant = LocalDateTime.of(today.minusDays(daysAgo), java.time.LocalTime.of(h, m)).atZone(ZoneId.systemDefault()).toInstant()

    private val google = LinkedAccount(Provider.GOOGLE, "1", "Adam Oudeh", "adam.oudeh0@gmail.com", "https://x/y.png")
    private val discord = LinkedAccount(Provider.DISCORD, "2", "Adam", "@adam", "https://x/z.png")
    private val linked = AccountState(
        google = google, discord = discord, photoFrom = Provider.GOOGLE,
        status = BackupStatus(lastAt = at(23, 30, 1), lastUploadedBytes = 18_400, usedBytes = 2_640_000, backups = 12),
    )

    private fun shot(name: String, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvideAppLanguage(language) {
                    HyperTheme(themeMode = theme) {
                        Box(Modifier.fillMaxSize().background(Hyper.colors.bg)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private fun app(language: AppLanguage = AppLanguage.MIXED) =
        AppState(AppSettings(onboardingDone = true, language = language, programStart = LocalDate.of(2026, 10, 4)), profile, today)

    @Test fun account_linked() = shot("account_linked") { AccountScreen(app(), false, null, {}, AccountDeps(linked)) }

    @Test fun account_one_linked_busy() = shot("account_google_only") {
        AccountScreen(app(), false, null, {}, AccountDeps(AccountState(google = google, photoFrom = Provider.GOOGLE), busy = Provider.DISCORD))
    }

    @Test fun account_english() = shot("account_english", AppLanguage.ENGLISH) {
        AccountScreen(app(AppLanguage.ENGLISH), false, null, {}, AccountDeps(linked))
    }

    @Test fun backup_not_linked() = shot("backup_not_linked") { BackupScreen(AccountDeps(AccountState()), {}) }

    @Test fun backup_linked() = shot("backup_linked") { BackupScreen(AccountDeps(linked), {}) }

    @Test fun backup_running_dark() = shot("backup_running_dark", theme = ThemeMode.DARK) {
        BackupScreen(AccountDeps(linked, backupWork = BackupWork.BACKING_UP), {})
    }

    @Test fun backup_error() = shot("backup_error") {
        BackupScreen(AccountDeps(linked.copy(status = linked.status.copy(lastError = "SETUP"), backup = BackupPrefs(auto = false))), {})
    }

    @Test fun data_screen() = shot("data_screen") { DataScreen(DataWork.EXPORT_CSV, DataActions(), {}) }

    @Test fun ob_account_linked_restore() = shot("ob5_account_linked") {
        OnboardingScreen(
            OnboardingState(loaded = true, step = OnboardingStep.ACCOUNT, language = AppLanguage.MIXED, profile = profile, programStart = LocalDate.of(2026, 10, 4), today = today),
            PermissionController(emptyMap(), {}, {}),
            OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}),
            AccountDeps(AccountState(google = google), latestBackup = at(23, 30, 1)),
        )
    }

    @Test fun appearance_presets_dark() = shot("appearance_presets_dark", theme = ThemeMode.DARK) {
        val p = ThemePreset.All[1]
        AppearanceScreen(AppSettings(themeMode = p.mode, accentArgb = p.accentArgb, secondaryArgb = p.secondaryArgb), null, {})
    }
}
