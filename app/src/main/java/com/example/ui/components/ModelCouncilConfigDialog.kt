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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.VeniceModelInfo
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.AmberSkepticLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.PurpleTheoristLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

@Composable
fun ModelCouncilConfigDialog(
    availableModels: List<VeniceModelInfo>,
    currentTheorist: String,
    currentSkeptic: String,
    currentSynthesizer: String,
    onDismiss: () -> Unit,
    onLaunchCycle: (theorist: String, skeptic: String, synthesizer: String, userGuidance: String?) -> Unit
) {
    var selectedTheorist by remember { mutableStateOf(currentTheorist) }
    var selectedSkeptic by remember { mutableStateOf(currentSkeptic) }
    var selectedSynthesizer by remember { mutableStateOf(currentSynthesizer) }
    var activeRoleTab by remember { mutableIntStateOf(0) } // 0: Theorist, 1: Skeptic, 2: Synthesizer
    var userGuidanceText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .testTag("model_council_dialog"),
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
                            text = "Совет Моделей Venice.ai",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Настройте состав нейросетей для следующего цикла",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_council_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Role Tab Selector
                TabRow(
                    selectedTabIndex = activeRoleTab,
                    containerColor = SurfaceCard,
                    contentColor = CyanAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeRoleTab]),
                            color = when (activeRoleTab) {
                                0 -> PurpleTheorist
                                1 -> AmberSkeptic
                                else -> CyanAccent
                            }
                        )
                    }
                ) {
                    RoleTabItem(
                        title = "Теоретик",
                        icon = Icons.Default.Psychology,
                        selected = activeRoleTab == 0,
                        accentColor = PurpleTheorist,
                        assignedModel = selectedTheorist,
                        onClick = { activeRoleTab = 0 }
                    )
                    RoleTabItem(
                        title = "Скептик",
                        icon = Icons.Default.Shield,
                        selected = activeRoleTab == 1,
                        accentColor = AmberSkeptic,
                        assignedModel = selectedSkeptic,
                        onClick = { activeRoleTab = 1 }
                    )
                    RoleTabItem(
                        title = "Синтезатор",
                        icon = Icons.Default.Science,
                        selected = activeRoleTab == 2,
                        accentColor = CyanAccent,
                        assignedModel = selectedSynthesizer,
                        onClick = { activeRoleTab = 2 }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Models List for selected role
                val activeSelectedModel = when (activeRoleTab) {
                    0 -> selectedTheorist
                    1 -> selectedSkeptic
                    else -> selectedSynthesizer
                }

                val currentRoleAccent = when (activeRoleTab) {
                    0 -> PurpleTheorist
                    1 -> AmberSkeptic
                    else -> CyanAccent
                }

                var providerFilter by remember { mutableStateOf("ALL") } // ALL, VENICE, GEMINI

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Выберите модель:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(
                            "ALL" to "Все",
                            "VENICE" to "Venice",
                            "GEMINI" to "Gemini",
                            "OPENROUTER" to "Router",
                            "OLLAMA" to "Ollama"
                        ).forEach { (key, label) ->
                            val isChipSelected = providerFilter == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChipSelected) currentRoleAccent.copy(alpha = 0.25f) else SurfaceCard)
                                    .border(
                                        0.5.dp,
                                        if (isChipSelected) currentRoleAccent else BorderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { providerFilter = key }
                                    .padding(horizontal = 5.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isChipSelected) TextPrimary else TextSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                val filteredModels = availableModels.filter {
                    when (providerFilter) {
                        "VENICE" -> it.provider == "VENICE"
                        "GEMINI" -> it.provider == "GEMINI"
                        "OPENROUTER" -> it.provider == "OPENROUTER"
                        "OLLAMA" -> it.provider == "OLLAMA"
                        else -> true
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(filteredModels) { modelInfo ->
                        val isSelected = modelInfo.id == activeSelectedModel
                        ModelSelectionItem(
                            modelInfo = modelInfo,
                            isSelected = isSelected,
                            accentColor = currentRoleAccent,
                            onClick = {
                                when (activeRoleTab) {
                                    0 -> selectedTheorist = modelInfo.id
                                    1 -> selectedSkeptic = modelInfo.id
                                    2 -> selectedSynthesizer = modelInfo.id
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional human guidance input
                OutlinedTextField(
                    value = userGuidanceText,
                    onValueChange = { userGuidanceText = it },
                    label = { Text("Вектор от человека (необязательно)") },
                    placeholder = { Text("Например: Учесть эффект Казимира или ограничения криостата...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guidance_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanAccent
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Launch Button
                Button(
                    onClick = {
                        val guidance = userGuidanceText.trim().ifEmpty { null }
                        onLaunchCycle(selectedTheorist, selectedSkeptic, selectedSynthesizer, guidance)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_cycle_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = VoidDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Запустить эволюционный цикл",
                        color = VoidDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleTabItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    assignedModel: String,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) accentColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (selected) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = assignedModel.take(12),
                color = if (selected) accentColor else TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ModelSelectionItem(
    modelInfo: VeniceModelInfo,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("model_item_${modelInfo.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceCardHighlight else SurfaceCard
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modelInfo.displayName,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepSpace)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = modelInfo.tag,
                            color = when (modelInfo.provider) {
                                "GEMINI" -> Color(0xFF4E80EE)
                                "OPENROUTER" -> Color(0xFF6366F1)
                                "OLLAMA" -> Color(0xFF10B981)
                                else -> CyanAccent
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = modelInfo.description,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Выбрано",
                        tint = VoidDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
