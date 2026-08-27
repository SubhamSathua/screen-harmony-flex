# ScreenHarmony Flex

ScreenHarmony Flex is a modern, privacy-first Android application designed for granular digital wellbeing, active distraction blocking, and parental control enforcement. Built entirely with Kotlin, Jetpack Compose, and Material 3 Expressive design, it delivers real-time application and website restrictions with zero-flicker lock screens and zero background throttling.

---

## Key Highlights

- **Usage Access Engine**: Enforces application restrictions in real time via `UsageStatsManager` without requiring high-privilege accessibility services for app blocking.
- **Zero-Flicker Lock Wall**: Transitions instantly to the focus lock screen using system overlay layers, eliminating home-screen flashing.
- **Website Domain Filtering**: Inspects browser address bars dynamically across major browsers (Chrome, Firefox, Brave, Opera, Samsung Internet) using a lightweight accessibility hook.
- **Interactive Visual Scheduling**: Configures active hours and weekly schedules using an integrated 24-hour canvas timeline graph and Material 3 time pickers.
- **Configurable Pause Challenges**: Restricts unpausing through Strict mode (no pausing), countdown delay sliders (3s to 60s), or text typing challenges.
- **Material 3 Expressive Theming**: Features 7 curated color schemes, dynamic system recoloring, automatic light/dark switching, and true pitch-black AMOLED mode.
- **Gradle Sync-Free Versioning**: Reads application version codes directly from an isolated `version.properties` file, preventing Gradle build cache invalidation during version bumps.

---

## Core Architecture

The application uses a decoupled dual-engine architecture to ensure compatibility across Android 10 through Android 15.

```
+-------------------------------------------------------------------+
|                        ScreenHarmony Flex                         |
+---------------------------------+---------------------------------+
|        App Blocker Engine       |     Website Blocker Engine      |
|  (Foreground Service + Polling) |     (Accessibility Service)     |
+---------------------------------+---------------------------------+
| - PACKAGE_USAGE_STATS           | - BIND_ACCESSIBILITY_SERVICE    |
| - SYSTEM_ALERT_WINDOW (Overlay) | - Real-time browser URL check   |
| - Continuous 200ms loop         | - Automatic about:blank reroute |
| - High-Priority Intent Launch   | - Chrome, Firefox, Brave, Opera |
+---------------------------------+---------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Zero-Flicker Lock Activity                     |
|           - App Icon & Title Header                               |
|           - Mindfulness Quote Container                           |
|           - Interactive Countdown Delay Timer                     |
|           - Single-Task Foreground Transition                     |
+-------------------------------------------------------------------+
```

---

## Features

### 1. Distraction & App Blocking
- **Installed App Discovery**: Fetches all launchable packages asynchronously with high-resolution application icons.
- **Layout Switcher**: Supports detailed list view and responsive adaptive grid view.
- **Package Identifier Toggle**: Toggles technical package names on or off for developer analysis.
- **Real-Time Search**: Filters applications instantly by name or package identifier.

### 2. Time & Schedule Management
- **Day Bitmask Engine**: Configures rules across custom day combinations (Weekdays, Weekends, or specific days).
- **24-Hour Visual Graph**: Visualizes active block segments on a weekly 7-column canvas.
- **Dual Time Pickers**: Sets start and end times with native Material 3 time dialogs.

### 3. Pause & Enforcement Controls
- **Strict Mode**: Disables pausing completely to enforce unbreakable focus sessions.
- **Delay Slider**: Requires users to wait a designated duration (3s to 60s) before pausing.
- **Text Typing Challenge**: Demands typing randomly generated alphanumeric strings to unlock.
- **Mindfulness Countdown**: Displays a 5-second countdown safeguard before allowing rule deletion or disabling.

### 4. Appearance & Personalization
- **Theme Modes**: Supports System Default, Pure Light, and Pitch Dark.
- **AMOLED Black**: Converts background and surface containers to pure black (`#000000`) for OLED power savings.
- **Color Palettes**: Includes Teal Sage, Ocean Blue, Emerald Green, Sunset Coral, Lavender Purple, Rose Pink, Amber Gold, and Dynamic Material You.
- **Connected Containers**: Implements grouped card sections with internal dividers for unified visual hierarchy.

---

## Required Permissions

