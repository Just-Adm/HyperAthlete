package com.hyperathlete.app.domain

import com.hyperathlete.app.core.auth.DiscordOAuth
import com.hyperathlete.app.core.auth.Pkce
import com.hyperathlete.app.core.backup.BackupPlan
import com.hyperathlete.app.core.backup.BackupWorker
import com.hyperathlete.app.core.backup.DataTransfer
import com.hyperathlete.app.core.backup.DriveFile
import com.hyperathlete.app.core.backup.Manifest
import com.hyperathlete.app.core.backup.Snapshot
import com.hyperathlete.app.core.backup.TableChunk
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.ui.theme.ThemePreset
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream

class AccountBackupTest {

    /* ---------------- Discord PKCE ---------------- */

    @Test fun `S256 challenge matches the RFC 7636 example`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test fun `verifier is long, url-safe and random`() {
        val a = Pkce.verifier()
        assertTrue(a.length in 43..128)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(a, Pkce.verifier())
    }

    @Test fun `authorize url carries PKCE and the registered redirect, never a secret`() {
        val url = DiscordOAuth.authorizeUrl("1560515980344426606", "abc", "st")
        assertTrue(url.startsWith("https://discord.com/oauth2/authorize?"))
        assertTrue(url.contains("redirect_uri=discord-1560515980344426606%3A%2Fauthorize%2Fcallback"))
        assertTrue(url.contains("code_challenge=abc") && url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("scope=identify") && url.contains("state=st"))
        assertFalse(url.contains("secret"))
        assertEquals("discord-1560515980344426606:/authorize/callback", DiscordOAuth.redirectUri("1560515980344426606"))
    }

    @Test fun `discord avatar urls`() {
        assertEquals("https://cdn.discordapp.com/avatars/1/abc.png?size=256", DiscordOAuth.avatarUrl("1", "abc", "0"))
        assertEquals("https://cdn.discordapp.com/avatars/1/a_abc.png?size=256", DiscordOAuth.avatarUrl("1", "a_abc", "0"))
        // New usernames: (id >> 22) % 6
        val id = (5L shl 22).toString()
        assertEquals("https://cdn.discordapp.com/embed/avatars/5.png", DiscordOAuth.avatarUrl(id, null, "0"))
        assertEquals("https://cdn.discordapp.com/embed/avatars/2.png", DiscordOAuth.avatarUrl("9", null, "1337"))
    }

    /* ---------------- Backup chunks ---------------- */

    private fun snap(rows: Int, extra: String = "x", at: Long = 1000) = Snapshot(
        dbVersion = 4, appVersion = "t", createdAt = at,
        tables = (0 until rows).chunked(Snapshot.CHUNK_ROWS).mapIndexed { i, r ->
            TableChunk("set_log", i, listOf("id", "w", "note"), r.map { listOf(JsonPrimitive(it), JsonPrimitive(it * 2.5), if (it == rows - 1) JsonPrimitive(extra) else JsonNull) })
        } + TableChunk("user_profile", 0, listOf("id", "name"), listOf(listOf(JsonPrimitive(1), JsonPrimitive("آدم")))),
        settings = Snapshot.encodeSettings(mapOf("language" to "MIXED", "haptics" to true, "accent_argb" to 4294929152L, "w_volume" to 0.8f, "days" to setOf("SUNDAY"))),
    )

    @Test fun `encode then decode gives the same snapshot`() {
        val s = snap(1200)
        val e = BackupPlan.encode(s)
        val back = BackupPlan.decode(e.manifest, e.files)
        assertEquals(s.tables, back.tables)
        assertEquals(s.settings, back.settings)
        assertEquals(mapOf("language" to "MIXED", "haptics" to true, "accent_argb" to 4294929152L, "w_volume" to 0.8f, "days" to setOf("SUNDAY")), Snapshot.decodeSettings(back.settings))
        assertEquals(3 + 1 + 1, e.files.size) // 3 set_log parts, profile, settings
    }

    @Test fun `only the changed chunk gets a new name (incremental upload)`() {
        val a = BackupPlan.encode(snap(1200, "x", at = 1)).manifest
        val b = BackupPlan.encode(snap(1200, "y", at = 2)).manifest
        assertEquals(a.chunks.take(2), b.chunks.take(2))
        assertNotEquals(a.chunks[2], b.chunks[2])
        assertEquals(a.chunks[3], b.chunks[3])
        assertEquals(a.settings, b.settings)
        // Same data twice → same files → nothing new to upload.
        assertEquals(BackupPlan.refs(a), BackupPlan.refs(BackupPlan.encode(snap(1200, "x", at = 99)).manifest))
    }

