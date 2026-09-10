package com.dejabit.compass.model

import android.hardware.SensorManager

/**
 * Immutable state representing current compass telemetry, calibration, and display modes.
 */
data class CompassState(
    val azimuth: Float = 0f,              // Degrees [0, 360)
    val pitch: Float = 0f,                // Tilt forward/backward
    val roll: Float = 0f,                 // Tilt left/right
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isTrueNorth: Boolean = true,      // True North enabled (via GeomagneticField)
    val declination: Float = 0f,          // Local magnetic declination angle in degrees
    val hasLocationPermission: Boolean = false,
    val isCalibrating: Boolean = false,
    val isAmbient: Boolean = false
) {
    val isAccurate: Boolean
        get() = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM

    val needsCalibration: Boolean
        get() = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE

    val formattedDegrees: String
        get() = "%03d°".format(azimuth.toInt().coerceIn(0, 359))

    val cardinalDirection: String
        get() = when ((((azimuth + 22.5f) % 360f) / 45f).toInt()) {
            0 -> "N"
            1 -> "NE"
            2 -> "E"
            3 -> "SE"
            4 -> "S"
            5 -> "SW"
            6 -> "W"
            7 -> "NW"
            else -> "N"
        }
}
