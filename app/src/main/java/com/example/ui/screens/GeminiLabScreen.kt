package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

val GeminiBlueAccent = Color(0xFF4E80EE)
val GeminiPurpleAccent = Color(0xFF8B5CF6)

@Composable
fun GeminiLabScreen(
    session: ResearchSessionEntity?,
    currentHypothesis: ScientificHypothesisEntity?,
    geminiModels: List<VeniceModelInfo>,
    isFetchingModels: Boolean,
    geminiApiKeyConfigured: Boolean,
    onRefreshGeminiModels: () -> Unit,
    onRunAnalysis: (model: String, prompt: String, onResult: (String?) -> Unit) -> Unit,
    onInjectToSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedGeminiModel by remember {
        mutableStateOf(geminiModels.firstOrNull()?.id ?: "gemini-2.5-flash")
    }

    var customPrompt by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }

    val quickActionPrompts = listOf(
        "Аудит текущей гипотезы" to "Проведи глубокий критический аудит текущей научной гипотезы: «${currentHypothesis?.title ?: session?.topic ?: ""}». Выяви скрытые физические допущения и возможные термодинамические уязвимости.",
        "Поиск контрпримеров" to "Сформулируй 3 строгих контрпримера или мысленных эксперимента, способных опровергнуть идею: «${session?.topic ?: ""}».",
        "Альтернативный протокол" to "Предложи альтернативный, ранее не рассмотренный сценарий экспериментальной проверки для темы: «${session?.topic ?: ""}» с акцентом на современные квантовые или спектральные технологии."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("gemini_lab_header"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlueAccent.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GeminiBlueAccent.copy(alpha = 0.2f))
                                .border(1.dp, GeminiBlueAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GeminiBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google Gemini Lab",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (geminiApiKeyConfigured) GeminiBlueAccent else Color(0xFFF59E0B))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (geminiApiKeyConfigured) "DEFAULT TIER" else "ДЕМО РЕЖИМ",
                                        color = VoidDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Интеграция доступных моделей Gemini по API-ключу",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshGeminiModels,
                        enabled = !isFetchingModels,
                        modifier = Modifier.testTag("refresh_gemini_models_btn")
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = GeminiBlueAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить модели Gemini",
                                tint = GeminiBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Models Selector Bar
            item {
                Text(
                    text = "Доступные модели Gemini (${geminiModels.size}):",
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(geminiModels) { model ->
                        val isSelected = model.id == selectedGeminiModel
                        Card(
                            modifier = Modifier
                                .clickable { selectedGeminiModel = model.id }
                                .testTag("gemini_chip_${model.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SurfaceCardHighlight else SurfaceCard
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, GeminiBlueAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName,
                                        color = if (isSelected) GeminiBlueAccent else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = GeminiBlueAccent,
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

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Quick Prompt Chips
            item {
                Text(
                    text = "Быстрый анализ текущей темы:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Text(
                                text = label,
                                color = GeminiBlueAccent,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Input Prompt
            item {
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Запрос в лабораторию Gemini") },
                    placeholder = { Text("Задайте вопрос или используйте быстрый аудит...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeminiBlueAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GeminiBlueAccent
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
                            onRunAnalysis(selectedGeminiModel, prompt) { result ->
                                isAnalyzing = false
                                analysisResult = result
                            }
                        }
                    },
                    enabled = customPrompt.trim().isNotEmpty() && !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("run_gemini_analysis_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiBlueAccent)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = VoidDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Анализ Gemini...", color = VoidDark, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = VoidDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Запустить анализ ($selectedGeminiModel)",
                            color = VoidDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Analysis Result Card
            if (analysisResult != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("gemini_result_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlueAccent.copy(alpha = 0.5f))
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
                                        tint = GeminiBlueAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Вывод Gemini ($selectedGeminiModel)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row {
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
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = analysisResult ?: "",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Inject to session button
                            if (session != null) {
                                OutlinedButton(
                                    onClick = {
                                        analysisResult?.let { res ->
                                            onInjectToSession("Gemini ($selectedGeminiModel) аудит:\n$res")
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
