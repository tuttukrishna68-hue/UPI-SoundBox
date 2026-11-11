package com.upisoundbox.feature.home

import android.app.Activity
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.component.AppChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine

@Composable
fun CounterModeScreen(
    repository: PaymentRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val lastPayment by repository.getLastPayment().collectAsState(initial = null)
    val todayTotal by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)

    var keepAwake by remember { mutableStateOf(true) }

    // Keep screen awake while in counter mode
    DisposableEffect(keepAwake) {
        val window = (context as? Activity)?.window
        if (keepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10)) // Pure dark high-contrast mode
            .padding(24.dp)
            .clickable { onClose() }
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COUNTER MODE",
                    color = Color(0xFFF25C2E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Tap anywhere to exit",
                    color = Color(0xFF8A8A8F),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Center: Giant Amount readable from 2 meters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (lastPayment != null) {
                val p = lastPayment!!
                Text(
                    text = "LATEST PAYMENT",
                    color = Color(0xFF8A8A8F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = AmountParser.formatPaise(p.amountPaise),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = p.payer ?: "Customer",
                    color = Color(0xFFFCE3D8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppChip(
                    appName = AnnouncementEngine.mapPackageToDisplayName(p.appPackage)
                )

                val timeStr = DateUtils.getRelativeTimeSpanString(p.receivedAt)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$timeStr",
                    color = Color(0xFF8A8A8F),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "READY FOR PAYMENTS",
                    color = Color(0xFF8A8A8F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "₹0",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp)
                )
            }
        }

        // Bottom Bar: Today's Summary & Keep Awake toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today's Total: ${AmountParser.formatPaise(todayTotal ?: 0L)}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$todayCount payments received",
                    color = Color(0xFF8A8A8F),
                    fontSize = 13.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { keepAwake = !keepAwake }
            ) {
                Checkbox(
                    checked = keepAwake,
                    onCheckedChange = { keepAwake = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFF25C2E),
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    text = "Keep Screen Awake",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

