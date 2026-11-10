package com.hyperathlete.app.core.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Export (JSON / CSV), import and clear — all on the phone, through files the user picks. */
@Singleton
class DataTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snapshots: SnapshotStore,
) {
    fun suggestedName(ext: String, today: LocalDate = LocalDate.now()) = "HyperAthlete-$today.$ext"

    /** Full snapshot — the same thing "Import" reads back. */
    suspend fun exportJson(uri: Uri) {
        val s = snapshots.take()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Snapshot.json.encodeToString(Snapshot.serializer(), s).toByteArray()) }
        }
    }

    /** One CSV per table inside a .zip (opens in Excel / Google Sheets). */
    suspend fun exportCsv(uri: Uri) {
        val s = snapshots.take()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { writeCsvZip(s, it) }
        }
    }

    /** Reads an exported JSON and replaces everything with it. */
    suspend fun importJson(uri: Uri) {
        val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { String(it.readBytes()) } }
        val s = runCatching { Snapshot.json.decodeFromString(Snapshot.serializer(), text) }
            .getOrElse { throw SnapshotException(SnapshotException.Reason.NOT_OURS) }
        snapshots.apply(s)
    }

    suspend fun clearAll() = snapshots.clearAll()

    companion object {
        fun writeCsvZip(s: Snapshot, out: OutputStream) {
            ZipOutputStream(out).use { zip ->
                s.tables.groupBy { it.table }.forEach { (table, parts) ->
                    zip.putNextEntry(ZipEntry("$table.csv"))
                    // BOM so Excel reads the Arabic food names as UTF-8.
                    zip.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    val cols = parts.first().columns
                    zip.write((cols.joinToString(",") { csv(it) } + "\r\n").toByteArray())
                    parts.sortedBy { it.part }.forEach { p ->
                        p.rows.forEach { row -> zip.write((row.joinToString(",") { csv(cellText(it)) } + "\r\n").toByteArray()) }
                    }
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("settings.csv"))
                zip.write("key,value\r\n".toByteArray())
                s.settings.toSortedMap().forEach { (k, v) -> zip.write((csv(k) + "," + csv(cellText(v.v)) + "\r\n").toByteArray()) }
                zip.closeEntry()
            }
        }

        private fun cellText(e: JsonElement): String = when (e) {
            is JsonNull -> ""
            is JsonPrimitive -> e.content
            is JsonObject -> "(binary)"
            else -> e.toString()
        }

        fun csv(v: String): String = if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
}
