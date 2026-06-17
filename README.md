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

## 🔥 Setting up Firebase Firestore Configuration

This application comes with full **real-time Firebase backend synchronization** via `FirestoreHelper.kt`! It continuously uploads and updates:
- Active order states and driver locations to the `"all_orders"` collection.
- Passenger and Driver workspace user profiles to the `"users"` collection.

By default, the simulation uses an in-memory fallback if Firestore is not initialized. To enable live database synchronization across physical devices or multiple emulators:

### 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** and follow the setup flow.
3. Once the project is created, click the **Android icon** to add an Android App to the project.
4. Provide the exact product package name: `com.aistudio.ordertracker.fbygxw` (as defined in `/app/build.gradle.kts`).
5. Download your custom `google-services.json` file.

### 2. Add `google-services.json` to the Project
Place the downloaded `google-services.json` file into the `/app/` directory of your Android project structure:
```text
/your-project-root/
└── app/
    ├── src/
    ├── build.gradle.kts
    └── google-services.json   <--- PLACE FILE HERE
```

### 3. Add Google Services Plugin to Build Configuration
To let the app parse the configurations, add the Google Services dependency rules.

Add the plugin to your project-level `/build.gradle.kts`:
```kotlin
// In your root build.gradle.kts file:
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false // <-- Add this line
}
```

Then apply the plugin in your app-level `/app/build.gradle.kts`:
```kotlin
// In your app-level build.gradle.kts file:
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    id("com.google.gms.google-services") // <-- Add this line
}
```

### 4. Setup Firestore Database & Rules
1. In the Firestore console, click **Create database**.
2. Select your test location.
3. Access the **Rules** tab, and set permission rules to allow reads and writes (secure them accordingly for production deployment):
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if true; // Unsecured for fast prototyping/simulating
       }
     }
   }
   ```
4. Run the app! It will initialize Firestore automatically and begin streaming ride events live over the cloud between drivers and passenger nodes.

## 🛠️ Local Android Studio Iguana 2023.2.1 Compatibility & Windows Cache Solutions

This project was carefully refactored with backward-compatible dependencies:
- **AGP version**: Configured to `8.3.2` which is the maximum fully-supported version for **Android Studio Iguana**.
- **compileSdk & targetSdk**: Targets **API 35** (Android 15) utilizing stable gradle definitions rather than preview-level nested configurations.
- **core-ktx**: Avoids compileSdk 36 constraints by using stable version `1.15.0`.
- **gradle.properties**: Enables default `android.useAndroidX=true` and suppresses SDK mismatches with `android.suppressUnsupportedCompileSdk=35`.

### Resolving Bouncy Castle (`bcprov-jdk18on-1.79.jar`) Extraction issues on Windows
If you run into an extraction error like:
`Failed to create Jar file C:\Users\<Name>\.gradle\caches\jars-9\...\bcprov-jdk18on-1.79.jar`

This is a known Windows OS issue where file locks (usually caused by Windows Defender, security software, or multiple simultaneous Java/Gradle daemon instances) prevent Gradle from registering signed libraries inside its internal zip cache directory. Follow these quick steps to resolve it on your system:

1. **Close Android Studio** and all Java/Kotlin development tasks.
2. Open your terminal or powershell and run:
   ```cmd
   gradlew --stop
   ```
3. Locate active locked jar files by clearing your local Gradle transforms cache. In Windows File Explorer, navigate to:
   `C:\Users\<YourUsername>\.gradle\caches\`
   And delete the `jars-9` or `transforms-3` subdirectory inside files (Gradle will automatically regenerate them next time you build).
4. **Configure exclusions inside Windows Defender / Antivirus**:
   Exclude the directory `C:\Users\<YourUsername>\.gradle\` from real-time files scanning so Windows does not lock Gradle's zip extraction cycles.
5. Restart **Android Studio Iguana** and click **Sync Project with Gradle Files**.

---

## 💾 Uploading and Committing Your Project to GitHub

AI Studio handles secure, direct code syncing and versioning natively:

1. Look at the top-right toolbar or project configuration in **Google AI Studio**.
2. Click the **GitHub icon** or open **Project Settings > Export/Push**.
3. Link your GitHub account, specify your target repository name (e.g., `yerevan-yandex-navigator`), and hit **Push/Commit**.
4. Once completed, your custom-crafted Yerevan Nav design is safely committed, ready for your final SDK keys!
