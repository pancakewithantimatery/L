package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.KeepBufferExporter
import com.example.data.KeepBufferPayload
import com.example.domain.VoiceCommand
import com.example.ui.components.ArxivSearchDialog
import com.example.ui.components.CustomRoleDesignerDialog
import com.example.ui.components.ExportFormat
import com.example.ui.components.ExportMarkdownDialog
import com.example.ui.components.HypothesisDagGraph
import com.example.ui.components.ModelCouncilConfigDialog
import com.example.ui.components.NewSessionDialog
import com.example.ui.components.VoiceCommandHud
import com.example.ui.screens.AiLabScreen
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.CouncilScreen
import com.example.ui.screens.HypothesesScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.spatial.LocalFocusGraph
import com.example.ui.spatial.focusOrderNode
import com.example.ui.spatial.performSpatialHaptic
import com.example.ui.spatial.rememberProgressorFocusGraph
import com.example.ui.spatial.requestFocusSafely
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrueBlackOled
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialSurfaceColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ProgressorTab(val title: String, val icon: ImageVector) {
    ARENA("Арена", Icons.Default.ChatBubbleOutline),
    HYPOTHESES("Гипотезы", Icons.Default.Biotech),
    AI_LAB("Лаб", Icons.Default.AutoAwesome),
    COUNCIL("Совет", Icons.Default.Groups),
    TOPICS("Темы", Icons.Default.Folder)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainAppScreen(
    viewModel: ProgressorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current
    val focusGraph = rememberProgressorFocusGraph()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun dispatchKeepExport(payload: KeepBufferPayload?) {
        if (payload == null) {
            Toast.makeText(context, "Нет активной сессии для буфера Keep", Toast.LENGTH_SHORT).show()
            return
        }
        val result = KeepBufferExporter.exportToGoogleKeep(context, payload)
        val statusMsg = when {
            result.sentDirectlyToKeep -> "Отправлено в Google Keep и скопировано в буфер"
            result.fallbackChooserUsed -> "Keep не найден — открыт Share Sheet (скопировано в буфер)"
            else -> "Резюме для Keep скопировано в буфер обмена"
        }
        viewModel.announceEyesFree(
            if (result.sentDirectlyToKeep) "Отправлено в Keep" else "Резюме скопировано",
            false
        )
        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = statusMsg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Persist navigation state safely across multi-window resizes / display transitions
    var currentTabName by rememberSaveable { mutableStateOf(ProgressorTab.ARENA.name) }
    val currentTab = remember(currentTabName) {
        runCatching { ProgressorTab.valueOf(currentTabName) }.getOrDefault(ProgressorTab.ARENA)
    }
    fun selectTab(tab: ProgressorTab, focusContentAfterSwitch: Boolean = false) {
        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
        currentTabName = tab.name
        if (focusContentAfterSwitch) {
            coroutineScope.launch {
                delay(60)
                focusGraph.contentEntryForTab(tab.ordinal).requestFocusSafely()
            }
        }
    }

    // Screen and dialog visibility states
    var showCouncilConfigDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showNewSessionDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showArxivDialog by rememberSaveable { mutableStateOf(false) }
    var arxivInitialQuery by rememberSaveable { mutableStateOf("") }
    var showRoleDesignerDialog by rememberSaveable { mutableStateOf(false) }
    var showVoiceHelpSheet by rememberSaveable { mutableStateOf(false) }
    var pendingHandsFreeAfterPermission by remember { mutableStateOf(false) }

    // Observe ViewModel states safely with Lifecycle
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val currentCycles by viewModel.currentCycles.collectAsStateWithLifecycle()
    val currentHypotheses by viewModel.currentHypotheses.collectAsStateWithLifecycle()
    val currentScenarios by viewModel.currentScenarios.collectAsStateWithLifecycle()
    val currentCitations by viewModel.currentCitations.collectAsStateWithLifecycle()
    val customRoles by viewModel.customRoles.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val isCycleRunning by viewModel.isCycleRunning.collectAsStateWithLifecycle()
    val progressState by viewModel.progressState.collectAsStateWithLifecycle()

    // arXiv Search states
    val arxivResults by viewModel.arxivSearchResults.collectAsStateWithLifecycle()
    val isSearchingArxiv by viewModel.isSearchingArxiv.collectAsStateWithLifecycle()
    val arxivError by viewModel.arxivError.collectAsStateWithLifecycle()

    // Providers & Keys
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val ollamaHost by viewModel.ollamaHost.collectAsStateWithLifecycle()

    // XREAL Spatial Settings
    val xrealSpatialMode by viewModel.xrealSpatialMode.collectAsStateWithLifecycle()
    val xrealAutoDetect by viewModel.xrealAutoDetect.collectAsStateWithLifecycle()
    val xrealZeroGravityLock by viewModel.xrealZeroGravityLock.collectAsStateWithLifecycle()
    val xrealOverscanDp by viewModel.xrealOverscanDp.collectAsStateWithLifecycle()
    val xrealEyesFreeFeedback by viewModel.xrealEyesFreeFeedback.collectAsStateWithLifecycle()

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
    val createMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.exportMarkdownToFile(context, uri)
            Toast.makeText(
                context,
                if (success) "Отчет .md сохранен на устройство!" else "Не удалось записать файл .md",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // SAF Document Creator for saving .tex LaTeX file
    val createLatexLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-tex")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.exportLatexToFile(context, uri)
            Toast.makeText(
                context,
                if (success) "Препринт LaTeX (.tex) сохранен для Overleaf!" else "Не удалось записать файл .tex",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Runtime RECORD_AUDIO Permission Launcher for Android SpeechRecognizer
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingHandsFreeAfterPermission) {
                pendingHandsFreeAfterPermission = false
                viewModel.setHandsFreeVoiceMode(true)
            } else {
                viewModel.toggleVoiceListening()
            }
        } else {
            pendingHandsFreeAfterPermission = false
            viewModel.reportVoicePermissionDenied()
            Toast.makeText(
                context,
                "Разрешите доступ к микрофону для голосового управления XREAL AR",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestOrStartVoice(enableHandsFree: Boolean = false) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (enableHandsFree) {
                viewModel.setHandsFreeVoiceMode(!voiceState.isHandsFreeEnabled)
            } else if (spatialConfig.isSpatialModeActive && !voiceState.isListening && !voiceState.isHandsFreeEnabled) {
                // In XREAL AR mode, tapping mic immediately starts continuous hands-free listening
                viewModel.setHandsFreeVoiceMode(true)
            } else {
                viewModel.toggleVoiceListening()
            }
        } else {
            pendingHandsFreeAfterPermission = enableHandsFree || spatialConfig.isSpatialModeActive
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Collect & execute parsed voice commands from SpeechRecognizer
    LaunchedEffect(viewModel) {
        viewModel.voiceCommands.collect { command ->
            performSpatialHaptic(
                context = context,
                hapticFeedback = haptic,
                enabled = spatialConfig.isEyesFreeFeedbackEnabled,
                strong = command is VoiceCommand.LaunchDebateCycle || command is VoiceCommand.HumanIntervention
            )
            when (command) {
                is VoiceCommand.NavigateTab -> {
                    val tab = ProgressorTab.values().getOrElse(command.tabIndex) { ProgressorTab.ARENA }
                    showSettingsScreen = false
                    selectTab(tab, focusContentAfterSwitch = true)
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.OpenSettings -> {
                    showSettingsScreen = true
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.CloseOverlay -> {
                    when {
                        showCouncilConfigDialog -> showCouncilConfigDialog = false
                        showArxivDialog -> showArxivDialog = false
                        showExportDialog -> showExportDialog = false
                        showNewSessionDialog -> showNewSessionDialog = false
                        showRoleDesignerDialog -> showRoleDesignerDialog = false
                        showSettingsScreen -> showSettingsScreen = false
                        showVoiceHelpSheet -> showVoiceHelpSheet = false
                        currentTab != ProgressorTab.ARENA -> selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                    }
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.LaunchDebateCycle -> {
                    showSettingsScreen = false
                    selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = false)
                    if (!isCycleRunning) {
                        viewModel.launchNextCycle(null)
                        viewModel.announceEyesFree(command.confirmationPhrase, false)
                    } else {
                        viewModel.announceEyesFree("Цикл уже выполняется", false)
                    }
                }
                is VoiceCommand.HumanIntervention -> {
                    showSettingsScreen = false
                    selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = false)
                    viewModel.sendHumanIntervention(command.guidance)
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                is VoiceCommand.CreateNewSession -> {
                    showSettingsScreen = false
                    if (!command.topic.isNullOrBlank()) {
                        viewModel.createNewSession(command.topic, activeSession?.domain ?: "Междисциплинарный синтез")
                        selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                    } else {
                        showNewSessionDialog = true
                    }
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.OpenCouncilConfig -> {
                    showSettingsScreen = false
                    showCouncilConfigDialog = true
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                is VoiceCommand.SearchArxiv -> {
                    showSettingsScreen = false
                    val query = command.query ?: activeSession?.topic.orEmpty()
                    arxivInitialQuery = query
                    showArxivDialog = true
                    if (query.isNotBlank()) {
                        viewModel.searchArxiv(query)
                    }
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.TtsPlay -> {
                    selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = false)
                    viewModel.toggleTtsPlayback(null)
                }
                VoiceCommand.TtsPause -> {
                    viewModel.ttsStop()
                    viewModel.announceEyesFree("Пауза", true)
                }
                VoiceCommand.TtsNext -> viewModel.ttsNext()
                VoiceCommand.TtsPrevious -> viewModel.ttsPrevious()
                VoiceCommand.ExportToKeep -> {
                    dispatchKeepExport(viewModel.buildKeepBufferPayload())
                }
                VoiceCommand.CopySynthesis -> {
                    val payload = viewModel.buildCrossPostSynthesisText()
                    if (payload.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(payload))
                        viewModel.announceEyesFree(command.confirmationPhrase, false)
                        Toast.makeText(context, "Синтез скопирован голосовой командой", Toast.LENGTH_SHORT).show()
                    }
                }
                is VoiceCommand.SetXrealMode -> {
                    viewModel.setXrealSpatialMode(command.enabled)
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.StopHandsFree -> {
                    showVoiceHelpSheet = false
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                VoiceCommand.ShowHelp -> {
                    showVoiceHelpSheet = true
                    viewModel.announceEyesFree(command.confirmationPhrase, false)
                }
                is VoiceCommand.Unrecognized -> {
                    if (command.rawText.isNotBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = "Не распознано: «${command.rawText}». Скажите «Помощь» для списка команд.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalFocusGraph provides focusGraph) {
        // Root container applies strict #000000 True Black + Birdbath Safe Area Overscan Padding (24..32dp)
        // plus global Eyes-Free keyboard shortcuts for instant focus & tab routing
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(spatialCanvasColor())
                .padding(spatialConfig.overscanPadding)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown || showSettingsScreen) return@onPreviewKeyEvent false

                    val modPressed = keyEvent.isAltPressed || keyEvent.isCtrlPressed
                    when {
                        // Alt/Ctrl + 1..5: Instant Eyes-Free Tab Switching + Focus Entry
                        modPressed && keyEvent.key == Key.One -> {
                            selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                            viewModel.announceEyesFree("Арена", false)
                            true
                        }
                        modPressed && keyEvent.key == Key.Two -> {
                            selectTab(ProgressorTab.HYPOTHESES, focusContentAfterSwitch = true)
                            viewModel.announceEyesFree("Гипотезы", false)
                            true
                        }
                        modPressed && keyEvent.key == Key.Three -> {
                            selectTab(ProgressorTab.AI_LAB, focusContentAfterSwitch = true)
                            viewModel.announceEyesFree("Лаборатория", false)
                            true
                        }
                        modPressed && keyEvent.key == Key.Four -> {
                            selectTab(ProgressorTab.COUNCIL, focusContentAfterSwitch = true)
                            viewModel.announceEyesFree("Совет", false)
                            true
                        }
                        modPressed && keyEvent.key == Key.Five -> {
                            selectTab(ProgressorTab.TOPICS, focusContentAfterSwitch = true)
                            viewModel.announceEyesFree("Темы", false)
                            true
                        }
                        // Ctrl+L or F5: Launch Next Evolution Cycle eyes-free
                        (keyEvent.isCtrlPressed && keyEvent.key == Key.L) || keyEvent.key == Key.F5 -> {
                            if (!isCycleRunning) {
                                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                                viewModel.announceEyesFree("Эволюционный цикл запущен", false)
                                viewModel.launchNextCycle(null)
                            }
                            true
                        }
                        // Ctrl+Shift+C: Cross-post synthesis copy from anywhere
                        keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.C -> {
                            val payload = viewModel.buildCrossPostSynthesisText()
                            if (payload.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(payload))
                                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                viewModel.announceEyesFree("Синтез скопирован", false)
                                Toast.makeText(context, "Синтез скопирован в буфер", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                        // Ctrl+K: Instant Eyes-Free Export of Operational Buffer to Google Keep
                        keyEvent.isCtrlPressed && keyEvent.key == Key.K -> {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                            dispatchKeepExport(viewModel.buildKeepBufferPayload())
                            true
                        }
                        // Ctrl+M / Ctrl+Shift+M: Hands-Free SpeechRecognizer Voice Control
                        keyEvent.isCtrlPressed && keyEvent.key == Key.M -> {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                            requestOrStartVoice(enableHandsFree = keyEvent.isShiftPressed)
                            true
                        }
                        // Ctrl+G: Focus Live DAG Graph directly
                        keyEvent.isCtrlPressed && keyEvent.key == Key.G -> {
                            if (!focusGraph.dagCanvas.requestFocusSafely()) {
                                selectTab(ProgressorTab.HYPOTHESES, focusContentAfterSwitch = false)
                                coroutineScope.launch {
                                    delay(80)
                                    focusGraph.dagCanvas.requestFocusSafely()
                                }
                            }
                            viewModel.announceEyesFree("Граф гипотез", false)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            if (showSettingsScreen) {
                SettingsScreen(
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
                    xrealSpatialMode = xrealSpatialMode,
                    xrealAutoDetect = xrealAutoDetect,
                    xrealZeroGravityLock = xrealZeroGravityLock,
                    xrealOverscanDp = xrealOverscanDp,
                    xrealEyesFreeFeedback = xrealEyesFreeFeedback,
                    onToggleXrealSpatialMode = { enabled ->
                        performSpatialHaptic(context, haptic, xrealEyesFreeFeedback, strong = true)
                        viewModel.setXrealSpatialMode(enabled)
                        viewModel.announceEyesFree(if (enabled) "Режим XREAL включён" else "Режим XREAL выключен", false)
                    },
                    onToggleXrealAutoDetect = { viewModel.setXrealAutoDetect(it) },
                    onToggleXrealZeroGravityLock = { viewModel.setXrealZeroGravityLock(it) },
                    onSelectXrealOverscanDp = { viewModel.setXrealOverscanDp(it) },
                    onToggleXrealEyesFreeFeedback = { viewModel.setXrealEyesFreeFeedback(it) },
                    onSaveApiKey = { viewModel.saveApiKey(it) },
                    onSaveGeminiApiKey = { viewModel.saveGeminiApiKey(it) },
                    onSaveOpenRouterApiKey = { viewModel.saveOpenRouterApiKey(it) },
                    onSaveOllamaHost = { viewModel.saveOllamaHost(it) },
                    onToggleSimulation = { viewModel.toggleSimulationMode(it) },
                    onRefreshAllModels = { viewModel.refreshAllProviders() },
                    onTestVenice = { key, cb -> viewModel.testVeniceConnection(key, cb) },
                    onTestOpenRouter = { key, cb -> viewModel.testOpenRouterConnection(key, cb) },
                    onTestOllama = { host, cb -> viewModel.testOllamaConnection(host, cb) },
                    onTestGemini = { key, cb -> viewModel.testGeminiConnection(key, cb) },
                    onBack = { showSettingsScreen = false }
                )
                return@Box
            }

            // BackHandler: Navigate back to Arena from other tabs
            BackHandler(enabled = currentTab != ProgressorTab.ARENA) {
                selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
            }

            // Adaptive container-based layout for Beam Pro Freeform / DeX 16:9 / Handheld
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideSpatialLayout = maxWidth >= 720.dp
                val activeTabContentEntry = focusGraph.contentEntryForTab(currentTab.ordinal)
                val activeTabContentBottom = focusGraph.contentBottomForTab(currentTab.ordinal)
                val activeNavRequester = focusGraph.navRequesterForIndex(currentTab.ordinal)

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = spatialCanvasColor(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(CyanAccent.copy(alpha = 0.2f))
                                            .border(1.dp, CyanAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ПРОГРЕССОР",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (apiKey.isNotEmpty() && !isSimulationMode) CyanAccent else AmberSkeptic)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (apiKey.isNotEmpty() && !isSimulationMode) "4x LLM" else "SIM",
                                                color = VoidDark,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (spatialConfig.isSpatialModeActive) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TrueBlackOled)
                                                    .border(1.dp, EmeraldHuman, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (spatialConfig.hasExternalDisplay) "XREAL EXT" else "OLED XR",
                                                    color = EmeraldHuman,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                Row(
                                    modifier = Modifier
                                        .focusGroup()
                                        .testTag("top_bar_focus_group"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Top Focus Node 0: Hands-Free Voice Control (SpeechRecognizer)
                                    IconButton(
                                        onClick = {
                                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                                            requestOrStartVoice(enableHandsFree = false)
                                        },
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = focusGraph.topVoiceMic,
                                                next = focusGraph.topXrealToggle,
                                                previous = if (isWideSpatialLayout) activeNavRequester else FocusRequester.Default,
                                                left = if (isWideSpatialLayout) activeNavRequester else FocusRequester.Cancel,
                                                right = focusGraph.topXrealToggle,
                                                up = FocusRequester.Cancel,
                                                down = activeTabContentEntry
                                            )
                                            .spatialInteractive(
                                                accentColor = if (voiceState.isListening || voiceState.isHandsFreeEnabled) EmeraldHuman else CyanAccent,
                                                shape = CircleShape
                                            )
                                            .testTag("top_voice_mic_button")
                                    ) {
                                        Icon(
                                            imageVector = if (voiceState.isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                            contentDescription = if (voiceState.isListening) {
                                                "Остановить голосовое управление"
                                            } else {
                                                "Голосовое управление XREAL AR (SpeechRecognizer)"
                                            },
                                            tint = when {
                                                voiceState.isListening -> EmeraldHuman
                                                voiceState.isHandsFreeEnabled -> CyanAccent
                                                else -> TextSecondary
                                            }
                                        )
                                    }

                                    // Top Focus Node 1: XREAL Spatial Toggle
                                    IconButton(
                                        onClick = {
                                            val nextState = !xrealSpatialMode
                                            performSpatialHaptic(context, haptic, xrealEyesFreeFeedback, strong = true)
                                            viewModel.setXrealSpatialMode(nextState)
                                            viewModel.announceEyesFree(
                                                if (nextState) "Режим XREAL включён" else "Режим XREAL выключен",
                                                false
                                            )
                                            Toast.makeText(
                                                context,
                                                if (nextState) "XREAL True Black OLED & Safe Area активны" else "Обычный режим дисплея",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = focusGraph.topXrealToggle,
                                                next = focusGraph.topNewSession,
                                                previous = focusGraph.topVoiceMic,
                                                left = focusGraph.topVoiceMic,
                                                right = focusGraph.topNewSession,
                                                up = FocusRequester.Cancel,
                                                down = activeTabContentEntry
                                            )
                                            .spatialInteractive(accentColor = EmeraldHuman, shape = CircleShape)
                                            .testTag("top_xreal_spatial_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewInAr,
                                            contentDescription = "Переключить режим очков XREAL Air 2 Pro",
                                            tint = if (spatialConfig.isSpatialModeActive) EmeraldHuman else TextSecondary
                                        )
                                    }

                                    // Top Focus Node 2: New Research Topic
                                    IconButton(
                                        onClick = {
                                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                            showNewSessionDialog = true
                                        },
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = focusGraph.topNewSession,
                                                next = focusGraph.topSettings,
                                                previous = focusGraph.topXrealToggle,
                                                left = focusGraph.topXrealToggle,
                                                right = focusGraph.topSettings,
                                                up = FocusRequester.Cancel,
                                                down = activeTabContentEntry
                                            )
                                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                                            .testTag("top_add_session_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Новая тема",
                                            tint = CyanAccent
                                        )
                                    }

                                    // Top Focus Node 3: Settings
                                    IconButton(
                                        onClick = {
                                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                            showSettingsScreen = true
                                        },
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = focusGraph.topSettings,
                                                next = activeTabContentEntry,
                                                previous = focusGraph.topNewSession,
                                                left = focusGraph.topNewSession,
                                                right = FocusRequester.Cancel,
                                                up = FocusRequester.Cancel,
                                                down = activeTabContentEntry
                                            )
                                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                                            .testTag("top_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Настройки API провайдеров и XREAL",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = spatialSurfaceColor(),
                                titleContentColor = TextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        if (!isWideSpatialLayout) {
                            val tabs = ProgressorTab.values()
                            NavigationBar(
                                containerColor = spatialSurfaceColor(),
                                contentColor = CyanAccent,
                                tonalElevation = if (spatialConfig.isTrueBlackOled) 0.dp else 8.dp,
                                modifier = Modifier
                                    .focusGroup()
                                    .focusProperties {
                                        enter = { activeNavRequester }
                                    }
                                    .testTag("bottom_navigation_bar")
                            ) {
                                tabs.forEachIndexed { idx, tab ->
                                    val selected = currentTab == tab
                                    val selfReq = focusGraph.navRequesterForIndex(idx)
                                    val prevReq = if (idx > 0) focusGraph.navRequesterForIndex(idx - 1) else activeTabContentBottom
                                    val nextReq = if (idx < tabs.lastIndex) focusGraph.navRequesterForIndex(idx + 1) else focusGraph.topXrealToggle
                                    val leftReq = if (idx > 0) focusGraph.navRequesterForIndex(idx - 1) else FocusRequester.Cancel
                                    val rightReq = if (idx < tabs.lastIndex) focusGraph.navRequesterForIndex(idx + 1) else FocusRequester.Cancel

                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectTab(tab) },
                                        alwaysShowLabel = false,
                                        icon = {
                                            TabNavIcon(tab = tab, hypothesesCount = currentHypotheses.size)
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = VoidDark,
                                            selectedTextColor = CyanAccent,
                                            indicatorColor = CyanAccent,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = selfReq,
                                                next = nextReq,
                                                previous = prevReq,
                                                left = leftReq,
                                                right = rightReq,
                                                up = activeTabContentBottom,
                                                down = FocusRequester.Cancel
                                            )
                                            .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(12.dp))
                                            .testTag("tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(spatialCanvasColor())
                    ) {
                        // Wide 16:9 Spatial NavigationRail with Deterministic FocusGroup
                        if (isWideSpatialLayout) {
                            val tabs = ProgressorTab.values()
                            NavigationRail(
                                containerColor = spatialSurfaceColor(),
                                contentColor = CyanAccent,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .border(0.5.dp, spatialBorderColor(BorderSubtle))
                                    .focusGroup()
                                    .focusProperties {
                                        enter = { activeNavRequester }
                                    }
                                    .testTag("spatial_navigation_rail")
                            ) {
                                tabs.forEachIndexed { idx, tab ->
                                    val selected = currentTab == tab
                                    val selfReq = focusGraph.navRequesterForIndex(idx)
                                    val upReq = if (idx > 0) focusGraph.navRequesterForIndex(idx - 1) else focusGraph.topXrealToggle
                                    val downReq = if (idx < tabs.lastIndex) focusGraph.navRequesterForIndex(idx + 1) else FocusRequester.Cancel
                                    val nextReq = if (idx < tabs.lastIndex) focusGraph.navRequesterForIndex(idx + 1) else activeTabContentEntry
                                    val prevReq = if (idx > 0) focusGraph.navRequesterForIndex(idx - 1) else focusGraph.topSettings

                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectTab(tab) },
                                        alwaysShowLabel = false,
                                        icon = {
                                            TabNavIcon(tab = tab, hypothesesCount = currentHypotheses.size)
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = VoidDark,
                                            selectedTextColor = CyanAccent,
                                            indicatorColor = CyanAccent,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier
                                            .focusOrderNode(
                                                requester = selfReq,
                                                next = nextReq,
                                                previous = prevReq,
                                                up = upReq,
                                                down = downReq,
                                                left = FocusRequester.Cancel,
                                                right = activeTabContentEntry
                                            )
                                            .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(12.dp))
                                            .testTag("rail_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Main Content Area + XREAL AR Voice Command HUD
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            VoiceCommandHud(
                                voiceState = voiceState,
                                showHelpSheet = showVoiceHelpSheet,
                                onToggleListen = { requestOrStartVoice(enableHandsFree = false) },
                                onToggleHandsFree = { enabled ->
                                    if (enabled) {
                                        requestOrStartVoice(enableHandsFree = true)
                                    } else {
                                        viewModel.setHandsFreeVoiceMode(false)
                                    }
                                },
                                onToggleHelp = { showVoiceHelpSheet = !showVoiceHelpSheet },
                                onQuickVoiceSample = { phrase ->
                                    viewModel.dispatchVoiceSamplePhrase(phrase)
                                },
                                onDismiss = {
                                    showVoiceHelpSheet = false
                                    viewModel.stopVoiceListening()
                                    viewModel.clearVoiceStatusBanner()
                                }
                            )

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (currentTab) {
                                ProgressorTab.ARENA -> {
                                    if (isWideSpatialLayout) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            ArenaScreen(
                                                session = activeSession,
                                                messages = currentMessages,
                                                citationsCount = currentCitations.size,
                                                ttsState = ttsState,
                                                isCycleRunning = isCycleRunning,
                                                progressState = progressState,
                                                isWideSpatialLayout = true,
                                                onOpenCouncil = { showCouncilConfigDialog = true },
                                                onOpenArxiv = {
                                                    arxivInitialQuery = activeSession?.topic ?: ""
                                                    showArxivDialog = true
                                                },
                                                onCopyCrossPostSynthesis = { viewModel.buildCrossPostSynthesisText() },
                                                onLaunchNextCycle = { viewModel.launchNextCycle(null) },
                                                onSendHumanMessage = { viewModel.sendHumanIntervention(it) },
                                                onExportReport = { showExportDialog = true },
                                                onExportToKeep = { dispatchKeepExport(viewModel.buildKeepBufferPayload()) },
                                                onExportMessageToKeep = { msg ->
                                                    dispatchKeepExport(viewModel.buildMessageKeepBufferPayload(msg))
                                                },
                                                onTtsPlayPause = { msgId -> viewModel.toggleTtsPlayback(msgId) },
                                                onTtsNext = { viewModel.ttsNext() },
                                                onTtsPrev = { viewModel.ttsPrevious() },
                                                onTtsCycleSpeed = { viewModel.ttsCycleSpeed() },
                                                onTtsStop = { viewModel.ttsStop() },
                                                onEyesFreeAnnounce = { phrase, force -> viewModel.announceEyesFree(phrase, force) },
                                                modifier = Modifier.weight(0.56f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .fillMaxHeight()
                                                    .background(spatialBorderColor(BorderSubtle))
                                            )
                                            // Side-by-side Live Interactive DAG Graph + Inspector Pane
                                            Column(
                                                modifier = Modifier
                                                    .weight(0.44f)
                                                    .fillMaxHeight()
                                                    .background(spatialCanvasColor())
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("spatial_dual_dag_pane")
                                            ) {
                                                Text(
                                                    text = "ЖИВОЙ DAG ГИПОТЕЗ • ПОППЕР",
                                                    color = CyanAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.8.sp,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                HypothesisDagGraph(
                                                    cycles = currentCycles,
                                                    hypotheses = currentHypotheses,
                                                    scenarios = currentScenarios,
                                                    isDualPaneSidePanel = true,
                                                    onSearchArxivForQuery = { query ->
                                                        arxivInitialQuery = query.ifBlank { activeSession?.topic ?: "" }
                                                        showArxivDialog = true
                                                    },
                                                    onCrossPostCopied = { phrase ->
                                                        viewModel.announceEyesFree(phrase, false)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    } else {
                                        ArenaScreen(
                                            session = activeSession,
                                            messages = currentMessages,
                                            citationsCount = currentCitations.size,
                                            ttsState = ttsState,
                                            isCycleRunning = isCycleRunning,
                                            progressState = progressState,
                                            isWideSpatialLayout = false,
                                            onOpenCouncil = { showCouncilConfigDialog = true },
                                            onOpenArxiv = {
                                                arxivInitialQuery = activeSession?.topic ?: ""
                                                showArxivDialog = true
                                            },
                                            onCopyCrossPostSynthesis = { viewModel.buildCrossPostSynthesisText() },
                                            onLaunchNextCycle = { viewModel.launchNextCycle(null) },
                                            onSendHumanMessage = { viewModel.sendHumanIntervention(it) },
                                            onExportReport = { showExportDialog = true },
                                            onExportToKeep = { dispatchKeepExport(viewModel.buildKeepBufferPayload()) },
                                            onExportMessageToKeep = { msg ->
                                                dispatchKeepExport(viewModel.buildMessageKeepBufferPayload(msg))
                                            },
                                            onTtsPlayPause = { msgId -> viewModel.toggleTtsPlayback(msgId) },
                                            onTtsNext = { viewModel.ttsNext() },
                                            onTtsPrev = { viewModel.ttsPrevious() },
                                            onTtsCycleSpeed = { viewModel.ttsCycleSpeed() },
                                            onTtsStop = { viewModel.ttsStop() },
                                            onEyesFreeAnnounce = { phrase, force -> viewModel.announceEyesFree(phrase, force) }
                                        )
                                    }
                                }
                                ProgressorTab.HYPOTHESES -> {
                                    HypothesesScreen(
                                        hypotheses = currentHypotheses,
                                        scenarios = currentScenarios,
                                        cycles = currentCycles,
                                        citationsCount = currentCitations.size,
                                        isWideSpatialLayout = isWideSpatialLayout,
                                        onSearchArxiv = { query ->
                                            arxivInitialQuery = query.ifBlank { activeSession?.topic ?: "" }
                                            showArxivDialog = true
                                        },
                                        onCrossPostCopied = { phrase ->
                                            viewModel.announceEyesFree(phrase, false)
                                        }
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
                                            selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                                        }
                                    )
                                }
                                ProgressorTab.COUNCIL -> {
                                    CouncilScreen(
                                        session = activeSession,
                                        cycles = currentCycles,
                                        customRoles = customRoles,
                                        theoristModel = selectedTheoristModel,
                                        skepticModel = selectedSkepticModel,
                                        synthesizerModel = selectedSynthesizerModel,
                                        isWideSpatialLayout = isWideSpatialLayout,
                                        onOpenCouncilConfig = { showCouncilConfigDialog = true },
                                        onOpenRoleDesigner = { showRoleDesignerDialog = true },
                                        onToggleCustomRole = { role ->
                                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                            viewModel.toggleCustomRole(role)
                                            viewModel.announceEyesFree("Роль обновлена", false)
                                        },
                                        onDeleteCustomRole = { roleId -> viewModel.deleteCustomRole(roleId) },
                                        onFastLaunchNextCycle = {
                                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                                            viewModel.announceEyesFree("Цикл запущен", false)
                                            viewModel.launchNextCycle(null)
                                        }
                                    )
                                }
                                ProgressorTab.TOPICS -> {
                                    SessionsScreen(
                                        sessions = allSessions,
                                        activeSessionId = activeSessionId,
                                        isWideSpatialLayout = isWideSpatialLayout,
                                        onSelectSession = { id ->
                                            viewModel.selectSession(id)
                                            selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                                        },
                                        onDeleteSession = { id -> viewModel.deleteSession(id) },
                                        onOpenNewSessionDialog = { showNewSessionDialog = true }
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

    // Export Markdown / LaTeX Dialog
    if (showExportDialog) {
        ExportMarkdownDialog(
            session = activeSession,
            hypothesesCount = currentHypotheses.size,
            scenariosCount = currentScenarios.size,
            citationsCount = currentCitations.size,
            onSaveFile = { format ->
                val cleanTopic = (activeSession?.topic ?: "session")
                    .take(20)
                    .replace("[^a-zA-Z0-9а-яА-Я_]".toRegex(), "_")
                showExportDialog = false
                if (format == ExportFormat.MARKDOWN) {
                    createMarkdownLauncher.launch("progressor_${cleanTopic}_${System.currentTimeMillis() % 10000}.md")
                } else {
                    createLatexLauncher.launch("progressor_${cleanTopic}_${System.currentTimeMillis() % 10000}.tex")
                }
            },
            onShare = { format ->
                showExportDialog = false
                val isMd = format == ExportFormat.MARKDOWN
                val content = if (isMd) viewModel.generateMarkdownReport() else viewModel.generateLatexReport()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (isMd) "text/markdown" else "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Отчет Прогрессора (${if (isMd) ".md" else ".tex"}): ${activeSession?.topic ?: ""}")
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться отчетом"))
            },
            onCopyClipboard = { format ->
                showExportDialog = false
                val isMd = format == ExportFormat.MARKDOWN
                val content = if (isMd) viewModel.generateMarkdownReport() else viewModel.generateLatexReport()
                clipboardManager.setText(AnnotatedString(content))
                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                viewModel.announceEyesFree("Отчёт скопирован", false)
                Toast.makeText(
                    context,
                    if (isMd) "Markdown скопирован в буфер" else "LaTeX код скопирован для Overleaf",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // arXiv & DOI Search Dialog
    if (showArxivDialog) {
        ArxivSearchDialog(
            initialQuery = arxivInitialQuery,
            searchResults = arxivResults,
            pinnedCitations = currentCitations,
            isSearching = isSearchingArxiv,
            errorMessage = arxivError,
            onSearch = { q -> viewModel.searchArxiv(q) },
            onPinPaper = { paper ->
                viewModel.pinArxivPaper(paper)
                Toast.makeText(context, "Статья прикреплена к сессии", Toast.LENGTH_SHORT).show()
            },
            onDeleteCitation = { id -> viewModel.deleteCitation(id) },
            onInjectPaperToCycle = { paper ->
                showArxivDialog = false
                selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                viewModel.announceEyesFree("Статья добавлена в цикл", false)
                viewModel.injectPaperIntoCycle(paper)
            },
            onDismiss = { showArxivDialog = false }
        )
    }

    // Custom Council Role Designer Dialog
    if (showRoleDesignerDialog) {
        CustomRoleDesignerDialog(
            onSaveRole = { role ->
                viewModel.saveCustomRole(role)
                showRoleDesignerDialog = false
                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                viewModel.announceEyesFree("Роль активирована", false)
                Toast.makeText(context, "Роль «${role.name}» активирована", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRoleDesignerDialog = false }
        )
    }

    // Model Council Configuration Dialog
    if (showCouncilConfigDialog) {
        ModelCouncilConfigDialog(
            availableModels = allAvailableModels,
            currentTheorist = selectedTheoristModel,
            currentSkeptic = selectedSkepticModel,
            currentSynthesizer = selectedSynthesizerModel,
            onDismiss = { showCouncilConfigDialog = false },
            onLaunchCycle = { theorist, skeptic, synth, guidance ->
                performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                viewModel.setModelsForNextCycle(theorist, skeptic, synth)
                showCouncilConfigDialog = false
                selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
                viewModel.announceEyesFree("Состав Совета обновлён, цикл запущен", false)
                viewModel.launchNextCycle(guidance)
            }
        )
    }

    // New Session Dialog
    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onCreateSession = { topic, domain ->
                viewModel.createNewSession(topic, domain)
                showNewSessionDialog = false
                selectTab(ProgressorTab.ARENA, focusContentAfterSwitch = true)
            }
        )
    }
}

@Composable
private fun TabNavIcon(tab: ProgressorTab, hypothesesCount: Int) {
    if (tab == ProgressorTab.HYPOTHESES && hypothesesCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = PurpleTheorist,
                    contentColor = VoidDark
                ) {
                    Text("$hypothesesCount")
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
}
