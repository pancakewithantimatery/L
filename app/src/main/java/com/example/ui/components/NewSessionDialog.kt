package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

data class TopicPreset(
    val title: String,
    val domain: String,
    val topic: String
)

val TOPIC_PRESETS = listOf(
    TopicPreset(
        title = "Квантовая биология",
        domain = "Квантовая биофизика",
        topic = "Квантовая когерентность и топологический перенос в фотосинтетических комплексах"
    ),
    TopicPreset(
        title = "Сверхпроводимость",
        domain = "Физика твердого тела",
        topic = "Высокотемпературная сверхпроводимость при квазикомнатном давлении в полигидридах"
    ),
    TopicPreset(
        title = "Оптические BCI",
        domain = "Нейрофотоника",
        topic = "Неинвазивные нейроинтерфейсы оптического типа на терагерцовых импульсах"
    ),
    TopicPreset(
        title = "Биогеронтология",
        domain = "Синтетическая биология",
        topic = "Эпигенетическое репрограммирование клеточного возраста без онкогенеза"
    ),
    TopicPreset(
        title = "Фотонные нейросети",
        domain = "Вычислительная физика",
        topic = "Полностью оптические тензорные ядра с субпикосекундной задержкой"
    )
)

@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreateSession: (topic: String, domain: String) -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    var domainInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                .testTag("new_session_dialog"),
            color = DeepSpace
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Новое исследование Прогрессора",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Задайте научную тему для совета моделей",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_new_session_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets horizontal list
                Text(
                    text = "Быстрые научные темы:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TOPIC_PRESETS) { preset ->
                        Card(
                            modifier = Modifier
                                .clickable {
                                    topicInput = preset.topic
                                    domainInput = preset.domain
                                }
                                .testTag("preset_${preset.title}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = preset.title,
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = preset.domain,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Topic Input
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("Тема исследования") },
                    placeholder = { Text("Например: Механизм квантового запутывания в микротрубочках...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_topic_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanAccent
                    ),
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Domain Input
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("Научная область / Дисциплина") },
                    placeholder = { Text("Например: Биофизика, Наноэлектроника...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_domain_input"),
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

                Spacer(modifier = Modifier.height(18.dp))

                // Launch Button
                Button(
                    onClick = {
                        val topic = topicInput.trim()
                        val domain = domainInput.trim().ifEmpty { "Междисциплинарные исследования" }
                        if (topic.isNotEmpty()) {
                            onCreateSession(topic, domain)
                        }
                    },
                    enabled = topicInput.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_session_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = VoidDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Запустить Прогрессор",
                        color = VoidDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
