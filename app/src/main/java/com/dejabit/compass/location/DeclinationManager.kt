package com.dejabit.compass.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages magnetic declination calculations using Android's offline GeomagneticField model.
 * Strictly operates on-device; no coordinates are ever uploaded or transmitted over network.
 */
class DeclinationManager(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _declination = MutableStateFlow(0f)
    val declination: StateFlow<Float> = _declination.asStateFlow()

    private val _hasPermission = MutableStateFlow(checkPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    fun checkPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _hasPermission.value = granted
        return granted
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshDeclination(): Float {
        if (!checkPermission()) {
            _declination.value = 0f
            return 0f
        }

        try {
            // First check fast fused location
            val location: Location? = fusedLocationClient.lastLocation.await()
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            location?.let {
                val geomagneticField = GeomagneticField(
                    it.latitude.toFloat(),
                    it.longitude.toFloat(),
                    it.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                val dec = geomagneticField.declination
                _declination.value = dec
                return dec
            }
        } catch (_: Exception) {
            // Fallback to 0 if location unavailable or provider disabled
        }
        return _declination.value
    }
}
