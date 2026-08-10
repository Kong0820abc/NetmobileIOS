package com.xyz.netmobile

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Probing : NetworkStatus()
}

expect class NetworkObserver() {
    val status: NetworkStatus
    fun clear()
}
