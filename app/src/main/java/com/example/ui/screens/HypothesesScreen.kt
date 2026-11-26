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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import com.example.ui.components.HypothesisCard
import com.example.ui.components.HypothesisDagGraph
import com.example.ui.components.VerificationScenarioCard
import com.example.ui.spatial.LocalFocusGraph
import com.example.ui.spatial.focusOrderNode
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialCanvasColor
import com.example.ui.theme.spatialCardColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HypothesesScreen(
    hypotheses: List<ScientificHypothesisEntity>,
    scenarios: List<VerificationScenarioEntity>,
    cycles: List<EvolutionCycleEntity>,
    citationsCount: Int,
    isWideSpatialLayout: Boolean = false,
    onSearchArxiv: (String) -> Unit,
    onCrossPostCopied: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusGraph = LocalFocusGraph.current
    val leftRailTarget = if (isWideSpatialLayout) focusGraph.navHypotheses else FocusRequester.Cancel
    var viewModeTab by remember { mutableIntStateOf(0) } // 0: DAG Graph, 1: Cards List
    var selectedCycleFilter by remember { mutableStateOf<Long?>(null) }
    var filterType by remember { mutableStateOf("ALL") }

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
            .background(spatialCanvasColor())
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Header Row with arXiv button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "База Знаний Прогрессора",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "DAG эволюции, критерии Поппера и arXiv ($citationsCount)",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }
            IconButton(
                onClick = { onSearchArxiv(hypotheses.firstOrNull()?.title ?: "") },
                modifier = Modifier
                    .size(48.dp)
                    .focusOrderNode(
                        requester = focusGraph.hypArxiv,
                        next = if (viewModeTab == 0) focusGraph.dagResetView else FocusRequester.Default,
                        previous = focusGraph.hypTabCards,
                        left = focusGraph.hypTabCards,
                        right = FocusRequester.Cancel,
                        up = focusGraph.topSettings,
                        down = if (viewModeTab == 0) focusGraph.dagResetView else FocusRequester.Default
                    )
                    .spatialInteractive(accentColor = EmeraldHuman, shape = CircleShape)
                    .testTag("hypotheses_arxiv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Поиск публикаций arXiv",
                    tint = EmeraldHuman
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // View Mode Switcher: [Граф DAG | Карточки]
        TabRow(
            selectedTabIndex = viewModeTab,
            containerColor = spatialCardColor(),
            contentColor = CyanAccent,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[viewModeTab]),
                    color = CyanAccent
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .focusGroup()
                .testTag("hypotheses_view_mode_tabs")
        ) {
            Tab(
                selected = viewModeTab == 0,
                onClick = { viewModeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Граф DAG", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier
                    .focusOrderNode(
                        requester = focusGraph.hypTabDag,
                        next = focusGraph.hypTabCards,
                        previous = focusGraph.topSettings,
                        left = leftRailTarget,
                        right = focusGraph.hypTabCards,
                        up = focusGraph.topXrealToggle,
                        down = if (viewModeTab == 0) focusGraph.dagCanvas else FocusRequester.Default
                    )
                    .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(10.dp))
                    .testTag("hypotheses_tab_dag")
            )
            Tab(
                selected = viewModeTab == 1,
                onClick = { viewModeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Карточки (${hypotheses.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier
                    .focusOrderNode(
                        requester = focusGraph.hypTabCards,
                        next = focusGraph.hypArxiv,
                        previous = focusGraph.hypTabDag,
                        left = focusGraph.hypTabDag,
                        right = focusGraph.hypArxiv,
                        up = focusGraph.topNewSession,
                        down = if (viewModeTab == 0) focusGraph.dagCanvas else FocusRequester.Default
                    )
                    .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(10.dp))
                    .testTag("hypotheses_tab_cards")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModeTab == 0) {
            // Interactive DAG Graph Mode
            HypothesisDagGraph(
                cycles = cycles,
                hypotheses = hypotheses,
                scenarios = scenarios,
                onSearchArxivForQuery = onSearchArxiv,
                onCrossPostCopied = onCrossPostCopied,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Detailed Filterable Cards Mode
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
                            containerColor = spatialCardColor(),
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
                            containerColor = spatialCardColor(),
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("filter_cycle_${cycle.cycleNumber}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("ALL" to "Все", "HYPOTHESES" to "Гипотезы", "SCENARIOS" to "Сценарии")
                tabs.forEach { (key, label) ->
                    val isSelected = filterType == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = key },
                        label = { Text(label, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.25f),
                            selectedLabelColor = CyanAccent,
                            containerColor = spatialCardColor(),
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("filter_type_${key.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredHypotheses.isEmpty() && filteredScenarios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Гипотезы и протоколы верификации появятся после завершения эволюционного цикла.",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("hypotheses_lazy_column"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filterType == "ALL" || filterType == "HYPOTHESES") {
                        items(filteredHypotheses, key = { "hyp_${it.id}" }) { hyp ->
                            val cycleNumber = cycles.find { it.id == hyp.cycleId }?.cycleNumber ?: 1
                            HypothesisCard(
                                hypothesis = hyp,
                                cycleNumber = cycleNumber
                            )
                        }
                    }

                    if (filterType == "ALL" || filterType == "SCENARIOS") {
                        items(filteredScenarios, key = { "scen_${it.id}" }) { scen ->
                            val cycleNumber = cycles.find { it.id == scen.cycleId }?.cycleNumber ?: 1
                            VerificationScenarioCard(
                                scenario = scen,
                                cycleNumber = cycleNumber
                            )
                        }
                    }
                }
            }
        }
    }
}
