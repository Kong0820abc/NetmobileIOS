# Kotlin Multiplatform Migration Plan for Netmobile

This plan outlines the steps to migrate the Android-only `Netmobile` project to a Kotlin Multiplatform (KMP) project that runs on both iOS and Android.

## User Review Required

> [!IMPORTANT]
> **Android-Specific APIs**: The original project uses several Android-only APIs (e.g., `ANDROID_ID`, `SharedPreferences`, `WebView`, `CustomTabs`). These will be refactored into a shared `expect`/`actual` structure or replaced with cross-platform libraries.
>
> **Firebase**: We will switch to the `GitLive` Firebase KMP SDK to ensure database functionality works on both platforms.
>
> **Resources**: Android XML resources (drawables, strings) will be migrated to the Compose Multiplatform resource system.

## Proposed Changes

### Configuration & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/gradle/libs.versions.toml)
Add KMP-compatible dependencies:
- `firebase-kotlin-sdk` (GitLive)
- `multiplatform-settings`
- `compose-webview-multiplatform`
- `kotlinx-coroutines-play-services`

#### [MODIFY] [shared/build.gradle.kts](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/build.gradle.kts)
Configure the `shared` module to include these new dependencies in `commonMain`.

---

### Resource Migration

#### [NEW] Resources in `shared/src/commonMain/composeResources`
Copy drawables (`logo.png`, `hkflag.png`, etc.) and strings from the source project.

---

### Code Migration & Refactoring

#### [NEW] [Platform.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/src/commonMain/kotlin/com/xyz/netmobile/Platform.kt)
Define `expect` interfaces for:
- `getDeviceId()`
- `openUrlInBrowser(url: String)`
- `keepScreenOn(enabled: Boolean)`

#### [MODIFY] [shared/src/androidMain/kotlin/com/xyz/netmobile/Platform.android.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/src/androidMain/kotlin/com/xyz/netmobile/Platform.android.kt)
Implement `actual` for Android using `ANDROID_ID`, `Intent`, and `Window`.

#### [MODIFY] [shared/src/iosMain/kotlin/com/xyz/netmobile/Platform.ios.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/src/iosMain/kotlin/com/xyz/netmobile/Platform.ios.kt)
Implement `actual` for iOS using `identifierForVendor`, `UIApplication.shared.open()`, and `isIdleTimerDisabled`.

#### [NEW] [App.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/src/commonMain/kotlin/com/xyz/netmobile/App.kt)
Migrate the UI and logic from `MainActivity.kt` (123KB) into a multiplatform `App()` composable.
- Replace `SharedPreferences` with `Settings`.
- Replace `AndroidView(WebView)` with `WebView`.
- Replace Android `Firebase` with GitLive `Firebase`.
- Refactor `MainActivity`'s screens (`LoginScreen`, `HomeScreen`, etc.) into this file or separate files in `commonMain`.

#### [NEW] [NetworkObserver.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/shared/src/commonMain/kotlin/com/xyz/netmobile/NetworkObserver.kt)
Refactor the network observer to be platform-agnostic or use `expect`/`actual`.

---

### Entry Points

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/androidApp/src/main/kotlin/com/xyz/netmobile/MainActivity.kt)
Ensure it properly initializes the shared `App` and handles `enableEdgeToEdge`.

#### [MODIFY] [iosApp/iosApp/ContentView.swift](file:///C:/Users/kong0/AndroidStudioProjects/Netmobile%20IOS/iosApp/iosApp/ContentView.swift)
Ensure the SwiftUI view correctly hosts the Compose Multiplatform `App`.

## Verification Plan

### Automated Tests
- Run `./gradlew :shared:assemble` to verify compilation.
- Run `./gradlew :androidApp:assembleDebug` to verify Android build.

### Manual Verification
- Deploy to an Android Emulator/Device and verify all screens.
- Deploy to an iOS Simulator (if available in environment) and verify UI and basic functionality.
