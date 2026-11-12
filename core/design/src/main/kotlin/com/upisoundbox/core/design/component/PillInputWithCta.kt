package com.upisoundbox.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxColors
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun PillInputWithCta(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    val colors = SoundboxTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = colors.bubbleGrey,
                shape = CircleShape
            )
            .padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SoundboxTheme.typography.bodyLarge,
                    color = colors.textMuted
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = SoundboxTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary
                ),
                cursorBrush = SolidColor(colors.accentOrange),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = onCtaClick,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier
                .height(44.dp)
                .padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (colors.isDark) Color.White else SoundboxColors.Ink,
                contentColor = if (colors.isDark) SoundboxColors.Ink else Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(
                text = ctaText,
                style = SoundboxTheme.typography.labelLarge
            )
        }
    }
}
