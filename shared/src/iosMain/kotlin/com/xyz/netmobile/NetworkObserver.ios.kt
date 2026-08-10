package com.xyz.netmobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

actual class NetworkObserver actual constructor() {
    actual val status: NetworkStatus = NetworkStatus.Available
    
    actual fun clear() {
    }
}
