package com.cherryclip.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as CherryClipApp).repository

        when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!selectedText.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.addTextClip(selectedText)
                    }
                    Toast.makeText(this, "已存入 CherryClip", Toast.LENGTH_SHORT).show()
                }
            }

            Intent.ACTION_SEND -> {
                val mime = intent.type.orEmpty()
                if (mime.startsWith("image/")) {
                    val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (uri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.addImageClip(uri)
                        }
                        Toast.makeText(this, "图片已保存到 CherryClip", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!text.isNullOrBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.addTextClip(text)
                        }
                        Toast.makeText(this, "已保存到 CherryClip", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        finish()
    }
}
