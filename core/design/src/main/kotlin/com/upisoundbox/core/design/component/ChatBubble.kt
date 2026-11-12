package com.upisoundbox.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxShapes
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun ChatBubble(
    text: String,
    timestamp: String,
    modifier: Modifier = Modifier,
    appChip: (@Composable () -> Unit)? = null
) {
    val colors = SoundboxTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(SoundboxShapes.ChatBubble)
                .background(colors.bubbleGrey)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp
                )
        ) {
            if (appChip != null) {
                appChip()
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = text,
                style = SoundboxTheme.typography.bodyLarge,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = timestamp,
                    style = SoundboxTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
