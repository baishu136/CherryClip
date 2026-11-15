package com.cherryclip.app.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.FileProvider
import java.io.File

object ClipboardHelper {

    sealed class ClipboardReadResult {
        data class Text(val value: String) : ClipboardReadResult()
        data class ImageUri(val uri: Uri) : ClipboardReadResult()
        data object Empty : ClipboardReadResult()
    }

    fun readCurrentClipboard(context: Context): ClipboardReadResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return ClipboardReadResult.Empty
        if (!cm.hasPrimaryClip()) return ClipboardReadResult.Empty
        val clip = cm.primaryClip ?: return ClipboardReadResult.Empty
        if (clip.itemCount == 0) return ClipboardReadResult.Empty

        val item = clip.getItemAt(0)
        val mimeDesc = clip.description
        val uri = item.uri
        if (uri != null && mimeDesc != null && (0 until mimeDesc.mimeTypeCount).any {
                mimeDesc.getMimeType(it).startsWith("image/")
            }) {
            return ClipboardReadResult.ImageUri(uri)
        }

        val text = item.coerceToText(context)?.toString()
        if (!text.isNullOrBlank()) {
            return ClipboardReadResult.Text(text)
        }
        return ClipboardReadResult.Empty
    }

    fun copyText(
        context: Context,
        text: String,
        label: String = "CherryClip",
        isSensitive: Boolean = false
    ) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
    }

    fun copyImage(context: Context, imagePath: String): Boolean {
        val file = File(imagePath)
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = ClipData.newUri(context.contentResolver, "CherryClip Image", uri)
        cm.setPrimaryClip(clip)
        return true
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "分享内容").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareImage(context: Context, imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享图片").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        val normalized = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
    }

    fun dialPhone(context: Context, phone: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
    }

    fun sendEmail(context: Context, email: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.trim()}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
    }
}
