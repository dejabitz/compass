# Wear OS Compass: Zero-to-Hero Architecture & Code Walkthrough

Welcome to the comprehensive technical guide for **Compass**, an open-source, zero-network, battery-efficient compass application built for modern Wear OS (Wear OS 3, 4, and 5).

This document is designed for developers who already understand programming concepts in languages like **Java** or **Python**, but who may have **little or no prior experience with Android, Kotlin, or Wear OS**. 

By the end of this guide, you will understand:
1. The mental model of modern Android development (Declarative UI vs. Imperative OOP).
2. The exact mechanics of watch hardware (magnetometers, gyroscopes, and coordinate spaces).
3. How this codebase is organized, file-by-file, with line-by-line code explanations.
4. The mechanics of **Bearing Lock** (navigating with the watch crown) and the roadmap for **Waypoint GPS Navigation** (geocaching style).

---

## Table of Contents
1. [Android & Wear OS Mental Model for Java/Python Devs](#1-android--wear-os-mental-model-for-javapython-devs)
2. [Project & Build Architecture (Gradle & Manifest)](#2-project--build-architecture-gradle--manifest)
3. [File-by-File Code Walkthrough](#3-file-by-file-code-walkthrough)
   - [A. Data Model (`CompassState.kt`)](#a-data-model-compassstatekt)
   - [B. Sensor & Coordinate Geometry (`CompassSensorManager.kt`)](#b-sensor--coordinate-geometry-compasssensormanagerkt)
   - [C. Offline Geomagnetic Declination (`DeclinationManager.kt`)](#c-offline-geomagnetic-declination-declinationmanagerkt)
   - [D. State Management (`CompassViewModel.kt`)](#d-state-management-compassviewmodelkt)
   - [E. Custom Canvas UI (`CompassDial.kt`)](#e-custom-canvas-ui-compassdialkt)
   - [F. Interaction & Rationale (`CompassScreen.kt` & Dialogs)](#f-interaction--rationale-compassscreenkt--dialogs)
   - [G. The Entry Point (`MainActivity.kt`)](#g-the-entry-point-mainactivitykt)
   - [H. Watch Face Complications (`CompassComplicationService.kt`)](#h-watch-face-complications-compasscomplicationservicekt)
4. [Technique Deep Dive: Crown Bearing Lock](#4-technique-deep-dive-crown-bearing-lock)
5. [Roadmap: Geocaching & Waypoint Navigation](#5-roadmap-geocaching--waypoint-navigation)

---

## 1. Android & Wear OS Mental Model for Java/Python Devs

If you come from standard Java (Swing/JavaFX) or Python (Tkinter/PyQt), traditional desktop programming relies on **imperative mutation**: you instantiate a button, attach a listener, and when something happens, you call `label.setText("New Angle")`.

Modern Android uses **Jetpack Compose**, which is **declarative and reactive** (conceptually similar to React or Flutter):

$$\text{State} \longrightarrow f(\text{State}) = \text{UI}$$

- You never manually update a widget. Instead, you declare what the screen looks like for a given `CompassState`.
- When the hardware sensor detects rotation, it updates a `StateFlow` (an observable stream of values, like Python's `asyncio.Queue` or a Java `Publisher`).
- The Compose engine automatically detects the change and redraws (**recomposes**) only the parts of the screen that depend on that value.

### Watch Constraints vs. Phone Apps
1. **Screen Shape:** Watches are round. Screen corners are clipped off. Everything must be measured from a center pivot.
2. **Battery:** A smartwatch battery is tiny (often 300–400 mAh, roughly 1/10th of a phone). If an app listens to sensors at 100 Hz continuously, the battery will die in 2 hours. Listeners must be aggressively paused when the wrist drops.
3. **Always-On Display (Ambient Mode):** When the user lowers their arm, Wear OS doesn't close the app; it enters an "ambient" state where pixels are dimmed to black & white at 1 Hz.

---

## 2. Project & Build Architecture (Gradle & Manifest)

### Gradle Build Files
Android projects use **Gradle**. In modern setups, dependencies are listed in a Version Catalog ([`gradle/libs.versions.toml`](file:///Users/debajit/Code/wear/compass/gradle/libs.versions.toml)) and configured using Kotlin DSL (`build.gradle.kts`):

- [`settings.gradle.kts`](file:///Users/debajit/Code/wear/compass/settings.gradle.kts): Declares where to download dependencies (Google's Maven repo and Maven Central) and includes the `:app` module.
- [`app/build.gradle.kts`](file:///Users/debajit/Code/wear/compass/app/build.gradle.kts): Defines:
  - `minSdk = 30`: Wear OS 3+ (Android 11+).
  - `targetSdk = 34`: Wear OS 4 / Android 14.
  - `applicationId = "com.dejabit.compass"`.

### The Manifest & Security Guarantee
Every Android app has an [`AndroidManifest.xml`](file:///Users/debajit/Code/wear/compass/app/src/main/AndroidManifest.xml) that tells the OS what the app needs:

```xml
<!-- 1. Enforce watch hardware -->
<uses-feature android:name="android.hardware.type.watch" android:required="true" />

<!-- 2. HARDWARE FILTER: Require a physical magnetometer -->
<uses-feature android:name="android.hardware.sensor.compass" android:required="true" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />

<!-- 3. Offline location for True North calculation -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.VIBRATE" />

<!-- NOTICE: android.permission.INTERNET is completely missing! -->
```

> **Why this matters:**
> - `android.hardware.sensor.compass` with `required="true"` instructs Google Play and device package managers to hide or reject installation on watches that do not have a hardware magnetic sensor.
> - By completely omitting `INTERNET`, the OS gives this app **no network socket access**. It cannot leak coordinates or show ads even if an external library attempted to.

---

## 3. File-by-File Code Walkthrough

### A. Data Model (`CompassState.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/model/CompassState.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/model/CompassState.kt)

In Python you might use a `@dataclass`, or in Java 17 a `record`. In Kotlin, we use a `data class`:

```kotlin
data class CompassState(
    val azimuth: Float = 0f,              // Degrees [0, 360)
    val pitch: Float = 0f,                // Forward/backward tilt
    val roll: Float = 0f,                 // Left/right tilt
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isTrueNorth: Boolean = true,      // True North vs Magnetic North
    val declination: Float = 0f,          // Local magnetic offset in degrees
    val hasLocationPermission: Boolean = false,
    val isAmbient: Boolean = false
)
```

Kotlin allows us to add calculated properties directly on the class:
```kotlin
val formattedDegrees: String
    get() = "%03d°".format(azimuth.toInt().coerceIn(0, 359))

val cardinalDirection: String
    get() = when (((azimuth + 22.5f) % 360) / 45) {
        0f, 0 -> "N"
        1f, 1 -> "NE"
        2f, 2 -> "E"
        3f, 3 -> "SE"
        4f, 4 -> "S"
        5f, 5 -> "SW"
        6f, 6 -> "W"
        7f, 7 -> "NW"
        else -> "N"
    }
```
*How this math works:* The compass rose divides $360^\circ$ into 8 segments of $45^\circ$ each. Adding $22.5^\circ$ offsets the division so that "North" spans from $337.5^\circ$ to $22.5^\circ$.

---

### B. Sensor & Coordinate Geometry (`CompassSensorManager.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/sensor/CompassSensorManager.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/sensor/CompassSensorManager.kt)

This class talks to the watch's Linux kernel sensor drivers via Android's `SensorEventListener`.

#### 1. Fused Sensors vs Raw Magnetometers
Instead of reading a raw, noisy magnetometer directly, we register for `Sensor.TYPE_ROTATION_VECTOR`:
```kotlin
private val rotationSensor: Sensor? =
    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
```
*Explanation:* The OS runs an internal sensor fusion algorithm (Kalman filter) combining accelerometer, gyroscope, and magnetometer. This eliminates high-frequency magnetic noise from nearby electronics.

#### 2. Compensating for Wrist Tilt
When reading a watch, your wrist is tilted towards your eyes. In raw Android phone coordinates, $Y$ points up towards the top edge of the screen and $Z$ points out perpendicular from the glass.
If you tilt the watch, $Y$ and $Z$ rotate in 3D space.

```kotlin
SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

// Remap coordinate axes: X remains X, but Z becomes the forward axis
SensorManager.remapCoordinateSystem(
    rotationMatrix,
    SensorManager.AXIS_X,
    SensorManager.AXIS_Z,
    remappedMatrix
)

SensorManager.getOrientation(remappedMatrix, orientation)
```
This transformation ensures the compass heading points in the direction the 12 o'clock bezel is facing, even when your arm is angled.

#### 3. Solving the $359^\circ \leftrightarrow 0^\circ$ Wrap-Around Problem
If you are facing North ($0^\circ$) and rotate slightly left to $359^\circ$, a standard linear interpolation:
$$\text{lerp}(0^\circ, 359^\circ, 0.2) = 0 + (359 - 0) \times 0.2 = 71.8^\circ$$
would cause the compass needle to spin backwards $358^\circ$ all the way around the dial!

We solve this using **modular shortest-path angular delta math**:
```kotlin
fun calculateShortestPathSmoothing(current: Float, target: Float, alpha: Float): Float {
    // Computes difference strictly constrained to [-180, +180]
    var diff = (target - current + 540f) % 360f - 180f
    return ((current + diff * alpha) + 360f) % 360f
}
```
- From $0^\circ$ to $359^\circ$: `diff = -1°`. The needle nudges $1^\circ$ to the left.
- From $359^\circ$ to $1^\circ$: `diff = +2°`. The needle nudges $2^\circ$ to the right.
- This is covered by unit tests in [`CompassSensorManagerTest.kt`](file:///Users/debajit/Code/wear/compass/app/src/test/java/com/dejabit/compass/sensor/CompassSensorManagerTest.kt).

---

### C. Offline Geomagnetic Declination (`DeclinationManager.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/location/DeclinationManager.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/location/DeclinationManager.kt)

Magnetic North is not the same as True Geographic North. Depending on where you are on Earth, the magnetic pole can differ by up to $\pm 30^\circ$ (magnetic declination).

Most apps make HTTP requests to an API to find declination. **We do not need the internet:** Android includes the **World Magnetic Model (WMM)** built directly into its offline OS library via `android.hardware.GeomagneticField`:

```kotlin
val geomagneticField = GeomagneticField(
    location.latitude.toFloat(),
    location.longitude.toFloat(),
    location.altitude.toFloat(),
    System.currentTimeMillis() // Date matters because magnetic poles drift over years
)
val declination = geomagneticField.declination // in degrees (+ = East, - = West)
```

We only request `ACCESS_COARSE_LOCATION` (city/neighborhood level accuracy). That is more than enough for magnetic field tables (which only vary over tens of kilometers).

---

### D. State Management (`CompassViewModel.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/presentation/CompassViewModel.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/presentation/CompassViewModel.kt)

A `ViewModel` in Android survives configuration changes (like screen rotations or theme changes). It connects the low-level sensors to the high-level UI.

We combine multiple asynchronous flows into a single unified `uiState`:
```kotlin
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
    // If True North is selected AND permission is granted, apply the offset:
    val effectiveAzimuth = if (isTrueNorth && hasPerm) {
        (rawAzimuth + declination + 360f) % 360f
    } else {
        rawAzimuth // Fallback to raw magnetic reading
    }

    CompassState(
        azimuth = effectiveAzimuth,
        isTrueNorth = isTrueNorth && hasPerm,
        ...
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompassState())
```

---

### E. Custom Canvas UI (`CompassDial.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/ui/components/CompassDial.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/ui/components/CompassDial.kt)

Smartwatch UI needs to be rendered on hardware-accelerated 2D Canvas for fluid 60 FPS performance without creating heavy object allocations on every frame.

#### 1. The Rotating Rose
Instead of keeping the rose static and drawing a rotating needle, marine and field compasses rotate the entire dial so that the direction you are walking is at the top (12 o'clock):
```kotlin
// Negative azimuth rotates the dial counter-clockwise
rotate(degrees = -state.azimuth, pivot = center) {
    // Draw 360 tick marks
    for (deg in 0 until 360 step 5) {
        val rad = Math.toRadians(deg.toDouble() - 90.0)
        ...
        drawLine(color = tickColor, start = Offset(startX, startY), end = Offset(endX, endY))
    }
    // Draw North arrow
    drawCenterNeedle(...)
}
```

#### 2. Needle Styling & User Customization
- **True North:** Solid Vibrant Safety Orange arrow (`#FF5722`), with standard white typography. No distracting badges.
- **Magnetic North:** Distinct Cyan (`#00E5FF`) with a dashed/hollow needle style and ghosted `"MAG"` text:
```kotlin
if (state.isTrueNorth) {
    drawPath(path = northPath, color = TrueNorthAccent) // Solid orange
} else {
    drawPath(
        path = northPath,
        color = MagneticNorthAccent,
        style = Stroke(
            width = 2.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f) // Dashed
        )
    )
}
```

---

### F. Interaction & Rationale (`CompassScreen.kt` & Dialogs)
**File:** [`app/src/main/java/com/dejabit/compass/ui/CompassScreen.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/ui/CompassScreen.kt)

#### Long-Press on the Needle
Accidental touches on watches happen constantly due to sleeves and jackets. Instead of a simple tap, toggling between True North and Magnetic North requires a **deliberate long-press directly on the needle**:
```kotlin
Box(
    modifier = Modifier
        .size(130.dp) // Central target area
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
```

#### Tactile North Lock Haptics
When your body turns and aligns directly with North ($0^\circ$), the watch vibrates with a subtle confirmation pulse:
```kotlin
val isNearNorth = state.azimuth in 0f..2f || state.azimuth in 358f..360f
LaunchedEffect(isNearNorth) {
    if (isNearNorth && !state.isAmbient) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}
```

---

### G. The Entry Point (`MainActivity.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/MainActivity.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/MainActivity.kt)

An `Activity` in Android represents a single window with a user interface.
- `onResume()`: Triggered when the user looks at the watch. We start sensor listeners and recalculate declination.
- `onPause()`: Triggered when the screen dims or user navigates away. We **immediately unregister sensor listeners** to stop battery drain.

---

### H. Watch Face Complications (`CompassComplicationService.kt`)
**File:** [`app/src/main/java/com/dejabit/compass/complication/CompassComplicationService.kt`](file:///Users/debajit/Code/wear/compass/app/src/main/java/com/dejabit/compass/complication/CompassComplicationService.kt)

A "complication" is a small widget on a watch face (e.g. battery percentage, step count, or compass).

```kotlin
class CompassComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        // PendingIntent to launch the app when tapped
        val tapIntent = PendingIntent.getActivity(...)

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(headingText).build(),
            contentDescription = PlainComplicationText.Builder("Compass").build()
        )
        .setMonochromaticImage(compassIcon)
        .setTapAction(tapIntent)
        .build()
    }
}
```
*Platform Constraint:* Complications on Wear OS are rate-limited by the operating system (updates occur roughly every 20–60 seconds to preserve battery). Tapping the complication opens `MainActivity`, which runs the real-time 60 FPS sensor loop.

---

## 4. Technique Deep Dive: Crown Bearing Lock

### What is it?
In navigation and orienteering, this technique is known as **Course Deviation Indicator (CDI)** or **Sight 'N Go (Bearing Lock)**:
1. You spot a landmark (e.g. a distant mountain peak at $045^\circ$).
2. You lock $045^\circ$ as your **Target Bearing**.
3. As you hike through trees and uneven terrain where you lose sight of the peak, the compass dial shows an **off-course indicator**:
   - If you walk straight towards $045^\circ$, the indicator is centered and green.
   - If you drift to $060^\circ$, it tells you: `Turn Left 15°`.

### How to Implement via Wear OS Crown
Wear OS watches feature a physical rotating digital crown or rotating bezel. In Compose for Wear OS, we can capture this with `onRotaryScrollEvent`:

```kotlin
// 1. In State:
var targetBearing: Float? by remember { mutableStateOf(null) }

// 2. Attached to the Screen Modifier:
Modifier
    .onRotaryScrollEvent { event ->
        val delta = event.verticalScrollPixels / 10f
        val current = targetBearing ?: state.azimuth
        targetBearing = (current + delta + 360f) % 360f
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        true
    }
    .focusRequester(focusRequester)
    .focusable()
```

### Visual Representation on the Dial:
- A distinct translucent wedge/arc drawn on the dial rim at the `targetBearing`.
- When `abs(state.azimuth - targetBearing) < 3°`, fire `HapticFeedbackConstants.CONFIRM` and light up the wedge in neon green.

---

## 5. Roadmap: Geocaching & Waypoint Navigation

The next milestone for this application is **point-to-point GPS waypoint navigation** (the style used by handheld Garmin units for backcountry hiking and geocaching).

### Mathematical Principles

Given your current coordinates $(\text{lat}_1, \text{lon}_1)$ and a target waypoint $(\text{lat}_2, \text{lon}_2)$:

#### 1. Great Circle Bearing (Direction to walk)
$$\theta = \operatorname{atan2}\left(\sin(\Delta\lambda)\cos(\varphi_2),\; \cos(\varphi_1)\sin(\varphi_2) - \sin(\varphi_1)\cos(\varphi_2)\cos(\Delta\lambda)\right)$$
where $\varphi$ is latitude in radians and $\Delta\lambda$ is longitude difference in radians.

In Kotlin:
```kotlin
fun calculateBearing(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
    val phi1 = Math.toRadians(fromLat)
    val phi2 = Math.toRadians(toLat)
    val deltaLambda = Math.toRadians(toLon - fromLon)

    val y = Math.sin(deltaLambda) * Math.cos(phi2)
    val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
    val bearing = Math.toDegrees(Math.atan2(y, x)).toFloat()
    return (bearing + 360f) % 360f
}
```

#### 2. Distance to Waypoint (Haversine Formula)
$$d = 2R \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\varphi}{2}\right) + \cos(\varphi_1)\cos(\varphi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)$$
where $R \approx 6,371,000\text{ m}$ (Earth's radius).

### UI Implementation for Waypoints
1. **Target Bug / Pointer:** On the rotating compass dial, a floating marker (e.g. a small neon beacon pin) points toward the target waypoint.
2. **Distance Readout:** In the center of the dial, below the degrees, the remaining distance is displayed:
   - `1.4 km` when far away.
   - `42 m` when close (geocaching hunt radius).
3. **Arrival Haptic:** A continuous vibration pulse when within $10\text{ m}$ of the waypoint.
4. **Offline GPX / Waypoint Entry:** Coordinates entered manually via a clean Wear OS number-picker or imported via standard `.gpx` files.

---

## 6. Roadmap: Mobile Companion App & Offline Sync

To enhance backcountry navigation without sacrificing privacy or offline capabilities, a companion **Android mobile app** can work seamlessly with the Wear OS app.

### Offline Peer-to-Peer Architecture (Wearable Data Layer API)
Smartphones and watches communicate locally via Bluetooth or direct Wi-Fi using the Google Play Services **Wearable Data Layer API**:
- **Zero Internet Required:** The phone and watch communicate directly without routing through external servers or the cloud.
- **Role Division:**
  - **Mobile Phone (Rich UI & Management):** Phone displays large offline topo maps, parses downloaded `.gpx` files, and allows naming and organizing geocaching waypoints.
  - **Wear OS Watch (Glanceable Navigation):** Receives the active target waypoint and uses its local magnetometer and sensors to guide the user hands-free on the trail.

```kotlin
// Example: Sending a Waypoint from Phone to Watch via MessageClient
val waypointPayload = """{"name":"Cache #42","lat":37.7749,"lon":-122.4194}""".toByteArray()
Wearable.getMessageClient(context).sendMessage(watchNodeId, "/waypoint/active", waypointPayload)
```

---

## Summary

This architecture gives you:
- **Absolute Privacy:** 0 network permissions.
- **Hardware Precision:** Rotation vector sensor fusion with wrist tilt compensation.
- **True North:** Automated on-device declination without an internet connection.
- **Watch Ergonomics:** Custom Canvas drawing, high-contrast ambient mode, and haptic feedback.
