package com.cherryclip.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.model.ClipSnippet
import com.cherryclip.app.model.ClipType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ClipDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val gson = Gson()
    private val tagsType = object : TypeToken<List<String>>() {}.type

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CLIPS (
                id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                type TEXT NOT NULL,
                tags_json TEXT NOT NULL DEFAULT '[]',
                is_pinned INTEGER NOT NULL DEFAULT 0,
                is_sensitive INTEGER NOT NULL DEFAULT 0,
                image_path TEXT,
                metadata TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                copy_count INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clips_sort ON $TABLE_CLIPS(is_pinned DESC, updated_at DESC)")
        db.execSQL("CREATE INDEX idx_clips_type ON $TABLE_CLIPS(type)")

        db.execSQL(
            """
            CREATE TABLE $TABLE_SNIPPETS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT '常用',
                is_favorite INTEGER NOT NULL DEFAULT 0,
                usage_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_snippets_sort ON $TABLE_SNIPPETS(is_favorite DESC, updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations
    }

    fun getAllClips(): List<ClipItem> {
        val result = mutableListOf<ClipItem>()
        readableDatabase.query(
            TABLE_CLIPS,
            null,
            null,
            null,
            null,
            null,
            "is_pinned DESC, updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val typeStr = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val tagsJson = cursor.getString(cursor.getColumnIndexOrThrow("tags_json"))
                val tags: List<String> = try {
                    gson.fromJson(tagsJson, tagsType) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                result.add(
                    ClipItem(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        type = runCatching { ClipType.valueOf(typeStr) }.getOrDefault(ClipType.TEXT),
                        tags = tags,
                        isPinned = cursor.getInt(cursor.getColumnIndexOrThrow("is_pinned")) == 1,
                        isSensitive = cursor.getInt(cursor.getColumnIndexOrThrow("is_sensitive")) == 1,
                        imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")),
                        metadata = cursor.getString(cursor.getColumnIndexOrThrow("metadata")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                        copyCount = cursor.getInt(cursor.getColumnIndexOrThrow("copy_count"))
                    )
                )
            }
        }
        return result
    }

    fun upsertClip(item: ClipItem) {
        val values = ContentValues().apply {
            put("id", item.id)
            put("content", item.content)
            put("title", item.title)
            put("type", item.type.name)
            put("tags_json", gson.toJson(item.tags))
            put("is_pinned", if (item.isPinned) 1 else 0)
            put("is_sensitive", if (item.isSensitive) 1 else 0)
            put("image_path", item.imagePath)
            put("metadata", item.metadata)
            put("created_at", item.createdAt)
            put("updated_at", item.updatedAt)
            put("copy_count", item.copyCount)
        }
        writableDatabase.insertWithOnConflict(TABLE_CLIPS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteClipsByIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.chunked(200).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                db.delete(TABLE_CLIPS, "id IN ($placeholders)", chunk.toTypedArray())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearUnpinnedClips() {
        writableDatabase.delete(TABLE_CLIPS, "is_pinned = 0", null)
    }

    fun getAllSnippets(): List<ClipSnippet> {
        val result = mutableListOf<ClipSnippet>()
        readableDatabase.query(
            TABLE_SNIPPETS,
            null,
            null,
            null,
            null,
            null,
            "is_favorite DESC, usage_count DESC, updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ClipSnippet(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
                        usageCount = cursor.getInt(cursor.getColumnIndexOrThrow("usage_count")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                    )
                )
            }
        }
        return result
    }

    fun upsertSnippet(snippet: ClipSnippet) {
        val values = ContentValues().apply {
            put("id", snippet.id)
            put("title", snippet.title)
            put("content", snippet.content)
            put("category", snippet.category)
            put("is_favorite", if (snippet.isFavorite) 1 else 0)
            put("usage_count", snippet.usageCount)
            put("created_at", snippet.createdAt)
            put("updated_at", snippet.updatedAt)
        }
        writableDatabase.insertWithOnConflict(TABLE_SNIPPETS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteSnippet(id: String) {
        writableDatabase.delete(TABLE_SNIPPETS, "id = ?", arrayOf(id))
    }

    companion object {
        private const val DB_NAME = "cherry_clip.db"
        private const val DB_VERSION = 1
        private const val TABLE_CLIPS = "clips"
        private const val TABLE_SNIPPETS = "snippets"
    }
}
