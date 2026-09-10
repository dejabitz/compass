package com.dejabit.compass.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Prefer fused rotation vector (gyro + accel + mag), fallback to geomagnetic rotation vector
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    private val _rawAzimuth = MutableStateFlow(0f)
    val rawAzimuth: StateFlow<Float> = _rawAzimuth.asStateFlow()

    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val accuracy: StateFlow<Int> = _accuracy.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var smoothedAzimuth = 0f
    private var isListening = false

    fun startListening() {
        if (isListening || rotationSensor == null) return
        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = true
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Remap coordinates for natural wrist viewing angle (top of watch is forward / North)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )

        SensorManager.getOrientation(remappedMatrix, orientation)

        val azimuthRad = orientation[0]
        val pitchRad = orientation[1]
        val rollRad = orientation[2]

        val targetAzimuth = ((Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f)
        
        // Shortest-path angular smoothing to prevent 359 -> 0 whip around
        smoothedAzimuth = calculateShortestPathSmoothing(smoothedAzimuth, targetAzimuth, alpha = 0.20f)

        _rawAzimuth.value = smoothedAzimuth
        _pitch.value = Math.toDegrees(pitchRad.toDouble()).toFloat()
        _roll.value = Math.toDegrees(rollRad.toDouble()).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _accuracy.value = accuracy
    }

    companion object {
        /**
         * Calculates smooth interpolated angle accounting for modular 360-degree boundary wrap.
         */
        fun calculateShortestPathSmoothing(current: Float, target: Float, alpha: Float): Float {
            var diff = (target - current + 540f) % 360f - 180f
            return ((current + diff * alpha) + 360f) % 360f
        }
    }
}
