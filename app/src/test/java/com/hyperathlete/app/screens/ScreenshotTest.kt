package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.core.permissions.AppPermission
import com.hyperathlete.app.core.permissions.PermissionState
import com.hyperathlete.app.core.security.Secret
import com.hyperathlete.app.data.weather.WeatherState
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.weather.DailyForecast
import com.hyperathlete.app.domain.weather.WeatherSnapshot
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.main.MainContent
import com.hyperathlete.app.ui.main.Tab
import com.hyperathlete.app.ui.main.WeatherDetails
import com.hyperathlete.app.ui.onboarding.OnboardingActions
import com.hyperathlete.app.ui.onboarding.OnboardingScreen
import com.hyperathlete.app.ui.onboarding.OnboardingState
import com.hyperathlete.app.ui.onboarding.OnboardingStep
import com.hyperathlete.app.ui.permissions.AutostartGuideCard
import com.hyperathlete.app.ui.permissions.PermissionCard
import com.hyperathlete.app.ui.permissions.PermissionController
import com.hyperathlete.app.ui.settings.AppearanceScreen
import com.hyperathlete.app.ui.settings.LanguageScreen
import com.hyperathlete.app.ui.settings.SubScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders the screens to app/screenshots/ (Roborazzi + Robolectric native graphics)
 * at the X7e's size (720×1604 px ≈ 360×802 dp). Run: ./gradlew :app:recordRoborazziDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class ScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15) // Thursday
    private val profileAr = UserProfile("آدم", LocalDate.of(2009, 3, 20), 67.0, 176.0)
    private val profileEn = profileAr.copy(name = "Adam")

    private fun ob(step: OnboardingStep, profile: UserProfile = profileAr, language: AppLanguage = AppLanguage.MIXED) = OnboardingState(
        loaded = true, step = step, language = language, profile = profile,
        programStart = LocalDate.of(2026, 10, 4), today = today,
    )

    private val noop = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})

    private val perms = PermissionController(
        states = AppPermission.entries.associateWith {
            when (it) {
                AppPermission.NOTIFICATIONS, AppPermission.EXACT_ALARMS, AppPermission.CALENDAR -> PermissionState.GRANTED
                else -> PermissionState.DENIED
            }
        },
        request = {}, openHonorSettings = {},
    )

    private val weather = WeatherSnapshot(
        lat = 31.95, lon = 35.93, city = "عمّان", fetchedAtMillis = 0, tempC = 24.4, feelsLikeC = 25.8, code = 1, isDay = true,
        windKmh = 12.0,
        daily = listOf(
            DailyForecast("2026-10-15", 1, 28.0, 16.0, 0, 18.0),
            DailyForecast("2026-10-16", 61, 22.0, 14.0, 70, 25.0),
            DailyForecast("2026-10-17", 3, 24.0, 15.0, 20, 15.0),
            DailyForecast("2026-10-18", 0, 27.0, 15.0, 0, 10.0),
            DailyForecast("2026-10-19", 0, 29.0, 16.0, 0, 10.0),
            DailyForecast("2026-10-20", 2, 30.0, 17.0, 5, 12.0),
            DailyForecast("2026-10-21", 95, 25.0, 15.0, 60, 30.0),
        ),
        // A typical October day in Amman: cool night, warm afternoon, a shower in the evening.
        hourly = (0..24).map { h ->
            val t = if (h in 6..23) 15.0 + 13.0 * kotlin.math.sin(Math.PI * (h - 6) / 18.0) else 15.0 - h % 24 * 0.2
            com.hyperathlete.app.domain.weather.HourlyForecast(
                time = if (h == 24) "2026-10-16T00:00" else "2026-10-15T%02d:00".format(h),
                tempC = t, code = if (h in 19..21) 61 else if (h in 12..17) 1 else 0,
                rainChance = if (h in 18..22) 40 else 0, isDay = h in 6..18,
            )
        },
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

    private fun appState(profile: UserProfile = profileAr, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, day: LocalDate = today) =
        AppState(AppSettings(onboardingDone = true, language = language, themeMode = theme, programStart = LocalDate.of(2026, 10, 4)), profile, day)

    @Composable
    private fun Main(
        st: AppState = appState(),
        tab: Tab = Tab.HOME,
        edit: Boolean = false,
        layout: PageLayout = PageLayout.default(PageId.HOME),
        w: WeatherState = WeatherState.Ready(weather),
    ) = MainContent(
        state = st, autostartAcknowledged = false, vm = null, open = {},
        layouts = mapOf(PageId.HOME to layout), onLayoutChange = {}, onLayoutReset = {},
        weather = w, onWeatherRefresh = {}, onEnableLocation = {}, initialTab = tab, startInEditMode = edit,
    )

    // Onboarding
    @Test fun ob1_welcome() = shot("ob1_welcome") { OnboardingScreen(ob(OnboardingStep.WELCOME), perms, noop) }
    @Test fun ob2_profile() = shot("ob2_profile") { OnboardingScreen(ob(OnboardingStep.PROFILE), perms, noop) }
    @Test fun ob3_program() = shot("ob3_program") { OnboardingScreen(ob(OnboardingStep.PROGRAM), perms, noop) }
    @Test fun ob4_permissions() = shot("ob4_permissions") { OnboardingScreen(ob(OnboardingStep.PERMISSIONS), perms, noop) }
    @Test fun ob5_account() = shot("ob5_account") { OnboardingScreen(ob(OnboardingStep.ACCOUNT), perms, noop) }
    @Test fun ob6_ready() = shot("ob6_ready") { OnboardingScreen(ob(OnboardingStep.READY), perms, noop) }

    // Main
    @Test fun main_home() = shot("main_home") { Main() }
    @Test fun main_home_wednesday_alert() = shot("main_home_wed_alert") { Main(st = appState(day = LocalDate.of(2026, 10, 14)), w = WeatherState.Ready(weather.copy(daily = weather.daily.map { it.copy(date = LocalDate.parse(it.date).minusDays(1).toString()) }.let { list -> list.mapIndexed { i, d -> if (i == 1) d.copy(rainChance = 70, code = 61) else d } }))) }
    @Test fun main_home_no_location() = shot("main_home_noloc") { Main(w = WeatherState.NoPermission) }
    @Test fun main_home_edit() = shot("main_home_edit") { Main(edit = true, layout = PageLayout.default(PageId.HOME).move(BoxId.HOME_WEEK, BoxId.HOME_STREAK).toggle(BoxId.HOME_TODAY)) }
    @Test fun main_account() = shot("main_account") { Main(tab = Tab.ACCOUNT) }
    @Test fun main_account_dark() = shot("main_account_dark", AppLanguage.ENGLISH, ThemeMode.DARK) { Main(st = appState(profileEn, AppLanguage.ENGLISH, ThemeMode.DARK), tab = Tab.ACCOUNT) }
    @Test fun main_nutrition_placeholder() = shot("main_nutrition") { Main(tab = Tab.NUTRITION) }
    @Test fun weather_details() = shot("weather_details") {
        Column(Modifier.verticalScroll(rememberScrollState())) { WeatherDetails(weather, today, {}, now = java.time.LocalTime.of(14, 20)) }
    }

    // Settings
    @Test fun settings_power() = shot("settings_power") {
        com.hyperathlete.app.ui.settings.PowerSettingsScreen(appState(), null, false, {}, {}, controller = perms)
    }
    @Test fun settings_power_ready() = shot("settings_power_ready") {
        val ok = PermissionController(AppPermission.entries.associateWith { PermissionState.GRANTED }, {}, {})
        com.hyperathlete.app.ui.settings.PowerSettingsScreen(appState(), null, true, {}, {}, controller = ok)
    }
    @Test fun settings_language() = shot("settings_language") { LanguageScreen(AppLanguage.MIXED, {}, {}) }
    @Test fun settings_appearance() = shot("settings_appearance") { AppearanceScreen(AppSettings(), null, {}) }
    @Test fun honor_guide() = shot("perm_honor_guide") {
        Column(Modifier.padding(20.dp)) { AutostartGuideCard(PermissionState.DENIED, false, {}, {}) }
    }

    // Other language modes / dark theme
    @Test fun en_dark_welcome() = shot("en_dark_ob1_welcome", AppLanguage.ENGLISH, ThemeMode.DARK) {
        OnboardingScreen(ob(OnboardingStep.WELCOME, profileEn, AppLanguage.ENGLISH), perms, noop)
    }
    @Test fun en_dark_home() = shot("en_dark_main_home", AppLanguage.ENGLISH, ThemeMode.DARK) {
        Main(st = appState(profileEn, AppLanguage.ENGLISH, ThemeMode.DARK), w = WeatherState.Ready(weather.copy(city = "Amman")))
    }
    @Test fun ar_full_program() = shot("arfull_ob3_program", AppLanguage.ARABIC) {
        OnboardingScreen(ob(OnboardingStep.PROGRAM, language = AppLanguage.ARABIC), perms, noop)
    }
    @Test fun dark_home_edit() = shot("dark_main_home_edit", theme = ThemeMode.DARK) { Main(st = appState(theme = ThemeMode.DARK), edit = true) }
}
