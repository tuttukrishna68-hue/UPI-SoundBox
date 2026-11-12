package com.upisoundbox.feature.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Launch Screen (Splash Screen) matching reference image media_1794457943571_81b3d337.jpg
 */
@Composable
fun SplashScreen(
    preferences: SoundboxPreferences,
    onNavigateHome: () -> Unit,
    onNavigateOnboarding: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1400)
        val isCompleted = preferences.isOnboardingCompleted.first()
        if (isCompleted) {
            onNavigateHome()
        } else {
            onNavigateOnboarding()
        }
    }

    // Audio equalizer animation
    val transition = rememberInfiniteTransition(label = "equalizer")
    val bar1 by transition.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1")
    val bar2 by transition.animateFloat(initialValue = 0.8f, targetValue = 0.2f, animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "b2")
    val bar3 by transition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b3")
    val bar4 by transition.animateFloat(initialValue = 0.9f, targetValue = 0.3f, animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "b4")
    val bar5 by transition.animateFloat(initialValue = 0.2f, targetValue = 0.7f, animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "b5")

    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF060907),
            Color(0xFF0A140F),
            Color(0xFF060907)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Master App Logo in rounded squircle
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "UPI Soundbox",
                modifier = Modifier
                    .size(115.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1E3A29)), RoundedCornerShape(26.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "UPI Soundbox",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Instant voice payment announcements",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Soundwave Visualizer Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left accent line
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF10B981))
                            )
                        )
                )

                Spacer(modifier = Modifier.width(4.dp))

                EqualizerBar(scale = bar1)
                EqualizerBar(scale = bar2)
                EqualizerBar(scale = bar3)
                EqualizerBar(scale = bar4)
                EqualizerBar(scale = bar5)

                Spacer(modifier = Modifier.width(4.dp))

                // Right accent line
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF10B981), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun EqualizerBar(scale: Float) {
    val height = 6.dp + (16.dp * scale)
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF10B981))
    )
}

/**
 * Onboarding Carousel Screen matching reference images:
 * Slide 1: media_1794461221758_f7fa7b7e.jpg
 * Slide 2: media_1794461221758_67442ad1.jpg
 * Slide 3: media_1794461221762_f56a4acb.jpg
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagesCount = 3
    val pagerState = rememberPagerState(pageCount = { pagesCount })
    val scope = rememberCoroutineScope()

    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF070B09),
            Color(0xFF0C1811),
            Color(0xFF070B09)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // Subtle ambient curved glow in bottom-right matching reference
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.2f, size.height)
                cubicTo(
                    size.width * 0.6f, size.height * 0.92f,
                    size.width * 0.85f, size.height * 0.82f,
                    size.width, size.height * 0.80f
                )
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3310B981), Color(0x0010B981)),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f),
                    radius = size.width * 0.6f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Top Bar with Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pagesCount - 1) {
                    Text(
                        text = "Skip",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onFinish() }
                            .padding(8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(34.dp))
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Visual Hero
                    when (pageIndex) {
                        0 -> OnboardingVisualSlide1()
                        1 -> OnboardingVisualSlide2()
                        2 -> OnboardingVisualSlide3()
                    }

                    Spacer(modifier = Modifier.height(44.dp))

                    // Headline with exact highlighted words matching reference
                    when (pageIndex) {
                        0 -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Hear every\npayment ")
                                    withStyle(SpanStyle(color = Color(0xFF34D399))) {
                                        append("instantly")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontSize = 26.sp,
                                lineHeight = 32.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Never miss a customer payment during rush hours. Spoken loud and clear in your chosen Indian language.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                        1 -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Works with\nyour ")
                                    withStyle(SpanStyle(color = Color(0xFF34D399))) {
                                        append("UPI apps")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontSize = 26.sp,
                                lineHeight = 32.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Compatible with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, and merchant business soundboxes.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                        2 -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Private and\n")
                                    withStyle(SpanStyle(color = Color(0xFF34D399))) {
                                        append("100% offline")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontSize = 26.sp,
                                lineHeight = 32.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No internet permission, zero cloud uploads. Your payment transactions stay encrypted right on your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Elongated Pill Dots Indicator matching reference images
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagesCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(20.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F382B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom CTA Pill Button: Next → or Get Started →
            Button(
                onClick = {
                    if (pagerState.currentPage < pagesCount - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pagesCount - 1) "Get Started  →" else "Next  →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Slide 1 Visual: Tilted 3D phone with glowing emerald rim + Rupee + soundwaves + shop canopy silhouette
 * Reference: media_1794461221758_f7fa7b7e.jpg
 */
