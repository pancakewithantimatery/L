package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import com.example.ui.components.HypothesisCard
import com.example.ui.components.VerificationScenarioCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark

@Composable
fun HypothesesScreen(
    hypotheses: List<ScientificHypothesisEntity>,
    scenarios: List<VerificationScenarioEntity>,
    cycles: List<EvolutionCycleEntity>,
    modifier: Modifier = Modifier
) {
    var selectedCycleFilter by remember { mutableStateOf<Long?>(null) } // null = All cycles
    var filterType by remember { mutableStateOf("ALL") } // ALL, HYPOTHESES, SCENARIOS

    val filteredHypotheses = if (selectedCycleFilter == null) {
        hypotheses
    } else {
        hypotheses.filter { it.cycleId == selectedCycleFilter }
    }

    val filteredScenarios = if (selectedCycleFilter == null) {
        scenarios
    } else {
        scenarios.filter { it.cycleId == selectedCycleFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top filter bar
        Text(
            text = "База Знаний Прогрессора",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Сгенерированные гипотезы и критерии фальсификации",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cycle filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCycleFilter == null,
                    onClick = { selectedCycleFilter = null },
                    label = { Text("Все циклы (${cycles.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = VoidDark,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("filter_cycle_all")
                )
            }
            items(cycles) { cycle ->
                FilterChip(
                    selected = selectedCycleFilter == cycle.id,
                    onClick = { selectedCycleFilter = cycle.id },
                    label = { Text("Цикл #${cycle.cycleNumber}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleTheorist,
                        selectedLabelColor = VoidDark,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("filter_cycle_${cycle.cycleNumber}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content Type filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text("Всё") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepSpace,
                    selectedLabelColor = CyanAccent
                )
            )
            FilterChip(
                selected = filterType == "HYPOTHESES",
                onClick = { filterType = "HYPOTHESES" },
                label = { Text("Только гипотезы") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepSpace,
                    selectedLabelColor = PurpleTheorist
                )
            )
            FilterChip(
                selected = filterType == "SCENARIOS",
                onClick = { filterType = "SCENARIOS" },
                label = { Text("Только сценарии проверки") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepSpace,
                    selectedLabelColor = CyanAccent
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("hypotheses_lazy_column"),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filteredHypotheses.isEmpty() && filteredScenarios.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Пока нет гипотез для выбранного цикла.\nЗапустите эволюционный цикл на Арене.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                if (filterType == "ALL" || filterType == "HYPOTHESES") {
                    items(filteredHypotheses, key = { "hyp_${it.id}" }) { hyp ->
                        val cycleNumber = cycles.find { it.id == hyp.cycleId }?.cycleNumber ?: 1
                        HypothesisCard(hypothesis = hyp, cycleNumber = cycleNumber)
                    }
                }

                if (filterType == "ALL" || filterType == "SCENARIOS") {
                    items(filteredScenarios, key = { "scen_${it.id}" }) { scen ->
                        val cycleNumber = cycles.find { it.id == scen.cycleId }?.cycleNumber ?: 1
                        VerificationScenarioCard(scenario = scen, cycleNumber = cycleNumber)
                    }
                }
            }
        }
    }
}
