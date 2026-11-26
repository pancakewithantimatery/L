package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.GeminiBlueAccent
import com.example.ui.screens.OllamaAccent
import com.example.ui.screens.OpenRouterAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

@Composable
fun SettingsDialog(
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
    onSaveApiKey: (String) -> Unit,
    onSaveGeminiApiKey: (String) -> Unit,
    onSaveOpenRouterApiKey: (String) -> Unit,
    onSaveOllamaHost: (String) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onRefreshModels: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProviderTab by remember { mutableIntStateOf(0) } // 0: Venice, 1: Gemini, 2: OpenRouter, 3: Ollama
    var veniceKeyInput by remember { mutableStateOf(currentApiKey) }
    var geminiKeyInput by remember { mutableStateOf(currentGeminiApiKey) }
    var openRouterKeyInput by remember { mutableStateOf(currentOpenRouterApiKey) }
    var ollamaHostInput by remember { mutableStateOf(currentOllamaHost) }

    val tabColor = when (selectedProviderTab) {
        0 -> CyanAccent
        1 -> GeminiBlueAccent
        2 -> OpenRouterAccent
        else -> OllamaAccent
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, tabColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .testTag("settings_dialog"),
            color = DeepSpace
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = tabColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Настройки Провайдеров LLM",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Provider Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedProviderTab,
                    containerColor = SurfaceCard,
                    contentColor = tabColor,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedProviderTab]),
                            color = tabColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedProviderTab == 0,
                        onClick = { selectedProviderTab = 0 },
                        text = {
                            Text(
                                "Venice ($veniceModelsCount)",
                                color = if (selectedProviderTab == 0) CyanAccent else TextSecondary,
                                fontWeight = if (selectedProviderTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedProviderTab == 1,
                        onClick = { selectedProviderTab = 1 },
                        text = {
                            Text(
                                "Gemini ($geminiModelsCount)",
                                color = if (selectedProviderTab == 1) GeminiBlueAccent else TextSecondary,
                                fontWeight = if (selectedProviderTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedProviderTab == 2,
                        onClick = { selectedProviderTab = 2 },
                        text = {
                            Text(
                                "OpenRouter ($openRouterModelsCount)",
                                color = if (selectedProviderTab == 2) OpenRouterAccent else TextSecondary,
                                fontWeight = if (selectedProviderTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedProviderTab == 3,
                        onClick = { selectedProviderTab = 3 },
                        text = {
                            Text(
                                "Ollama ($ollamaModelsCount)",
                                color = if (selectedProviderTab == 3) OllamaAccent else TextSecondary,
                                fontWeight = if (selectedProviderTab == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedProviderTab) {
                    0 -> {
                        // Venice.ai
                        OutlinedTextField(
                            value = veniceKeyInput,
                            onValueChange = { veniceKeyInput = it },
                            label = { Text("Venice API Key") },
                            placeholder = { Text("Вставьте ключ из venice.ai...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("venice_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CyanAccent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onSaveApiKey(veniceKeyInput.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_venice_api_key_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Text("Сохранить ключ Venice", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    1 -> {
                        // Google Gemini
                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            label = { Text("Google Gemini API Key") },
                            placeholder = { Text("Оставьте пустым для дефолтного tier подписки") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeminiBlueAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = GeminiBlueAccent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "По умолчанию подтягивает доступные модели из tier с лимитами по подписке AI Studio.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onSaveGeminiApiKey(geminiKeyInput.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_gemini_api_key_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiBlueAccent)
                        ) {
                            Text("Сохранить ключ Gemini", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        // OpenRouter
                        OutlinedTextField(
                            value = openRouterKeyInput,
                            onValueChange = { openRouterKeyInput = it },
                            label = { Text("OpenRouter API Key") },
                            placeholder = { Text("sk-or-v1-...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openrouter_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OpenRouterAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = OpenRouterAccent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Доступ к сотням моделей: Claude 3.5 Sonnet, GPT-4o, DeepSeek R1, Llama 3.3.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onSaveOpenRouterApiKey(openRouterKeyInput.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_openrouter_api_key_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRouterAccent)
                        ) {
                            Text("Сохранить ключ OpenRouter", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        // Ollama Local Server
                        OutlinedTextField(
                            value = ollamaHostInput,
                            onValueChange = { ollamaHostInput = it },
                            label = { Text("Адрес сервера Ollama (Host:Port)") },
                            placeholder = { Text("http://10.0.2.2:11434") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_ollama_host_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OllamaAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = OllamaAccent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Для эмулятора Android: используйте http://10.0.2.2:11434. Для локальной сети: IP вашего ПК (например, http://192.168.1.100:11434).",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onSaveOllamaHost(ollamaHostInput.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_ollama_host_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OllamaAccent)
                        ) {
                            Text("Сохранить адрес Ollama", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simulation mode switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Демонстрационный режим",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Генерация научных циклов без расхода токенов",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = onToggleSimulation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoidDark,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("simulation_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Refresh Models Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Синхронизация всех каталогов",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Всего доступно моделей: ${veniceModelsCount + geminiModelsCount + openRouterModelsCount + ollamaModelsCount}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = onRefreshModels,
                        enabled = !isFetchingModels,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("refresh_models_button")
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = tabColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                tint = tabColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VoidDark)
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ключи сохраняются локально в защищенном хранилище приложения.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
