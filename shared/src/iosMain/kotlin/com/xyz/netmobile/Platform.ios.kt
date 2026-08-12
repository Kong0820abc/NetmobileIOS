package com.xyz.netmobile

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.Foundation.NSBundle

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    override val appVersion: String by lazy {
        NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "0.0.0"
    }

    override val versionCode: Long by lazy {
        val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
        version?.toLongOrNull() ?: 0L
    }
    
    override fun getDeviceId(): String {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
    }
    
    override fun openUrlInBrowser(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
    
    override fun keepScreenOn(enabled: Boolean) {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
    }

    override fun getCurrentDate(): String {
        val formatter = platform.Foundation.NSDateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.stringFromDate(platform.Foundation.NSDate())
    }

    override fun exitApp() {
        platform.posix.exit(0)
    }

    override fun showToast(message: String) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        val alert = platform.UIKit.UIAlertController.alertControllerWithTitle(
            title = null,
            message = message,
            preferredStyle = platform.UIKit.UIAlertControllerStyleAlert
        )
        rootViewController?.presentViewController(alert, animated = true, completion = null)
        platform.Foundation.NSTimer.scheduledTimerWithTimeInterval(2.0, false) {
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override fun setOrientation(landscape: Boolean) {
        val orientation = if (landscape) platform.UIKit.UIInterfaceOrientationLandscapeRight else platform.UIKit.UIInterfaceOrientationPortrait
        val device = platform.UIKit.UIDevice.currentDevice
        val selector = platform.Foundation.NSSelectorFromString("setOrientation:")
        // Kotlin/Native 会自动将 Long/Int 映射到 Objective-C 的数值类型
        device.performSelector(selector, withObject = orientation as Any?)
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun createSettings(): com.russhwolf.settings.Settings {
    return com.russhwolf.settings.NSUserDefaultsSettings(platform.Foundation.NSUserDefaults.standardUserDefaults)
}
