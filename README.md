# Compass for Wear OS

A clean, battery-efficient, no-network, no-ads, open-source compass application built for modern Wear OS devices (Wear OS 3, 4, and 5) with Jetpack Compose.

## Features

- **Strictly Offline & Zero Network:** No `android.permission.INTERNET` requested. Zero analytics, trackers, or external ads.
- **Hardware Enforced:** Requires a physical magnetometer (`android.hardware.sensor.compass`) so it will only install on supported smartwatches.
- **True North via Offline Geomagnetic Field:**
  - Uses `ACCESS_COARSE_LOCATION` exclusively to compute local magnetic declination via Android's built-in World Magnetic Model (`android.hardware.GeomagneticField`).
  - No location coordinates ever leave your wrist.
  - Seamless fallback to Magnetic North if permission is denied.
- **Distinct Needle & Indicator Styling:**
  - **True North (Default):** Clean, solid safety accent needle.
  - **Magnetic North (Fallback / Toggle):** Distinct needle styling with muted ghost text `MAG` indicator.
  - Long-press the central needle to toggle between True and Magnetic North (with tactile haptic feedback).
- **Smooth Rotation:** Shortest-path angular delta smoothing preventing $359^\circ \leftrightarrow 0^\circ$ jump spins.
- **Wear OS Complication:** Watch-face complication showing current cardinal heading and degree with tap-to-launch.
- **Haptic Feedback:** Tactile ticks when turning and crisp confirmation feedback on True North.
- **Calibration Hint:** Subtle in-app alert when the magnetometer reports low or unreliable accuracy.

## Roadmap

- **Bearing Lock (Crown Navigation):** Rotate the digital crown to lock a target bearing with off-course guidance and haptic feedback.
- **Waypoint GPS Navigation:** Direct-to waypoint navigation (geocaching style) showing Great Circle bearing, distance, and arrival haptics.
- **Mobile Companion App:** Companion Android mobile app syncing offline GPX tracks and waypoints to the watch via the Wearable Data Layer API (peer-to-peer over Bluetooth without requiring `INTERNET` permission).

## Building

This project is built using standard Gradle:

```bash
./gradlew assembleDebug
```

## Package & License

- **Package:** `com.dejabit.compass`
- **License:** Apache 2.0
