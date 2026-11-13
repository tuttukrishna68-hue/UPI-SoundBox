package com.upisoundbox.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: SoundboxPreferences,
    onRequestBiometricConfirmation: (onSuccess: () -> Unit) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateVoiceSettings: () -> Unit,
    onNavigateUpiApps: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateHelp: () -> Unit,
    onNavigateDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val currentFontScale by preferences.fontScale.collectAsState(initial = "compact")
    val userName by preferences.userName.collectAsState(initial = SoundboxPreferences.DEFAULT_USER_NAME)
    val currentLang by preferences.languageTag.collectAsState(initial = "en-IN")
    val isAppLock by preferences.isAppLockEnabled.collectAsState(initial = false)
    val isSecureScreen by preferences.isSecureScreenEnabled.collectAsState(initial = false)
    val selectedLanguage = SUPPORTED_LANGUAGES
        .firstOrNull { it.tag.equals(currentLang, ignoreCase = true) }
        ?.name
        ?: currentLang
    var updateState by remember { mutableStateOf<UpdateCheckResult>(UpdateCheckResult.Idle) }
    val currentVersionName = remember { UpdateChecker.currentVersionName(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp
                )
                Text(
                    text = "Make your soundbox work your way",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateProfile)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF55E5AA), Color(0xFF0E8E62))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "M",
                            color = Color(0xFF04281A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { SoundboxPreferences.DEFAULT_USER_NAME },
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.emeraldGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Merchant profile · Private on this phone",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit profile",
                        tint = colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            SettingsSectionTitle(
                title = "Preferences",
                subtitle = "Text size and readability"
            )
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.FormatSize,
                    title = "Text size",
                    subtitle = when (currentFontScale.lowercase()) {
                        "large" -> "Large"
                        "standard" -> "Standard"
                        else -> "Compact"
                    }
                )
                SettingsChoiceRow(
                    options = listOf("Compact", "Standard", "Large"),
                    selected = currentFontScale.replaceFirstChar { it.uppercase() },
                    onSelected = { choice ->
                        scope.launch { preferences.setFontScale(choice.lowercase()) }
                    }
                )
            }

            SettingsSectionTitle(
                title = "Audio & announcements",
                subtitle = "Control what your customers hear"
            )
            SettingsGroup {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Voice settings",
                    subtitle = "$selectedLanguage · Phrases, repeats and volume",
                    onClick = onNavigateVoiceSettings
                )
            }

            SettingsSectionTitle(
                title = "Tools & connections",
                subtitle = "Keep the soundbox connected and observable"
            )
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "UPI apps & parser",
                    subtitle = "Supported apps and payment routing",
                    onClick = onNavigateUpiApps
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Diagnostics",
                    subtitle = "Connection status, logs and recent events",
                    onClick = onNavigateDiagnostics
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.SettingsBrightness,
                    title = "Help & guidance",
                    subtitle = "Battery, permissions and setup help",
                    onClick = onNavigateHelp
                )
            }

            SettingsSectionTitle(
                title = "Privacy & security",
                subtitle = "Protect your ledger on this device"
            )
            SettingsGroup {
                SettingsToggleRow(
                    icon = Icons.Default.Person,
                    title = "Biometric / PIN lock",
                    subtitle = "Require authentication before viewing history",
                    checked = isAppLock,
                    onCheckedChange = { requestedState ->
                        // Never change the lock state before the user confirms the
                        // security change with the device biometric/PIN prompt.
                        onRequestBiometricConfirmation {
                            scope.launch { preferences.setAppLock(requestedState) }
                        }
                    }
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    title = "Secure screen",
                    subtitle = "Block screenshots and screen recording",
                    checked = isSecureScreen,
                    onCheckedChange = { scope.launch { preferences.setSecureScreen(it) } }
                )
            }

            SettingsSectionTitle(
                title = "About",
                subtitle = "Built for private, offline-first merchant payments"
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.upisoundbox.core.design.R.drawable.ic_app_logo),
                            contentDescription = "App logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UPI Soundbox",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "100% offline · On-device TTS",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "v$currentVersionName",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(11.dp))
                    Text(
                        text = "Payment data stays on this phone. Update checks are optional and connect only to GitHub.",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PillButton(
                        text = if (updateState is UpdateCheckResult.Checking) "Checking…" else "Check for updates",
                        onClick = {
                            if (updateState !is UpdateCheckResult.Checking) {
                                scope.launch {
                                    updateState = UpdateCheckResult.Checking
                                    updateState = UpdateChecker.check(context)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    when (val state = updateState) {
                        UpdateCheckResult.Idle,
                        UpdateCheckResult.Checking -> Unit

                        is UpdateCheckResult.UpToDate -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "You're up to date · v${state.currentVersionName}",
                                color = colors.success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        is UpdateCheckResult.Available -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Update available · v${state.info.versionName}",
                                color = colors.success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.info.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.info.releaseNotes,
                                    color = colors.textMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            PillButton(
                                text = "Download v${state.info.versionName}",
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(state.info.downloadUrl)
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // Keep the app usable if a browser is unavailable.
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is UpdateCheckResult.Failed -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.message,
                                color = colors.warning,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    subtitle: String
) {
    val colors = LocalSoundboxColors.current
    Column {
        Text(
            text = title,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            color = colors.textMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSoundboxColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
        content = content
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalSoundboxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSoundboxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.emeraldGreen,
                checkedBorderColor = colors.emeraldGreen,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.background,
                uncheckedBorderColor = colors.cardBorder
            )
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    val colors = LocalSoundboxColors.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.pillBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.pillText,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    val colors = LocalSoundboxColors.current
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = colors.divider,
        thickness = 1.dp
    )
}

@Composable
private fun SettingsChoiceRow(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        options.forEach { option ->
            SettingsChoice(
                text = option,
                selected = option.equals(selected, ignoreCase = true),
                onClick = { onSelected(option) },
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun SettingsChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) colors.emeraldGreen else colors.background)
            .border(
                BorderStroke(1.dp, if (selected) colors.emeraldGreen else colors.cardBorder),
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF04281A) else colors.textMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}


