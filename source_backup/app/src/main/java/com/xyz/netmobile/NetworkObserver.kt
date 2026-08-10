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

/**
 * 极速网络观察者：采用 Compose State 直接驱动，实现零延迟消失
 */
class NetworkObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    sealed class Status {
        object Available : Status()
        object Unavailable : Status()
        object Probing : Status() // 新增：正在探测互联网可用性
        override fun toString(): String = this.javaClass.simpleName
    }

    // 直接暴露给 Compose 的状态，读取无延迟，由 SnapshotState 系统自动追踪重绘
    var status by mutableStateOf<Status>(Status.Available)
        private set

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // 只要硬件层连接，若非已可用，则先标为探测中
            if (status != Status.Available) {
                status = Status.Probing
            }
            
            // 【核心优化】：手动极速拨测，一旦成功立即更新为 Available，让提示窗消失
            scope.launch(Dispatchers.IO) {
                if (fastVerifyInternet()) {
                    withContext(Dispatchers.Main) {
                        status = Status.Available
                    }
                } else {
                    // 如果手动拨测失败，且当前不是探测中（可能是断开），则标记为不可用
                    withContext(Dispatchers.Main) {
                        if (status != Status.Available) {
                            status = Status.Unavailable
                        }
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            // 【零延迟唤起】断开瞬间标记为不可用
            status = Status.Unavailable
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                hasInternet
            }
            
            // 状态机精细化：防止系统较慢的 VALIDATED 回调覆盖掉我们手动拨测已确认的 Available 结果
            when {
                isValidated -> {
                    status = Status.Available
                }
                hasInternet -> {
                    // 仅在当前不是 Available 时才设为 Probing，防止“回滚”
                    if (status != Status.Available) {
                        status = Status.Probing
                    }
                }
                else -> {
                    status = Status.Unavailable
                }
            }
        }
    }

    init {
        // 1. 同步初始检查：在 UI 渲染第一帧前获取状态，防止启动闪烁
        val isInitialAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNet = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNet)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
        
        status = if (isInitialAvailable) Status.Available else Status.Unavailable

        // 2. 注册网络监听
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun fastVerifyInternet(): Boolean {
        return try {
            Socket().use { socket ->
                // 使用 Google DNS 端口，超时严格控制在 1.2 秒内
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1200)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun clear() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) { /* 忽略 */ }
        scope.cancel()
    }
}
