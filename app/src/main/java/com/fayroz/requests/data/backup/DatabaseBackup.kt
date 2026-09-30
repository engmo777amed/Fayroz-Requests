package com.fayroz.requests.data.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.fayroz.requests.data.db.FayrozDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

object DatabaseBackup {
    private const val FORMAT_VERSION = 1

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

    fun write(database: FayrozDatabase, output: OutputStream) {
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
        output.writer(Charsets.UTF_8).use { it.write(root.toString()) }
    }

    suspend fun restore(database: FayrozDatabase, input: InputStream) {
        val root = input.reader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        require(root.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "صيغة النسخة الاحتياطية غير مدعومة."
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
        }
        database.invalidationTracker.refreshAsync()
    }
}
