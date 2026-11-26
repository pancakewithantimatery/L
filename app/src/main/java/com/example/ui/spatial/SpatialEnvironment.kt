package com.example.ui.spatial

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.OledFocusRing

data class SpatialDisplayStatus(
    val hasExternalDisplay: Boolean = false,
    val isRunningOnExternalDisplay: Boolean = false,
    val externalDisplayName: String = "",
    val isInMultiWindow: Boolean = false,
    val displayCount: Int = 1
)

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Observes Android DisplayManager and Activity multi-window/display state in real time
 * to detect XREAL Air 2 Pro (via Beam Pro Nebula OS / USB-C DP Alt Mode / Samsung DeX).
 */
@Composable
fun rememberSpatialDisplayStatus(): SpatialDisplayStatus {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var status by remember { mutableStateOf(readSpatialDisplayStatus(context, activity)) }

    DisposableEffect(context, activity) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                status = readSpatialDisplayStatus(context, activity)
            }

            override fun onDisplayRemoved(displayId: Int) {
                status = readSpatialDisplayStatus(context, activity)
            }

            override fun onDisplayChanged(displayId: Int) {
                status = readSpatialDisplayStatus(context, activity)
            }
        }
        displayManager?.registerDisplayListener(listener, null)
        status = readSpatialDisplayStatus(context, activity)

        onDispose {
            displayManager?.unregisterDisplayListener(listener)
        }
    }

    return status
}

private fun readSpatialDisplayStatus(context: Context, activity: Activity?): SpatialDisplayStatus {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    val displays = runCatching { displayManager?.displays?.toList().orEmpty() }.getOrDefault(emptyList())
    val externalDisplays = displays.filter { it.displayId != Display.DEFAULT_DISPLAY }

    val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { activity?.display?.displayId }.getOrNull() ?: Display.DEFAULT_DISPLAY
    } else {
        @Suppress("DEPRECATION")
        runCatching { activity?.windowManager?.defaultDisplay?.displayId }.getOrNull() ?: Display.DEFAULT_DISPLAY
    }

    val isMultiWindow = runCatching { activity?.isInMultiWindowMode ?: false }.getOrDefault(false)
    val externalName = externalDisplays.firstOrNull()?.name?.takeIf { it.isNotBlank() }
        ?: if (currentDisplayId != Display.DEFAULT_DISPLAY) "XREAL / External Display" else ""

    return SpatialDisplayStatus(
        hasExternalDisplay = externalDisplays.isNotEmpty() || currentDisplayId != Display.DEFAULT_DISPLAY,
        isRunningOnExternalDisplay = currentDisplayId != Display.DEFAULT_DISPLAY,
        externalDisplayName = externalName,
        isInMultiWindow = isMultiWindow,
        displayCount = displays.size.coerceAtLeast(1)
    )
}

/**
 * Zero-Gravity / Reclined View Orientation Lock:
 * When active (e.g., lying down with XREAL Air 2 Pro or Beam Pro), ignores the phone's
 * accelerometer auto-rotation so the virtual window remains pinned and steady.
 * Also keeps the screen awake in Spatial Mode to prevent virtual display dimming.
 */
@Composable
fun ApplyZeroGravityOrientationLock(
    isSpatialModeActive: Boolean,
    zeroGravityLockEnabled: Boolean,
    isInMultiWindow: Boolean
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity, isSpatialModeActive, zeroGravityLockEnabled, isInMultiWindow) {
        if (activity != null) {
            if (isSpatialModeActive) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (zeroGravityLockEnabled && !isInMultiWindow) {
                    // SCREEN_ORIENTATION_LOCKED freezes current orientation and ignores gravity sensor
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else if (!zeroGravityLockEnabled) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            // Do not aggressively unlock on minor recomposition; lifecycle unmount resets if needed
        }
    }
}

/**
 * Eyes-Free Pointer Hover & Keyboard/D-Pad Focus Highlight Modifier:
 * Renders a crisp high-contrast glow ring when hovered by the Beam Pro Spatial Pointer / mouse
 * or focused via Bluetooth keyboard / D-Pad, ensuring clear targeting in Birdbath optics.
 *
 * Set [makeFocusable] to true ONLY on custom non-Material containers (like Box/Canvas) that don't
 * already supply their own clickable/focusable target, preventing duplicate Tab/D-Pad stops.
 */
fun Modifier.spatialInteractive(
    accentColor: Color = CyanAccent,
    shape: Shape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null,
    makeFocusable: Boolean = false
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isHovered by source.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    val isSpatial = LocalSpatialConfig.current.isSpatialModeActive

    val highlightColor = when {
        isFocused -> OledFocusRing
        isHovered -> accentColor
        else -> Color.Transparent
    }
    val bgTint = when {
        isFocused -> OledFocusRing.copy(alpha = if (isSpatial) 0.18f else 0.13f)
        isHovered -> accentColor.copy(alpha = if (isSpatial) 0.14f else 0.08f)
        else -> Color.Transparent
    }

    this
        .pointerHoverIcon(PointerIcon.Hand)
        .hoverable(interactionSource = source)
        .onFocusChanged { state ->
            isFocused = state.isFocused
        }
        .then(if (makeFocusable) Modifier.focusable(interactionSource = source) else Modifier)
        .then(
            if (isHovered || isFocused) {
                Modifier
                    .background(bgTint, shape)
                    .border(
                        width = if (isFocused) 2.dp else 1.5.dp,
                        color = highlightColor,
                        shape = shape
                    )
            } else {
                Modifier
            }
        )
}

/**
 * Reliable tactile feedback for Eyes-Free operation on Beam Pro / phone trackpad.
 */
fun performSpatialHaptic(
    context: Context,
    hapticFeedback: HapticFeedback,
    enabled: Boolean,
    strong: Boolean = false
) {
    if (!enabled) return
    runCatching {
        hapticFeedback.performHapticFeedback(
            if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
    }
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(
                    if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
                )
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        if (strong) 38L else 18L,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }
    }
}
