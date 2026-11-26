package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AgentMessageEntity
import com.example.data.local.ResearchSessionEntity
import com.example.domain.ProgressorEngine
import com.example.domain.TtsPlaybackState
import com.example.ui.components.AgentMessageBubble
import com.example.ui.spatial.LocalFocusGraph
import com.example.ui.spatial.focusOrderNode
import com.example.ui.spatial.performSpatialHaptic
import com.example.ui.spatial.requestFocusSafely
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor
import com.example.ui.theme.spatialSurfaceColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ArenaScreen(
    session: ResearchSessionEntity?,
    messages: List<AgentMessageEntity>,
    citationsCount: Int,
    ttsState: TtsPlaybackState,
    isCycleRunning: Boolean,
    progressState: ProgressorEngine.ProgressState?,
    isWideSpatialLayout: Boolean = false,
    onOpenCouncil: () -> Unit,
    onOpenArxiv: () -> Unit,
    onCopyCrossPostSynthesis: () -> String,
    onLaunchNextCycle: () -> Unit,
    onSendHumanMessage: (String) -> Unit,
    onExportReport: () -> Unit,
    onExportToKeep: () -> Unit = {},
    onExportMessageToKeep: (AgentMessageEntity) -> Unit = {},
    onTtsPlayPause: (Long?) -> Unit,
    onTtsNext: () -> Unit,
    onTtsPrev: () -> Unit,
    onTtsCycleSpeed: () -> Unit,
    onTtsStop: () -> Unit,
    onEyesFreeAnnounce: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current
    val focusGraph = LocalFocusGraph.current

    val listState = rememberLazyListState()
    var humanInputText by remember { mutableStateOf("") }
    var showTtsBar by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-reveal TTS bar when playback starts
    LaunchedEffect(ttsState.isPlaying) {
        if (ttsState.isPlaying) {
            showTtsBar = true
        }
    }

    if (session == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(spatialCanvasColor())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет активного исследования.\nСоздайте тему на вкладке «Темы».",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    val belowHeaderTarget = if (showTtsBar) focusGraph.ttsPlayPause else focusGraph.arenaPrimaryAction
    val aboveBottomTarget = if (showTtsBar) focusGraph.ttsPlayPause else focusGraph.arenaArxiv
    val rightPaneTarget = if (isWideSpatialLayout) focusGraph.dagCanvas else FocusRequester.Cancel
    val leftRailTarget = if (isWideSpatialLayout) focusGraph.navArena else FocusRequester.Cancel
    val bottomNavTarget = if (isWideSpatialLayout) FocusRequester.Cancel else focusGraph.navArena

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(spatialCanvasColor())
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        keyEvent.key == Key.MediaPlayPause || (keyEvent.isCtrlPressed && keyEvent.key == Key.Spacebar) -> {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            if (ttsState.isPlaying) {
                                onTtsPlayPause(null)
                                onEyesFreeAnnounce("Пауза", true)
                            } else {
                                showTtsBar = true
                                onTtsPlayPause(null)
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Minimalist Icon-First Compact Header with Deterministic FocusGroup
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("arena_header_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
            border = androidx.compose.foundation.BorderStroke(1.dp, spatialBorderColor(BorderSubtle))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic + Cycle pill
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanAccent.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Ц#${session.totalCycles}",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.topic,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (spatialConfig.isSpatialModeActive) 14.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Icon Action Strip: arXiv -> TTS -> Copy Synth -> Export -> Council
                Row(
                    modifier = Modifier
                        .focusGroup()
                        .testTag("arena_header_focus_group"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onOpenArxiv()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaArxiv,
                                next = focusGraph.arenaTts,
                                previous = focusGraph.topSettings,
                                left = leftRailTarget,
                                right = focusGraph.arenaTts,
                                up = focusGraph.topXrealToggle,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = EmeraldHuman, shape = CircleShape)
                            .testTag("arena_arxiv_button")
                    ) {
                        if (citationsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = EmeraldHuman, contentColor = VoidDark) {
                                        Text("$citationsCount", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Поиск публикаций arXiv и DOI",
                                    tint = EmeraldHuman,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Поиск публикаций arXiv и DOI",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            if (!showTtsBar) {
                                showTtsBar = true
                                if (!ttsState.isPlaying && messages.isNotEmpty()) {
                                    onTtsPlayPause(null)
                                }
                            } else {
                                showTtsBar = false
                                onTtsStop()
                                onEyesFreeAnnounce("Подкаст остановлен", true)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaTts,
                                next = focusGraph.arenaCopySynth,
                                previous = focusGraph.arenaArxiv,
                                left = focusGraph.arenaArxiv,
                                right = focusGraph.arenaCopySynth,
                                up = focusGraph.topXrealToggle,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("arena_tts_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Аудио-подкаст дебатов TTS",
                            tint = if (ttsState.isPlaying || showTtsBar) CyanAccent else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // 1-tap Cross-Posting Copy: copies latest synthesis + thesis + Popper + arXiv refs
                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            val payload = onCopyCrossPostSynthesis()
                            if (payload.isNotBlank()) {
                                clipboard.setText(AnnotatedString(payload))
                                onEyesFreeAnnounce("Синтез скопирован", false)
                                Toast.makeText(context, "Тезис и синтез скопированы для соседних окон", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Нет данных для копирования", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaCopySynth,
                                next = focusGraph.arenaKeepExport,
                                previous = focusGraph.arenaTts,
                                left = focusGraph.arenaTts,
                                right = focusGraph.arenaKeepExport,
                                up = focusGraph.topNewSession,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = EmeraldHuman, shape = CircleShape)
                            .testTag("arena_copy_synthesis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать тезис и синтез для кросс-постинга",
                            tint = EmeraldHuman,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 1-tap Operational Buffer Export to Google Keep (focusable via D-Pad / Tab / Ctrl+K)
                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                            onExportToKeep()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaKeepExport,
                                next = focusGraph.arenaExport,
                                previous = focusGraph.arenaCopySynth,
                                left = focusGraph.arenaCopySynth,
                                right = focusGraph.arenaExport,
                                up = focusGraph.topNewSession,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = AmberSkeptic, shape = CircleShape)
                            .testTag("arena_keep_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StickyNote2,
                            contentDescription = "Экспорт оперативного резюме в Google Keep",
                            tint = AmberSkeptic,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onExportReport()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaExport,
                                next = focusGraph.arenaCouncil,
                                previous = focusGraph.arenaKeepExport,
                                left = focusGraph.arenaKeepExport,
                                right = focusGraph.arenaCouncil,
                                up = focusGraph.topNewSession,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Экспорт отчета Markdown или LaTeX",
                            tint = CyanAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onOpenCouncil()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .focusOrderNode(
                                requester = focusGraph.arenaCouncil,
                                next = belowHeaderTarget,
                                previous = focusGraph.arenaExport,
                                left = focusGraph.arenaExport,
                                right = rightPaneTarget,
                                up = focusGraph.topSettings,
                                down = belowHeaderTarget
                            )
                            .spatialInteractive(accentColor = PurpleTheorist, shape = CircleShape)
                            .testTag("open_council_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Совет моделей",
                            tint = PurpleTheorist,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // Collapsible Scientific Podcast TTS Player Bar (FocusGroup defaults entry directly to Play/Pause)
        AnimatedVisibility(visible = showTtsBar) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .focusGroup()
                    .focusProperties {
                        enter = { focusGraph.ttsPlayPause }
                    }
                    .testTag("tts_podcast_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = spatialCardHighlightColor()),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (ttsState.currentSpeakerTitle.isNotEmpty()) {
                                "Подкаст: ${ttsState.currentSpeakerTitle}"
                            } else {
                                "Научный подкаст (Android TTS)"
                            },
                            color = CyanAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ttsState.errorMessage ?: if (ttsState.totalMessages > 0) {
                                "Реплика ${ttsState.currentIndex + 1} из ${ttsState.totalMessages}"
                            } else {
                                "Нажмите Play для озвучивания"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onTtsPrev()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .focusOrderNode(
                                requester = focusGraph.ttsPrev,
                                next = focusGraph.ttsPlayPause,
                                previous = focusGraph.arenaCouncil,
                                left = leftRailTarget,
                                right = focusGraph.ttsPlayPause,
                                up = focusGraph.arenaTts,
                                down = focusGraph.arenaPrimaryAction
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("tts_prev_button")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущая реплика", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                            val wasPlaying = ttsState.isPlaying
                            onTtsPlayPause(null)
                            if (wasPlaying) {
                                onEyesFreeAnnounce("Пауза подкаста", true)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanAccent)
                            .focusOrderNode(
                                requester = focusGraph.ttsPlayPause,
                                next = focusGraph.ttsNext,
                                previous = focusGraph.ttsPrev,
                                left = focusGraph.ttsPrev,
                                right = focusGraph.ttsNext,
                                up = focusGraph.arenaTts,
                                down = focusGraph.arenaPrimaryAction
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("tts_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (ttsState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ttsState.isPlaying) "Пауза" else "Воспроизвести",
                            tint = VoidDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onTtsNext()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .focusOrderNode(
                                requester = focusGraph.ttsNext,
                                next = focusGraph.ttsSpeed,
                                previous = focusGraph.ttsPlayPause,
                                left = focusGraph.ttsPlayPause,
                                right = focusGraph.ttsSpeed,
                                up = focusGraph.arenaCopySynth,
                                down = focusGraph.arenaPrimaryAction
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("tts_next_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующая реплика", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(spatialSurfaceColor())
                            .border(1.dp, spatialBorderColor(BorderSubtle), RoundedCornerShape(6.dp))
                            .focusOrderNode(
                                requester = focusGraph.ttsSpeed,
                                next = focusGraph.ttsClose,
                                previous = focusGraph.ttsNext,
                                left = focusGraph.ttsNext,
                                right = focusGraph.ttsClose,
                                up = focusGraph.arenaExport,
                                down = focusGraph.arenaPrimaryAction
                            )
                            .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(6.dp))
                            .clickable {
                                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                onTtsCycleSpeed()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("tts_speed_button")
                    ) {
                        Text(
                            text = "${ttsState.speechSpeed}x",
                            color = CyanAccent,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            onTtsStop()
                            showTtsBar = false
                            onEyesFreeAnnounce("Подкаст выключен", true)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .focusOrderNode(
                                requester = focusGraph.ttsClose,
                                next = focusGraph.arenaPrimaryAction,
                                previous = focusGraph.ttsSpeed,
                                left = focusGraph.ttsSpeed,
                                right = rightPaneTarget,
                                up = focusGraph.arenaCouncil,
                                down = focusGraph.arenaPrimaryAction
                            )
                            .spatialInteractive(accentColor = TextMuted, shape = CircleShape)
                            .testTag("tts_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Скрыть плеер", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Live Progress Indicator Bar (if cycle is currently running)
        AnimatedVisibility(visible = isCycleRunning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("running_progress_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = spatialCardHighlightColor()),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = progressState?.step ?: "Синтез моделей...",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Expanded Scientific Dialogue Stream
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
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нажмите иконку цикла внизу справа для запуска эволюционного диалога Совета моделей",
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    AgentMessageBubble(
                        message = message,
                        isSpeaking = ttsState.isPlaying && ttsState.currentMessageId == message.id,
                        onSpeakClick = {
                            showTtsBar = true
                            onTtsPlayPause(message.id)
                        },
                        onCopied = {
                            onEyesFreeAnnounce("Скопировано", false)
                        },
                        onExportToKeep = {
                            onExportMessageToKeep(message)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Minimalist Single-Row Icon-First Bottom Action Bar
        // FocusGroup directs entry straight to primary action (Launch Cycle / Send) for eyes-free control
        val hasInput = humanInputText.trim().isNotEmpty()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(spatialCardColor())
                .border(1.dp, spatialBorderColor(BorderSubtle), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .focusGroup()
                .focusProperties {
                    enter = { focusGraph.arenaPrimaryAction }
                }
                .testTag("arena_bottom_focus_group"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = humanInputText,
                onValueChange = { humanInputText = it },
                placeholder = {
                    Text(
                        "Вмешательство человека...",
                        color = TextMuted,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusOrderNode(
                        requester = focusGraph.arenaInput,
                        next = focusGraph.arenaPrimaryAction,
                        previous = aboveBottomTarget,
                        left = leftRailTarget,
                        right = focusGraph.arenaPrimaryAction,
                        up = aboveBottomTarget,
                        down = bottomNavTarget,
                        hapticOnFocus = false
                    )
                    .testTag("human_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldHuman,
                    unfocusedBorderColor = spatialBorderColor(BorderSubtle).copy(alpha = 0.7f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldHuman
                ),
                singleLine = true
            )

            if (hasInput) {
                IconButton(
                    onClick = {
                        val text = humanInputText.trim()
                        if (text.isNotEmpty()) {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                            onEyesFreeAnnounce("Указание отправлено, цикл запущен", false)
                            onSendHumanMessage(text)
                            humanInputText = ""
                        }
                    },
                    enabled = !isCycleRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldHuman)
                        .focusOrderNode(
                            requester = focusGraph.arenaPrimaryAction,
                            next = if (isWideSpatialLayout) focusGraph.dagCanvas else focusGraph.navArena,
                            previous = focusGraph.arenaInput,
                            left = focusGraph.arenaInput,
                            right = rightPaneTarget,
                            up = aboveBottomTarget,
                            down = bottomNavTarget
                        )
                        .spatialInteractive(accentColor = EmeraldHuman, shape = CircleShape)
                        .testTag("send_human_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить вмешательство",
                        tint = VoidDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Primary Accent Icon Button: Launch Next Evolution Cycle
                IconButton(
                    onClick = {
                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                        onEyesFreeAnnounce("Эволюционный цикл запущен", false)
                        onLaunchNextCycle()
                    },
                    enabled = !isCycleRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (!isCycleRunning) CyanAccent else spatialSurfaceColor())
                        .focusOrderNode(
                            requester = focusGraph.arenaPrimaryAction,
                            next = focusGraph.arenaChangeCouncil,
                            previous = focusGraph.arenaInput,
                            left = focusGraph.arenaInput,
                            right = focusGraph.arenaChangeCouncil,
                            up = aboveBottomTarget,
                            down = bottomNavTarget
                        )
                        .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                        .testTag("arena_launch_cycle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = "Запустить новый цикл диалога",
                        tint = if (!isCycleRunning) VoidDark else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Change Council Composition Icon Button
                IconButton(
                    onClick = {
                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                        onEyesFreeAnnounce("Настройка состава Совета", false)
                        onOpenCouncil()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(spatialCardHighlightColor())
                        .border(1.dp, PurpleTheorist.copy(alpha = 0.55f), CircleShape)
                        .focusOrderNode(
                            requester = focusGraph.arenaChangeCouncil,
                            next = if (isWideSpatialLayout) focusGraph.dagCanvas else focusGraph.navArena,
                            previous = focusGraph.arenaPrimaryAction,
                            left = focusGraph.arenaPrimaryAction,
                            right = if (isWideSpatialLayout) focusGraph.dagCopyThesis else FocusRequester.Cancel,
                            up = focusGraph.arenaCouncil,
                            down = bottomNavTarget
                        )
                        .spatialInteractive(accentColor = PurpleTheorist, shape = CircleShape)
                        .testTag("arena_change_models_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Сменить состав моделей",
                        tint = PurpleTheorist,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
