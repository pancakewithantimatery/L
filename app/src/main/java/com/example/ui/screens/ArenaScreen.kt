package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AgentMessageEntity
import com.example.data.local.ResearchSessionEntity
import com.example.domain.ProgressorEngine
import com.example.ui.components.AgentMessageBubble
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

@Composable
fun ArenaScreen(
    session: ResearchSessionEntity?,
    messages: List<AgentMessageEntity>,
    isCycleRunning: Boolean,
    progressState: ProgressorEngine.ProgressState?,
    onOpenCouncil: () -> Unit,
    onLaunchNextCycle: () -> Unit,
    onSendHumanMessage: (String) -> Unit,
    onExportReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var humanInputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (session == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет активного исследования.\nСоздайте тему на вкладке 'Исследования'.",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("arena_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ДИАЛОГОВАЯ АРЕНА • ЦИКЛ #${session.totalCycles}",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onExportReport,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("export_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Экспорт отчета",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onOpenCouncil,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("open_council_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Совет моделей",
                                tint = PurpleTheorist,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = session.topic,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Active Models Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelMiniTag("T:", session.activeTheoristModel, PurpleTheorist)
                    ModelMiniTag("S:", session.activeSkepticModel, AmberSkeptic)
                    ModelMiniTag("P:", session.activeSynthesizerModel, CyanAccent)
                }
            }
        }

        // Live Progress Indicator Bar (if cycle is currently running)
        AnimatedVisibility(visible = isCycleRunning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("running_progress_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardHighlight),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = progressState?.step ?: "Синтез моделей Venice.ai...",
                        color = CyanAccent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("messages_lazy_column")
        ) {
            if (messages.isEmpty() && !isCycleRunning) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нажмите 'Запустить цикл' для старта диалога между моделями Venice.ai",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    AgentMessageBubble(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Human Intervention Input & Action Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = humanInputText,
                        onValueChange = { humanInputText = it },
                        placeholder = {
                            Text(
                                "Вмешательство человека в диалог...",
                                color = TextMuted,
                                fontSize = 12.5.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("human_input_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldHuman,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = EmeraldHuman
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = humanInputText.trim()
                            if (text.isNotEmpty()) {
                                onSendHumanMessage(text)
                                humanInputText = ""
                            }
                        },
                        enabled = humanInputText.trim().isNotEmpty() && !isCycleRunning,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (humanInputText.trim().isNotEmpty()) EmeraldHuman else DeepSpace)
                            .testTag("send_human_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (humanInputText.trim().isNotEmpty()) VoidDark else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLaunchNextCycle,
                        enabled = !isCycleRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("arena_launch_cycle_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = VoidDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Новый цикл диалога",
                            color = VoidDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    Button(
                        onClick = onOpenCouncil,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("arena_change_models_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardHighlight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = PurpleTheorist,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Сменить состав",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelMiniTag(prefix: String, modelName: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DeepSpace)
            .border(0.5.dp, BorderSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prefix,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = modelName.take(14),
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
