package com.xyz.netmobile

interface Platform {
    val name: String
    val appVersion: String
    val versionCode: Long
    fun getDeviceId(): String
    fun openUrlInBrowser(url: String)
    fun keepScreenOn(enabled: Boolean)
    fun getCurrentDate(): String
    fun exitApp()
    fun showToast(message: String)
}

expect fun getPlatform(): Platform

expect fun createSettings(): com.russhwolf.settings.Settings
