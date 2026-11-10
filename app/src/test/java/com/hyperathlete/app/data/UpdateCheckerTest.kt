package com.hyperathlete.app.data

import com.hyperathlete.app.core.update.UpdateChecker
import com.hyperathlete.app.core.update.UpdateResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateCheckerTest {

    private fun manifest(code: Int = 17, minSdk: Int = 26, url: String = "https://example.com/HyperAthlete-3.4.0.apk") = """
        {"versionCode": $code, "versionName": "3.4.0", "apkUrl": "$url",
         "sha256": "ab12", "releaseNotes": {"en": "- New things", "ar": "- أشياء جديدة"}, "minSdk": $minSdk, "extra": true}
    """.trimIndent()

    @Test fun `a newer version is offered, the same or older is not`() {
        val m = UpdateChecker.parse(manifest())!!
        assertEquals("- أشياء جديدة", m.releaseNotes.ar)
        assertTrue(UpdateChecker.decide(m, currentCode = 16, sdk = 34) is UpdateResult.Available)
        assertEquals(UpdateResult.UpToDate, UpdateChecker.decide(m, currentCode = 17, sdk = 34))
        assertEquals(UpdateResult.UpToDate, UpdateChecker.decide(m, currentCode = 18, sdk = 34))
    }

    @Test fun `a version that needs a newer Android is not offered as an update`() {
        val m = UpdateChecker.parse(manifest(minSdk = 35))!!
        assertTrue(UpdateChecker.decide(m, currentCode = 16, sdk = 30) is UpdateResult.Incompatible)
    }

    @Test fun `bad or unsafe files are ignored`() {
        assertNull(UpdateChecker.parse("<html>404</html>"))
        assertNull(UpdateChecker.parse("{}"))
        assertNull(UpdateChecker.parse(manifest(url = "http://example.com/x.apk")))
        assertNull(UpdateChecker.parse(manifest(code = 0)))
    }

    @Test fun `offline or unreachable fails quietly and quickly`() = runBlocking {
        val checker = UpdateChecker(OkHttpClient())
        val started = System.currentTimeMillis()
        // Nothing listens on port 1: connection refused, like a phone with no route to the server.
        assertEquals(UpdateResult.Unavailable, checker.check("https://127.0.0.1:1/version.json", 16, 34))
        // Plain http is never fetched.
        assertEquals(UpdateResult.Unavailable, checker.check("http://127.0.0.1:1/version.json", 16, 34))
        assertTrue(System.currentTimeMillis() - started < 13_000)
    }

    @Test fun `the published version json matches this build`() {
        val f = listOf(File("../downloads/version.json"), File("downloads/version.json")).firstOrNull { it.exists() } ?: return
        val m = UpdateChecker.parse(f.readText())
        assertNotNull(m)
        assertEquals(com.hyperathlete.app.BuildConfig.VERSION_CODE, m!!.versionCode)
        assertEquals(com.hyperathlete.app.BuildConfig.VERSION_NAME.removeSuffix("-debug"), m.versionName)
        assertEquals(64, m.sha256.length)
        assertTrue(m.releaseNotes.en.isNotBlank() && m.releaseNotes.ar.isNotBlank())
    }
}
