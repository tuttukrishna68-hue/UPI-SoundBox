package com.upisoundbox.feature.onboarding

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.upisoundbox.core.design.theme.LocalSoundboxColors
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
            .background(bgGradient),
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
 * Onboarding Carousel Screen matching reference image media_1794457935647_616c5b6a.jpg
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
            Color(0xFF0C1610),
            Color(0xFF070B09)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Top bar with Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
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
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Visual Hero based on pageIndex
                when (pageIndex) {
                    0 -> OnboardingVisualSlide1()
                    1 -> OnboardingVisualSlide2()
                    2 -> OnboardingVisualSlide3()
                }

                Spacer(modifier = Modifier.height(38.dp))

                // Headline with highlighted words matching reference
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
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Never miss a customer payment during rush hours. Spoken loud and clear in your chosen Indian language.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    1 -> {
                        Text(
                            text = "Works with\nyour UPI apps",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Compatible with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, and merchant business soundboxes.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
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
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No internet permission, zero cloud uploads. Your payment transactions stay encrypted right on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dots indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagesCount) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1F382A))
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom CTA Button: Next → or Get Started →
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
            shape = RoundedCornerShape(16.dp),
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

/**
 * Slide 1 Visual: Glowing Phone + Canopy Storefront
 */
@Composable
private fun OnboardingVisualSlide1() {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Canopy storefront glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F2B1C))
        )
        // Center Phone with Rupee & Soundwave
        Box(
            modifier = Modifier
                .width(95.dp)
                .height(145.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF09140D))
                .border(BorderStroke(2.dp, Color(0xFF10B981)), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "₹ )))",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}

/**
 * Slide 2 Visual: Center Rupee Card + Floating Real UPI Apps
 */
@Composable
private fun OnboardingVisualSlide2() {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Rupee Card
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F2E1E))
                .border(BorderStroke(2.dp, Color(0xFF10B981)), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White
            )
        }

        // Floating GPay (Top-Left)
        Box(
            modifier = Modifier
                .offset(x = (-55).dp, y = (-55).dp)
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_gpay),
                contentDescription = "Google Pay",
                modifier = Modifier.size(34.dp)
            )
        }

        // Floating PhonePe (Top-Right)
        Box(
            modifier = Modifier
                .offset(x = 55.dp, y = (-55).dp)
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF5F259F)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_phonepe),
                contentDescription = "PhonePe",
                modifier = Modifier.size(40.dp)
            )
        }

        // Floating Paytm (Bottom-Left)
        Box(
            modifier = Modifier
                .offset(x = (-55).dp, y = 55.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_paytm),
                contentDescription = "Paytm",
                modifier = Modifier.size(38.dp)
            )
        }

        // Floating BHIM (Bottom-Right)
        Box(
            modifier = Modifier
                .offset(x = 55.dp, y = 55.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_bhim),
                contentDescription = "BHIM",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * Slide 3 Visual: Shield + Lock + 100% Offline
 */
@Composable
private fun OnboardingVisualSlide3() {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing shield container
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0F2B1C))
                .border(BorderStroke(2.dp, Color(0xFF10B981)), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(48.dp)
            )
        }

        // Small badge with Wifi-off icon
        Box(
            modifier = Modifier
                .offset(x = 45.dp, y = 45.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF09140D))
                .border(BorderStroke(2.dp, Color(0xFF10B981)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = "Offline",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
