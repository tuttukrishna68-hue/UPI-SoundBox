package com.upisoundbox.core.design.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = SoundboxTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = SoundboxTheme.typography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = SoundboxTheme.typography.titleSmall,
                color = colors.accentOrange,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(4.dp)
            )
        }
    }
}
