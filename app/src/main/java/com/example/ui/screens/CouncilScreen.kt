package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

@Composable
fun CouncilScreen(
    session: ResearchSessionEntity?,
    cycles: List<EvolutionCycleEntity>,
    theoristModel: String,
    skepticModel: String,
    synthesizerModel: String,
    onOpenCouncilConfig: () -> Unit,
    onFastLaunchNextCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Совет Моделей Venice.ai",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Конфигурация агентов и эволюция циклов",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            OutlinedButton(
                onClick = onOpenCouncilConfig,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("configure_council_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Настроить", color = CyanAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Three Active Role Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CouncilRoleCard(
                title = "Агент 1: Теоретик-Генератор",
                assignedModel = theoristModel,
                roleDescription = "Генерация фундаментальных гипотез и теоретических механизмов",
                accentColor = PurpleTheorist,
                icon = Icons.Default.Psychology
            )
            CouncilRoleCard(
                title = "Агент 2: Скептик-Верификатор",
                assignedModel = skepticModel,
                roleDescription = "Критика, попперовская фальсификация и дизайн экспериментов",
                accentColor = AmberSkeptic,
                icon = Icons.Default.Shield
            )
            CouncilRoleCard(
                title = "Агент 3: Прогрессор-Синтезатор",
                assignedModel = synthesizerModel,
                roleDescription = "Разрешение парадоксов, расчет индекса жизнеспособности и следующий шаг",
                accentColor = CyanAccent,
                icon = Icons.Default.Science
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Fast Action Button
        Button(
            onClick = onOpenCouncilConfig,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
                text = "Сменить состав и запустить новый цикл",
                color = VoidDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History of Evolution Cycles
        Text(
            text = "Хронология Эволюционных Циклов (${cycles.size})",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("cycles_history_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (cycles.isEmpty()) {
                item {
                    Text(
                        text = "Циклы еще не завершены",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(cycles) { cycle ->
                    CycleHistoryCard(cycle = cycle)
                }
            }
        }
    }
}

@Composable
private fun CouncilRoleCard(
    title: String,
    assignedModel: String,
    roleDescription: String,
    accentColor: Color,
    icon: ImageVector
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
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepSpace)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = assignedModel,
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
                    lineHeight = 15.sp
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
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
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
