package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.domain.VoiceControlState
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.RoseWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor
import com.example.ui.theme.spatialSurfaceColor

/**
 * Compact XREAL AR Hands-Free Voice Control HUD.
 * Displays live SpeechRecognizer status, streaming transcript, RMS voice pulse,
 * continuous AR hands-free toggle, and an expandable quick-commands cheat sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandHud(
    voiceState: VoiceControlState,
    showHelpSheet: Boolean,
    onToggleListen: () -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onToggleHelp: () -> Unit,
    onQuickVoiceSample: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spatialConfig = LocalSpatialConfig.current
    val isVisible = voiceState.isListening ||
        voiceState.isHandsFreeEnabled ||
        voiceState.errorMessage != null ||
        voiceState.lastCommandLabel.isNotEmpty() ||
        showHelpSheet

    AnimatedVisibility(visible = isVisible, modifier = modifier) {
        val pulseTransition = rememberInfiniteTransition(label = "voice_hud_pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_pulse"
        )

        val accentColor = when {
            voiceState.errorMessage != null -> RoseWarning
            voiceState.isListening -> EmeraldHuman
            voiceState.isHandsFreeEnabled -> CyanAccent
            else -> AmberSkeptic
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("voice_command_hud"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (voiceState.isListening) spatialCardHighlightColor() else spatialCardColor()
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (voiceState.isListening) 1.5.dp else 1.dp,
                color = accentColor.copy(alpha = if (spatialConfig.isTrueBlackOled) 0.9f else 0.65f)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Mic Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (voiceState.isListening) (pulseScale + (voiceState.rmsDb / 48f)).coerceAtMost(1.22f) else 1f)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (voiceState.isListening) 0.25f else 0.14f))
                            .border(1.dp, accentColor, CircleShape)
                            .spatialInteractive(accentColor = accentColor, shape = CircleShape)
                            .clickable(onClick = onToggleListen)
                            .testTag("voice_hud_mic_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.GraphicEq else if (voiceState.isHandsFreeEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (voiceState.isListening) "Остановить прослушивание" else "Активировать голос",
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Live transcript / status text
                    Column(modifier = Modifier.weight(1f)) {
                        val headline = when {
                            voiceState.errorMessage != null -> voiceState.errorMessage
                            voiceState.partialTranscript.isNotBlank() -> "«${voiceState.partialTranscript}»"
                            voiceState.isListening && voiceState.isHandsFreeEnabled -> "Слушаю (XREAL Hands-Free)..."
                            voiceState.isListening -> "Говорите команду..."
                            voiceState.lastCommandLabel.isNotEmpty() -> "✓ ${voiceState.lastCommandLabel}"
                            voiceState.isHandsFreeEnabled -> "AR Голос активен (ожидание)"
                            else -> "Голосовое управление"
                        }
                        Text(
                            text = headline,
                            color = if (voiceState.errorMessage != null) RoseWarning else TextPrimary,
                            fontSize = if (spatialConfig.isSpatialModeActive) 13.5.sp else 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (voiceState.lastTranscript.isNotBlank() && voiceState.partialTranscript.isBlank() && voiceState.errorMessage == null) {
                            Text(
                                text = "Фраза: «${voiceState.lastTranscript}»",
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Continuous AR Hands-Free Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (voiceState.isHandsFreeEnabled) EmeraldHuman else spatialSurfaceColor())
                            .border(
                                1.dp,
                                if (voiceState.isHandsFreeEnabled) EmeraldHuman else spatialBorderColor(BorderSubtle),
                                RoundedCornerShape(8.dp)
                            )
                            .spatialInteractive(accentColor = EmeraldHuman, shape = RoundedCornerShape(8.dp))
                            .clickable { onToggleHandsFree(!voiceState.isHandsFreeEnabled) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("voice_hands_free_pill")
                    ) {
                        Text(
                            text = if (voiceState.isHandsFreeEnabled) "AR АВТО" else "АВТО",
                            color = if (voiceState.isHandsFreeEnabled) VoidDark else TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Help cheat-sheet toggle
                    IconButton(
                        onClick = onToggleHelp,
                        modifier = Modifier
                            .size(34.dp)
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("voice_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Список голосовых команд",
                            tint = if (showHelpSheet) CyanAccent else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Dismiss HUD / Stop Voice
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .spatialInteractive(accentColor = TextMuted, shape = CircleShape)
                            .testTag("voice_hud_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть панель голоса",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expandable Hands-Free Voice Command Guide
                AnimatedVisibility(visible = showHelpSheet) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(spatialSurfaceColor())
                            .border(0.8.dp, spatialBorderColor(BorderSubtle), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ГОЛОСОВЫЕ КОМАНДЫ XREAL AR (нажмите или произнесите):",
                            color = CyanAccent,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val samples = listOf(
                            "Запусти цикл",
                            "Гипотезы",
                            "Арена",
                            "Совет",
                            "Темы",
                            "В Keep",
                            "Озвучь",
                            "Пауза",
                            "Поиск arXiv",
                            "Указание: проверь шум"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            samples.forEach { sample ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(spatialCardColor())
                                        .border(0.8.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                        .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(6.dp))
                                        .clickable { onQuickVoiceSample(sample) }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "«$sample»",
                                        color = TextPrimary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
