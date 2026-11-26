package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExportMarkdownDialog
import com.example.ui.components.ModelCouncilConfigDialog
import com.example.ui.components.NewSessionDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AiLabScreen
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.CouncilScreen
import com.example.ui.screens.HypothesesScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

enum class ProgressorTab(val title: String, val icon: ImageVector) {
    ARENA("Арена", Icons.Default.ChatBubbleOutline),
    HYPOTHESES("Гипотезы", Icons.Default.Biotech),
    AI_LAB("Лаборатория", Icons.Default.AutoAwesome),
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
    val clipboardManager = LocalClipboardManager.current
    var currentTab by remember { mutableStateOf(ProgressorTab.ARENA) }

    // Dialog visibility states
    var showCouncilConfigDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

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

    // Providers & Keys
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val ollamaHost by viewModel.ollamaHost.collectAsStateWithLifecycle()

    // Models per provider & combined
    val availableVeniceModels by viewModel.availableVeniceModels.collectAsStateWithLifecycle()
    val availableGeminiModels by viewModel.availableGeminiModels.collectAsStateWithLifecycle()
    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsStateWithLifecycle()
    val availableOllamaModels by viewModel.availableOllamaModels.collectAsStateWithLifecycle()
    val allAvailableModels by viewModel.allAvailableModels.collectAsStateWithLifecycle()

    val isFetchingModels by viewModel.isFetchingModels.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()

    val selectedTheoristModel by viewModel.selectedTheoristModel.collectAsStateWithLifecycle()
    val selectedSkepticModel by viewModel.selectedSkepticModel.collectAsStateWithLifecycle()
    val selectedSynthesizerModel by viewModel.selectedSynthesizerModel.collectAsStateWithLifecycle()

    // SAF Document Creator for saving .md file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.exportMarkdownToFile(context, uri)
            if (success) {
                Toast.makeText(context, "Отчет .md сохранен на устройство!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Не удалось записать файл .md", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                                        text = if (apiKey.isNotEmpty() && !isSimulationMode) "4x LLM API" else "ПРОТОТИП",
                                        color = VoidDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Venice • Gemini • OpenRouter • Ollama",
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
                            contentDescription = "Настройки API провайдеров",
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
                                fontSize = 10.sp,
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
                        onExportReport = { showExportDialog = true }
                    )
                }
                ProgressorTab.HYPOTHESES -> {
                    HypothesesScreen(
                        hypotheses = currentHypotheses,
                        scenarios = currentScenarios,
                        cycles = currentCycles
                    )
                }
                ProgressorTab.AI_LAB -> {
                    AiLabScreen(
                        session = activeSession,
                        currentHypothesis = currentHypotheses.firstOrNull(),
                        geminiModels = availableGeminiModels,
                        openRouterModels = availableOpenRouterModels,
                        ollamaModels = availableOllamaModels,
                        isGeminiKeyConfigured = geminiApiKey.isNotEmpty(),
                        isOpenRouterKeyConfigured = openRouterApiKey.isNotEmpty(),
                        ollamaHostUrl = ollamaHost,
                        onSaveOllamaHost = { viewModel.saveOllamaHost(it) },
                        onRefreshGemini = { viewModel.refreshGeminiModels() },
                        onRefreshOpenRouter = { viewModel.refreshOpenRouterModels() },
                        onRefreshOllama = { viewModel.refreshOllamaModels() },
                        onRunAnalysis = { provider, model, prompt, onResult ->
                            viewModel.runAiLabAnalysis(provider, model, prompt, onResult)
                        },
                        onInjectToSession = { insightText ->
                            viewModel.sendHumanIntervention("[AI Lab Инсайт]: $insightText")
                            currentTab = ProgressorTab.ARENA
                        }
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

    // Export Markdown Dialog
    if (showExportDialog) {
        ExportMarkdownDialog(
            session = activeSession,
            hypothesesCount = currentHypotheses.size,
            scenariosCount = currentScenarios.size,
            onSaveFile = {
                val cleanTopic = (activeSession?.topic ?: "session")
                    .take(20)
                    .replace("[^a-zA-Z0-9а-яА-Я_]".toRegex(), "_")
                val filename = "progressor_${cleanTopic}_${System.currentTimeMillis() % 10000}.md"
                showExportDialog = false
                createDocumentLauncher.launch(filename)
            },
            onShare = {
                showExportDialog = false
                val report = viewModel.generateMarkdownReport()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_SUBJECT, "Отчет Прогрессора: ${activeSession?.topic ?: ""}")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться научным отчетом .md"))
            },
            onCopyClipboard = {
                showExportDialog = false
                val report = viewModel.generateMarkdownReport()
                clipboardManager.setText(AnnotatedString(report))
                Toast.makeText(context, "Отчет Markdown скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Model Council Configuration Dialog (Uses all 4 providers: Venice, Gemini, OpenRouter, Ollama)
    if (showCouncilConfigDialog) {
        ModelCouncilConfigDialog(
            availableModels = allAvailableModels,
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

    // Multi-Provider Settings Dialog (Venice, Gemini, OpenRouter, Ollama)
    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = apiKey,
            currentGeminiApiKey = geminiApiKey,
            currentOpenRouterApiKey = openRouterApiKey,
            currentOllamaHost = ollamaHost,
            isSimulationMode = isSimulationMode,
            isFetchingModels = isFetchingModels,
            veniceModelsCount = availableVeniceModels.size,
            geminiModelsCount = availableGeminiModels.size,
            openRouterModelsCount = availableOpenRouterModels.size,
            ollamaModelsCount = availableOllamaModels.size,
            onSaveApiKey = { key ->
                viewModel.saveApiKey(key)
                Toast.makeText(context, "Ключ Venice.ai сохранен", Toast.LENGTH_SHORT).show()
            },
            onSaveGeminiApiKey = { key ->
                viewModel.saveGeminiApiKey(key)
                Toast.makeText(context, "Ключ Gemini сохранен", Toast.LENGTH_SHORT).show()
            },
            onSaveOpenRouterApiKey = { key ->
                viewModel.saveOpenRouterApiKey(key)
                Toast.makeText(context, "Ключ OpenRouter сохранен", Toast.LENGTH_SHORT).show()
            },
            onSaveOllamaHost = { host ->
                viewModel.saveOllamaHost(host)
                Toast.makeText(context, "Адрес Ollama сохранен", Toast.LENGTH_SHORT).show()
            },
            onToggleSimulation = { enabled ->
                viewModel.toggleSimulationMode(enabled)
            },
            onRefreshModels = {
                viewModel.refreshAllProviders()
                Toast.makeText(context, "Обновление моделей со всех провайдеров...", Toast.LENGTH_SHORT).show()
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
