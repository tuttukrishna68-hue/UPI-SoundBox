package com.upisoundbox.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxTheme
import kotlin.math.abs

fun defaultAppColor(appName: String): Color {
    val lower = appName.lowercase()
    return when {
        "google" in lower || "gpay" in lower -> Color(0xFF4285F4)
        "phonepe" in lower -> Color(0xFF5F259F)
        "paytm" in lower -> Color(0xFF002970)
        "bhim" in lower -> Color(0xFF00796B)
        "cred" in lower -> Color(0xFF1B1B1B)
        "amazon" in lower -> Color(0xFFFF9900)
        else -> {
            val hash = appName.hashCode()
            val palette = listOf(
                Color(0xFF4A90E2),
                Color(0xFF7B1FA2),
                Color(0xFF00897B),
                Color(0xFFD81B60),
                Color(0xFFF4511E)
            )
            palette[abs(hash) % palette.size]
        }
    }
}

@Composable
fun AppChip(
    appName: String,
    modifier: Modifier = Modifier,
    dotColor: Color = defaultAppColor(appName)
) {
    val colors = SoundboxTheme.colors

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.bubbleGrey)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = appName,
            style = SoundboxTheme.typography.labelSmall,
            color = colors.textPrimary
        )
    }
}