| Permission | Identifier | Purpose |
| :--- | :--- | :--- |
| **Usage Access** | `android.permission.PACKAGE_USAGE_STATS` | Detects currently active foreground applications. |
| **Display Over Other Apps** | `android.permission.SYSTEM_ALERT_WINDOW` | Displays the lock screen immediately over running apps in the background. |
| **Unrestricted Battery** | `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents Android OS from killing the background monitoring service. |
| **Accessibility Service** | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Optional. Inspects browser URL address bars for blocked websites. |
| **Post Notifications** | `android.permission.POST_NOTIFICATIONS` | Maintains the persistent foreground service notification on Android 13+. |

---

## Technology Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (BOM 2026+)
- **Design System**: Material Design 3 (M3 Expressive)
- **Adaptive Components**: Compose Material 3 Adaptive Navigation Suite
- **Asynchronous Architecture**: Kotlin Coroutines, StateFlow, Lifecycle-aware composables
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with version catalog (`libs.versions.toml`)

---

## Project Structure

```
ScreenHarmonyFlex/
|-- app/
|   |-- src/main/
|   |   |-- java/com/prism/screenharmony/flex/
|   |   |   |-- data/
|   |   |   |   |-- BlockRepository.kt      # Reactive state storage for block rules
|   |   |   |   `-- Models.kt               # BlockRule, TimeSlot, DayBitmask data models
|   |   |   |-- service/
|   |   |   |   |-- AppBlockerService.kt    # Usage Access monitoring foreground service
|   |   |   |   |-- BootReceiver.kt         # Device reboot auto-start receiver
|   |   |   |   `-- WebsiteAccessibilityService.kt # Browser URL inspection service
|   |   |   |-- ui/
|   |   |   |   |-- blocker/
|   |   |   |   |   `-- BlockedActivity.kt  # Fullscreen lock wall activity
|   |   |   |   |-- components/
|   |   |   |   |   |-- CommonUI.kt         # Option cards, dialogs, async app icon loaders
|   |   |   |   |   |-- PauseComponents.kt  # Delay sliders and typing challenge inputs
|   |   |   |   |   `-- ScheduleComponents.kt # Visual timeline graph & time pickers
|   |   |   |   |-- screens/
|   |   |   |   |   |-- AppListScreen.kt    # Full installed application selector
|   |   |   |   |   |-- BlocksPage.kt       # Active, paused, and disabled rule list
|   |   |   |   |   `-- CreateBlockPage.kt  # Block creation and configuration flow
|   |   |   |   `-- theme/
|   |   |   |       |-- Color.kt            # 7 M3 palettes and AMOLED transformations
|   |   |   |       |-- Theme.kt            # ThemeState provider and dynamic scheme builder
|   |   |   |       `-- Type.kt             # Typography definitions
|   |   |   |-- utils/
|   |   |   |   |-- PermissionHelper.kt     # Permission status evaluation & intent dispatchers
|   |   |   |   `-- TimeUtils.kt            # Delay formatting & slider conversion utilities
|   |   |   `-- MainActivity.kt             # Navigation suite scaffold and main screens
|   |   |-- res/                            # Drawables, mipmaps, strings, accessibility XML
|   |   `-- AndroidManifest.xml             # Service, receiver, and activity declarations
|   `-- build.gradle.kts                    # App module build configuration
|-- version.properties                      # Isolated semantic version storage
|-- bump-version.ps1                        # PowerShell script for automated semver bumping
|-- bump-version.sh                         # Shell script for automated semver bumping
|-- build.gradle.kts                        # Root project build configuration
`-- settings.gradle.kts                     # Project settings and plugin repositories
```

---

## Build & Installation

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17 or JDK 21
- Android SDK Platform 35 (API 35)

### Build Debug APK
Execute the Gradle wrapper from the root directory:

```bash
# Windows PowerShell / CMD
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Automated Version Management

ScreenHarmony Flex uses an isolated `version.properties` file to update version codes and semantic versions without modifying `build.gradle.kts` files. This eliminates mandatory Gradle project syncs in Android Studio when updating versions.

To increment the version:

```powershell
# Interactive prompt (Major / Minor / Patch)
.\bump-version.ps1

# Direct increment
.\bump-version.ps1 -Type patch
.\bump-version.ps1 -Type minor
.\bump-version.ps1 -Type major
```

---

## License

Copyright (c) 2026 ScreenHarmony Flex Project. All rights reserved.
Licensed under the Apache License, Version 2.0.
