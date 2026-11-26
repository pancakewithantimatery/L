package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VeniceModelInfo
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor

val OpenRouterAccent = Color(0xFF6366F1)
val OllamaAccent = Color(0xFF10B981)

@Composable
fun AiLabScreen(
    session: ResearchSessionEntity?,
    currentHypothesis: ScientificHypothesisEntity?,
    geminiModels: List<VeniceModelInfo>,
    openRouterModels: List<VeniceModelInfo>,
    ollamaModels: List<VeniceModelInfo>,
    isGeminiKeyConfigured: Boolean,
    isOpenRouterKeyConfigured: Boolean,
    ollamaHostUrl: String,
    onSaveOllamaHost: (String) -> Unit,
    onRefreshGemini: () -> Unit,
    onRefreshOpenRouter: () -> Unit,
    onRefreshOllama: () -> Unit,
    onRunAnalysis: (provider: String, model: String, prompt: String, onResult: (String?) -> Unit) -> Unit,
    onInjectToSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedLabTab by remember { mutableIntStateOf(0) } // 0: Gemini, 1: OpenRouter, 2: Ollama Local

    // Active selected models per provider
    var selectedGeminiModel by remember { mutableStateOf(geminiModels.firstOrNull()?.id ?: "gemini-2.5-flash") }
    var selectedOpenRouterModel by remember { mutableStateOf(openRouterModels.firstOrNull()?.id ?: "anthropic/claude-3.5-sonnet") }
    var selectedOllamaModel by remember { mutableStateOf(ollamaModels.firstOrNull()?.id ?: "ollama/llama3.2") }

    var customPrompt by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var hostUrlInput by remember { mutableStateOf(ollamaHostUrl) }

    val currentProviderName = when (selectedLabTab) {
        0 -> "Google Gemini"
        1 -> "OpenRouter.ai"
        else -> "Ollama Local"
    }

    val currentAccentColor = when (selectedLabTab) {
        0 -> GeminiBlueAccent
        1 -> OpenRouterAccent
        else -> OllamaAccent
    }

    val activeModels = when (selectedLabTab) {
        0 -> geminiModels
        1 -> openRouterModels
        else -> ollamaModels
    }

    val activeSelectedModel = when (selectedLabTab) {
        0 -> selectedGeminiModel
        1 -> selectedOpenRouterModel
        else -> selectedOllamaModel
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(spatialCanvasColor())
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Provider Tabs
        TabRow(
            selectedTabIndex = selectedLabTab,
            containerColor = spatialCardColor(),
            contentColor = currentAccentColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedLabTab]),
                    color = currentAccentColor
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(0.5.dp, BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedLabTab == 0,
                onClick = { selectedLabTab = 0; analysisResult = null },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedLabTab == 0) GeminiBlueAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gemini", fontSize = 12.sp, fontWeight = if (selectedLabTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedLabTab == 1,
                onClick = { selectedLabTab = 1; analysisResult = null },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = if (selectedLabTab == 1) OpenRouterAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("OpenRouter", fontSize = 12.sp, fontWeight = if (selectedLabTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedLabTab == 2,
                onClick = { selectedLabTab = 2; analysisResult = null },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = if (selectedLabTab == 2) OllamaAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ollama", fontSize = 12.sp, fontWeight = if (selectedLabTab == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Ollama Server Config Banner (shown when Ollama tab is active)
            if (selectedLabTab == 2) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OllamaAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = OllamaAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Локальный Ollama Сервер",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OllamaAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "АКТИВЕН",
                                        color = OllamaAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hostUrlInput,
                                    onValueChange = { hostUrlInput = it },
                                    label = { Text("Адрес сервера Ollama") },
                                    placeholder = { Text("http://10.0.2.2:11434") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ollama_host_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OllamaAccent,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = OllamaAccent
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onSaveOllamaHost(hostUrlInput.trim())
                                        onRefreshOllama()
                                        Toast.makeText(context, "Адрес сохранен, опрос моделей...", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OllamaAccent),
                                    modifier = Modifier.testTag("save_ollama_host_btn")
                                ) {
                                    Text("Тест", color = VoidDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Models Selector Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Модели $currentProviderName (${activeModels.size}):",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            when (selectedLabTab) {
                                0 -> onRefreshGemini()
                                1 -> onRefreshOpenRouter()
                                else -> onRefreshOllama()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = currentAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeModels) { model ->
                        val isSelected = model.id == activeSelectedModel
                        Card(
                            modifier = Modifier
                                .clickable {
                                    when (selectedLabTab) {
                                        0 -> selectedGeminiModel = model.id
                                        1 -> selectedOpenRouterModel = model.id
                                        else -> selectedOllamaModel = model.id
                                    }
                                }
                                .testTag("lab_chip_${model.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) spatialCardHighlightColor() else spatialCardColor()
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, currentAccentColor)
                            } else {
                                androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName,
                                        color = if (isSelected) currentAccentColor else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = currentAccentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = model.tag,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Prompts
            item {
                Text(
                    text = "Шаблоны анализа текущей темы:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val quickActionPrompts = listOf(
                    "Экспресс-аудит" to "Проведи глубокий критический аудит текущей научной гипотезы: «${currentHypothesis?.title ?: session?.topic ?: ""}». Выяви скрытые физические допущения и термодинамические риски.",
                    "Поиск фальсификаций" to "Предложи строгий критерий фальсификации по Карлу Попперу для темы: «${session?.topic ?: ""}».",
                    "Альтернативный синтез" to "Сформулируй альтернативный механизм решения проблемы для темы: «${session?.topic ?: ""}»."
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickActionPrompts) { (label, promptText) ->
                        Card(
                            modifier = Modifier
                                .clickable { customPrompt = promptText }
                                .testTag("quick_action_${label}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Text(
                                text = label,
                                color = currentAccentColor,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Prompt Input & Submit
            item {
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Запрос в лабораторию ($currentProviderName)") },
                    placeholder = { Text("Введите исследовательский запрос...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lab_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentAccentColor,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = currentAccentColor
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val prompt = customPrompt.trim()
                        if (prompt.isNotEmpty()) {
                            isAnalyzing = true
                            analysisResult = null
                            val providerTag = when (selectedLabTab) {
                                0 -> "GEMINI"
                                1 -> "OPENROUTER"
                                else -> "OLLAMA"
                            }
                            onRunAnalysis(providerTag, activeSelectedModel, prompt) { result ->
                                isAnalyzing = false
                                analysisResult = result
                            }
                        }
                    },
                    enabled = customPrompt.trim().isNotEmpty() && !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("run_lab_analysis_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccentColor)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = VoidDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Анализ модели...", color = VoidDark, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = VoidDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Запустить анализ ($activeSelectedModel)",
                            color = VoidDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Results Card
            if (analysisResult != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("lab_result_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                        border = androidx.compose.foundation.BorderStroke(1.dp, currentAccentColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = currentAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Вывод $currentProviderName ($activeSelectedModel)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        analysisResult?.let { text ->
                                            clipboard.setText(AnnotatedString(text))
                                            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Копировать",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = analysisResult ?: "",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (session != null) {
                                OutlinedButton(
                                    onClick = {
                                        analysisResult?.let { res ->
                                            onInjectToSession("$currentProviderName ($activeSelectedModel):\n$res")
                                            Toast.makeText(context, "Отправлено в диалог Арены", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = EmeraldHuman,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Интегрировать вывод в диалог исследования",
                                        color = EmeraldHuman,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
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
