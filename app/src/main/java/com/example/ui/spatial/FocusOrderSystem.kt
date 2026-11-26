package com.example.ui.spatial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.theme.LocalSpatialConfig

/**
 * Centralized FocusOrder graph for Eyes-Free D-Pad & Bluetooth keyboard navigation.
 * Connects TopAppBar, Primary Navigation (NavigationBar / NavigationRail), Arena controls,
 * TTS Podcast bar, DAG Graph & Inspector, Council controls, and Sessions list into a
 * deterministic 1D (Tab / Shift+Tab) and 2D (Up / Down / Left / Right) traversal flow.
 */
@Immutable
class ProgressorFocusGraph {
    // --- Zone 1: Top App Bar ---
    val topVoiceMic = FocusRequester()
    val topXrealToggle = FocusRequester()
    val topNewSession = FocusRequester()
    val topSettings = FocusRequester()

    // --- Zone 2: Primary Navigation (Bottom Bar or Spatial NavigationRail) ---
    val navArena = FocusRequester()
    val navHypotheses = FocusRequester()
    val navAiLab = FocusRequester()
    val navCouncil = FocusRequester()
    val navTopics = FocusRequester()

    // --- Zone 3: Arena Header Strip ---
    val arenaArxiv = FocusRequester()
    val arenaTts = FocusRequester()
    val arenaCopySynth = FocusRequester()
    val arenaKeepExport = FocusRequester()
    val arenaExport = FocusRequester()
    val arenaCouncil = FocusRequester()

    // --- Zone 3b: Arena TTS Podcast Bar (when visible) ---
    val ttsPrev = FocusRequester()
    val ttsPlayPause = FocusRequester()
    val ttsNext = FocusRequester()
    val ttsSpeed = FocusRequester()
    val ttsClose = FocusRequester()

    // --- Zone 3c: Arena Bottom Action Bar ---
    val arenaInput = FocusRequester()
    val arenaPrimaryAction = FocusRequester() // Launch Cycle or Send Intervention
    val arenaChangeCouncil = FocusRequester()

    // --- Zone 4: Hypotheses & Live DAG Pane ---
    val hypArxiv = FocusRequester()
    val hypTabDag = FocusRequester()
    val hypTabCards = FocusRequester()
    val dagResetView = FocusRequester()
    val dagCanvas = FocusRequester()
    val dagCopyThesis = FocusRequester()
    val dagSearchArxiv = FocusRequester()

    // --- Zone 5: Council Screen ---
    val councilConfigBtn = FocusRequester()
    val councilRoleDesignerBtn = FocusRequester()
    val councilFastLaunchBtn = FocusRequester()

    // --- Zone 6: Sessions Screen ---
    val sessionsNewBtn = FocusRequester()

    fun navRequesterForIndex(index: Int): FocusRequester = when (index) {
        0 -> navArena
        1 -> navHypotheses
        2 -> navAiLab
        3 -> navCouncil
        else -> navTopics
    }

    /**
     * Returns the primary focus entry target for a given tab index.
     */
    fun contentEntryForTab(tabIndex: Int): FocusRequester = when (tabIndex) {
        0 -> arenaArxiv
        1 -> hypTabDag
        3 -> councilConfigBtn
        4 -> sessionsNewBtn
        else -> FocusRequester.Default
    }

    /**
     * Returns the bottom-most focus target for a given tab index (for moving Up from Bottom NavigationBar).
     */
    fun contentBottomForTab(tabIndex: Int): FocusRequester = when (tabIndex) {
        0 -> arenaPrimaryAction
        1 -> dagCopyThesis
        3 -> councilFastLaunchBtn
        4 -> sessionsNewBtn
        else -> FocusRequester.Default
    }
}

val LocalFocusGraph = staticCompositionLocalOf { ProgressorFocusGraph() }

@Composable
fun rememberProgressorFocusGraph(): ProgressorFocusGraph {
    return remember { ProgressorFocusGraph() }
}

/**
 * Safely requests focus without throwing if the target is currently detached from the composition.
 */
fun FocusRequester.requestFocusSafely(): Boolean {
    return runCatching {
        requestFocus()
        true
    }.getOrDefault(false)
}

/**
 * Attaches a node to the Progressor FocusOrder system with explicit 1D (Tab / Shift+Tab)
 * and 2D (D-Pad Up / Down / Left / Right) neighbors, plus subtle eyes-free tactile tick on focus.
 */
fun Modifier.focusOrderNode(
    requester: FocusRequester,
    next: FocusRequester = FocusRequester.Default,
    previous: FocusRequester = FocusRequester.Default,
    up: FocusRequester = FocusRequester.Default,
    down: FocusRequester = FocusRequester.Default,
    left: FocusRequester = FocusRequester.Default,
    right: FocusRequester = FocusRequester.Default,
    hapticOnFocus: Boolean = true,
    onFocused: (() -> Unit)? = null
): Modifier = composed {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current

    this
        .focusRequester(requester)
        .focusProperties {
            this.next = next
            this.previous = previous
            this.up = up
            this.down = down
            this.left = left
            this.right = right
        }
        .onFocusChanged { state ->
            if (state.isFocused) {
                if (hapticOnFocus && spatialConfig.isSpatialModeActive && spatialConfig.isEyesFreeFeedbackEnabled) {
                    performSpatialHaptic(context, haptic, enabled = true, strong = false)
                }
                onFocused?.invoke()
            }
        }
}
