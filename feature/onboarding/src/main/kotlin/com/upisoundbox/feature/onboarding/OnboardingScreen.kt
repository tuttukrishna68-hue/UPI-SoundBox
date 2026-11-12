package com.upisoundbox.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.R
import com.upisoundbox.core.design.component.DotPagination
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.component.SkipButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    preferences: SoundboxPreferences,
    onNavigateHome: () -> Unit,
    onNavigateOnboarding: () -> Unit
) {
    val colors = LocalSoundboxColors.current

    LaunchedEffect(Unit) {
        delay(1200)
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
            .background(colors.paper),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Soundbox",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(30.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "UPI Soundbox",
                style = MaterialTheme.typography.displayMedium,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Instant voice payment announcements",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
        }
    }
}

data class OnboardingPageData(
    val illustrationRes: Int,
    val title: String,
    val subtitle: String
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    val pages = listOf(
        OnboardingPageData(
            illustrationRes = R.drawable.ic_illust_shopkeeper,
            title = "Hear every payment\ninstantly",
            subtitle = "Never miss a customer payment during rush hours. Spoken loud and clear in your chosen Indian language."
        ),
        OnboardingPageData(
            illustrationRes = R.drawable.ic_illust_notification,
            title = "Works with your\nUPI apps",
            subtitle = "Compatible with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, and merchant business soundboxes."
        ),
        OnboardingPageData(
            illustrationRes = R.drawable.ic_illust_speaker_check,
            title = "Private and\n100% offline",
            subtitle = "No internet permission, zero cloud uploads. Your payment transactions stay encrypted right on your phone."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Top bar with Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                SkipButton(
                    onClick = onFinish,
                    text = "Skip"
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = page.illustrationRes),
                    contentDescription = page.title,
                    modifier = Modifier.size(240.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    color = colors.ink
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = colors.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pagination Dots (Elongated Orange Active Dot)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DotPagination(
                pageCount = pages.size,
                currentPage = pagerState.currentPage
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom CTA Pill Button
        PillButton(
            text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
