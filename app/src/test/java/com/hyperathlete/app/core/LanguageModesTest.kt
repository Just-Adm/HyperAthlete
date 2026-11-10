package com.hyperathlete.app.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.AppLocales
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = android.app.Application::class)
class LanguageModesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun `mixed mode = Arabic interface with English terms`() {
        val l = Localizer(context, AppLanguage.MIXED)
        assertEquals("الرئيسية", l.string(R.string.nav_home))
        assertEquals("Sets", l.term(R.string.term_sets))
        assertEquals("Biceps", l.term(R.string.focus_biceps))
        assertEquals("صدر", l.string(R.string.focus_chest))
    }

    @Test fun `full Arabic translates terms too`() {
        val l = Localizer(context, AppLanguage.ARABIC)
        assertEquals("جولات", l.term(R.string.term_sets))
        assertEquals("باي", l.term(R.string.focus_biceps))
    }

    @Test fun `English mode is English everywhere`() {
        val l = Localizer(context, AppLanguage.ENGLISH)
        assertEquals("Home", l.string(R.string.nav_home))
        assertEquals("Sets", l.term(R.string.term_sets))
    }

    @Test fun `Arabic formatting keeps Latin digits`() {
        val l = Localizer(context, AppLanguage.MIXED)
        val text = l.string(R.string.week_of, 3, 9)
        assertEquals("الأسبوع 3 من 9", text)
        assertTrue(text.none { it in '٠'..'٩' })
    }

    @Test fun `layout direction follows the mode`() {
        assertTrue(AppLocales.isRtl(AppLanguage.MIXED))
        assertTrue(AppLocales.isRtl(AppLanguage.ARABIC))
        assertTrue(!AppLocales.isRtl(AppLanguage.ENGLISH))
    }
}