@Composable
private fun OnboardingVisualSlide1() {
    Box(
        modifier = Modifier
            .size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Stylized shop canopy awning silhouette in background
        Canvas(modifier = Modifier.size(190.dp)) {
            val awningWidth = size.width * 0.88f
            val awningHeight = size.height * 0.45f
            val startX = (size.width - awningWidth) / 2
            val startY = size.height * 0.12f

            val path = Path().apply {
                moveTo(startX, startY + awningHeight * 0.7f)
                // Draw scalloped ridges
                val scallops = 5
                val scallopW = awningWidth / scallops
                for (i in 0 until scallops) {
                    val cx = startX + (i + 0.5f) * scallopW
                    val cy = startY + awningHeight
                    val ex = startX + (i + 1) * scallopW
                    val ey = startY + awningHeight * 0.7f
                    quadraticTo(cx, cy, ex, ey)
                }
                lineTo(startX + awningWidth, startY)
                lineTo(startX, startY)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0A2216), Color(0xFF06140D))
                )
            )
        }

        // 2. Floor spotlight glow oval beneath the phone
        Box(
            modifier = Modifier
                .offset(y = 65.dp)
                .width(170.dp)
                .height(44.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x7710B981), Color.Transparent),
                        radius = 85f
                    ),
                    shape = CircleShape
                )
        )

        // 3. Tilted 3D Smartphone with glowing rim & Rupee + soundwaves
        Box(
            modifier = Modifier
                .offset(y = (-4).dp)
                .graphicsLayer {
                    rotationZ = -12f
                }
                .width(108.dp)
                .height(168.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F261B), Color(0xFF06110B))
                    )
                )
                .border(
                    BorderStroke(
                        2.dp,
                        Brush.verticalGradient(
                            listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF047857))
                        )
                    ),
                    RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Speaker ear slit at top of phone
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F3A29))
            )

            // Inner glowing screen with ₹ and soundwave arcs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "₹",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Soundwave arcs
                Canvas(modifier = Modifier.size(28.dp, 36.dp)) {
                    val strokeW = 3f
                    val emerald = Color(0xFF34D399)

                    // Arc 1 (inner)
                    drawArc(
                        color = emerald,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = strokeW),
                        topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.3f, size.height * 0.25f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.5f)
                    )
                    // Arc 2 (middle)
                    drawArc(
                        color = emerald,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = strokeW),
                        topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, size.height * 0.15f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.85f, size.height * 0.7f)
                    )
                    // Arc 3 (outer)
                    drawArc(
                        color = emerald,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = strokeW),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.05f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.95f, size.height * 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Slide 2 Visual: Orbiting dashed circle with center emerald Rupee card & 4 real UPI apps
 * Reference: media_1794461221758_67442ad1.jpg
 */
@Composable
private fun OnboardingVisualSlide2() {
    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient glow behind center
        Box(
            modifier = Modifier
                .size(170.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x4410B981), Color.Transparent),
                        radius = 85f
                    ),
                    shape = CircleShape
                )
        )

        // 2. Circular dashed orbit ring
        Canvas(modifier = Modifier.size(170.dp)) {
            drawCircle(
                color = Color(0x5510B981),
                radius = size.minDimension / 2f,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )
        }

        // 3. Center Rupee Squircle Card
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF123B25), Color(0xFF081B11))
                    )
                )
                .border(
                    BorderStroke(2.dp, Color(0xFF10B981)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹",
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = Color.White
            )
        }

        // 4. Floating App 1: Google Pay (Top-Left)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_gpay),
            contentDescription = "Google Pay",
            modifier = Modifier
                .offset(x = (-58).dp, y = (-58).dp)
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(13.dp))
        )

        // 5. Floating App 2: PhonePe (Top-Right)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_phonepe),
            contentDescription = "PhonePe",
            modifier = Modifier
                .offset(x = 58.dp, y = (-58).dp)
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(13.dp))
        )

        // 6. Floating App 3: Paytm (Bottom-Left)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_paytm),
            contentDescription = "Paytm",
            modifier = Modifier
                .offset(x = (-58).dp, y = 58.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(13.dp))
        )

        // 7. Floating App 4: BHIM (Bottom-Right)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_bhim),
            contentDescription = "BHIM",
            modifier = Modifier
                .offset(x = 58.dp, y = 58.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(13.dp))
        )
    }
}

/**
 * Slide 3 Visual: 3D Emerald Shield + White Lock + Offline Wifi Badge
 * Reference: media_1794461221762_f56a4acb.jpg
 */
@Composable
private fun OnboardingVisualSlide3() {
    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient floor reflection glow
        Box(
            modifier = Modifier
                .offset(y = 70.dp)
                .width(160.dp)
                .height(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x6610B981), Color.Transparent),
                        radius = 80f
                    ),
                    shape = CircleShape
                )
        )

        // 2. Authentic Heraldic 3D Emerald Glass Shield with Glowing Contour
        Box(
            modifier = Modifier.size(136.dp, 156.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Precise shield geometry
                val shieldPath = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    // Top crest right curve
                    cubicTo(w * 0.75f, 0f, w, h * 0.08f, w, h * 0.28f)
                    // Down right side curve to bottom point
                    cubicTo(w, h * 0.65f, w * 0.7f, h * 0.88f, w * 0.5f, h)
                    // Down left side curve back from bottom point
                    cubicTo(w * 0.3f, h * 0.88f, 0f, h * 0.65f, 0f, h * 0.28f)
                    // Top crest left curve
                    cubicTo(0f, h * 0.08f, w * 0.25f, 0f, w * 0.5f, 0f)
                    close()
                }

                // Fill with translucent dark emerald glass gradient
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF133D27), Color(0xFF07190F))
                    )
                )

                // Glowing neon emerald border
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669))
                    ),
                    style = Stroke(width = 4f)
                )
            }

            // Crisp 3D Lock inside shield
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = Color.White,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(46.dp)
            )
        }

        // 3. Circular Badge at Bottom-Right of Shield: Crossed-Out Wifi (100% Offline)
        Box(
            modifier = Modifier
                .offset(x = 50.dp, y = 46.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0C2417), Color(0xFF06140D))
                    )
                )
                .border(
                    BorderStroke(2.dp, Color(0xFF10B981)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = "Offline",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
