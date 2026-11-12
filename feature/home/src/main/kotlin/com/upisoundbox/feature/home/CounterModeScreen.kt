package com.upisoundbox.feature.home

import android.app.Activity
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.util.AppLogoProvider
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine

private val CounterEmerald = Color(0xFF34D399)
private val CounterEmeraldDeep = Color(0xFF0E2B1D)
private val CounterBackground = Color(0xFF060B08)
private val CounterSurface = Color(0xFF0D1711)
private val CounterMuted = Color(0xFF8B9D92)
private val CounterRed = Color(0xFFFF4D5D)

@Composable
fun CounterModeScreen(
    repository: PaymentRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lastPayment by repository.getLastPayment().collectAsState(initial = null)
    val todayTotal by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)

    // A merchant counter should stay awake for the whole session.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "counterPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "counterPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        CounterBackground,
                        Color(0xFF0A1710),
                        CounterBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient emerald light keeps the full-screen counter mode feeling like a
        // premium, purpose-built surface rather than a plain dialog.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(250.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CounterEmerald.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(290.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0C7C52).copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            CounterHeader(
                pulseScale = pulseScale,
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val payment = lastPayment
                if (payment != null) {
                    CounterPaymentHero(payment = payment)
                } else {
                    CounterIdleHero(pulseScale = pulseScale)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            CounterSummary(
                todayTotal = todayTotal ?: 0L,
                todayCount = todayCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CounterEmerald)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Listening automatically · screen stays awake",
                    color = CounterMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CounterHeader(
    pulseScale: Float,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(CounterRed)
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.3.sp
                    )
                    Text(
                        text = "COUNTER MODE",
                        color = CounterMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Listening for UPI payments",
                    color = CounterMuted,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CounterSurface.copy(alpha = 0.86f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close counter mode",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CounterPaymentHero(payment: PaymentEntity) {
    val logoRes = AppLogoProvider.getLogoForPackage(payment.appPackage)
    val appName = AnnouncementEngine.mapPackageToDisplayName(payment.appPackage)
    val timeAgo = DateUtils.getRelativeTimeSpanString(payment.receivedAt)

    CounterGlass(
        modifier = Modifier.fillMaxWidth(),
        accent = CounterEmerald
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LATEST PAYMENT",
                color = CounterEmerald,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CounterEmeraldDeep)
                    .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.48f)), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = appName,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = appName,
                color = CounterMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AmountParser.formatPaise(payment.amountPaise),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp,
                letterSpacing = (-1.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Received from ${payment.payer ?: "Customer"}",
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CounterEmerald.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.24f)), CircleShape)
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CounterEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Payment announced",
                        color = CounterEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$appName · $timeAgo",
                color = CounterMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CounterIdleHero(pulseScale: Float) {
    CounterGlass(
        modifier = Modifier.fillMaxWidth(),
        accent = CounterEmerald
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(158.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.20f)), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(CounterEmerald.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.35f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(CounterEmerald.copy(alpha = 0.42f), CounterEmeraldDeep)
                                )
                            )
                            .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.64f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CounterEmerald,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Listening for payments",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "Incoming payments will be announced here.",
                color = CounterMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CounterEmerald.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, CounterEmerald.copy(alpha = 0.20f)), CircleShape)
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                        text = "Speaker active · Offline mode",
                    color = CounterEmerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CounterSummary(
    todayTotal: Long,
    todayCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CounterMetric(
            modifier = Modifier.weight(1f),
            label = "TODAY",
            value = AmountParser.formatPaise(todayTotal),
            supporting = "Total collected"
        )
        CounterMetric(
            modifier = Modifier.weight(1f),
            label = "TRANSACTIONS",
            value = "$todayCount",
            supporting = if (todayCount == 1) "payment today" else "payments today"
        )
    }
}

@Composable
private fun CounterMetric(
    modifier: Modifier,
    label: String,
    value: String,
    supporting: String
) {
    CounterGlass(
        modifier = modifier,
        accent = CounterEmerald.copy(alpha = 0.72f)
    ) {
        Column {
            Text(
                text = label,
                color = CounterMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = supporting,
                color = CounterEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CounterGlass(
    modifier: Modifier,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        CounterSurface.copy(alpha = 0.92f),
                        Color(0xFF0A120D).copy(alpha = 0.88f),
                        accent.copy(alpha = 0.08f)
                    )
                )
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.20f)), shape)
            .padding(18.dp),
        content = content
    )
}
