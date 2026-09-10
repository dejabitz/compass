package com.dejabit.compass.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassSensorManagerTest {

    @Test
    fun calculateShortestPathSmoothing_standardForwardMovement() {
        // Turning from 10 deg to 20 deg with alpha 0.5 -> 15 deg
        val result = CompassSensorManager.calculateShortestPathSmoothing(10f, 20f, 0.5f)
        assertEquals(15f, result, 0.01f)
    }

    @Test
    fun calculateShortestPathSmoothing_standardBackwardMovement() {
        // Turning from 20 deg to 10 deg with alpha 0.5 -> 15 deg
        val result = CompassSensorManager.calculateShortestPathSmoothing(20f, 10f, 0.5f)
        assertEquals(15f, result, 0.01f)
    }

    @Test
    fun calculateShortestPathSmoothing_wraparoundAcrossZeroForward() {
        // Moving from 358 deg to 2 deg (crossing 0/360)
        // Shortest delta is +4 degrees, NOT -356 degrees
        val result = CompassSensorManager.calculateShortestPathSmoothing(358f, 2f, 0.5f)
        // 358 + (4 * 0.5) = 360 = 0 deg
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun calculateShortestPathSmoothing_wraparoundAcrossZeroBackward() {
        // Moving from 2 deg to 358 deg
        // Shortest delta is -4 degrees, NOT +356 degrees
        val result = CompassSensorManager.calculateShortestPathSmoothing(2f, 358f, 0.5f)
        // 2 + (-4 * 0.5) = 0 deg
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun calculateShortestPathSmoothing_staysWithin360Range() {
        for (current in 0..359 step 15) {
            for (target in 0..359 step 15) {
                val smoothed = CompassSensorManager.calculateShortestPathSmoothing(
                    current.toFloat(),
                    target.toFloat(),
                    0.25f
                )
                assertTrue("Angle $smoothed must be >= 0", smoothed >= 0f)
                assertTrue("Angle $smoothed must be < 360", smoothed < 360f)
            }
        }
    }
}
