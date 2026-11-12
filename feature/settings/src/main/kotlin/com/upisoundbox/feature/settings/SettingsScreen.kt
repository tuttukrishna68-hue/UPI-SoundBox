package com.upisoundbox.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    onNavigateBack: () -> Unit,
    onNavigateVoiceSettings: () -> Unit,
    onNavigateUpiApps: () -> Unit,
    onNavigateHelp: () -> Unit,
    onNavigateDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val currentTheme by preferences.themeMode.collectAsState(initial = "dark")
    val currentFontScale by preferences.fontScale.collectAsState(initial = "compact")
    val userName by preferences.userName.collectAsState(initial = "Budiarti")
    val currentLang by preferences.languageTag.collectAsState(initial = "en-IN")
    var testLang by remember(currentLang) { mutableStateOf(currentLang) }
    val isAppLock by preferences.isAppLockEnabled.collectAsState(initial = false)
    val isSecureScreen by preferences.isSecureScreenEnabled.collectAsState(initial = false)

    var showNameDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Top Bar
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
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 19.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 0. Merchant Profile Section
            Text(
                text = "MERCHANT PROFILE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editingName = userName
                        showNameDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merchant Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Dashboard displays: 'Hello, $userName'",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            // 1. Appearance & Display Section (Theme + Font Scale)
            Text(
                text = "APPEARANCE & DISPLAY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Dark theme is default, matching merchant soundbox display.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionPill(
                            title = "Dark",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentTheme.equals("dark", ignoreCase = true),
                            onClick = { scope.launch { preferences.setThemeMode("dark") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionPill(
                            title = "Light",
                            icon = Icons.Default.LightMode,
                            isSelected = currentTheme.equals("light", ignoreCase = true),
                            onClick = { scope.launch { preferences.setThemeMode("light") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionPill(
                            title = "System",
                            icon = Icons.Default.SettingsBrightness,
                            isSelected = currentTheme.equals("system", ignoreCase = true),
                            onClick = { scope.launch { preferences.setThemeMode("system") } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Text & Icon Scale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Compact fits entire soundbox on one screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionPill(
                            title = "Compact",
                            icon = Icons.Default.FormatSize,
                            isSelected = currentFontScale.equals("compact", ignoreCase = true),
                            onClick = { scope.launch { preferences.setFontScale("compact") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionPill(
                            title = "Standard",
                            icon = Icons.Default.FormatSize,
                            isSelected = currentFontScale.equals("standard", ignoreCase = true),
                            onClick = { scope.launch { preferences.setFontScale("standard") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionPill(
                            title = "Large",
                            icon = Icons.Default.FormatSize,
                            isSelected = currentFontScale.equals("large", ignoreCase = true),
                            onClick = { scope.launch { preferences.setFontScale("large") } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Demo Voice Test Card (Multi-language verification)
            Text(
                text = "SOUNDBOX SPEAKER TEST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.pillBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speaker",
                                tint = colors.pillText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Test Voice Announcement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Instant offline voice test across Indian languages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selector Chips
                    val testLangs = listOf(
                        "en-IN" to "English",
                        "hi-IN" to "हिन्दी",
                        "ml-IN" to "മലയാളം",
                        "ta-IN" to "தமிழ்",
                        "te-IN" to "తెలుగు",
                        "kn-IN" to "ಕನ್ನಡ"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(testLangs) { (tag, label) ->
                            val isSelected = testLang.equals(tag, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    testLang = tag
                                    scope.launch { preferences.setLanguageTag(tag) }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.pillBackground,
                                    labelColor = colors.textMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) Color(0xFF10B981) else colors.cardBorder,
                                    selectedBorderColor = Color(0xFF10B981),
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Live Preview Phrase
                    val previewPhrase = when (testLang.lowercase().replace("_", "-")) {
                        "hi-in", "hi" -> "PhonePe पर Rahul Sharma से पाँच सौ रुपये प्राप्त हुए"
                        "ml-in", "ml" -> "PhonePe വഴി Rahul Sharma ൽ നിന്ന് 500 രൂപ ലഭിച്ചു"
                        "ta-in", "ta" -> "PhonePe மூலம் Rahul Sharma இடமிருந்து 500 ரூபாய் பெறப்பட்டது"
                        "te-in", "te" -> "PhonePe ద్వారా Rahul Sharma నుండి 500 రూపాయలు అందింది"
                        "kn-in", "kn" -> "PhonePe ಮೂಲಕ Rahul Sharma ರಿಂದ 500 ರೂಪಾಯಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ"
                        else -> "Received 500 rupees on PhonePe from Rahul Sharma"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.pillBackground)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "\"$previewPhrase\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.pillText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            try {
                                scope.launch {
                                    try {
                                        announcementQueue.enqueue(
                                            AnnouncementRequest.Test(
                                                sampleAmountPaise = 50000L,
                                                samplePayer = "Rahul Sharma",
                                                languageTag = testLang
                                            )
                                        )
                                        val displayLang = testLangs.find { it.first.equals(testLang, ignoreCase = true) }?.second ?: testLang
                                        Toast.makeText(
                                            context,
                                            "📢 Playing ($displayLang): ₹500 on PhonePe",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Audio error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Demo Voice (${testLangs.find { it.first.equals(testLang, ignoreCase = true) }?.second ?: "Voice"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Soundbox Controls
            Text(
                text = "SOUNDBOX CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavCard(
                title = "Voice & Soundbox",
                subtitle = "10 Indian languages, custom phrase template, volume boost",
                onClick = onNavigateVoiceSettings
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavCard(
                title = "UPI Apps & Parser Tester",
                subtitle = "App detection status & live notification test tool",
                onClick = onNavigateUpiApps
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavCard(
                title = "Help & FAQ",
                subtitle = "OEM battery troubleshooting & conversational guide",
                onClick = onNavigateHelp
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavCard(
                title = "Diagnostics & System Logs",
                subtitle = "Redacted log inspector and export real notification samples",
                onClick = onNavigateDiagnostics
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Security & Privacy
            Text(
                text = "SECURITY & PRIVACY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Biometric Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric / PIN Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Require fingerprint or PIN to view transaction ledger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAppLock,
                            onCheckedChange = { scope.launch { preferences.setAppLock(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secure Screen (FLAG_SECURE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Secure Screen (Anti-Screenshot)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Blocks screenshots and screen recording for privacy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isSecureScreen,
                            onCheckedChange = { scope.launch { preferences.setSecureScreen(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. About UPI Soundbox (Using Real Official Brand Logo)
            Text(
                text = "ABOUT UPI SOUNDBOX",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.upisoundbox.core.design.R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UPI Soundbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Version 1.0.0 (100% Offline & Open Source)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zero internet permission, no cloud uploads. Runs 100% offline with on-device hardware Text-To-Speech.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PillButton(
                        text = "Check for Updates (GitHub)",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tuttukrishna/upi-soundbox/releases")).apply {
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

        // Edit Merchant Name Dialog
        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = {
                    Text(
                        text = "Merchant Name",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Set the name displayed on the soundbox home dashboard:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            placeholder = { Text("e.g. Budiarti, Rahul, Store Name", color = colors.textMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.cardBackground,
                                unfocusedContainerColor = colors.cardBackground,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = colors.cardBorder,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val newName = editingName.trim().ifEmpty { "Budiarti" }
                                scope.launch { preferences.setUserName(newName) }
                                showNameDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                },
                dismissButton = {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showNameDialog = false }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ThemeOptionPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    val bgColor = if (isSelected) Color(0xFF10B981) else colors.cardBackground
    val textColor = if (isSelected) Color.White else colors.textPrimary
    val borderColor = if (isSelected) Color(0xFF10B981) else colors.cardBorder

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor
            )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
