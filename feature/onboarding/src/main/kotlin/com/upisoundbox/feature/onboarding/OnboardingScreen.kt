package com.upisoundbox.feature.onboarding

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifiOff
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
 * Splash screen using the supplied brand artwork as a full-bleed, edge-to-edge image.
 * The navigation decision remains separate from the visual so the reference artwork
 * is shown consistently on both first launch and returning launches.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00130D))
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen_reference),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )
    }
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
            Color(0xFF03110C),
            Color(0xFF071D14),
            Color(0xFF03100B)
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
                    colors = listOf(Color(0x3325D995), Color(0x0025D995)),
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
            // Top bar: the reference keeps Skip visible on every page, including the final card.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Skip",
                    color = Color(0xFFB6C2BE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onFinish() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
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
                                fontSize = 28.sp,
                                lineHeight = 35.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Never miss a customer payment during rush hours. Spoken loud and clear in your chosen Indian language.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                lineHeight = 21.sp
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
                                fontSize = 28.sp,
                                lineHeight = 35.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Compatible with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, and merchant business soundboxes.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                lineHeight = 21.sp
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
                                fontSize = 28.sp,
                                lineHeight = 35.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No internet permission, zero cloud uploads. Your payment transactions stay encrypted right on your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Small circular indicator dots matching the reference cards.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagesCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(if (isSelected) 8.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF35E6A1) else Color(0xFF29443A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom CTA: green gradient, white label and a separate arrow like the reference.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2AE09B), Color(0xFF079B68))
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Color(0xFF55F1B5).copy(alpha = 0.75f)),
                        RoundedCornerShape(30.dp)
                    )
                    .clickable {
                        if (pagerState.currentPage < pagesCount - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (pagerState.currentPage == pagesCount - 1) "Get Started" else "Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
            .size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Stylized shop canopy awning silhouette in background
        Canvas(modifier = Modifier.size(210.dp)) {
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
                .offset(y = 72.dp)
                .width(190.dp)
                .height(48.dp)
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
                .width(125.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(25.dp))
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
                    RoundedCornerShape(25.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Speaker ear slit at top of phone
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F3A29))
            )

            // Inner glowing screen with ₹ and soundwave arcs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "₹",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Soundwave arcs
                Canvas(modifier = Modifier.size(34.dp, 46.dp)) {
                    val strokeW = 3.5f
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
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient glow behind center
        Box(
            modifier = Modifier
                .size(195.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x4410B981), Color.Transparent),
                        radius = 98f
                    ),
                    shape = CircleShape
                )
        )

        // 2. Circular dashed orbit ring
        Canvas(modifier = Modifier.size(195.dp)) {
            drawCircle(
                color = Color(0x5510B981),
                radius = size.minDimension / 2f,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 11f), 0f)
                )
            )
        }

        // 3. Center Rupee Squircle Card
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF123B25), Color(0xFF081B11))
                    )
                )
                .border(
                    BorderStroke(2.dp, Color(0xFF10B981)),
                    RoundedCornerShape(23.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹",
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                color = Color.White
            )
        }

        // 4. Floating App 1: Google Pay (Top-Left)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_gpay),
            contentDescription = "Google Pay",
            modifier = Modifier
                .offset(x = (-67).dp, y = (-67).dp)
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(15.dp))
        )

        // 5. Floating App 2: PhonePe (Top-Right)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_phonepe),
            contentDescription = "PhonePe",
            modifier = Modifier
                .offset(x = 67.dp, y = (-67).dp)
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(15.dp))
        )

        // 6. Floating App 3: Paytm (Bottom-Left)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_paytm),
            contentDescription = "Paytm",
            modifier = Modifier
                .offset(x = (-67).dp, y = 67.dp)
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(15.dp))
        )

        // 7. Floating App 4: BHIM (Bottom-Right)
        Image(
            painter = painterResource(id = R.drawable.ic_brand_bhim),
            contentDescription = "BHIM",
            modifier = Modifier
                .offset(x = 67.dp, y = 67.dp)
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(15.dp))
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
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient floor reflection glow
        Box(
            modifier = Modifier
                .offset(y = 82.dp)
                .width(185.dp)
                .height(46.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x6610B981), Color.Transparent),
                        radius = 92f
                    ),
                    shape = CircleShape
                )
        )

        // 2. Authentic Heraldic 3D Emerald Glass Shield with Glowing Contour
        Box(
            modifier = Modifier.size(154.dp, 176.dp),
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
                    .size(54.dp)
            )
        }

        // 3. Circular Badge at Bottom-Right of Shield: Crossed-Out Wifi (100% Offline)
        Box(
            modifier = Modifier
                .offset(x = 58.dp, y = 54.dp)
                .size(54.dp)
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
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
