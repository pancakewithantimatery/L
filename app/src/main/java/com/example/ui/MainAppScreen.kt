package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ModelCouncilConfigDialog
import com.example.ui.components.NewSessionDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.CouncilScreen
import com.example.ui.screens.HypothesesScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

enum class ProgressorTab(val title: String, val icon: ImageVector) {
    ARENA("Арена", Icons.Default.ChatBubbleOutline),
    HYPOTHESES("Гипотезы", Icons.Default.Biotech),
    COUNCIL("Совет", Icons.Default.Groups),
    TOPICS("Темы", Icons.Default.Folder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ProgressorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ProgressorTab.ARENA) }

    // Dialog visibility states
    var showCouncilConfigDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }

    // Observe ViewModel states safely with Lifecycle
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val currentCycles by viewModel.currentCycles.collectAsStateWithLifecycle()
    val currentHypotheses by viewModel.currentHypotheses.collectAsStateWithLifecycle()
    val currentScenarios by viewModel.currentScenarios.collectAsStateWithLifecycle()
    val isCycleRunning by viewModel.isCycleRunning.collectAsStateWithLifecycle()
    val progressState by viewModel.progressState.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isFetchingModels by viewModel.isFetchingModels.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()

    val selectedTheoristModel by viewModel.selectedTheoristModel.collectAsStateWithLifecycle()
    val selectedSkepticModel by viewModel.selectedSkepticModel.collectAsStateWithLifecycle()
    val selectedSynthesizerModel by viewModel.selectedSynthesizerModel.collectAsStateWithLifecycle()

    // BackHandler: Navigate back to Arena from other tabs
    BackHandler(enabled = currentTab != ProgressorTab.ARENA) {
        currentTab = ProgressorTab.ARENA
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .border(1.dp, CyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ПРОГРЕССОР",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (apiKey.isNotEmpty()) CyanAccent else AmberSkeptic)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (apiKey.isNotEmpty() && !isSimulationMode) "VENICE API" else "ПРОТОТИП",
                                        color = VoidDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Мультиагентная эволюция научных гипотез",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNewSessionDialog = true },
                        modifier = Modifier.testTag("top_add_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Новая тема",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки Venice API",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpace,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepSpace,
                contentColor = CyanAccent,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                ProgressorTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == ProgressorTab.HYPOTHESES && currentHypotheses.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = PurpleTheorist,
                                            contentColor = VoidDark
                                        ) {
                                            Text("${currentHypotheses.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoidDark,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ProgressorTab.ARENA -> {
                    ArenaScreen(
                        session = activeSession,
                        messages = currentMessages,
                        isCycleRunning = isCycleRunning,
                        progressState = progressState,
                        onOpenCouncil = { showCouncilConfigDialog = true },
                        onLaunchNextCycle = { viewModel.launchNextCycle(null) },
                        onSendHumanMessage = { viewModel.sendHumanIntervention(it) },
                        onExportReport = {
                            val report = viewModel.generateFullSessionReport()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Отчет Прогрессора: ${activeSession?.topic ?: ""}")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться отчетом Прогрессора"))
                        }
                    )
                }
                ProgressorTab.HYPOTHESES -> {
                    HypothesesScreen(
                        hypotheses = currentHypotheses,
                        scenarios = currentScenarios,
                        cycles = currentCycles
                    )
                }
                ProgressorTab.COUNCIL -> {
                    CouncilScreen(
                        session = activeSession,
                        cycles = currentCycles,
                        theoristModel = selectedTheoristModel,
                        skepticModel = selectedSkepticModel,
                        synthesizerModel = selectedSynthesizerModel,
                        onOpenCouncilConfig = { showCouncilConfigDialog = true },
                        onFastLaunchNextCycle = { viewModel.launchNextCycle(null) }
                    )
                }
                ProgressorTab.TOPICS -> {
                    SessionsScreen(
                        sessions = allSessions,
                        activeSessionId = activeSessionId,
                        onSelectSession = { id ->
                            viewModel.selectSession(id)
                            currentTab = ProgressorTab.ARENA
                        },
                        onDeleteSession = { id -> viewModel.deleteSession(id) },
                        onOpenNewSessionDialog = { showNewSessionDialog = true }
                    )
                }
            }
        }
    }

    // Model Council Configuration Dialog
    if (showCouncilConfigDialog) {
        ModelCouncilConfigDialog(
            availableModels = availableModels,
            currentTheorist = selectedTheoristModel,
            currentSkeptic = selectedSkepticModel,
            currentSynthesizer = selectedSynthesizerModel,
            onDismiss = { showCouncilConfigDialog = false },
            onLaunchCycle = { theorist, skeptic, synth, guidance ->
                viewModel.setModelsForNextCycle(theorist, skeptic, synth)
                showCouncilConfigDialog = false
                currentTab = ProgressorTab.ARENA
                viewModel.launchNextCycle(guidance)
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = apiKey,
            isSimulationMode = isSimulationMode,
            isFetchingModels = isFetchingModels,
            availableModelsCount = availableModels.size,
            onSaveApiKey = { key ->
                viewModel.saveApiKey(key)
                Toast.makeText(context, "Ключ Venice.ai сохранен", Toast.LENGTH_SHORT).show()
                showSettingsDialog = false
            },
            onToggleSimulation = { enabled ->
                viewModel.toggleSimulationMode(enabled)
            },
            onRefreshModels = {
                viewModel.refreshModelsFromVenice()
                Toast.makeText(context, "Запрос моделей Venice.ai...", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // New Session Dialog
    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onCreateSession = { topic, domain ->
                viewModel.createNewSession(topic, domain)
                showNewSessionDialog = false
                currentTab = ProgressorTab.ARENA
            }
        )
    }
}
