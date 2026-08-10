package com.xyz.netmobile

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun SoccerScoresScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    val adKeywords = remember {
        listOf(
            "googleads", "doubleclick", "adsbygoogle", "amazon-adsystem",
            "popads", "adservice", "analytics", "facebook.com/tr",
            "adsystem", "adnxs", "smartadserver", "sofascore-ads"
        )
    }

    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    val isEng = selectedLanguage == "ENG"
    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)

    val targetUrl = remember(selectedLanguage) {
        if (isEng) "https://www.sofascore.com/" else "https://www.boti.net/football/"
    }

    var isWebLoading by remember { mutableStateOf(true) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isWebLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "ContentAlpha"
    )

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
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
                text = if (isEng) "Soccer Scores" else "足球比分",
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

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val requestUrl = request?.url?.toString()?.lowercase() ?: ""
                                if (adKeywords.any { requestUrl.contains(it) }) {
                                    return WebResourceResponse("text/plain", "utf-8", null)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isWebLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                val script = """
                                    (function() {
                                        var meta = document.querySelector('meta[name="viewport"]');
                                        if (!meta) { 
                                            meta = document.createElement('meta'); 
                                            meta.name = 'viewport'; 
                                            document.head.appendChild(meta); 
                                        }
                                        
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            .adsbygoogle, ins.adsbygoogle, [id*="google_ads"], 
                                            iframe[src*="ads"], div[class*="ad-"], div[id*="ad-"],
                                            .sofascore-ads, [class*="AdWrapper"], .top-ad-container { 
                                                display: none !important; 
                                            }
                                            html, body { overflow-x: hidden !important; width: auto !important; transform-origin: top left; }
                                        `;
                                        
                                        function hideAds() {
                                            var selectors = [
                                                '.adsbygoogle', 'ins.adsbygoogle', '[id*="google_ads"]',
                                                '.sofascore-ads', '[class*="AdWrapper"]', '.top-ad-container'
                                            ];
                                            selectors.forEach(function(s) {
                                                var nodes = document.querySelectorAll(s);
                                                nodes.forEach(function(el) { 
                                                    el.style.setProperty('display', 'none', 'important'); 
                                                });
                                            });
                                        }
                                        
                                        document.head.appendChild(style);
                                        hideAds();
                                        setTimeout(hideAds, 1000);
                                        setTimeout(hideAds, 3000);

                                        setTimeout(function() {
                                            var contentWidth = Math.max(document.documentElement.scrollWidth, document.body.scrollWidth);
                                            var screenWidth = window.innerWidth;
                                            if (contentWidth > screenWidth) {
                                                var scale = screenWidth / contentWidth;
                                                meta.content = 'width=' + contentWidth + ', initial-scale=' + scale + ', minimum-scale=' + scale + ', maximum-scale=' + scale + ', user-scalable=no';
                                            } else {
                                                meta.content = 'width=device-width, initial-scale=1.0, user-scalable=no';
                                            }
                                            window.dispatchEvent(new Event('resize'));
                                        }, 200);
                                    })();
                                """.trimIndent()
                                evaluateJavascript(script, null)
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
                            // 关键修复：仅英文版禁止自动播放声音
                            mediaPlaybackRequiresUserGesture = isEng
                        }
                        loadUrl(targetUrl)
                    }
                },
                update = { view ->
                    if (view.url != targetUrl) {
                        view.loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }
            )

            if (isWebLoading) {
                CircularProgressIndicator(color = Color(0xFFB71C1C))
            }
        }
    }
}
