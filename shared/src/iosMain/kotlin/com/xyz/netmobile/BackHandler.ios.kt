package com.xyz.netmobile

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS as it typically uses navigation controllers or gestures
}
