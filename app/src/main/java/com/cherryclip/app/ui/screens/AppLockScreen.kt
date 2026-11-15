package com.cherryclip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryclip.app.ui.components.CherryBrandMark
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme

@Composable
fun AppLockScreen(
    onVerifyPin: (String) -> Boolean
) {
    val palette = CherryTheme.palette
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CherryBrandMark(size = 56.dp)
        Spacer(Modifier.height(16.dp))
        Text("CherryClip 已锁定", style = MaterialTheme.typography.titleLarge, color = palette.foreground)
        Spacer(Modifier.height(6.dp))
        Text(
            text = errorMsg ?: "请输入 PIN 码解锁剪贴板历史",
            style = MaterialTheme.typography.bodyMedium,
            color = if (errorMsg != null) palette.destructive else palette.mutedForeground
        )

        Spacer(Modifier.height(24.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val dots = maxOf(4, pin.length)
            for (i in 0 until dots) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) palette.primary else palette.secondary)
                        .border(1.dp, if (filled) palette.primary else palette.borderStrong, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "OK")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(CherryDimens.RadiusXl))
                                .background(if (key == "OK") palette.primary else palette.card)
                                .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusXl))
                                .clickable {
                                    errorMsg = null
                                    when (key) {
                                        "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        "OK" -> {
                                            if (!onVerifyPin(pin)) {
                                                errorMsg = "PIN 码不正确，请重试"
                                                pin = ""
                                            }
                                        }
                                        else -> {
                                            if (pin.length < 8) {
                                                pin += key
                                                if (pin.length >= 4 && onVerifyPin(pin)) {
                                                    // Auto-unlocked on match
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "退格", tint = palette.foreground)
                                "OK" -> Icon(Icons.Rounded.Check, contentDescription = "解锁", tint = palette.primaryForeground)
                                else -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = palette.foreground)
                            }
                        }
                    }
                }
            }
        }
    }
}
