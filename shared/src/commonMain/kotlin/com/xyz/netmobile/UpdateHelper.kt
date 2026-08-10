package com.xyz.netmobile

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.first

object UpdateHelper {
    suspend fun checkUpdate(platform: Platform, onUpdateFound: (version: String, url: String) -> Unit) {
        try {
            val database = Firebase.database.reference("NetmobileUpdate")
            val snapshot = database.valueEvents.first()
            
            val latestVersion = snapshot.child("version").value<String?>()
            val updateUrl = snapshot.child("url").value<String?>()
            
            val currentVersion = platform.appVersion

            if (latestVersion != null && updateUrl != null && latestVersion != currentVersion) {
                onUpdateFound(latestVersion, updateUrl)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
