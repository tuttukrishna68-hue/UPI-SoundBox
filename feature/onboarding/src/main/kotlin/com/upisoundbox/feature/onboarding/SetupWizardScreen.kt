package com.upisoundbox.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    announcementEngine: AnnouncementEngine,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Dynamic Permission & State tracking
    var hasNotificationAccess by remember { mutableStateOf(false) }
    var hasPostNotificationPermission by remember { mutableStateOf(false) }
    var hasBatteryExemption by remember { mutableStateOf(false) }
    var hasVoiceInstalled by remember { mutableStateOf(true) }
    var isTestSuccessful by remember { mutableStateOf<Boolean?>(null) }

    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }
    var showSmsDisclosureDialog by remember { mutableStateOf(false) }
    var showOemGuideDialog by remember { mutableStateOf(false) }

    val isSmsEnabled by preferences.isSmsSourceEnabled.collectAsState(initial = false)
    val languageTag by preferences.languageTag.collectAsState(initial = "en-IN")

    // Launcher for POST_NOTIFICATIONS
    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotificationPermission = granted
    }

    // Launcher for SMS permissions
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.RECEIVE_SMS] == true
        scope.launch {
            preferences.setSmsSourceEnabled(granted)
        }
    }

    fun refreshStates() {
        // 1. Notification access
        hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        // 2. Post notification permission
        hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // 3. Battery exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        hasBatteryExemption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        // 4. Voice data
        scope.launch {
            hasVoiceInstalled = announcementEngine.isVoiceInstalled(languageTag)
        }
    }

    // Lifecycle observer to re-check states when returning from system Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refreshStates()
    }

    val allEssentialReady = hasNotificationAccess && hasPostNotificationPermission && hasBatteryExemption
    val requiredPermissionCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 3 else 2
    val readyPermissionCount = listOf(
        hasNotificationAccess,
        hasPostNotificationPermission,
        hasBatteryExemption
    ).count { it }
    val setupProgress = (readyPermissionCount.toFloat() / requiredPermissionCount.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.paper,
                        Color(0xFF071A12),
                        colors.paper
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SetupProgressHeader(
            readyCount = readyPermissionCount,
            totalCount = requiredPermissionCount,
            progress = setupProgress
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Required access",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Connect the essentials so every payment is detected and announced reliably.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Notification Listener Access
        WizardItemCard(
            icon = Icons.Default.Notifications,
            stepLabel = "01 · CORE ACCESS",
            title = "Notification access",
            description = "Allows detecting incoming payment alerts from allow-listed UPI apps.",
            isCompleted = hasNotificationAccess,
            buttonText = if (hasNotificationAccess) "Granted" else "Enable",
            onAction = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            },
            onHelp = { showRestrictedSettingsHelp = true },
            helpLabel = "Restricted settings help?"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Post Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WizardItemCard(
                icon = Icons.Default.NotificationsNone,
                stepLabel = "02 · CORE ACCESS",
                title = "Notification permission",
                description = "Required to keep the Soundbox active in the background.",
                isCompleted = hasPostNotificationPermission,
                buttonText = if (hasPostNotificationPermission) "Granted" else "Allow",
                onAction = {
                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Step 3: Battery Optimization Exemption
        WizardItemCard(
            icon = Icons.Default.BatteryChargingFull,
            stepLabel = "03 · CORE ACCESS",
            title = "Battery optimization",
            description = "Exempt Soundbox so Android doesn't kill it during screen-off sleep.",
            isCompleted = hasBatteryExemption,
            buttonText = if (hasBatteryExemption) "Exempted" else "Fix",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: OEM Background Guide
        WizardItemCard(
            icon = Icons.Default.SettingsIcon,
            stepLabel = "04 · RECOMMENDED",
            title = "Autostart & OEM settings",
            description = "Configure autostart on Xiaomi, Samsung, Vivo, Oppo, Realme devices.",
            isCompleted = false,
            buttonText = "Open Guide",
            onAction = { showOemGuideDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 5: TTS Voice Check
        WizardItemCard(
            icon = Icons.Default.RecordVoiceOver,
            stepLabel = "05 · AUDIO",
            title = "Speech voice data",
            description = "Verifies voice synthesis for your selected language ($languageTag).",
            isCompleted = hasVoiceInstalled,
            buttonText = if (hasVoiceInstalled) "Ready" else "Install Voice",
            onAction = {
                try {
                    val installIntent = announcementEngine.getVoiceInstallIntent()
                    if (installIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(installIntent)
                    } else {
                        context.startActivity(announcementEngine.getTtsSettingsIntent())
                    }
                } catch (_: Exception) {
                    try {
                        context.startActivity(announcementEngine.getTtsSettingsIntent())
                    } catch (_: Exception) {}
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 6: Optional SMS Detection
        GlassPanel(accent = colors.accentOrange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PermissionIcon(
                    icon = Icons.Default.Sms,
                    accent = colors.accentOrange,
                    isCompleted = isSmsEnabled
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optional backup",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bank SMS detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "A private backup when payment notifications arrive late.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
                Switch(
                    checked = isSmsEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showSmsDisclosureDialog = true
                        } else {
                            scope.launch { preferences.setSmsSourceEnabled(false) }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accentOrange,
                        checkedTrackColor = colors.accentOrange.copy(alpha = 0.28f),
                        uncheckedThumbColor = colors.muted,
                        uncheckedTrackColor = colors.background
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 7: Test Payment Announcement
        GlassPanel(accent = colors.success) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PermissionIcon(
                    icon = Icons.Default.VolumeUp,
                    accent = colors.success,
                    isCompleted = isTestSuccessful == true
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FINAL CHECK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.success
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Test the announcement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Confirm your phone can speak a payment clearly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isTestSuccessful = null
                            isTestSuccessful = announcementQueue.enqueueAndAwait(
                                AnnouncementRequest.Custom(
                                    text = "Audio check complete.",
                                    languageTag = languageTag
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentOrange,
                        contentColor = Color(0xFF04281A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(text = "Play test", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (isTestSuccessful != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isTestSuccessful == true) {
                            "Test announcement played"
                        } else {
                            "${announcementEngine.getLastFailureReason()} Tap to open Text-to-Speech settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTestSuccessful == true) colors.success else colors.error,
                        textAlign = TextAlign.End
                    )
                    if (isTestSuccessful == false) {
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(announcementEngine.getTtsSettingsIntent())
                                } catch (_: Exception) {}
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 0.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text("Open TTS settings", color = colors.accentOrange)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Complete Button
        PillButton(
            text = "Complete Setup & Open Dashboard",
            onClick = {
                scope.launch {
                    preferences.setOnboardingCompleted(true)
                    onComplete()
                }
            },
            enabled = allEssentialReady,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog: Android 13+ Restricted Settings Guide
    if (showRestrictedSettingsHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsHelp = false },
            title = {
                Text(
                    text = "Allowing Restricted Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
            },
            text = {
                Column {
                    Text(
                        text = "On Android 13 and newer, sideloaded apps have sensitive toggles greyed out by default.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Open your phone's Settings > Apps > UPI Soundbox.\n" +
                                "2. Tap the three dots (⋮) in the top-right corner.\n" +
                                "3. Tap 'Allow restricted settings'.\n" +
                                "4. Authenticate with fingerprint or PIN.\n" +
                                "5. Return here and toggle Notification Access ON.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestrictedSettingsHelp = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open App Info", color = colors.accentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsHelp = false }) {
                    Text("Dismiss", color = colors.muted)
                }
            }
        )
    }

    // Dialog: Plain-language SMS Privacy Disclosure
    if (showSmsDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showSmsDisclosureDialog = false },
            title = {
                Text(text = "SMS Credit Detection Privacy Disclosure", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    Text(
                        text = "• What we read: Incoming SMS from verified bank sender IDs (e.g. HDFCBK, SBIINB) matching incoming credit keywords only.\n\n" +
                                "• What we store: Only the payment amount, payer name/VPA and timestamp in your encrypted local database.\n\n" +
                                "• What never leaves your phone: payment data. Network access is used only for an optional manual update check. No SMS text is ever uploaded or shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDisclosureDialog = false
                    smsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                }) {
                    Text("Agree & Enable", color = colors.accentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDisclosureDialog = false }) {
                    Text("Cancel", color = colors.muted)
                }
            }
        )
    }

    // Dialog: OEM Autostart Guide
    if (showOemGuideDialog) {
        AlertDialog(
            onDismissRequest = { showOemGuideDialog = false },
            title = {
                Text(text = "OEM Background Guides", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "• Xiaomi/Redmi/Poco (MIUI/HyperOS): Settings > Apps > Permissions > Autostart > Enable UPI Soundbox. Battery saver: No restrictions.\n\n" +
                                "• Samsung (OneUI): Settings > Battery and device care > Battery > Background usage limits > Never sleeping apps > Add UPI Soundbox.\n\n" +
                                "• Oppo/Realme (ColorOS): Settings > Battery > App Battery Management > Allow Auto-launch and background activity.\n\n" +
                                "• Vivo/iQOO (FuntouchOS): Settings > Battery > High background power consumption > Enable UPI Soundbox.\n\n" +
                                "• OnePlus (OxygenOS): Settings > Battery > More settings > Optimize battery use > Select 'Don't optimize'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOemGuideDialog = false }) {
                    Text("Got it", color = colors.accentOrange)
                }
            }
        )
    }
}

@Composable
private fun SetupProgressHeader(
    readyCount: Int,
    totalCount: Int,
    progress: Float
) {
    val colors = LocalSoundboxColors.current
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF123C2A).copy(alpha = 0.86f),
                        colors.surface.copy(alpha = 0.86f),
                        Color(0xFF0B2117).copy(alpha = 0.92f)
                    )
                )
            )
            .border(BorderStroke(1.dp, colors.accentOrange.copy(alpha = 0.24f)), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PermissionIcon(
                    icon = Icons.Default.Security,
                    accent = colors.accentOrange,
                    isCompleted = readyCount == totalCount
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOUND BOX SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentOrange
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (readyCount == totalCount) "You're all set" else "Connect your essentials",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Private, reliable payment announcements",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Core access",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted
                )
                Text(
                    text = "$readyCount of $totalCount ready",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (readyCount == totalCount) colors.success else colors.ink
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.background.copy(alpha = 0.72f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(colors.accentOrange.copy(alpha = 0.72f), colors.accentOrange)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSoundboxColors.current
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.surface.copy(alpha = 0.86f),
                        colors.background.copy(alpha = 0.68f),
                        colors.pillBackground.copy(alpha = 0.32f)
                    )
                )
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.16f)), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun PermissionIcon(
    icon: ImageVector,
    accent: Color,
    isCompleted: Boolean
) {
    val colors = LocalSoundboxColors.current
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.34f), colors.background.copy(alpha = 0.72f))
                )
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.44f)), RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(25.dp)
        )
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.success),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF04281A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun WizardItemCard(
    icon: ImageVector,
    stepLabel: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    onHelp: (() -> Unit)? = null,
    helpLabel: String? = null
) {
    val colors = LocalSoundboxColors.current
    val accent = if (isCompleted) colors.success else colors.accentOrange

    GlassPanel(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            PermissionIcon(icon = icon, accent = accent, isCompleted = isCompleted)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onHelp != null && helpLabel != null) {
                Text(
                    text = helpLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accentOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onHelp() }
                )
            } else {
                Text(
                    text = if (isCompleted) "Ready to use" else "Action needed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) colors.success else colors.muted,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) colors.pillBackground else colors.accentOrange,
                    contentColor = if (isCompleted) colors.pillText else Color(0xFF04281A)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 15.dp,
                    vertical = 9.dp
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isCompleted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
