package com.xyz.netmobile

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

@Composable
actual fun HorseLiveScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val isEng = selectedLanguage == "ENG"
    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)
    val colorBackground = Color(0xFFF5F7FA)

    val targetUrl = remember(selectedLanguage) {
        if (isEng) "https://horsereplaypage.netlify.app/?lang=en" else "https://horsereplaypage.netlify.app/?lang=zh"
    }

    var isWebLoading by remember { mutableStateOf(true) }
    var needsClearHistory by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isWebLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "ContentAlpha"
    )

    val toggleSystemBars = { show: Boolean ->
        window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            if (show) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    val hideFullscreen = {
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        toggleSystemBars(true)
    }

    BackHandler {
        if (customView != null) {
            hideFullscreen()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Surface(modifier = Modifier.fillMaxSize(), color = colorBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                if (customView == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorRed)
                            .statusBarsPadding()
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clickable { onBack() },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colorYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isEng) "Back" else "返回",
                                color = colorYellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = if (isEng) "Horse Replay" else "马赛重播",
                            color = colorYellow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        Text(
                            text = if (isEng) "中文" else "ENG",
                            color = colorYellow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable {
                                    onLanguageChange(if (isEng) "中文" else "ENG")
                                }
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                setBackgroundColor(android.graphics.Color.WHITE)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowCustomView(
                                        view: View?,
                                        callback: CustomViewCallback?
                                    ) {
                                        customView = view
                                        customViewCallback = callback
                                        activity?.requestedOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        toggleSystemBars(false)
                                    }

                                    override fun onHideCustomView() {
                                        hideFullscreen()
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?
                                    ) {
                                        isWebLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        if (needsClearHistory) {
                                            view?.clearHistory()
                                            needsClearHistory = false
                                        }
                                        postDelayed({ isWebLoading = false }, 500)
                                    }
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    textZoom = 100
                                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                    setSupportZoom(false)
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    mediaPlaybackRequiresUserGesture = false
                                }
                                loadUrl(targetUrl)
                            }
                        },
                        update = { view ->
                            if (view.url != targetUrl) {
                                needsClearHistory = true
                                view.loadUrl(targetUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }
                    )

                    if (isWebLoading) {
                        CircularProgressIndicator(color = Color(0xFFD32F2F))
                    }
                }
            }
        }

        if (customView != null) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize().background(Color.Black)
            )
        }
    }
}
