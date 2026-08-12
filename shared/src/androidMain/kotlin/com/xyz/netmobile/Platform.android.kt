package com.xyz.netmobile

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

lateinit var platformContext: Context

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    
    override val appVersion: String by lazy {
        try {
            val packageInfo = platformContext.packageManager.getPackageInfo(platformContext.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    override val versionCode: Long by lazy {
        try {
            val packageInfo = platformContext.packageManager.getPackageInfo(platformContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    override fun getDeviceId(): String {
        return Settings.Secure.getString(platformContext.contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    override fun openUrlInBrowser(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(platformContext, url.toUri())
    }
    
    override fun keepScreenOn(enabled: Boolean) {
        (platformContext as? Activity)?.window?.let { window ->
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    override fun exitApp() {
        (platformContext as? Activity)?.finish()
    }

    override fun showToast(message: String) {
        android.widget.Toast.makeText(platformContext, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun setOrientation(landscape: Boolean) {
        val activity = platformContext as? android.app.Activity
        activity?.requestedOrientation = if (landscape) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun createSettings(): com.russhwolf.settings.Settings {
    val sharedPrefs = platformContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return com.russhwolf.settings.SharedPreferencesSettings(sharedPrefs)
}
