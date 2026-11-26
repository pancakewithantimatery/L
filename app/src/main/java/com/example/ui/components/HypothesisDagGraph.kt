package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import com.example.ui.spatial.LocalFocusGraph
import com.example.ui.spatial.focusOrderNode
import com.example.ui.spatial.performSpatialHaptic
import com.example.ui.spatial.requestFocusSafely
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.OledHoverGlow
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.RoseWarning
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrueBlackOled
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialSurfaceColor
import kotlin.math.hypot

data class DagCycleNode(
    val cycle: EvolutionCycleEntity,
    val hypothesis: ScientificHypothesisEntity?,
    val scenario: VerificationScenarioEntity?,
    val popperStatus: String,
    val statusColor: Color
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HypothesisDagGraph(
    cycles: List<EvolutionCycleEntity>,
    hypotheses: List<ScientificHypothesisEntity>,
    scenarios: List<VerificationScenarioEntity>,
    onSearchArxivForQuery: (String) -> Unit,
    isDualPaneSidePanel: Boolean = false,
    onCrossPostCopied: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current
    val focusGraph = LocalFocusGraph.current

    val nodes = remember(cycles, hypotheses, scenarios) {
        cycles.sortedBy { it.cycleNumber }.map { cycle ->
            val hyp = hypotheses.find { it.cycleId == cycle.id }
            val scen = scenarios.find { it.cycleId == cycle.id }
            val score = cycle.viabilityScore.takeIf { it > 0 } ?: hyp?.confidenceScore ?: 65
            val (status, color) = when {
                score >= 75 -> "Подтверждена ($score%)" to EmeraldHuman
                score >= 50 -> "На проверке ($score%)" to AmberSkeptic
                else -> "Риск фальсификации ($score%)" to RoseWarning
            }
            DagCycleNode(cycle, hyp, scen, status, color)
        }
    }

    var selectedCycleId by remember(nodes) {
        mutableStateOf(nodes.lastOrNull()?.cycle?.id)
    }
    var hoveredCycleId by remember { mutableStateOf<Long?>(null) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var nodeCenters by remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }

    val selectedNode = nodes.find { it.cycle.id == selectedCycleId } ?: nodes.lastOrNull()
    val isTrueBlack = spatialConfig.isTrueBlackOled
    val aboveDagTarget = if (isDualPaneSidePanel) focusGraph.topSettings else focusGraph.hypTabDag
    val leftExitTarget = if (isDualPaneSidePanel) focusGraph.arenaCouncil else FocusRequester.Cancel
    val bottomExitTarget = if (isDualPaneSidePanel) FocusRequester.Cancel else focusGraph.navHypotheses

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusGroup()
    ) {
        // Legend + Reset View Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot("≥75%", EmeraldHuman)
                LegendDot("50-74%", AmberSkeptic)
                LegendDot("<50%", RoseWarning)
            }
            IconButton(
                onClick = {
                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                    panOffset = Offset.Zero
                    zoomScale = 1f
                },
                modifier = Modifier
                    .size(40.dp)
                    .focusOrderNode(
                        requester = focusGraph.dagResetView,
                        next = focusGraph.dagCanvas,
                        previous = if (isDualPaneSidePanel) focusGraph.arenaCouncil else focusGraph.hypArxiv,
                        left = leftExitTarget,
                        right = FocusRequester.Cancel,
                        up = aboveDagTarget,
                        down = focusGraph.dagCanvas
                    )
                    .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                    .testTag("dag_reset_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Сбросить масштаб графа",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Interactive DAG Canvas Box (True Black #000000 in XREAL Micro-OLED Mode)
        // Focusable node supporting D-Pad Left/Right cycle stepping & clean handoff to Inspector / Arena
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (isTrueBlack) {
                        Modifier.background(TrueBlackOled)
                    } else {
                        Modifier.background(
                            Brush.radialGradient(colors = listOf(DeepSpace, VoidDark))
                        )
                    }
                )
                .border(1.dp, spatialBorderColor(BorderSubtle), RoundedCornerShape(16.dp))
                .focusOrderNode(
                    requester = focusGraph.dagCanvas,
                    next = if (selectedNode != null) focusGraph.dagCopyThesis else bottomExitTarget,
                    previous = focusGraph.dagResetView,
                    up = focusGraph.dagResetView,
                    down = if (selectedNode != null) focusGraph.dagCopyThesis else bottomExitTarget,
                    left = leftExitTarget,
                    right = FocusRequester.Cancel
                )
                .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(16.dp), makeFocusable = true)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && nodes.isNotEmpty()) {
                        val currentIdx = nodes.indexOfFirst { it.cycle.id == selectedNode?.cycle?.id }.coerceAtLeast(0)
                        when (keyEvent.key) {
                            Key.DirectionLeft -> {
                                if (currentIdx > 0) {
                                    selectedCycleId = nodes[currentIdx - 1].cycle.id
                                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                    true
                                } else if (isDualPaneSidePanel) {
                                    focusGraph.arenaCouncil.requestFocusSafely()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (currentIdx < nodes.lastIndex) {
                                    selectedCycleId = nodes[currentIdx + 1].cycle.id
                                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                    true
                                } else {
                                    true
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .testTag("hypothesis_dag_canvas")
        ) {
            if (nodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Запустите хотя бы один цикл на Арене для построения DAG-графа эволюции гипотез.",
                        color = TextMuted,
                        fontSize = 12.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerHoverIcon(if (hoveredCycleId != null) PointerIcon.Hand else PointerIcon.Default)
                        .pointerInput(nodes, panOffset, zoomScale) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Move, PointerEventType.Enter -> {
                                            val pos = event.changes.firstOrNull()?.position
                                            if (pos != null) {
                                                val nearest = nodeCenters.entries.minByOrNull { (_, center) ->
                                                    hypot(center.x - pos.x, center.y - pos.y)
                                                }
                                                hoveredCycleId = if (
                                                    nearest != null &&
                                                    hypot(nearest.value.x - pos.x, nearest.value.y - pos.y) <= 52f * zoomScale
                                                ) {
                                                    nearest.key
                                                } else {
                                                    null
                                                }
                                            }
                                        }
                                        PointerEventType.Exit -> {
                                            hoveredCycleId = null
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(nodes) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.65f, 2.2f)
                                panOffset += pan
                            }
                        }
                        .pointerInput(nodes, panOffset, zoomScale) {
                            detectTapGestures { tapOffset ->
                                val hit = nodeCenters.entries.minByOrNull { (_, center) ->
                                    hypot(center.x - tapOffset.x, center.y - tapOffset.y)
                                }
                                if (hit != null && hypot(hit.value.x - tapOffset.x, hit.value.y - tapOffset.y) <= 56f * zoomScale) {
                                    selectedCycleId = hit.key
                                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Subtle cyber-grid (minimized in True Black OLED to keep background pure #000000)
                    val gridAlpha = if (isTrueBlack) 0.12f else 0.22f
                    val gridStep = 44f * zoomScale
                    var gx = panOffset.x % gridStep
                    while (gx < w) {
                        drawLine(
                            color = BorderSubtle.copy(alpha = gridAlpha),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        gx += gridStep
                    }
                    var gy = panOffset.y % gridStep
                    while (gy < h) {
                        drawLine(
                            color = BorderSubtle.copy(alpha = gridAlpha),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                        gy += gridStep
                    }

                    // Compute node positions in an evolutionary zigzag / branching DAG layout
                    val spacingX = 165f * zoomScale
                    val baseY = h * 0.46f + panOffset.y
                    val startX = (w / 2f) - ((nodes.size - 1) * spacingX / 2f) + panOffset.x

                    val computedCenters = mutableMapOf<Long, Offset>()
                    nodes.forEachIndexed { idx, node ->
                        val verticalOffset = when {
                            idx == 0 -> 0f
                            idx % 2 == 1 -> -52f * zoomScale
                            else -> 52f * zoomScale
                        }
                        val pos = Offset(startX + idx * spacingX, baseY + verticalOffset)
                        computedCenters[node.cycle.id] = pos
                    }
                    nodeCenters = computedCenters

                    // Draw Bezier evolution edges
                    for (i in 0 until nodes.size - 1) {
                        val from = computedCenters[nodes[i].cycle.id] ?: continue
                        val to = computedCenters[nodes[i + 1].cycle.id] ?: continue
                        val midX = (from.x + to.x) / 2f
                        val path = Path().apply {
                            moveTo(from.x, from.y)
                            cubicTo(midX, from.y, midX, to.y, to.x, to.y)
                        }
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = listOf(PurpleTheorist, CyanAccent),
                                start = from,
                                end = to
                            ),
                            style = Stroke(width = 3.5f * zoomScale, cap = StrokeCap.Round)
                        )
                    }

                    val nodeFill = if (isTrueBlack) TrueBlackOled else SurfaceCard
                    val satFill = if (isTrueBlack) TrueBlackOled else DeepSpace

                    // Draw Cycle nodes + Skeptic satellite micro-nodes + Beam Pro hover halo
                    nodes.forEach { node ->
                        val center = computedCenters[node.cycle.id] ?: return@forEach
                        val isSelected = node.cycle.id == selectedNode?.cycle?.id
                        val isHovered = node.cycle.id == hoveredCycleId

                        if (node.scenario != null) {
                            val satOffset = Offset(center.x, center.y + 62f * zoomScale)
                            drawLine(
                                color = AmberSkeptic.copy(alpha = 0.55f),
                                start = center,
                                end = satOffset,
                                strokeWidth = 2f * zoomScale
                            )
                            drawCircle(
                                color = satFill,
                                radius = 12f * zoomScale,
                                center = satOffset
                            )
                            drawCircle(
                                color = AmberSkeptic,
                                radius = 12f * zoomScale,
                                center = satOffset,
                                style = Stroke(width = 2f * zoomScale)
                            )
                        }

                        // Spatial Pointer Hover halo ring
                        if (isHovered) {
                            drawCircle(
                                color = OledHoverGlow.copy(alpha = 0.24f),
                                radius = 48f * zoomScale,
                                center = center
                            )
                            drawCircle(
                                color = OledHoverGlow,
                                radius = 40f * zoomScale,
                                center = center,
                                style = Stroke(width = 2f * zoomScale)
                            )
                        }

                        // Outer glow if selected
                        if (isSelected) {
                            drawCircle(
                                color = node.statusColor.copy(alpha = 0.26f),
                                radius = 44f * zoomScale,
                                center = center
                            )
                        }

                        // Main Cycle Node circle
                        drawCircle(
                            color = nodeFill,
                            radius = 30f * zoomScale,
                            center = center
                        )
                        drawCircle(
                            color = node.statusColor,
                            radius = 30f * zoomScale,
                            center = center,
                            style = Stroke(width = (if (isSelected || isHovered) 4f else 2.5f) * zoomScale)
                        )

                        // Inner core dot
                        drawCircle(
                            color = node.statusColor.copy(alpha = 0.9f),
                            radius = 10f * zoomScale,
                            center = center
                        )
                    }
                }

                // Overlay Cycle # pills at top-left for instant tap / D-Pad / pointer selection
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    nodes.forEach { node ->
                        val active = node.cycle.id == selectedNode?.cycle?.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (active) node.statusColor
                                    else if (isTrueBlack) TrueBlackOled
                                    else DeepSpace.copy(alpha = 0.85f)
                                )
                                .border(1.dp, node.statusColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .spatialInteractive(accentColor = node.statusColor, shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .pointerInput(node.cycle.id) {
                                    detectTapGestures {
                                        selectedCycleId = node.cycle.id
                                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                    }
                                }
                                .testTag("dag_node_chip_${node.cycle.cycleNumber}")
                        ) {
                            Text(
                                text = "Ц#${node.cycle.cycleNumber}",
                                color = if (active) TrueBlackOled else node.statusColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Selected Node Inspector Card with Quick Cross-Post Copy & arXiv Search
        if (selectedNode != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dag_inspector_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedNode.statusColor.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(selectedNode.statusColor.copy(alpha = 0.18f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ЦИКЛ #${selectedNode.cycle.cycleNumber} • ${selectedNode.popperStatus}",
                                color = selectedNode.statusColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Quick Cross-Post Copy Thesis + Popper Criterion
                            IconButton(
                                onClick = {
                                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                    val copyText = buildString {
                                        appendLine("ГИПОТЕЗА (Цикл #${selectedNode.cycle.cycleNumber}): ${selectedNode.hypothesis?.title ?: selectedNode.cycle.hypothesisTitle}")
                                        selectedNode.hypothesis?.let {
                                            appendLine("ТЕЗИС: ${it.thesis}")
                                            appendLine("МЕХАНИЗМ: ${it.mechanism}")
                                        }
                                        selectedNode.scenario?.let {
                                            appendLine("ФАЛЬСИФИКАЦИЯ ПОППЕРА: ${it.falsificationCriteria}")
                                        }
                                    }.trim()
                                    clipboard.setText(AnnotatedString(copyText))
                                    onCrossPostCopied?.invoke("Тезис скопирован")
                                    Toast.makeText(context, "Тезис и критерий Поппера скопированы", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .focusOrderNode(
                                        requester = focusGraph.dagCopyThesis,
                                        next = focusGraph.dagSearchArxiv,
                                        previous = focusGraph.dagCanvas,
                                        left = if (isDualPaneSidePanel) focusGraph.arenaChangeCouncil else FocusRequester.Cancel,
                                        right = focusGraph.dagSearchArxiv,
                                        up = focusGraph.dagCanvas,
                                        down = bottomExitTarget
                                    )
                                    .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                                    .testTag("dag_copy_thesis_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать тезис для кросс-постинга",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                                    val q = selectedNode.hypothesis?.title ?: selectedNode.cycle.hypothesisTitle
                                    onSearchArxivForQuery(q)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .focusOrderNode(
                                        requester = focusGraph.dagSearchArxiv,
                                        next = if (isDualPaneSidePanel) focusGraph.topXrealToggle else focusGraph.navHypotheses,
                                        previous = focusGraph.dagCopyThesis,
                                        left = focusGraph.dagCopyThesis,
                                        right = FocusRequester.Cancel,
                                        up = focusGraph.dagCanvas,
                                        down = bottomExitTarget
                                    )
                                    .spatialInteractive(accentColor = CyanAccent, shape = RoundedCornerShape(8.dp))
                                    .testTag("dag_search_arxiv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("arXiv", color = CyanAccent, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedNode.hypothesis?.title
                            ?: selectedNode.cycle.hypothesisTitle.ifEmpty { "Гипотеза цикла #${selectedNode.cycle.cycleNumber}" },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (spatialConfig.isSpatialModeActive) 14.5.sp else 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    selectedNode.hypothesis?.let { hyp ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hyp.thesis,
                            color = TextSecondary,
                            fontSize = if (spatialConfig.isSpatialModeActive) 12.5.sp else 11.5.sp,
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    selectedNode.scenario?.let { scen ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(spatialSurfaceColor())
                                .border(0.8.dp, AmberSkeptic.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Поппер: ${scen.falsificationCriteria}",
                                color = AmberSkeptic,
                                fontSize = 11.5.sp,
                                lineHeight = 15.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
