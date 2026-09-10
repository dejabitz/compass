package com.dejabit.compass.ui

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.dejabit.compass.presentation.CompassViewModel
import com.dejabit.compass.ui.components.CalibrationBanner
import com.dejabit.compass.ui.components.CompassDial
import com.dejabit.compass.ui.components.LocationRationaleDialog

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val showRationale by viewModel.showRationaleDialog.collectAsState()
    val view = LocalView.current

    // Permission launcher for coarse location (True North declination)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.dismissRationale()
        }
    }

    // Gentle tactile feedback when crossing exactly onto Cardinal North (within +/- 1.5 deg)
    val isNearNorth = state.azimuth in 0f..2f || state.azimuth in 358f..360f
    LaunchedEffect(isNearNorth) {
        if (isNearNorth && !state.isAmbient) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    Scaffold(
        timeText = {
            if (!state.isAmbient) {
                TimeText()
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Central rotating Compass Dial with custom Canvas
            CompassDial(state = state)

            // Center needle long-press area to toggle True / Magnetic North
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .pointerInput(state.isAmbient) {
                        if (!state.isAmbient) {
                            detectTapGestures(
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    viewModel.toggleNorthMode()
                                }
                            )
                        }
                    }
            )

            // Calibration Banner (shows when sensor reports UNRELIABLE)
            CalibrationBanner(
                visible = state.needsCalibration && !state.isAmbient,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Educational Rationale Dialog for Coarse Location (Requirement E)
    LocationRationaleDialog(
        show = showRationale,
        onConfirm = {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onDismiss = {
            viewModel.dismissRationale()
        }
    )
}
