package com.dejabit.compass.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dejabit.compass.location.DeclinationManager
import com.dejabit.compass.model.CompassState
import com.dejabit.compass.sensor.CompassSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager = CompassSensorManager(application)
    private val declinationManager = DeclinationManager(application)

    private val _isTrueNorth = MutableStateFlow(true)
    val isTrueNorth: StateFlow<Boolean> = _isTrueNorth.asStateFlow()

    private val _showRationaleDialog = MutableStateFlow(false)
    val showRationaleDialog: StateFlow<Boolean> = _showRationaleDialog.asStateFlow()

    private val _isAmbient = MutableStateFlow(false)
    val isAmbient: StateFlow<Boolean> = _isAmbient.asStateFlow()

    val uiState: StateFlow<CompassState> = combine(
        sensorManager.rawAzimuth,
        sensorManager.pitch,
        sensorManager.roll,
        sensorManager.accuracy,
        declinationManager.declination,
        _isTrueNorth,
        declinationManager.hasPermission,
        _isAmbient
    ) { rawAzimuth, pitch, roll, accuracy, declination, isTrueNorth, hasPerm, isAmbient ->
        val effectiveAzimuth = if (isTrueNorth && hasPerm) {
            (rawAzimuth + declination + 360f) % 360f
        } else {
            rawAzimuth
        }

        CompassState(
            azimuth = effectiveAzimuth,
            pitch = pitch,
            roll = roll,
            accuracy = accuracy,
            isTrueNorth = isTrueNorth && hasPerm,
            declination = declination,
            hasLocationPermission = hasPerm,
            isAmbient = isAmbient
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CompassState()
    )

    fun onResume() {
        declinationManager.checkPermission()
        sensorManager.startListening()
        refreshDeclination()
    }

    fun onPause() {
        sensorManager.stopListening()
    }

    fun setAmbientMode(ambient: Boolean) {
        _isAmbient.value = ambient
        if (ambient) {
            sensorManager.stopListening()
        } else {
            sensorManager.startListening()
        }
    }

    fun toggleNorthMode() {
        if (!declinationManager.checkPermission()) {
            // Need permission for True North -> trigger rationale
            _showRationaleDialog.value = true
        } else {
            _isTrueNorth.value = !_isTrueNorth.value
        }
    }

    fun onPermissionGranted() {
        _showRationaleDialog.value = false
        _isTrueNorth.value = true
        refreshDeclination()
    }

    fun dismissRationale() {
        _showRationaleDialog.value = false
        // Keep in Magnetic mode if user opts out
        _isTrueNorth.value = false
    }

    fun requestRationale() {
        _showRationaleDialog.value = true
    }

    private fun refreshDeclination() {
        viewModelScope.launch {
            declinationManager.refreshDeclination()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
    }
}
