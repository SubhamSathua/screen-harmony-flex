# Agent Plan: Simple Parent-Kid App Blocker (ScreenHarmony Flex)

## 1. Project Overview & Requirements

| Requirement | Specification |
| :--- | :--- |
| **Core Goal** | Simple app blocker without timers/schedules. Instant on/off toggle. |
| **Architecture** | **MVVM (Model-View-ViewModel)** with Unidirectional Data Flow (UDF) & Clean Architecture. |
| **Design System** | **Responsive Material 3 Expressive** (Dynamic Color, adaptive layouts across Compact/Medium/Expanded, expressive shapes, spring animations). |
| **Typography System** | • **App Font**: `Nunito` (General UI, Body, Buttons, Subtitles)<br>• **Numbers & Codes**: `JetBrains Mono` (Pairing code, counters, stats)<br>• **Branding & Hero Headlines**: `Playfair Display` (Logos, Hero headers, Top branding) |
| **Role Split** | **Parent Phone** (selects apps to block) & **Kid Phone** (enforces blocking). |
| **Permission Model** | **Usage Access Only** (`PACKAGE_USAGE_STATS`). Zero accessibility service requirement (works on sideloaded APKs on Android 13/14/15 without triggering "Restricted Settings"). |
| **Communication / Sync** | **100% Free & Zero Setup Cost** via Firebase Spark Free Tier (real-time cloud sync via pairing code) or Local Wi-Fi P2P / MQTT. |
| **Tech Stack** | Kotlin, Jetpack Compose, Material 3 Expressive, Navigation Suite, Coroutines, StateFlow. |

---

## 2. MVVM Architecture & Clean Layering

```
┌────────────────────────────────────────────────────────────────────────┐
│                               View Layer                               │
│  Jetpack Compose Screens (ParentDashboard, KidStatusScreen, Pairing)   │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │  Observes StateFlow<UiState>
                                     │  Dispatches UiEvents / Intents
┌────────────────────────────────────▼───────────────────────────────────┐
│                             ViewModel Layer                            │
│    ParentViewModel, KidViewModel, PairingViewModel, BlockedViewModel   │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │  Calls UseCases / Repositories
┌────────────────────────────────────▼───────────────────────────────────┐
│                               Domain Layer                             │
│      UseCases (ToggleBlockAppUseCase, SyncInstalledAppsUseCase, etc.)   │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │  Accesses Data Sources
┌────────────────────────────────────▼───────────────────────────────────┐
│                                Data Layer                              │
│  Repositories: FamilyRepository, AppListRepository, BlockerRepository │
│  DataSources: Firestore Spark (Free Tier) / Local Wi-Fi P2P / DataStore │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Layer Responsibilities
1. **Model / Data Layer**:
   - `FamilyRepository`: Manages pairing codes, device registry, and Firestore snapshot listeners.
   - `AppInfoRepository`: Scans installed applications on the Kid device using `PackageManager`.
   - `BlockerRepository`: Manages local persistence of blocked package names (DataStore / in-memory cache).
2. **ViewModel Layer**:
   - Exposes immutable `StateFlow<UiState>` to Compose UI.
   - Handles asynchronous actions via `viewModelScope`.
   - Unidirectional Data Flow: UI emits `UiEvent` -> ViewModel processes -> UI updates.
3. **Domain / Service Layer**:
   - `AppBlockerService`: Foreground Service querying `UsageStatsManager` every 200–300ms.
   - `BootReceiver`: Resumes foreground blocker service after device reboot.
4. **View Layer (Jetpack Compose)**:
   - Stateless composables with hoisted state.
   - Adaptive layouts reacting to Window Size Classes (Compact, Medium, Expanded).

---

## 3. Responsive Material 3 Expressive & Multi-Font Typography

### 3.1 Three-Tier Typography System

| Category | Font Family | Usage |
| :--- | :--- | :--- |
| **Branding & Hero Headlines** | `Playfair Display` | App logo, Screen titles, Hero banners, Modal titles |
| **General App UI** | `Nunito` | Body text, labels, button text, search fields, card descriptions |
| **Numbers & Codes** | `JetBrains Mono` | 6-digit pairing code, app counters, package IDs, timestamps |

#### `ui/theme/Type.kt` Implementation
```kotlin
package com.prism.screenharmony.flex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.R

// Font Families
val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_bold, FontWeight.Bold),
    Font(R.font.playfair_display_regular, FontWeight.Normal)
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

// Material 3 Expressive Typography Mapping
val AppTypography = Typography(
    // Branding & Expressive Headlines (Playfair Display)
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),

    // App General UI & Titles (Nunito)
    titleLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

