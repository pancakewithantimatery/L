package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.GeminiBlueAccent
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.OllamaAccent
import com.example.ui.theme.OpenRouterAccent
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.RoseWarning
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor
import com.example.ui.theme.spatialSurfaceColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    currentGeminiApiKey: String,
    currentOpenRouterApiKey: String,
    currentOllamaHost: String,
    isSimulationMode: Boolean,
    isFetchingModels: Boolean,
    veniceModelsCount: Int,
    geminiModelsCount: Int,
    openRouterModelsCount: Int,
    ollamaModelsCount: Int,
    xrealSpatialMode: Boolean = false,
    xrealAutoDetect: Boolean = true,
    xrealZeroGravityLock: Boolean = true,
    xrealOverscanDp: Int = 28,
    xrealEyesFreeFeedback: Boolean = true,
    onToggleXrealSpatialMode: (Boolean) -> Unit = {},
    onToggleXrealAutoDetect: (Boolean) -> Unit = {},
    onToggleXrealZeroGravityLock: (Boolean) -> Unit = {},
    onSelectXrealOverscanDp: (Int) -> Unit = {},
    onToggleXrealEyesFreeFeedback: (Boolean) -> Unit = {},
    onSaveApiKey: (String) -> Unit,
    onSaveGeminiApiKey: (String) -> Unit,
    onSaveOpenRouterApiKey: (String) -> Unit,
    onSaveOllamaHost: (String) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onRefreshAllModels: () -> Unit,
    onTestVenice: (String, (Boolean, String) -> Unit) -> Unit,
    onTestOpenRouter: (String, (Boolean, String) -> Unit) -> Unit,
    onTestOllama: (String, (Boolean, String) -> Unit) -> Unit,
    onTestGemini: (String, (Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val spatialConfig = LocalSpatialConfig.current

    var selectedProviderTab by remember { mutableIntStateOf(0) } // 0: Venice, 1: OpenRouter, 2: Ollama, 3: Gemini, 4: XREAL XR

    // Local form states (synced with DataStore properties)
    var veniceKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var openRouterKeyInput by remember(currentOpenRouterApiKey) { mutableStateOf(currentOpenRouterApiKey) }
    var ollamaHostInput by remember(currentOllamaHost) { mutableStateOf(currentOllamaHost) }
    var geminiKeyInput by remember(currentGeminiApiKey) { mutableStateOf(currentGeminiApiKey) }

    var showVeniceKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }

    // Test connection states: result message, isSuccess, isTesting
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val tabColor = when (selectedProviderTab) {
        0 -> CyanAccent
        1 -> OpenRouterAccent
        2 -> OllamaAccent
        3 -> GeminiBlueAccent
        else -> EmeraldHuman
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = spatialCanvasColor(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Настройки провайдеров",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tabColor.copy(alpha = 0.2f))
                                    .border(1.dp, tabColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = tabColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DATASTORE",
                                        color = tabColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Venice.ai • OpenRouter • Ollama Local • Gemini",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = CyanAccent
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onRefreshAllModels()
                            Toast.makeText(context, "Обновление моделей через API...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("settings_refresh_all_button")
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить все модели",
                                tint = CyanAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = spatialSurfaceColor(),
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(spatialCanvasColor())
                .padding(innerPadding)
        ) {
            // Provider & XREAL Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedProviderTab,
                containerColor = spatialSurfaceColor(),
                contentColor = tabColor,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedProviderTab]),
                        color = tabColor,
                        height = 3.dp
                    )
                },
                modifier = Modifier.testTag("settings_provider_tabs")
            ) {
                Tab(
                    selected = selectedProviderTab == 0,
                    onClick = {
                        selectedProviderTab = 0
                        testResultText = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Venice.ai", fontSize = 12.sp, fontWeight = if (selectedProviderTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = CyanAccent,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("settings_tab_venice")
                )
                Tab(
                    selected = selectedProviderTab == 1,
                    onClick = {
                        selectedProviderTab = 1
                        testResultText = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OpenRouter", fontSize = 12.sp, fontWeight = if (selectedProviderTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = OpenRouterAccent,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("settings_tab_openrouter")
                )
                Tab(
                    selected = selectedProviderTab == 2,
                    onClick = {
                        selectedProviderTab = 2
                        testResultText = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ollama (Local)", fontSize = 12.sp, fontWeight = if (selectedProviderTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = OllamaAccent,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("settings_tab_ollama")
                )
                Tab(
                    selected = selectedProviderTab == 3,
                    onClick = {
                        selectedProviderTab = 3
                        testResultText = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini", fontSize = 12.sp, fontWeight = if (selectedProviderTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = GeminiBlueAccent,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("settings_tab_gemini")
                )
                Tab(
                    selected = selectedProviderTab == 4,
                    onClick = {
                        selectedProviderTab = 4
                        testResultText = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("XREAL XR", fontSize = 12.sp, fontWeight = if (selectedProviderTab == 4) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = EmeraldHuman,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("settings_tab_xreal")
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Test Banner (if triggered)
                AnimatedVisibility(visible = testResultText != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connection_test_result_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (testResultSuccess) EmeraldHuman.copy(alpha = 0.12f) else RoseWarning.copy(alpha = 0.12f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (testResultSuccess) EmeraldHuman.copy(alpha = 0.5f) else RoseWarning.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (testResultSuccess) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (testResultSuccess) EmeraldHuman else RoseWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = testResultText ?: "",
                                color = if (testResultSuccess) EmeraldHuman else RoseWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { testResultText = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Скрыть",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                when (selectedProviderTab) {
                    0 -> {
                        // --- VENICE.AI SETTINGS ---
                        ProviderStatusHeader(
                            title = "Venice.ai API",
                            subtitle = "Приватный вывод открытых моделей без цензуры",
                            isConfigured = currentApiKey.isNotEmpty(),
                            modelsCount = veniceModelsCount,
                            accentColor = CyanAccent
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "API-КЛЮЧ VENICE.AI",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = veniceKeyInput,
                                    onValueChange = { veniceKeyInput = it },
                                    placeholder = { Text("Вставьте ключ (начинается с v_...)", color = TextMuted, fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("venice_api_key_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanAccent,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    visualTransformation = if (showVeniceKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { showVeniceKey = !showVeniceKey }) {
                                                Icon(
                                                    imageVector = if (showVeniceKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Показать ключ",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    veniceKeyInput = clip.trim()
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Вставить",
                                                    tint = CyanAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ключ сохраняется в защищенном локальном Jetpack DataStore устройства и используется для запросов к api.venice.ai.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val keyToTest = veniceKeyInput.trim()
                                            if (keyToTest.isEmpty()) {
                                                testResultText = "Введите API-ключ Venice для проверки"
                                                testResultSuccess = false
                                                return@OutlinedButton
                                            }
                                            isTestingConnection = true
                                            onTestVenice(keyToTest) { success, msg ->
                                                isTestingConnection = false
                                                testResultSuccess = success
                                                testResultText = msg
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("test_venice_connection_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanAccent)
                                        } else {
                                            Text("Проверить пинг", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onSaveApiKey(veniceKeyInput.trim())
                                            Toast.makeText(context, "Ключ Venice.ai сохранен в DataStore", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("save_venice_key_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = VoidDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Сохранить", color = VoidDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Simulation Mode Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Автономный эмулятор (Simulation Mode)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Генерация гипотез и синтеза по встроенным научным эвристикам без сетевых запросов к API.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = isSimulationMode,
                                    onCheckedChange = { onToggleSimulation(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = VoidDark,
                                        checkedTrackColor = AmberSkeptic,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DeepSpace
                                    ),
                                    modifier = Modifier.testTag("simulation_mode_switch")
                                )
                            }
                        }
                    }

                    1 -> {
                        // --- OPENROUTER SETTINGS ---
                        ProviderStatusHeader(
                            title = "OpenRouter.ai API",
                            subtitle = "Шлюз к Claude 3.5 Sonnet, GPT-4o, DeepSeek R1 и открытым моделям",
                            isConfigured = currentOpenRouterApiKey.isNotEmpty(),
                            modelsCount = openRouterModelsCount,
                            accentColor = OpenRouterAccent
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "API-КЛЮЧ OPENROUTER.AI",
                                    color = OpenRouterAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = openRouterKeyInput,
                                    onValueChange = { openRouterKeyInput = it },
                                    placeholder = { Text("sk-or-v1-...", color = TextMuted, fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("openrouter_api_key_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenRouterAccent,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                                Icon(
                                                    imageVector = if (showOpenRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Показать ключ",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    openRouterKeyInput = clip.trim()
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Вставить",
                                                    tint = OpenRouterAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Позволяет использовать более 100 различных LLM с единым балансом аккаунта OpenRouter.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val keyToTest = openRouterKeyInput.trim()
                                            if (keyToTest.isEmpty()) {
                                                testResultText = "Введите API-ключ OpenRouter для проверки"
                                                testResultSuccess = false
                                                return@OutlinedButton
                                            }
                                            isTestingConnection = true
                                            onTestOpenRouter(keyToTest) { success, msg ->
                                                isTestingConnection = false
                                                testResultSuccess = success
                                                testResultText = msg
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("test_openrouter_connection_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenRouterAccent),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OpenRouterAccent.copy(alpha = 0.5f))
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = OpenRouterAccent)
                                        } else {
                                            Text("Проверить пинг", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onSaveOpenRouterApiKey(openRouterKeyInput.trim())
                                            Toast.makeText(context, "Ключ OpenRouter.ai сохранен в DataStore", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("save_openrouter_key_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OpenRouterAccent)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = VoidDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Сохранить", color = VoidDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // --- OLLAMA LOCAL SERVER SETTINGS ---
                        ProviderStatusHeader(
                            title = "Локальный Ollama сервер",
                            subtitle = "Офлайн-инференс без отправки данных в облако",
                            isConfigured = ollamaHostInput.isNotEmpty(),
                            modelsCount = ollamaModelsCount,
                            accentColor = OllamaAccent
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "URL ЭНДПОИНТА СЕРВЕРА OLLAMA",
                                    color = OllamaAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = ollamaHostInput,
                                    onValueChange = { ollamaHostInput = it },
                                    placeholder = { Text("http://10.0.2.2:11434", color = TextMuted, fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ollama_host_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OllamaAccent,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                ollamaHostInput = clip.trim().removeSuffix("/")
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Вставить",
                                                tint = OllamaAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Host Presets
                                Text(
                                    text = "Быстрый выбор адреса:",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        QuickHostPill("10.0.2.2:11434 (Эмулятор)") {
                                            ollamaHostInput = "http://10.0.2.2:11434"
                                        }
                                    }
                                    item {
                                        QuickHostPill("localhost:11434 (Устройство)") {
                                            ollamaHostInput = "http://localhost:11434"
                                        }
                                    }
                                    item {
                                        QuickHostPill("192.168.1.100:11434 (LAN)") {
                                            ollamaHostInput = "http://192.168.1.100:11434"
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ollama должна быть запущена с переменной окружения OLLAMA_ORIGINS=\"*\" для доступа из Android-приложения по HTTP.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val hostToTest = ollamaHostInput.trim()
                                            if (hostToTest.isEmpty()) {
                                                testResultText = "Укажите адрес сервера Ollama"
                                                testResultSuccess = false
                                                return@OutlinedButton
                                            }
                                            isTestingConnection = true
                                            onTestOllama(hostToTest) { success, msg ->
                                                isTestingConnection = false
                                                testResultSuccess = success
                                                testResultText = msg
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("test_ollama_connection_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OllamaAccent),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OllamaAccent.copy(alpha = 0.5f))
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = OllamaAccent)
                                        } else {
                                            Text("Проверить сервер", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onSaveOllamaHost(ollamaHostInput.trim())
                                            Toast.makeText(context, "Адрес Ollama сохранен в DataStore", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("save_ollama_host_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OllamaAccent)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = VoidDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Сохранить", color = VoidDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // --- GOOGLE GEMINI SETTINGS ---
                        ProviderStatusHeader(
                            title = "Google Gemini API",
                            subtitle = "Модели подписки Google (2.5 Flash, 2.5 Pro, 2.0 Flash)",
                            isConfigured = currentGeminiApiKey.isNotEmpty(),
                            modelsCount = geminiModelsCount,
                            accentColor = GeminiBlueAccent
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "API-КЛЮЧ GOOGLE GEMINI",
                                    color = GeminiBlueAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = geminiKeyInput,
                                    onValueChange = { geminiKeyInput = it },
                                    placeholder = { Text("AIzaSy...", color = TextMuted, fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gemini_api_key_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GeminiBlueAccent,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                                Icon(
                                                    imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Показать ключ",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    geminiKeyInput = clip.trim()
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Вставить",
                                                    tint = GeminiBlueAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ключ из Google AI Studio подтягивает доступные модели вашей подписки без необходимости отдельного платного тира.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val keyToTest = geminiKeyInput.trim()
                                            if (keyToTest.isEmpty()) {
                                                testResultText = "Введите API-ключ Gemini для проверки"
                                                testResultSuccess = false
                                                return@OutlinedButton
                                            }
                                            isTestingConnection = true
                                            onTestGemini(keyToTest) { success, msg ->
                                                isTestingConnection = false
                                                testResultSuccess = success
                                                testResultText = msg
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("test_gemini_connection_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiBlueAccent),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlueAccent.copy(alpha = 0.5f))
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GeminiBlueAccent)
                                        } else {
                                            Text("Проверить пинг", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onSaveGeminiApiKey(geminiKeyInput.trim())
                                            Toast.makeText(context, "Ключ Gemini сохранен в DataStore", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("save_gemini_key_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiBlueAccent)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = VoidDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Сохранить", color = VoidDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // --- XREAL AIR 2 PRO SPATIAL / MULTI-WINDOW SETTINGS ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldHuman.copy(alpha = 0.55f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ViewInAr,
                                            contentDescription = null,
                                            tint = EmeraldHuman,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "XREAL Air 2 Pro • Spatial XR",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (spatialConfig.isSpatialModeActive) EmeraldHuman.copy(alpha = 0.2f) else AmberSkeptic.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (spatialConfig.isSpatialModeActive) "OLED #000000" else "СТАНДАРТ",
                                            color = if (spatialConfig.isSpatialModeActive) EmeraldHuman else AmberSkeptic,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Дисплей: " + if (spatialConfig.hasExternalDisplay) {
                                        "Внешний подключён (${spatialConfig.externalDisplayName.ifEmpty { "XREAL" }})"
                                    } else {
                                        "Встроенный экран (можно включить режим вручную)"
                                    },
                                    color = if (spatialConfig.hasExternalDisplay) EmeraldHuman else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Окно: ${if (spatialConfig.isInMultiWindow) "Freeform / Multi-Window (Nebula / DeX)" else "Полноэкранный режим"}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Switch 1: Manual XREAL True Black Spatial Mode
                        SpatialToggleCard(
                            title = "Режим XREAL Immersion (True Black #000000)",
                            description = "Переключает холст и карточки в строго #000000 (0% засветки micro-OLED) и включает Safe Area от краёв линз Birdbath.",
                            checked = xrealSpatialMode,
                            onCheckedChange = onToggleXrealSpatialMode,
                            tag = "xreal_spatial_mode_switch"
                        )

                        // Switch 2: Auto-Detect External Display
                        SpatialToggleCard(
                            title = "Автоопределение Beam Pro / USB-C дисплея",
                            description = "Автоматически активирует True Black OLED и 16:9 Split-Pane (Арена + живой DAG) при подключении очков.",
                            checked = xrealAutoDetect,
                            onCheckedChange = onToggleXrealAutoDetect,
                            tag = "xreal_auto_detect_switch"
                        )

                        // Switch 3: Zero-Gravity / Reclined View Lock
                        SpatialToggleCard(
                            title = "Zero-Gravity (Фиксация в лежачем положении)",
                            description = "Игнорирует акселерометр смартфона при активном XREAL-режиме и не даёт экрану гаснуть или переворачиваться.",
                            checked = xrealZeroGravityLock,
                            onCheckedChange = onToggleXrealZeroGravityLock,
                            tag = "xreal_zero_gravity_switch"
                        )

                        // Safe Area Overscan Padding Selector (24dp / 28dp / 32dp)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(1.dp, spatialBorderColor(BorderSubtle))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SAFE AREA (OVERSCAN PADDING)",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Защитный отступ по периметру, чтобы иконки Арены и счётчики циклов не попадали в краевое размытие оптики Birdbath.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(24, 28, 32).forEach { dpVal ->
                                        val selected = xrealOverscanDp == dpVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) CyanAccent else spatialSurfaceColor())
                                                .border(
                                                    1.dp,
                                                    if (selected) CyanAccent else spatialBorderColor(BorderSubtle),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onSelectXrealOverscanDp(dpVal) }
                                                .padding(vertical = 10.dp)
                                                .testTag("xreal_overscan_$dpVal"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${dpVal}.dp",
                                                color = if (selected) VoidDark else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Switch 4: Eyes-Free Haptics & Audio Confirmations
                        SpatialToggleCard(
                            title = "Слепая навигация (Вибро + Голосовой отклик)",
                            description = "Тактильная отдача и короткие голосовые подтверждения при запуске цикла, смене Совета, паузе TTS и копировании.",
                            checked = xrealEyesFreeFeedback,
                            onCheckedChange = onToggleXrealEyesFreeFeedback,
                            tag = "xreal_eyes_free_switch"
                        )
                    }
                }

                // Global DataStore Status Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("datastore_summary_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = spatialCardHighlightColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, spatialBorderColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "СТАТУС ХРАНИЛИЩА DATASTORE",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldHuman.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "АКТИВНО",
                                    color = EmeraldHuman,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ProviderRowStatus("Venice.ai", currentApiKey.isNotEmpty(), CyanAccent, veniceModelsCount)
                        Spacer(modifier = Modifier.height(6.dp))
                        ProviderRowStatus("OpenRouter.ai", currentOpenRouterApiKey.isNotEmpty(), OpenRouterAccent, openRouterModelsCount)
                        Spacer(modifier = Modifier.height(6.dp))
                        ProviderRowStatus("Ollama Local", currentOllamaHost.isNotEmpty(), OllamaAccent, ollamaModelsCount)
                        Spacer(modifier = Modifier.height(6.dp))
                        ProviderRowStatus("Google Gemini", currentGeminiApiKey.isNotEmpty(), GeminiBlueAccent, geminiModelsCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderStatusHeader(
    title: String,
    subtitle: String,
    isConfigured: Boolean,
    modelsCount: Int,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isConfigured) EmeraldHuman.copy(alpha = 0.2f) else AmberSkeptic.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isConfigured) "Настроен" else "Не настроен",
                            color = if (isConfigured) EmeraldHuman else AmberSkeptic,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepSpace)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$modelsCount мод.",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickHostPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DeepSpace)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = CyanAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ProviderRowStatus(name: String, isConfigured: Boolean, color: Color, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConfigured) EmeraldHuman else AmberSkeptic)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = if (isConfigured) "Активен ($count моделей)" else "Требует настройки",
            color = if (isConfigured) color else TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpatialToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) EmeraldHuman.copy(alpha = 0.5f) else spatialBorderColor(BorderSubtle)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidDark,
                    checkedTrackColor = EmeraldHuman,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = spatialSurfaceColor()
                ),
                modifier = Modifier.testTag(tag)
            )
        }
    }
}
