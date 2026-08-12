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

@Composable
actual fun HorseScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEng = selectedLanguage == "ENG"
    val targetUrl = if (isEng) "https://netmobile.me/app/horse/horse_info" else "https://netmobile.me/app/horse/horse_info?lang=zh-hk"
    
    val state = rememberWebViewState(targetUrl)
    val navigator = rememberWebViewNavigator()
    val platform = getPlatform()

    // 针对马赛重播页面，我们可以根据加载状态注入监听全屏的逻辑
    // 但为了确保用户体验，我们也可以直接在进入此屏幕时强制允许旋转
    DisposableEffect(Unit) {
        // 进入时不需要强制，让系统根据全屏行为自动处理，或者手动触发
        onDispose {
            platform.setOrientation(false) // 退出时强制回竖屏
        }
    }
    
    // 同步 Android 的 UserAgent
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"

    LaunchedEffect(targetUrl) {
        navigator.loadUrl(targetUrl)
    }

    WebScreenLayout(
        title = if (isEng) "Horse Racing" else "赛马信息",
        isEng = isEng,
        onBack = { if (navigator.canGoBack) navigator.navigateBack() else onBack() },
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator
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
    val navigator = rememberWebViewNavigator()
    val platform = getPlatform()

    // 针对马赛重播页面，我们可以根据加载状态注入监听全屏的逻辑
    // 但为了确保用户体验，我们也可以直接在进入此屏幕时强制允许旋转
    DisposableEffect(Unit) {
        // 进入时不需要强制，让系统根据全屏行为自动处理，或者手动触发
        onDispose {
            platform.setOrientation(false) // 退出时强制回竖屏
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"

    WebScreenLayout(
        title = if (isEng) "Horse Replay" else "马赛重播",
        isEng = isEng,
        onBack = { if (navigator.canGoBack) navigator.navigateBack() else onBack() },
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator
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
    val navigator = rememberWebViewNavigator()
    val platform = getPlatform()

    // 针对马赛重播页面，我们可以根据加载状态注入监听全屏的逻辑
    // 但为了确保用户体验，我们也可以直接在进入此屏幕时强制允许旋转
    DisposableEffect(Unit) {
        // 进入时不需要强制，让系统根据全屏行为自动处理，或者手动触发
        onDispose {
            platform.setOrientation(false) // 退出时强制回竖屏
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"

    WebScreenLayout(
        title = if (isEng) "Soccer Scores" else "足球比分",
        isEng = isEng,
        onBack = { if (navigator.canGoBack) navigator.navigateBack() else onBack() },
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator
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
    val navigator = rememberWebViewNavigator()
    val platform = getPlatform()

    // 针对马赛重播页面，我们可以根据加载状态注入监听全屏的逻辑
    // 但为了确保用户体验，我们也可以直接在进入此屏幕时强制允许旋转
    DisposableEffect(Unit) {
        // 进入时不需要强制，让系统根据全屏行为自动处理，或者手动触发
        onDispose {
            platform.setOrientation(false) // 退出时强制回竖屏
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"

    WebScreenLayout(
        title = if (isEng) "Soccer Odds" else "足球赔率",
        isEng = isEng,
        onBack = { if (navigator.canGoBack) navigator.navigateBack() else onBack() },
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator
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
    val navigator = rememberWebViewNavigator()
    val platform = getPlatform()

    // 针对马赛重播页面，我们可以根据加载状态注入监听全屏的逻辑
    // 但为了确保用户体验，我们也可以直接在进入此屏幕时强制允许旋转
    DisposableEffect(Unit) {
        // 进入时不需要强制，让系统根据全屏行为自动处理，或者手动触发
        onDispose {
            platform.setOrientation(false) // 退出时强制回竖屏
        }
    }
    
    state.webSettings.customUserAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"

    WebScreenLayout(
        title = if (isEng) "Lottery Results" else "开奖结果",
        isEng = isEng,
        onBack = { if (navigator.canGoBack) navigator.navigateBack() else onBack() },
        onLanguageChange = onLanguageChange,
        state = state,
        navigator = navigator
    )
}

@Composable
fun WebScreenLayout(
    title: String,
    isEng: Boolean,
    onBack: () -> Unit,
    onLanguageChange: (String) -> Unit,
    state: WebViewState,
    navigator: WebViewNavigator
) {
    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)

    // 核心修复：监听内容变化（通常由语言切换引起），并清理历史记录
    // 注意：ios 的 WebViewNavigator 本身没有 clearHistory，
    // 我们通过在语言切换时“重置”状态来模拟清理，或者在返回逻辑中进行拦截。
    
    // 我们定义一个基础 URL，如果当前 URL 等于基础 URL，则直接退出
    val baseUrl = state.lastLoadedUrl ?: ""

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
                        // 如果当前已经在该语言的首页面，则直接回主菜单
                        if (!navigator.canGoBack) {
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

    // 核心修复：当语言发生变化时，我们应该重置 Navigator 的状态
    // 我们通过在语言变化时强制销毁并重建 WebView 来实现最彻底的历史清理
    key(isEng) {
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }
        )
    }

            if (isWebLoading) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
        }
    }
    
    // 同步 Android 的 JS 注入逻辑 (针对视图缩放)
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
            })();
        """.trimIndent()
        LaunchedEffect(state.loadingState) {
            navigator.evaluateJavaScript(script)
        }
    }
}