    /* ---------------- 3 GB rule ---------------- */

    private val now = Instant.parse("2026-10-16T21:00:00Z")
    private fun m(name: String, daysAgo: Long, vararg chunks: String) =
        name to Manifest(createdAt = now.minus(daysAgo, ChronoUnit.DAYS).toEpochMilli(), dbVersion = 4, appVersion = "", chunks = chunks.toList(), settings = "c-s.gz")
    private fun f(name: String, size: Long) = DriveFile(name, name, size, Instant.EPOCH)

    @Test fun `under the cap nothing is deleted`() {
        val manifests = mapOf(m("m-1.json", 90, "c-a.gz"), m("m-2.json", 1, "c-b.gz"))
        val files = listOf(f("m-1.json", 1), f("m-2.json", 1), f("c-a.gz", 50), f("c-b.gz", 50), f("c-s.gz", 1))
        assertTrue(BackupPlan.prune(files, manifests, now, cap = 1000).isEmpty())
    }

    @Test fun `over the cap old backups go oldest first and the newest always stays`() {
        val manifests = mapOf(m("m-1.json", 90, "c-a.gz"), m("m-2.json", 60, "c-b.gz"), m("m-3.json", 40, "c-c.gz"), m("m-4.json", 1, "c-c.gz", "c-d.gz"))
        val files = listOf(
            f("m-1.json", 1), f("m-2.json", 1), f("m-3.json", 1), f("m-4.json", 1), f("c-s.gz", 1),
            f("c-a.gz", 400), f("c-b.gz", 400), f("c-c.gz", 300), f("c-d.gz", 100), f("c-orphan.gz", 50),
        )
        val gone = BackupPlan.prune(files, manifests, now, cap = 900).map { it.name }.toSet()
        // orphan first, then m-1 (+ c-a); 1255 - 50 - 401 = 804 ≤ 900 → stop.
        assertEquals(setOf("c-orphan.gz", "m-1.json", "c-a.gz"), gone)
        // Way over: everything older than a month goes, chunks the newest needs (c-c) stay.
        val all = BackupPlan.prune(files, manifests, now, cap = 10).map { it.name }.toSet()
        assertEquals(setOf("c-orphan.gz", "m-1.json", "c-a.gz", "m-2.json", "c-b.gz", "m-3.json"), all)
    }

    @Test fun `backups younger than a month are never pruned`() {
        val manifests = mapOf(m("m-1.json", 20, "c-a.gz"), m("m-2.json", 1, "c-b.gz"))
        val files = listOf(f("m-1.json", 1), f("m-2.json", 1), f("c-a.gz", 500), f("c-b.gz", 500), f("c-s.gz", 1))
        assertTrue(BackupPlan.prune(files, manifests, now, cap = 10).isEmpty())
    }

    @Test fun `nightly backup waits until the chosen time`() {
        assertEquals(30, BackupWorker.delayUntil(LocalDateTime.of(2026, 10, 16, 23, 0), LocalTime.of(23, 30)))
        assertEquals(23 * 60 + 30, BackupWorker.delayUntil(LocalDateTime.of(2026, 10, 16, 0, 0), LocalTime.of(23, 30)))
        assertEquals(24 * 60, BackupWorker.delayUntil(LocalDateTime.of(2026, 10, 16, 23, 30), LocalTime.of(23, 30)))
    }

    /* ---------------- Export / customisation ---------------- */

    @Test fun `csv export has one file per table and escapes commas and quotes`() {
        val s = snap(3, extra = "خبز, \"بلدي\"")
        val out = ByteArrayOutputStream().also { DataTransfer.writeCsvZip(s, it) }
        val entries = mutableMapOf<String, String>()
        ZipInputStream(out.toByteArray().inputStream()).use { z ->
            var e = z.nextEntry
            while (e != null) { entries[e.name] = String(z.readBytes()).removePrefix("﻿"); e = z.nextEntry }
        }
        assertEquals(setOf("set_log.csv", "user_profile.csv", "settings.csv"), entries.keys)
        val lines = entries.getValue("set_log.csv").trim().lines()
        assertEquals("id,w,note", lines[0])
        assertEquals("2,5.0,\"خبز, \"\"بلدي\"\"\"", lines[3])
    }

    @Test fun `default settings are the Calisthenics preset`() {
        val d = AppSettings()
        assertTrue(ThemePreset.All.first().matches(d.themeMode, d.accentArgb, d.secondaryArgb))
        assertEquals(1, ThemePreset.All.count { it.matches(d.themeMode, d.accentArgb, d.secondaryArgb) })
        assertEquals(ThemePreset.All.size, ThemePreset.All.map { it.id }.toSet().size)
    }
}
