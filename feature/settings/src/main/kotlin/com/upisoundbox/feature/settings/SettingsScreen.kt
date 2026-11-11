package com.upisoundbox.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: SoundboxPreferences,
    onNavigateBack: () -> Unit,
    onNavigateVoiceSettings: () -> Unit,
    onNavigateUpiApps: () -> Unit,
    onNavigateHelp: () -> Unit,
    onNavigateDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val isAppLock by preferences.isAppLockEnabled.collectAsState(initial = false)
    val isSecureScreen by preferences.isSecureScreenEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Cards
            SettingsNavCard(
                title = "Voice & Soundbox",
                subtitle = "10 languages, custom phrase templates, volume boost",
                onClick = onNavigateVoiceSettings
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsNavCard(
                title = "UPI Apps & Parser Tester",
                subtitle = "App detection status & real-time notification tester",
                onClick = onNavigateUpiApps
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsNavCard(
                title = "Help & FAQ",
                subtitle = "Conversational guide and OEM troubleshooting",
                onClick = onNavigateHelp
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsNavCard(
                title = "Diagnostics & Fixture Export",
                subtitle = "Redacted log inspector and export real notification samples",
                onClick = onNavigateDiagnostics
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Security Section
            Text(
                text = "SECURITY & PRIVACY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Biometric Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Biometric / PIN Lock", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(text = "Require fingerprint or PIN to view transaction ledger.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        Switch(
                            checked = isAppLock,
                            onCheckedChange = { scope.launch { preferences.setAppLock(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secure Screen (FLAG_SECURE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Secure Screen (Anti-Screenshot)", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(text = "Blocks screenshots and screen recording for extra privacy.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        Switch(
                            checked = isSecureScreen,
                            onCheckedChange = { scope.launch { preferences.setSecureScreen(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // About Section
            Text(
                text = "ABOUT UPI SOUNDBOX",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Version 1.0.0 (Open Source)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.ink)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Built with Jetpack Compose & Kotlin. Distributed via GitHub Releases.", style = MaterialTheme.typography.bodySmall, color = colors.muted)

                    Spacer(modifier = Modifier.height(14.dp))

                    PillButton(
                        text = "Check for Updates (GitHub)",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/upisoundbox/soundbox/releases")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsNavCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
        }
    }
}
