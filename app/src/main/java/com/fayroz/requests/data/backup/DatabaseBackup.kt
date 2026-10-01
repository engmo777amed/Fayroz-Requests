package com.fayroz.requests.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.fayroz.requests.data.db.FayrozDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object DatabaseBackup {
    private const val FORMAT_VERSION = 2
    private const val MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024

    private val insertOrder = listOf(
        "projects",
        "categories",
        "category_brands",
        "items",
        "suppliers",
        "request_sheets",
        "request_lines",
        "pricing_copies",
        "pricing_copy_lines",
        "price_lists",
        "supplier_discount_rules",
        "supplier_prices",
    )

    fun write(context: Context, database: FayrozDatabase, output: OutputStream) {
        val db = database.openHelper.readableDatabase
        val root = JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("createdAt", System.currentTimeMillis())
        val tables = JSONObject()

        insertOrder.forEach { table ->
            val rows = JSONArray()
            db.query("SELECT * FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        val name = cursor.getColumnName(i)
                        val value: Any? = when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                            Cursor.FIELD_TYPE_BLOB -> JSONObject().put(
                                "__blob",
                                Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP),
                            )
                            else -> JSONObject.NULL
                        }
                        row.put(name, value)
                    }
                    rows.put(row)
                }
            }
            tables.put(table, rows)
        }

        root.put("tables", tables)
        root.put("attachments", collectAttachments(context, db))
        output.writer(Charsets.UTF_8).use { it.write(root.toString()) }
    }

    suspend fun restore(context: Context, database: FayrozDatabase, input: InputStream) {
        val root = input.reader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        val version = root.optInt("formatVersion", -1)
        require(version in 1..FORMAT_VERSION) {
            "صيغة النسخة الاحتياطية غير مدعومة."
        }

        val restoredAttachments = if (version >= 2) {
            restoreAttachments(context, root.optJSONArray("attachments") ?: JSONArray())
        } else {
            emptyMap()
        }

        val tables = root.getJSONObject("tables")
        val db = database.openHelper.writableDatabase

        database.withTransaction {
            insertOrder.asReversed().forEach { table ->
                db.execSQL("DELETE FROM $table")
            }

            insertOrder.forEach { table ->
                val rows = tables.optJSONArray(table) ?: JSONArray()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()
                    row.keys().forEach { key ->
                        val value = row.get(key)
                        when (value) {
                            JSONObject.NULL -> values.putNull(key)
                            is Int -> values.put(key, value)
                            is Long -> values.put(key, value)
                            is Double -> values.put(key, value)
                            is String -> values.put(key, value)
                            is Boolean -> values.put(key, if (value) 1 else 0)
                            is JSONObject -> {
                                if (value.has("__blob")) {
                                    values.put(
                                        key,
                                        Base64.decode(value.getString("__blob"), Base64.NO_WRAP),
                                    )
                                }
                            }
                            else -> values.put(key, value.toString())
                        }
                    }
                    db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }

            restoredAttachments.forEach { (sheetId, path) ->
                val values = ContentValues().apply { put("attachmentUri", path) }
                db.update("request_sheets", SQLiteDatabase.CONFLICT_ABORT, values, "id = ?", arrayOf(sheetId.toString()))
            }
        }
        database.invalidationTracker.refreshAsync()
    }

    private fun collectAttachments(context: Context, db: androidx.sqlite.db.SupportSQLiteDatabase): JSONArray {
        val result = JSONArray()
        db.query(
            "SELECT id, attachmentUri FROM request_sheets WHERE attachmentUri IS NOT NULL AND TRIM(attachmentUri) != ''"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndex("id")
            val uriIndex = cursor.getColumnIndex("attachmentUri")
            while (cursor.moveToNext()) {
                val sheetId = cursor.getLong(idIndex)
                val reference = cursor.getString(uriIndex).orEmpty()
                val bytes = readAttachment(context, reference) ?: continue
                result.put(
                    JSONObject()
                        .put("sheetId", sheetId)
                        .put("extension", attachmentExtension(context, reference))
                        .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                )
            }
        }
        return result
    }

    private fun readAttachment(context: Context, reference: String): ByteArray? = runCatching {
        val input = if (reference.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(reference))
        } else {
            File(reference).takeIf { it.isFile }?.inputStream()
        } ?: return@runCatching null

        input.use { source ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = source.read(buffer)
                if (count <= 0) break
                total += count
                require(total <= MAX_ATTACHMENT_BYTES) {
                    "صورة كشف أكبر من الحد المسموح للنسخة الاحتياطية."
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }.getOrNull()

    private fun attachmentExtension(context: Context, reference: String): String {
        if (reference.startsWith("content://")) {
            return when (context.contentResolver.getType(Uri.parse(reference))) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
        }
        return File(reference).extension.lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
    }

    private fun restoreAttachments(context: Context, attachments: JSONArray): Map<Long, String> {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val restored = mutableMapOf<Long, String>()
        for (i in 0 until attachments.length()) {
            val entry = attachments.optJSONObject(i) ?: continue
            val sheetId = entry.optLong("sheetId", -1L)
            val encoded = entry.optString("data")
            if (sheetId <= 0L || encoded.isBlank()) continue

            val extension = entry.optString("extension", "jpg")
                .lowercase()
                .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
                ?: "jpg"
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size <= MAX_ATTACHMENT_BYTES) {
                "صورة كشف أكبر من الحد المسموح للاستعادة."
            }
            val file = File(dir, "restored_request_${sheetId}_${System.currentTimeMillis()}.$extension")
            file.writeBytes(bytes)
            restored[sheetId] = file.absolutePath
        }
        return restored
    }
}
