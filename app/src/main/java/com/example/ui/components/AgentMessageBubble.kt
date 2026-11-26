package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AgentMessageEntity
import com.example.ui.spatial.performSpatialHaptic
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.AmberSkepticLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.PurpleTheoristLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor
import com.example.ui.theme.spatialSurfaceColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentMessageBubble(
    message: AgentMessageEntity,
    isSpeaking: Boolean = false,
    onSpeakClick: (() -> Unit)? = null,
    onCopied: (() -> Unit)? = null,
    onExportToKeep: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current

    val (accentColor, lightAccent, icon, roleDesc) = when (message.role) {
        "THEORIST" -> Quadruple(
            PurpleTheorist,
            PurpleTheoristLight,
            Icons.Default.Psychology,
            "Генератор гипотез"
        )
        "SKEPTIC" -> Quadruple(
            AmberSkeptic,
            AmberSkepticLight,
            Icons.Default.Shield,
            "Критик и верификатор"
        )
        "SYNTHESIZER" -> Quadruple(
            CyanAccent,
            CyanAccent,
            Icons.Default.Science,
            "Прогрессор-синтез"
        )
        else -> Quadruple(
            EmeraldHuman,
            EmeraldHuman,
            Icons.Default.Person,
            "Человек-оператор"
        )
    }

    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .spatialInteractive(accentColor = accentColor, shape = RoundedCornerShape(16.dp))
            .testTag("agent_message_card_${message.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaking) spatialCardHighlightColor() else spatialCardColor()
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSpeaking) 2.dp else 1.dp,
            color = if (isSpeaking) accentColor else accentColor.copy(alpha = if (spatialConfig.isTrueBlackOled) 0.6f else 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            // Header Row: Avatar, Title, Model Badge, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isSpeaking) 0.3f else 0.18f))
                            .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = message.agentTitle,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Text(
                            text = message.agentTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (spatialConfig.isSpatialModeActive) 14.5.sp else 13.5.sp
                        )
                        Text(
                            text = if (isSpeaking) "Озвучивается сейчас..." else roleDesc,
                            color = accentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Speak, Copy Action & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeString,
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (onSpeakClick != null) {
                        IconButton(
                            onClick = {
                                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                onSpeakClick()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .spatialInteractive(accentColor = accentColor, shape = CircleShape)
                                .testTag("speak_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Озвучить реплику",
                                tint = if (isSpeaking) accentColor else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    if (onExportToKeep != null) {
                        IconButton(
                            onClick = {
                                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                onExportToKeep()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .spatialInteractive(accentColor = AmberSkeptic, shape = CircleShape)
                                .testTag("keep_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = "В оперативный буфер Google Keep",
                                tint = AmberSkeptic,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            val formatted = "[${message.agentTitle} • ${message.modelName}]\n${message.content}"
                            clipboard.setText(AnnotatedString(formatted))
                            onCopied?.invoke()
                            Toast.makeText(context, "Реплика скопирована для кросс-постинга", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("copy_button_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать реплику",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Model Badge
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(spatialSurfaceColor())
                    .border(0.8.dp, spatialBorderColor(BorderSubtle), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Модель: ${message.modelName}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message Body (slightly larger readable typography in XREAL Birdbath optics)
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = if (spatialConfig.isSpatialModeActive) 15.sp else 13.5.sp,
                lineHeight = if (spatialConfig.isSpatialModeActive) 22.sp else 20.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
