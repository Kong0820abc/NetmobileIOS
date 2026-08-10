package com.xyz.netmobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

actual class NetworkObserver actual constructor() {
    private val connectivityManager =
        platformContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _status = mutableStateOf<NetworkStatus>(NetworkStatus.Available)
    actual val status: NetworkStatus get() = _status.value

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (_status.value != NetworkStatus.Available) {
                _status.value = NetworkStatus.Probing
            }
            
            scope.launch(Dispatchers.IO) {
                if (fastVerifyInternet()) {
                    withContext(Dispatchers.Main) {
                        _status.value = NetworkStatus.Available
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (_status.value != NetworkStatus.Available) {
                            _status.value = NetworkStatus.Unavailable
                        }
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            _status.value = NetworkStatus.Unavailable
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                hasInternet
            }
            
            when {
                isValidated -> {
                    _status.value = NetworkStatus.Available
                }
                hasInternet -> {
                    if (_status.value != NetworkStatus.Available) {
                        _status.value = NetworkStatus.Probing
                    }
                }
                else -> {
                    _status.value = NetworkStatus.Unavailable
                }
            }
        }
    }

    init {
        val isInitialAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNet = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNet)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
        
        _status.value = if (isInitialAvailable) NetworkStatus.Available else NetworkStatus.Unavailable

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun fastVerifyInternet(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1200)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    actual fun clear() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) { /* 忽略 */ }
        scope.cancel()
    }
}
