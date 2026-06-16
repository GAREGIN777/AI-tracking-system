# Yandex Yerevan Navigator & Ride-Hailing App (Android)

Welcome to the Yerevan Navigator and Ride-Hailing application, written 100% in modern Kotlin utilizing Jetpack Compose, Material 3, and a highly polished bento-grid design.

This application is customized for Yerevan, Armenia, supporting localized multi-language controls (English, Armenian, and Russian), 30+ real Yerevan streets/malls/monuments, automatic price calculations, route tracing, and real-time sync between Passengers and Drivers.

---

## 🛠️ Simulation to Production Mode Transfer

To deliver a flawless, high-fidelity experience during review and prototyping, this app contains an active **Yandex Yerevan Sandbox Simulator Console** located at the bottom of the screen. 
- In this review build, simulated metrics allow you to switch roles (Passenger vs. Driver) and alter traffic jam levels (1 to 10) in-memory to see how our custom routing and pricing calculations respond instantly.
- In your live production build, these manual controls are disabled; traffic values, geocoded paths, and map drawing will be served directly by the official Yandex SDKs.

---

## 🚀 Swapping the Yerevan Canvas Map with Yandex MapKit

To connect the live map and traffic metrics with real Yandex services, follow these standard implementation guidelines after pushing the project to your GitHub repository:

### 1. Register a Developer Account & Obtain API Keys
1. Go to the [Yandex Developer Console](https://developer.tech.yandex.ru/).
2. Request an API key for **MapKit SDK**.
3. Optionally request an API key for the **Yandex Traffic/Matrix API** or **Router API** if you wish to compute precise physical road parameters.

### 2. Configure gradle.properties & AndroidManifest.xml
Add your map API key to your configuration. In a secure environment, place it in your AI Studio secrets panel or inject it via gradle:

```kotlin
// In your application-level build.gradle.kts dependencies block:
dependencies {
    implementation("com.yandex.android:maps.mobile:4.5.1-lite") // Or -full if you require detailed offline features
}
```

Add initialization code to your `Application` subclass or direct `Activity`:

```kotlin
import com.yandex.mapkit.MapKitFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Set the API key before MapKitFactory is initialized
        MapKitFactory.setApiKey("YOUR_YANDEX_MAPKIT_API_KEY")
    }
}
```

In your `AndroidManifest.xml`, make sure to declare appropriate access, background location permissions, and internet permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 3. Replace the Simulated Map Canvas with MapView
In `MainActivity.kt` under `InteractiveMapView`, substitute the custom `Canvas` drawing block with an AndroidView hosting a Yandex `MapView`:

```kotlin
import com.yandex.mapkit.mapview.MapView

@Composable
fun InteractiveMapView(viewModel: TrackerViewModel, selectionMode: MutableState<String>) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                MapKitFactory.initialize(context)
                // Access map layers, add coordinates, start camera
                map.move(
                    com.yandex.mapkit.map.CameraPosition(
                        com.yandex.mapkit.geometry.Point(40.1777, 44.5126), // Republic Square, Yerevan
                        14.0f, 0.0f, 0.0f
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            // Update custom map lines, user pins, or traffic layers here based on the ViewModel State
        }
    )
}
```

### 4. Fetch Live Traffic Levels via Yandex SDK
Instead of manual traffic selectors, subscribe to Yandex's traffic layer manager inside the `MapView` view block:

```kotlin
val trafficLayer = MapKitFactory.getInstance().createTrafficLayer(mapView.mapWindow)
trafficLayer.isTrafficVisible = true

// Handle traffic changes dynamically:
trafficLayer.addTrafficListener(object : com.yandex.mapkit.traffic.TrafficListener {
    override fun onTrafficChanged(trafficLevel: com.yandex.mapkit.traffic.TrafficLevel?) {
        if (trafficLevel != null) {
            // Update your state with actual live congestion metrics (0 to 10 score)
            viewModel.updateTrafficLevel(trafficLevel.color)
        }
    }
    override fun onTrafficLoading() {}
    override fun onTrafficExpired() {}
})
```

---

## 💾 Uploading and Committing Your Project to GitHub

AI Studio handles secure, direct code syncing and versioning natively:

1. Look at the top-right toolbar or project configuration in **Google AI Studio**.
2. Click the **GitHub icon** or open **Project Settings > Export/Push**.
3. Link your GitHub account, specify your target repository name (e.g., `yerevan-yandex-navigator`), and hit **Push/Commit**.
4. Once completed, your custom-crafted Yerevan Nav design is safely committed, ready for your final SDK keys!
