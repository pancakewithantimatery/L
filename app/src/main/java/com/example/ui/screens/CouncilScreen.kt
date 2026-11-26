package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import com.example.data.local.CustomAgentRoleEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.ui.spatial.LocalFocusGraph
import com.example.ui.spatial.focusOrderNode
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.RoseWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialCardHighlightColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CouncilScreen(
    session: ResearchSessionEntity?,
    cycles: List<EvolutionCycleEntity>,
    customRoles: List<CustomAgentRoleEntity>,
    theoristModel: String,
    skepticModel: String,
    synthesizerModel: String,
    onOpenCouncilConfig: () -> Unit,
    onOpenRoleDesigner: () -> Unit,
    onToggleCustomRole: (CustomAgentRoleEntity) -> Unit,
    onDeleteCustomRole: (Long) -> Unit,
    onFastLaunchNextCycle: () -> Unit,
    isWideSpatialLayout: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusGraph = LocalFocusGraph.current
    val leftRailTarget = if (isWideSpatialLayout) focusGraph.navCouncil else FocusRequester.Cancel
    val bottomNavTarget = if (isWideSpatialLayout) FocusRequester.Cancel else focusGraph.navCouncil

    val activeTheoristPersona = customRoles.find { it.baseSlot == "THEORIST" && it.isActive }
    val activeSkepticPersona = customRoles.find { it.baseSlot == "SKEPTIC" && it.isActive }
    val activeSynthPersona = customRoles.find { it.baseSlot == "SYNTHESIZER" && it.isActive }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(spatialCanvasColor())
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("council_screen_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Совет Моделей и Персоны",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Настройка ролей, промптов и мультипровайдеров",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                OutlinedButton(
                    onClick = onOpenCouncilConfig,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .focusOrderNode(
                            requester = focusGraph.councilConfigBtn,
                            next = focusGraph.councilRoleDesignerBtn,
                            previous = focusGraph.topSettings,
                            up = focusGraph.topXrealToggle,
                            down = focusGraph.councilRoleDesignerBtn,
                            left = leftRailTarget,
                            right = FocusRequester.Cancel
                        )
                        .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(10.dp))
                        .testTag("configure_council_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Модели", color = CyanAccent, fontSize = 12.sp)
                }
            }
        }

        // Three Active Role Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CouncilRoleCard(
                    title = activeTheoristPersona?.name ?: "Агент 1: Теоретик-Генератор",
                    assignedModel = theoristModel,
                    roleDescription = activeTheoristPersona?.systemPrompt
                        ?: "Генерация фундаментальных гипотез и теоретических механизмов",
                    accentColor = PurpleTheorist,
                    icon = Icons.Default.Psychology,
                    isCustomActive = activeTheoristPersona != null
                )
                CouncilRoleCard(
                    title = activeSkepticPersona?.name ?: "Агент 2: Скептик-Верификатор",
                    assignedModel = skepticModel,
                    roleDescription = activeSkepticPersona?.systemPrompt
                        ?: "Критика, попперовская фальсификация и дизайн экспериментов",
                    accentColor = AmberSkeptic,
                    icon = Icons.Default.Shield,
                    isCustomActive = activeSkepticPersona != null
                )
                CouncilRoleCard(
                    title = activeSynthPersona?.name ?: "Агент 3: Прогрессор-Синтезатор",
                    assignedModel = synthesizerModel,
                    roleDescription = activeSynthPersona?.systemPrompt
                        ?: "Разрешение парадоксов, расчет индекса жизнеспособности и следующий шаг",
                    accentColor = CyanAccent,
                    icon = Icons.Default.Science,
                    isCustomActive = activeSynthPersona != null
                )
            }
        }

        // Custom Agent Personas Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Конструктор ролей Совета (${customRoles.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "Нажмите на карточку, чтобы активировать персону на слоте",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                OutlinedButton(
                    onClick = onOpenRoleDesigner,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .focusOrderNode(
                            requester = focusGraph.councilRoleDesignerBtn,
                            next = focusGraph.councilFastLaunchBtn,
                            previous = focusGraph.councilConfigBtn,
                            up = focusGraph.councilConfigBtn,
                            down = focusGraph.councilFastLaunchBtn,
                            left = leftRailTarget,
                            right = FocusRequester.Cancel
                        )
                        .spatialInteractive(accentColor = PurpleTheorist, shape = RoundedCornerShape(10.dp))
                        .testTag("open_role_designer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PurpleTheorist, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", color = PurpleTheorist, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_roles_row")
            ) {
                items(customRoles, key = { it.id }) { role ->
                    CustomRoleChipCard(
                        role = role,
                        onToggle = { onToggleCustomRole(role) },
                        onDelete = if (!role.isPreset) { { onDeleteCustomRole(role.id) } } else null
                    )
                }
            }
        }

        // Fast Action Button
        item {
            Button(
                onClick = onOpenCouncilConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusOrderNode(
                        requester = focusGraph.councilFastLaunchBtn,
                        next = focusGraph.navCouncil,
                        previous = focusGraph.councilRoleDesignerBtn,
                        up = focusGraph.councilRoleDesignerBtn,
                        down = bottomNavTarget,
                        left = leftRailTarget,
                        right = FocusRequester.Cancel
                    )
                    .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(12.dp))
                    .testTag("launch_from_council_button"),
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
                    text = "Сменить состав и запустить цикл",
                    color = VoidDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }

        // History Header
        item {
            Text(
                text = "Хронология Эволюционных Циклов (${cycles.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (cycles.isEmpty()) {
            item {
                Text(
                    text = "Циклы еще не завершены",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            items(cycles, key = { it.id }) { cycle ->
                CycleHistoryCard(cycle = cycle)
            }
        }
    }
}

@Composable
private fun CustomRoleChipCard(
    role: CustomAgentRoleEntity,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val slotColor = when (role.baseSlot) {
        "THEORIST" -> PurpleTheorist
        "SKEPTIC" -> AmberSkeptic
        else -> CyanAccent
    }
    val slotLabel = when (role.baseSlot) {
        "THEORIST" -> "Теоретик"
        "SKEPTIC" -> "Скептик"
        else -> "Синтезатор"
    }

    Card(
        modifier = Modifier
            .width(220.dp)
            .spatialInteractive(
                accentColor = if (role.isActive) EmeraldHuman else slotColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .testTag("custom_role_card_${role.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (role.isActive) spatialCardHighlightColor() else spatialCardColor()
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (role.isActive) 1.5.dp else 1.dp,
            color = if (role.isActive) EmeraldHuman else spatialBorderColor(BorderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(slotColor.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$slotLabel • T=${String.format("%.1f", role.temperature)}",
                        color = slotColor,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (role.isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Активна",
                            tint = EmeraldHuman,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(26.dp)
                                .spatialInteractive(accentColor = RoseWarning, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить роль",
                                tint = RoseWarning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = role.name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = role.systemPrompt,
                color = TextSecondary,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CouncilRoleCard(
    title: String,
    assignedModel: String,
    roleDescription: String,
    accentColor: Color,
    icon: ImageVector,
    isCustomActive: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCustomActive) 1.5.dp else 1.dp,
            color = if (isCustomActive) EmeraldHuman.copy(alpha = 0.7f) else accentColor.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepSpace)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = assignedModel.take(18),
                            color = accentColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = roleDescription,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CycleHistoryCard(cycle: EvolutionCycleEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyanAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ЦИКЛ #${cycle.cycleNumber}",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (cycle.status == "COMPLETED") "Завершен" else "Выполняется",
                        color = if (cycle.status == "COMPLETED") CyanAccent else AmberSkeptic,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Жизнеспособность: ", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        "${cycle.viabilityScore}%",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (cycle.hypothesisTitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cycle.hypothesisTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!cycle.userGuidance.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Указание человека: \"${cycle.userGuidance}\"",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Модели: ", color = TextSecondary, fontSize = 10.sp)
                Text(
                    "${cycle.theoristModel.take(10)} • ${cycle.skepticModel.take(10)} • ${cycle.synthesizerModel.take(10)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
