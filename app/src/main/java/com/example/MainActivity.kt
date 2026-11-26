package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainAppScreen
import com.example.ui.ProgressorViewModel
import com.example.ui.spatial.ApplyZeroGravityOrientationLock
import com.example.ui.spatial.rememberSpatialDisplayStatus
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpatialUiConfig

class MainActivity : ComponentActivity() {
    private val viewModel: ProgressorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val manualSpatialMode by viewModel.xrealSpatialMode.collectAsStateWithLifecycle()
            val autoDetectExternal by viewModel.xrealAutoDetect.collectAsStateWithLifecycle()
            val zeroGravityLock by viewModel.xrealZeroGravityLock.collectAsStateWithLifecycle()
            val overscanDp by viewModel.xrealOverscanDp.collectAsStateWithLifecycle()
            val eyesFreeFeedback by viewModel.xrealEyesFreeFeedback.collectAsStateWithLifecycle()

            val displayStatus = rememberSpatialDisplayStatus()
            val isSpatialActive = manualSpatialMode || (autoDetectExternal && displayStatus.hasExternalDisplay)

            ApplyZeroGravityOrientationLock(
                isSpatialModeActive = isSpatialActive,
                zeroGravityLockEnabled = zeroGravityLock,
                isInMultiWindow = displayStatus.isInMultiWindow
            )

            val spatialConfig = SpatialUiConfig(
                isSpatialModeActive = isSpatialActive,
                isTrueBlackOled = isSpatialActive,
                overscanPadding = if (isSpatialActive) overscanDp.dp else 0.dp,
                isZeroGravityLocked = isSpatialActive && zeroGravityLock,
                isEyesFreeFeedbackEnabled = eyesFreeFeedback,
                hasExternalDisplay = displayStatus.hasExternalDisplay,
                externalDisplayName = displayStatus.externalDisplayName,
                isInMultiWindow = displayStatus.isInMultiWindow
            )

            MyApplicationTheme(spatialConfig = spatialConfig) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
