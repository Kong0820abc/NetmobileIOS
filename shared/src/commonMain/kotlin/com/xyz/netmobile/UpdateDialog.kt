package com.xyz.netmobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun UpdateDialog(
    apkUrl: String,
    selectedLanguage: String,
    isForce: Boolean = false,
    onDismiss: () -> Unit
) {
    val platform = getPlatform()
    val themeRed = Color(0xFFB71C1C)
    val isEng = selectedLanguage == "ENG"

    Dialog(
        onDismissRequest = { 
            if (!isForce) onDismiss() 
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isForce,
            dismissOnClickOutside = !isForce
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(themeRed.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = themeRed,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isEng) "Update Available" else "发现新版本",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isForce) {
                        if (isEng) "This version is no longer supported. You must update to the latest version to continue using the app."
                        else "当前版本已停用。您必须更新到最新版本才能继续使用应用。"
                    } else {
                        if (isEng) "A new version is ready with better features and stability. Please update now to ensure everything works perfectly."
                        else "为了确保系统的稳定性和新功能的使用，请立即升级到最新版本。"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color.Gray.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        platform.openUrlInBrowser(apkUrl)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeRed),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = if (isEng) "Update Now" else "立即更新",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isForce) {
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isEng) "Later" else "稍后再说",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