// Custom Typography Extensions for Numbers & Codes (JetBrains Mono)
object CodeTypography {
    val pairingCode = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 6.sp
    )
    val counterNumber = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    )
    val packageSubtitle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
}
```

### 3.2 Responsive & Adaptive Layout Architecture
- **Adaptive Navigation**: Uses Material 3 `NavigationSuiteScaffold`:
  - **Compact Screens (Phones)**: Bottom Navigation Bar with expressive rounded icons.
  - **Medium Screens (Foldables / Small Tablets)**: Side Navigation Rail.
  - **Expanded Screens (Tablets / Chromebooks)**: Expanded Navigation Drawer + Split-pane view (Left: Installed App List, Right: Live Kid Device Status & Quick Actions).
- **Expressive Shapes & Tokens**:
  - Rounded cards with `CornerSize(24.dp)`.
  - Spring-animated switches with fluid feedback when toggling blocked states.

---

## 4. Usage Access Only Detection Engine (No Accessibility Needed)

### 4.1 Why Accessibility is Avoided
- On **Android 13+ (API 33+)**, sideloaded apps trigger the **Restricted Setting** dialog when attempting to enable Accessibility Services.
- **Usage Access (`PACKAGE_USAGE_STATS`)** has **no such restriction** and is easily granted via `Settings.ACTION_USAGE_ACCESS_SETTINGS`.

### 4.2 Foreground Polling Service (`AppBlockerService.kt`)
```kotlin
class AppBlockerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val blockedPackages = mutableSetOf<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        observeBlockedApps()
        startUsageMonitorLoop()
        return START_STICKY
    }

    private fun startUsageMonitorLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val foregroundApp = getForegroundPackage(usageStatsManager)
                if (foregroundApp != null && blockedPackages.contains(foregroundApp)) {
                    launchBlockScreen(foregroundApp)
                }
                delay(250) // Fast 250ms polling loop
            }
        }
    }

    private fun getForegroundPackage(usm: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000
        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var latestApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestApp = event.packageName
            }
        }
        return latestApp
    }

    private fun launchBlockScreen(packageName: String) {
        val lockIntent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra("BLOCKED_PACKAGE", packageName)
        }
        startActivity(lockIntent)
    }
}
```

---

## 5. 100% Free Sync Architecture

### Firebase Firestore (Spark Plan - 100% Free Forever)
- **Zero Cost**: No credit card required.
- **Limits**: 50,000 document reads/day, 20,000 writes/day.
- **Data Model**:
```json
// Collection: families/{familyCode}
{
  "familyCode": "SH-7842",
  "createdAt": 1740660000000,
  "devices": {
    "kid_device_1": {
      "deviceName": "Kid Phone (Pixel)",
      "lastActive": 1740660100000,
      "blockedPackages": ["com.zhiliaoapp.musically", "com.instagram.android"],
      "installedApps": [
        {"name": "TikTok", "packageName": "com.zhiliaoapp.musically", "isBlocked": true},
        {"name": "Instagram", "packageName": "com.instagram.android", "isBlocked": true},
        {"name": "Calculator", "packageName": "com.google.android.calculator", "isBlocked": false}
      ]
    }
  }
}
```

---

## 6. Implementation Checklist & UI Flow

### Step 1: Font & M3 Expressive Theme Setup
- Place font files (`nunito_*.ttf`, `jetbrains_mono_*.ttf`, `playfair_display_*.ttf`) in `res/font/`.
- Configure `Type.kt` and `Theme.kt` with dynamic color & adaptive window sizing.

### Step 2: Role Selection & Pairing UI
- **Parent Choice**: Generates 6-character Code displayed in `JetBrains Mono` with Playfair Display title.
- **Kid Choice**: Input field with formatted monospace spacing to enter the 6-character code.

### Step 3: Kid Device Setup (Usage Access Only)
- Permission card displaying Usage Access status with 1-tap grant button.
- Sticky Foreground Service with auto-start on boot (`BOOT_COMPLETED`).

### Step 4: Parent Control Dashboard (MVVM + Jetpack Compose)
- Search bar to filter installed kid apps.
- Expressive cards with app icon, app title in `Nunito`, package name in `JetBrains Mono`, and instant block/unblock toggle switch.
- Adaptive split-pane layout for tablets and landscape orientations.

### Step 5: Fullscreen Interception (`BlockedActivity.kt`)
- Playfair Display header: *"App Blocked"*.
- Nunito descriptive body: *"This app is restricted by parental controls."*.
- Go Home button (`Intent.CATEGORY_HOME`).
