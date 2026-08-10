package com.xyz.netmobile

import android.app.DownloadManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun UpdateDialog(
    apkUrl: String,
    selectedLanguage: String,
    isForce: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { UpdateHelper(context) }
    val themeRed = Color(0xFFB71C1C)

    // ... (状态管理代码保持不变) ...
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var downloadId by remember { mutableLongStateOf(-1L) }

    val isEng = selectedLanguage == "ENG"

    // ... (进度轮询逻辑保持不变) ...
    if (isDownloading && downloadId != -1L) {
        LaunchedEffect(downloadId) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                        val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                        val bytesTotal = cursor.getInt(bytesTotalIndex)
                        if (bytesTotal > 0) {
                            progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                        }
                    }

                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            isDownloading = false
                            onDismiss()
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            isDownloading = false
                        }
                    }
                }
                cursor?.close()
                delay(500)
            }
        }
    }

    Dialog(
        onDismissRequest = { 
            if (!isDownloading && !isForce) onDismiss() 
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
                // 1. 图标部分 - 红色渐变背景效果
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

                // 2. 标题
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

                // 3. 正文描述
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

                // 4. 下载进度
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = themeRed,
                            trackColor = themeRed.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = themeRed,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 5. 按钮组
                if (!isDownloading) {
                    Button(
                        onClick = {
                            isDownloading = true
                            downloadId = helper.downloadAndInstall(apkUrl)
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
                } else {
                    Text(
                        text = if (isEng) "Downloading Assets..." else "正在下载资源...",
                        color = themeRed,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

