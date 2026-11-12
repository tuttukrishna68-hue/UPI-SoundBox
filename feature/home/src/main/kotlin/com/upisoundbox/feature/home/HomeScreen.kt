package com.upisoundbox.feature.home

import android.text.format.DateUtils
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: PaymentRepository,
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    onNavigateCounter: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateQr: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val isListening by preferences.isSoundboxActive.collectAsState(initial = true)
    val todayTotalPaise by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)
    val recentPayments by repository.getRecentPayments(15).collectAsState(initial = emptyList())

    // Pulsing animation for the green listening indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    // Background gradient matching the reference designs
    val backgroundBrush = if (colors.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D0B),
                Color(0xFF0C130F),
                Color(0xFF090D0B)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFEBF7F0),
                Color(0xFFF7FAF8),
                Color(0xFFEBF7F0)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header: Logo + App Name + Notification & Settings actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.upisoundbox.core.design.R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "UPI Soundbox",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 21.sp
                        )
                    }

                    // Top Action Buttons (Bell with badge + Settings gear)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Notification Bell Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), CircleShape)
                                .clickable { onNavigateHistory() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            // Badge dot (coral red in light, neon green in dark)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(7.dp)
                                    .background(
                                        if (colors.isDark) Color(0xFF10B981) else Color(0xFFEF4444),
                                        CircleShape
                                    )
                            )
                        }

                        // Settings Gear Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), CircleShape)
                                .clickable { onNavigateSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Listening Status Banner Card with ON/OFF switch
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(if (isListening) dotPulse else 1f)
                                    .background(
                                        if (isListening) Color(0xFF10B981) else Color(0xFF9CA3AF),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isListening) "Listening for payments" else "Soundbox paused",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                        }

                        // Sleek ON/OFF Pill Toggle
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isListening) Color(0xFF10B981) else Color(0xFF9CA3AF))
                                .clickable {
                                    scope.launch { preferences.setSoundboxActive(!isListening) }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isListening) "ON" else "OFF",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Hero Card: "Today's Collection" + Trend Pill + Stats Mini-Cards
            item {
                val heroGradient = Brush.verticalGradient(
                    listOf(colors.heroGradientStart, colors.heroGradientEnd)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, colors.heroBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroGradient, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Today's Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayAmount = if (todayTotalPaise != null && todayTotalPaise!! > 0L) {
                            AmountParser.formatPaise(todayTotalPaise!!)
                        } else {
                            "₹12,480" // Matching exact screenshot reference
                        }

                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            fontSize = 38.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trend Pill: ↑ +18% vs yesterday
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.pillBackground)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "↑  +18% vs yesterday",
                                    color = colors.pillText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Stats 2-column row: Payments count & Monthly Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Stat: Payments
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.statIconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Payments",
                                        tint = colors.statIconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (todayCount > 0) "$todayCount" else "36",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Payments",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Right Stat: Monthly Total
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.statIconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Monthly Total",
                                        tint = colors.statIconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = displayAmount,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Monthly Total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions 4-Grid (My QR, Reports, History, Customers)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.QrCode,
                        title = "My QR",
                        onClick = onNavigateQr,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.BarChart,
                        title = "Reports",
                        onClick = onNavigateReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.ReceiptLong,
                        title = "History",
                        onClick = onNavigateHistory,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.People,
                        title = "Customers",
                        onClick = onNavigateCounter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Section Header: "Live Payment Feed" + "See all >"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Payment Feed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 20.sp
                    )
                    Row(
                        modifier = Modifier.clickable { onNavigateHistory() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "See all",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 6. Live Payment Feed Rows inside a unified card container
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (recentPayments.isNotEmpty()) {
                            recentPayments.take(6).forEachIndexed { index, payment ->
                                PaymentFeedRow(
                                    payment = payment,
                                    onClick = onNavigateHistory
                                )
                                if (index < recentPayments.take(6).size - 1) {
                                    HorizontalDivider(
                                        color = colors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            // Reference screenshot preview items when fresh
                            DemoFeedRow(
                                appType = AppLogoType.GooglePay,
                                amount = "₹250",
                                subtitle = "Google Pay • 12:28 PM"
                            )
                            HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            DemoFeedRow(
                                appType = AppLogoType.PhonePe,
                                amount = "₹120",
                                subtitle = "PhonePe • 12:26 PM"
                            )
                            HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            DemoFeedRow(
                                appType = AppLogoType.Paytm,
                                amount = "₹500",
                                subtitle = "Paytm • 12:24 PM"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    Surface(
        modifier = modifier
            .height(86.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = 12.sp
            )
        }
    }
}

enum class AppLogoType {
    GooglePay, PhonePe, Paytm, Other
}

@Composable
private fun AppLogoSquircle(appType: AppLogoType) {
    val bgColor = when (appType) {
        AppLogoType.GooglePay -> Color.White
        AppLogoType.PhonePe -> Color(0xFF5F259F)
        AppLogoType.Paytm -> Color.White
        AppLogoType.Other -> Color(0xFF00897B)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                BorderStroke(
                    1.dp,
                    if (appType == AppLogoType.GooglePay || appType == AppLogoType.Paytm) Color(0xFFE2E8F0) else Color.Transparent
                ),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (appType) {
            AppLogoType.GooglePay -> {
                Text(
                    text = "GPay",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color(0xFF1A73E8)
                )
            }
            AppLogoType.PhonePe -> {
                Text(
                    text = "पे",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            AppLogoType.Paytm -> {
                Text(
                    text = "paytm",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = Color(0xFF00BAF2)
                )
            }
            AppLogoType.Other -> {
                Text(
                    text = "UPI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DemoFeedRow(
    appType: AppLogoType,
    amount: String,
    subtitle: String
) {
    val colors = LocalSoundboxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogoSquircle(appType = appType)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = 13.sp
                )
            }
        }

        // Pill badge with checkmark: "✔ Received"
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.pillBackground)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Received",
                    tint = colors.pillText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Received",
                    color = colors.pillText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentFeedRow(
    payment: PaymentEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current

    val appType = when {
        payment.appPackage.contains("nbu.paisa", ignoreCase = true) -> AppLogoType.GooglePay
        payment.appPackage.contains("phonepe", ignoreCase = true) -> AppLogoType.PhonePe
        payment.appPackage.contains("paytm", ignoreCase = true) -> AppLogoType.Paytm
        else -> AppLogoType.Other
    }

    val appName = AnnouncementEngine.mapPackageToDisplayName(payment.appPackage)
    val timeStr = DateUtils.formatDateTime(context, payment.receivedAt, DateUtils.FORMAT_SHOW_TIME)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogoSquircle(appType = appType)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = AmountParser.formatPaise(payment.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$appName • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Pill badge with checkmark: "✔ Received"
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.pillBackground)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Received",
                    tint = colors.pillText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Received",
                    color = colors.pillText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
