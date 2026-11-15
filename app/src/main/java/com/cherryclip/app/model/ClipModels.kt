package com.cherryclip.app.model

import java.util.UUID

enum class ClipType(val label: String) {
    TEXT("文本"),
    URL("链接"),
    CODE("代码"),
    OTP("验证码"),
    COLOR("颜色"),
    EMAIL("邮箱"),
    PHONE("电话"),
    IMAGE("图片")
}

enum class ClipFilter(val label: String, val type: ClipType? = null) {
    ALL("全部"),
    PINNED("置顶"),
    URL("链接", ClipType.URL),
    CODE("代码", ClipType.CODE),
    OTP("验证码", ClipType.OTP),
    COLOR("颜色", ClipType.COLOR),
    IMAGE("图片", ClipType.IMAGE)
}

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

enum class MainTab(val label: String) {
    HISTORY("剪贴板"),
    SNIPPETS("常用片段"),
    SETTINGS("设置")
}

data class ClipItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val title: String = "",
    val type: ClipType = ClipType.TEXT,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isSensitive: Boolean = false,
    val imagePath: String? = null,
    val metadata: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val copyCount: Int = 1
)

data class ClipSnippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "常用",
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoCaptureOnFocus: Boolean = true,
    val foregroundServiceEnabled: Boolean = false,
    val deduplicateClips: Boolean = true,
    val ignoreSensitiveClips: Boolean = false,
    val maxHistoryCount: Int = 500,
    val retentionDays: Int = 0, // 0 = never auto-delete
    val pinLockHash: String? = null,
    val hapticFeedback: Boolean = true
)

data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val clips: List<ClipItem> = emptyList(),
    val snippets: List<ClipSnippet> = emptyList()
)
