package com.xyz.netmobile

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
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.WebViewState
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult

// 广告拦截关键字列表
private val AD_KEYWORDS = listOf(
    "googleads", "doubleclick", "adsbygoogle", "amazon-adsystem",
    "popads", "adservice", "analytics", "facebook.com/tr",
    "adsystem", "adnxs", "smartadserver", "sofascore-ads"
)

// 通用的 iOS 请求拦截器，用于屏蔽广告加速加载
private val IOS_AD_INTERCEPTOR = object : RequestInterceptor {
    override fun onInterceptUrlRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult {
        val url = request.url.lowercase()
        if (AD_KEYWORDS.any { url.contains(it) }) {
            return WebRequestInterceptResult.Reject
        }
        return WebRequestInterceptResult.Allow
    }
}

@Composable
actual fun HorseScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://netmobile.me/app/horse/horse_info" else "https://netmobile.me/app/horse/horse_info?lang=zh-hk"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator(requestInterceptor = IOS_AD_INTERCEPTOR)
    val platform = getPlatform()

    DisposableEffect(Unit) {
        onDispose {
            platform.setOrientation(false)
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        if (state.lastLoadedUrl != targetUrl) {
            navigator.loadUrl(targetUrl)
        }
    }

    WebScreenLayout(
        title = if (isEng) "Horse Racing" else "赛马信息",
        isEng = isEng,
        onBack = onBack,
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator,
        targetUrl = targetUrl
    )
}

@Composable
actual fun HorseLiveScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://horsereplaypage.netlify.app/?lang=en" else "https://horsereplaypage.netlify.app/?lang=zh"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator(requestInterceptor = IOS_AD_INTERCEPTOR)
    val platform = getPlatform()

    DisposableEffect(Unit) {
        onDispose {
            platform.setOrientation(false)
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        if (state.lastLoadedUrl != targetUrl) {
            navigator.loadUrl(targetUrl)
        }
    }

    WebScreenLayout(
        title = if (isEng) "Horse Replay" else "马赛重播",
        isEng = isEng,
        onBack = onBack,
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator,
        targetUrl = targetUrl
    )
}

@Composable
actual fun SoccerScoresScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://www.sofascore.com/" else "https://www.boti.net/football/"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator(requestInterceptor = IOS_AD_INTERCEPTOR)
    val platform = getPlatform()

    DisposableEffect(Unit) {
        onDispose {
            platform.setOrientation(false)
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        if (state.lastLoadedUrl != targetUrl) {
            navigator.loadUrl(targetUrl)
        }
    }

    WebScreenLayout(
        title = if (isEng) "Soccer Scores" else "足球比分",
        isEng = isEng,
        onBack = onBack,
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator,
        targetUrl = targetUrl
    )
}

@Composable
actual fun SoccerOddsScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://netmobile.me/app/soccer/main2" else "https://netmobile.me/app/soccer/main2?lang=2"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator(requestInterceptor = IOS_AD_INTERCEPTOR)
    val platform = getPlatform()

    DisposableEffect(Unit) {
        onDispose {
            platform.setOrientation(false)
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        if (state.lastLoadedUrl != targetUrl) {
            navigator.loadUrl(targetUrl)
        }
    }

    WebScreenLayout(
        title = if (isEng) "Soccer Odds" else "足球赔率",
        isEng = isEng,
        onBack = onBack,
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator,
        targetUrl = targetUrl
    )
}

@Composable
actual fun LotteryScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://4dlotterypage.netlify.app/?lang=en" else "https://4dlotterypage.netlify.app/?lang=zh"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator(requestInterceptor = IOS_AD_INTERCEPTOR)
    val platform = getPlatform()

    DisposableEffect(Unit) {
        onDispose {
            platform.setOrientation(false)
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        if (state.lastLoadedUrl != targetUrl) {
            navigator.loadUrl(targetUrl)
        }
    }

    WebScreenLayout(
        title = if (isEng) "Lottery Results" else "开奖结果",
        isEng = isEng,
        onBack = onBack,
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator,
        targetUrl = targetUrl
    )
}

@Composable
fun WebScreenLayout(
    title: String,
    isEng: Boolean,
    onBack: () -> Unit,
    onLanguageChange: (String) -> Unit,
    state: WebViewState,
    navigator: WebViewNavigator,
    targetUrl: String
) {
    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)

    val currentUrl = state.lastLoadedUrl?.removeSuffix("/") ?: ""
    val normalizedTarget = targetUrl.removeSuffix("/")
    val isAtRoot = currentUrl == normalizedTarget || currentUrl.isEmpty()

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
                    .clickable { 
                        if (isAtRoot || !navigator.canGoBack) {
                            onBack()
                        } else {
                            navigator.navigateBack()
                        }
                    },
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
                text = title,
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
            val loadingState = state.loadingState
            val isWebLoading = loadingState is LoadingState.Loading
            
            val contentAlpha by animateFloatAsState(
                targetValue = if (isWebLoading) 0f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "ContentAlpha"
            )

            WebView(
                state = state,
                navigator = navigator,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }
            )

            if (isWebLoading) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
        }
    }
    
    if (state.loadingState is LoadingState.Finished) {
        val script = """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; document.head.appendChild(meta); }
                var style = document.createElement('style');
                style.innerHTML = 'html, body { overflow-x: hidden !important; width: auto !important; transform-origin: top left; }';
                document.head.appendChild(style);
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
                
                // 强制隐藏广告容器以加速渲染
                var adSelectors = ['.adsbygoogle', 'ins.adsbygoogle', '[id*="google_ads"]', '.sofascore-ads', '[class*="AdWrapper"]', '.top-ad-container'];
                adSelectors.forEach(function(s) {
                    document.querySelectorAll(s).forEach(function(el) { el.style.display = 'none'; });
                });
            })();
        """.trimIndent()
        LaunchedEffect(state.loadingState) {
            navigator.evaluateJavaScript(script)
        }
    }
}
