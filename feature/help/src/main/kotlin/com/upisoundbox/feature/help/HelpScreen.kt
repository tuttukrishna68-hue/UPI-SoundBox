package com.upisoundbox.feature.help

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.design.component.ChatBubble
import com.upisoundbox.core.design.theme.LocalSoundboxColors

data class ChatMessage(val sender: String, val message: String, val isUser: Boolean = false)

@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val colors = LocalSoundboxColors.current

    val messages = remember {
        mutableStateListOf(
            ChatMessage("assistant", "Hello! Welcome to UPI Soundbox Support."),
            ChatMessage("assistant", "Tap any topic below to learn how to optimize detection or solve OEM battery issues.")
        )
    }

    val quickQuestions = listOf(
        "Allow Restricted Settings",
        "Why are alerts delayed?",
        "Xiaomi / Poco Battery Fix",
        "Samsung Never Sleeping Apps",
        "How offline privacy works"
    )

    fun answerQuestion(q: String) {
        messages.add(ChatMessage("user", q, isUser = true))
        val answer = when (q) {
            "Allow Restricted Settings" ->
                "On Android 13+, sideloaded APKs have accessibility/listener toggles greyed out.\n\n" +
                        "Fix:\n" +
                        "1. Open Phone Settings > Apps > UPI Soundbox.\n" +
                        "2. Tap the three dots (⋮) in the top-right.\n" +
                        "3. Tap 'Allow restricted settings'.\n" +
                        "4. Authenticate and return to grant Notification Access."

            "Why are alerts delayed?" ->
                "Delays happen when Android puts the app to sleep. Ensure you have:\n" +
                        "1. Granted 'Ignore Battery Optimization' in Setup Wizard.\n" +
                        "2. Enabled 'Autostart' in your phone's Security app.\n" +
                        "3. Locked UPI Soundbox in your phone's Recent Apps view."

            "Xiaomi / Poco Battery Fix" ->
                "MIUI & HyperOS have aggressive killers:\n" +
                        "1. Open Security App > Manage Apps > UPI Soundbox.\n" +
                        "2. Enable 'Autostart'.\n" +
                        "3. Under 'Battery Saver', choose 'No restrictions'."

            "Samsung Never Sleeping Apps" ->
                "OneUI puts inactive apps into deep sleep:\n" +
                        "1. Settings > Battery and device care > Battery.\n" +
                        "2. Background usage limits > Never sleeping apps.\n" +
                        "3. Tap (+) and add UPI Soundbox."

            "How offline privacy works" ->
                "UPI Soundbox has ZERO INTERNET permission in its manifest.\n\n" +
                        "• It cannot connect to WiFi or cellular data.\n" +
                        "• All transactions are stored in a local encrypted SQLCipher database with keys in your device's Android Keystore.\n" +
                        "• Payer names are never shared or sent to any server."

            else -> "I'm here to help with any Soundbox setup or OEM reliability question!"
        }
        messages.add(ChatMessage("assistant", answer, isUser = false))
    }

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
                text = "Ask Us Anything",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }

        // Conversation Messages (Channel D style)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                if (msg.isUser) {
                    // Right aligned user pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.ink,
                            modifier = Modifier.padding(start = 48.dp)
                        ) {
                            Text(
                                text = msg.message,
                                color = colors.paper,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                } else {
                    // Left aligned incoming bubble
                    ChatBubble(
                        text = msg.message,
                        timestamp = "",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom Black Quick-Reply Pills (Reference 1 style!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "QUICK TOPICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickQuestions) { q ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF141414), // Solid black CTA pill
                        modifier = Modifier.clickable { answerQuestion(q) }
                    ) {
                        Text(
                            text = q,
                            color = Color(0xFFFAFAF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
