package com.cherryclip.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.cherryclip.app.model.AppSettings
import com.cherryclip.app.model.BackupPayload
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.model.ClipSnippet
import com.cherryclip.app.model.ClipType
import com.cherryclip.app.model.ContentClassifier
import com.cherryclip.app.model.ThemeMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class ClipRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = ClipDatabaseHelper(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _clips = MutableStateFlow<List<ClipItem>>(emptyList())
    val clips: StateFlow<List<ClipItem>> = _clips.asStateFlow()

    private val _snippets = MutableStateFlow<List<ClipSnippet>>(emptyList())
    val snippets: StateFlow<List<ClipSnippet>> = _snippets.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isLocked = MutableStateFlow(_settings.value.pinLockHash != null)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val imageDir: File
        get() = File(appContext.filesDir, "clip_images").apply { if (!exists()) mkdirs() }

    init {
        scope.launch {
            seedFirstRunIfNeeded()
            reloadFromDb()
            applyRetentionPolicyLocked()
        }
    }

    private fun reloadFromDb() {
        _clips.value = db.getAllClips()
        _snippets.value = db.getAllSnippets()
    }

    private fun seedFirstRunIfNeeded() {
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return
        val now = System.currentTimeMillis()
        val defaultSnippets = listOf(
            ClipSnippet(
                title = "今日日期",
                content = "{{date}}",
                category = "快捷变量",
                isFavorite = true,
                createdAt = now,
                updatedAt = now
            ),
            ClipSnippet(
                title = "当前时间",
                content = "{{datetime}}",
                category = "快捷变量",
                isFavorite = true,
                createdAt = now - 1000,
                updatedAt = now - 1000
            ),
            ClipSnippet(
                title = "随机 UUID",
                content = "{{uuid}}",
                category = "开发工具",
                isFavorite = false,
                createdAt = now - 2000,
                updatedAt = now - 2000
            ),
            ClipSnippet(
                title = "引用剪贴板整理格式",
                content = "【摘录 {{date}}】\n{{clipboard}}",
                category = "模板",
                isFavorite = false,
                createdAt = now - 3000,
                updatedAt = now - 3000
            )
        )
        defaultSnippets.forEach { db.upsertSnippet(it) }

        val starterClips = listOf(
            ClipItem(
                content = "欢迎使用 CherryClip 樱桃剪贴板！点击任意卡片即可一键复制，长按可进入多选合并模式。",
                title = "快速上手指南",
                type = ClipType.TEXT,
                tags = listOf("指南"),
                isPinned = true,
                metadata = "43 字符",
                createdAt = now,
                updatedAt = now
            ),
            ClipItem(
                content = "#FF5757",
                title = "Cherry Brand Coral",
                type = ClipType.COLOR,
                tags = listOf("配色"),
                metadata = "#FF5757",
                createdAt = now - 1000,
                updatedAt = now - 1000
            ),
            ClipItem(
                content = """{"app":"CherryClip","style":"Cherry Studio","features":["自动识别","常用片段","文本工具箱","二维码"]}""",
                title = "JSON 智能识别示例",
                type = ClipType.CODE,
                tags = listOf("JSON"),
                metadata = "JSON",
                createdAt = now - 2000,
                updatedAt = now - 2000
            ),
            ClipItem(
                content = "https://github.com/CherryHQ/cherry-studio-app",
                title = "Cherry Studio Mobile",
                type = ClipType.URL,
                tags = listOf("开源"),
                metadata = "github.com",
                createdAt = now - 3000,
                updatedAt = now - 3000
            )
        )
        starterClips.forEach { db.upsertClip(it) }
        prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    suspend fun addTextClip(
        rawText: String,
        title: String = "",
        tags: List<String> = emptyList(),
        fromAutoCapture: Boolean = false
    ): ClipItem? {
        val cleaned = rawText
        if (cleaned.isBlank()) return null

        val currentSettings = _settings.value
        val classification = ContentClassifier.classify(cleaned)
        if (fromAutoCapture && currentSettings.ignoreSensitiveClips && classification.isSensitive) {
            return null
        }

        return writeMutex.withLock {
            val now = System.currentTimeMillis()
            val existing = if (currentSettings.deduplicateClips) {
                db.getAllClips().firstOrNull { it.imagePath == null && it.content == cleaned }
            } else null

            val item = if (existing != null) {
                existing.copy(
                    updatedAt = now,
                    copyCount = existing.copyCount + 1,
                    tags = if (tags.isNotEmpty()) (existing.tags + tags).distinct() else existing.tags
                )
            } else {
                ClipItem(
                    content = cleaned,
                    title = title,
                    type = classification.type,
                    tags = tags,
                    isSensitive = classification.isSensitive,
                    metadata = classification.metadata,
                    createdAt = now,
                    updatedAt = now
                )
            }
            db.upsertClip(item)
            applyRetentionPolicyLocked()
            reloadFromDb()
            item
        }
    }

    suspend fun addImageClip(uri: Uri, caption: String = ""): ClipItem? {
        return writeMutex.withLock {
            val savedFile = copyUriToImageFile(uri) ?: return@withLock null
            val now = System.currentTimeMillis()
            val item = ClipItem(
                content = caption.ifBlank { "图片 (${savedFile.name})" },
                title = caption,
                type = ClipType.IMAGE,
                imagePath = savedFile.absolutePath,
                metadata = "${(savedFile.length() / 1024).coerceAtLeast(1)} KB",
                createdAt = now,
                updatedAt = now
            )
            db.upsertClip(item)
            applyRetentionPolicyLocked()
            reloadFromDb()
            item
        }
    }

    suspend fun addBitmapClip(bitmap: Bitmap, caption: String = ""): ClipItem? {
        return writeMutex.withLock {
            val file = File(imageDir, "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
            val ok = runCatching {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            }.isSuccess
            if (!ok) return@withLock null
            val now = System.currentTimeMillis()
            val item = ClipItem(
                content = caption.ifBlank { "剪贴板图片" },
                title = caption,
                type = ClipType.IMAGE,
                imagePath = file.absolutePath,
                metadata = "${bitmap.width}×${bitmap.height}",
                createdAt = now,
                updatedAt = now
            )
            db.upsertClip(item)
            applyRetentionPolicyLocked()
            reloadFromDb()
            item
        }
    }

    private fun copyUriToImageFile(uri: Uri): File? {
        return runCatching {
            val file = File(imageDir, "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (file.length() == 0L) {
                file.delete()
                return null
            }
            file
        }.getOrNull()
    }

    suspend fun updateClip(updated: ClipItem) {
        writeMutex.withLock {
            val classification = if (updated.imagePath != null) {
                ContentClassifier.classify(updated.content, hasImage = true)
            } else {
                ContentClassifier.classify(updated.content)
            }
            val finalItem = updated.copy(
                type = classification.type,
                metadata = if (updated.imagePath != null) updated.metadata else classification.metadata,
                isSensitive = classification.isSensitive,
                updatedAt = System.currentTimeMillis()
            )
            db.upsertClip(finalItem)
            reloadFromDb()
        }
    }

    suspend fun togglePin(id: String) {
        writeMutex.withLock {
            val target = db.getAllClips().firstOrNull { it.id == id } ?: return@withLock
            db.upsertClip(target.copy(isPinned = !target.isPinned))
            reloadFromDb()
        }
    }

    suspend fun pinClips(ids: Collection<String>, pinned: Boolean) {
        writeMutex.withLock {
            val idSet = ids.toSet()
            db.getAllClips().filter { it.id in idSet }.forEach {
                db.upsertClip(it.copy(isPinned = pinned))
            }
            reloadFromDb()
        }
    }

    suspend fun addTagToClips(ids: Collection<String>, tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return
        writeMutex.withLock {
            val idSet = ids.toSet()
            db.getAllClips().filter { it.id in idSet }.forEach {
                db.upsertClip(it.copy(tags = (it.tags + cleanTag).distinct()))
            }
            reloadFromDb()
        }
    }

    suspend fun recordCopyUsage(id: String) {
        writeMutex.withLock {
            val target = db.getAllClips().firstOrNull { it.id == id } ?: return@withLock
            db.upsertClip(target.copy(copyCount = target.copyCount + 1))
            reloadFromDb()
        }
    }

    suspend fun deleteClips(ids: Collection<String>) {
        writeMutex.withLock {
            val idSet = ids.toSet()
            db.getAllClips().filter { it.id in idSet }.forEach { item ->
                item.imagePath?.let { path -> runCatching { File(path).delete() } }
            }
            db.deleteClipsByIds(ids)
            reloadFromDb()
        }
    }

    suspend fun clearUnpinned() {
        writeMutex.withLock {
            db.getAllClips().filter { !it.isPinned }.forEach { item ->
                item.imagePath?.let { path -> runCatching { File(path).delete() } }
            }
            db.clearUnpinnedClips()
            reloadFromDb()
        }
    }

    suspend fun upsertSnippet(snippet: ClipSnippet) {
        writeMutex.withLock {
            db.upsertSnippet(snippet.copy(updatedAt = System.currentTimeMillis()))
            reloadFromDb()
        }
    }

    suspend fun incrementSnippetUsage(id: String) {
        writeMutex.withLock {
            val target = db.getAllSnippets().firstOrNull { it.id == id } ?: return@withLock
            db.upsertSnippet(target.copy(usageCount = target.usageCount + 1, updatedAt = System.currentTimeMillis()))
            reloadFromDb()
        }
    }

    suspend fun toggleSnippetFavorite(id: String) {
        writeMutex.withLock {
            val target = db.getAllSnippets().firstOrNull { it.id == id } ?: return@withLock
            db.upsertSnippet(target.copy(isFavorite = !target.isFavorite))
            reloadFromDb()
        }
    }

    suspend fun deleteSnippet(id: String) {
        writeMutex.withLock {
            db.deleteSnippet(id)
            reloadFromDb()
        }
    }

    fun exportBackupJson(): String {
        val payload = BackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            clips = _clips.value,
            snippets = _snippets.value
        )
        return gson.toJson(payload)
    }

    suspend fun importBackupJson(json: String): Result<Pair<Int, Int>> = runCatching {
        val payload = gson.fromJson(json, BackupPayload::class.java)
            ?: throw IllegalArgumentException("备份文件格式无效")
        writeMutex.withLock {
            payload.clips.forEach { clip ->
                // Clear stale local image path if file does not exist on this device
                val validPath = clip.imagePath?.takeIf { File(it).exists() }
                db.upsertClip(clip.copy(imagePath = validPath))
            }
            payload.snippets.forEach { snippet ->
                db.upsertSnippet(snippet)
            }
            reloadFromDb()
        }
        payload.clips.size to payload.snippets.size
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        saveSettings(next)
        if (next.pinLockHash == null) {
            _isLocked.value = false
        }
        scope.launch {
            writeMutex.withLock {
                applyRetentionPolicyLocked()
                reloadFromDb()
            }
        }
    }

    fun setPinLock(pin: String?) {
        val hash = pin?.takeIf { it.isNotBlank() }?.let { sha256(it) }
        updateSettings { it.copy(pinLockHash = hash) }
        _isLocked.value = false
    }

    fun verifyPinAndUnlock(pin: String): Boolean {
        val expected = _settings.value.pinLockHash ?: return true
        val matched = sha256(pin) == expected
        if (matched) {
            _isLocked.value = false
        }
        return matched
    }

    fun lockNowIfEnabled() {
        if (_settings.value.pinLockHash != null) {
            _isLocked.value = true
        }
    }

    private fun applyRetentionPolicyLocked() {
        val s = _settings.value
        val all = db.getAllClips()
        val unpinned = all.filter { !it.isPinned }
        val toDelete = mutableSetOf<String>()

        if (s.retentionDays > 0) {
            val cutoff = System.currentTimeMillis() - s.retentionDays * 24L * 3600L * 1000L
            unpinned.filter { it.updatedAt < cutoff }.forEach { toDelete.add(it.id) }
        }

        val remainingUnpinned = unpinned.filter { it.id !in toDelete }
        if (s.maxHistoryCount > 0 && remainingUnpinned.size > s.maxHistoryCount) {
            remainingUnpinned.drop(s.maxHistoryCount).forEach { toDelete.add(it.id) }
        }

        if (toDelete.isNotEmpty()) {
            all.filter { it.id in toDelete }.forEach { item ->
                item.imagePath?.let { path -> runCatching { File(path).delete() } }
            }
            db.deleteClipsByIds(toDelete)
        }
    }

    private fun loadSettings(): AppSettings {
        val themeName = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM),
            autoCaptureOnFocus = prefs.getBoolean(KEY_AUTO_FOCUS, true),
            foregroundServiceEnabled = prefs.getBoolean(KEY_FGS_ENABLED, false),
            deduplicateClips = prefs.getBoolean(KEY_DEDUP, true),
            ignoreSensitiveClips = prefs.getBoolean(KEY_IGNORE_SENSITIVE, false),
            maxHistoryCount = prefs.getInt(KEY_MAX_HISTORY, 500),
            retentionDays = prefs.getInt(KEY_RETENTION_DAYS, 0),
            pinLockHash = prefs.getString(KEY_PIN_HASH, null),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true)
        )
    }

    private fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.themeMode.name)
            .putBoolean(KEY_AUTO_FOCUS, s.autoCaptureOnFocus)
            .putBoolean(KEY_FGS_ENABLED, s.foregroundServiceEnabled)
            .putBoolean(KEY_DEDUP, s.deduplicateClips)
            .putBoolean(KEY_IGNORE_SENSITIVE, s.ignoreSensitiveClips)
            .putInt(KEY_MAX_HISTORY, s.maxHistoryCount)
            .putInt(KEY_RETENTION_DAYS, s.retentionDays)
            .putString(KEY_PIN_HASH, s.pinLockHash)
            .putBoolean(KEY_HAPTIC, s.hapticFeedback)
            .apply()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "cherry_clip_prefs"
        private const val KEY_INITIALIZED = "initialized_v1"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_AUTO_FOCUS = "auto_capture_focus"
        private const val KEY_FGS_ENABLED = "fgs_enabled"
        private const val KEY_DEDUP = "dedup_clips"
        private const val KEY_IGNORE_SENSITIVE = "ignore_sensitive"
        private const val KEY_MAX_HISTORY = "max_history"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_HAPTIC = "haptic_feedback"
    }
}
