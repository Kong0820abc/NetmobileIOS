package com.xyz.netmobile

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.xyz.netmobile.ui.theme.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.BLACK, android.graphics.Color.BLACK),

            )
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NetmobileTheme {
                val context = LocalContext.current
                    val networkObserver = remember { NetworkObserver(context) }
                    DisposableEffect(networkObserver) {
                        onDispose { networkObserver.clear() }
                    }
                    val networkStatus = networkObserver.status

                    LaunchedEffect(networkStatus) {
                        Log.d("MainActivity", "Network Status Changed: $networkStatus")
                    }

                    val androidId = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    val sharedPref = remember {
                        context.getSharedPreferences(
                            "user_prefs",
                            MODE_PRIVATE
                        )
                    }
                    val savedUser = remember { sharedPref.getString("logged_in_user", null) }

                    var selectedLanguage by remember {
                        mutableStateOf(sharedPref.getString("selected_lang", "ENG") ?: "ENG")
                    }

                    var currentScreen by remember { mutableStateOf("initial_auth") }
                    var loggedInUser by remember { mutableStateOf(savedUser ?: "") }

                    // --- 专属更新检测器 (读取 NetmobileUpdate 节点) ---
                    NetmobileUpdateChecker(selectedLanguage)

                    val onLanguageChange = { lang: String ->
                        selectedLanguage = lang
                        sharedPref.edit { putString("selected_lang", lang) }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(durationMillis = 400),
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                "initial_auth" -> {
                                    InitialAuthScreen(
                                        networkStatus = networkStatus,
                                        androidId = androidId,
                                        savedUser = savedUser,
                                        selectedLanguage = selectedLanguage,
                                        onResult = { user ->
                                            if (user != null) {
                                                loggedInUser = user
                                                sharedPref.edit { putString("logged_in_user", user) }
                                                currentScreen = "home"
                                            } else {
                                                currentScreen = "login"
                                            }
                                        }
                                    )
                                }

                                "login" -> {
                                    LoginScreen(
                                        networkStatus = networkStatus,
                                        initialUsername = loggedInUser,
                                        androidId = androidId,
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onLoginSuccess = { user ->
                                            sharedPref.edit { putString("logged_in_user", user) }
                                            loggedInUser = user
                                            currentScreen = "home"
                                        }
                                    )
                                }

                                "home" -> {
                                    HomeScreen(
                                        networkStatus = networkStatus,
                                        username = loggedInUser,
                                        androidId = androidId,
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onLogout = { currentScreen = "login" },
                                        onNavigateToHorse = { currentScreen = "horse" },
                                        onNavigateToSoccer = { currentScreen = "soccer" },
                                        onNavigateToLottery = { currentScreen = "lottery" },
                                        onNavigateToHorseLive = { currentScreen = "horse_live" }
                                    )
                                }

                                "horse_live" -> {
                                    HorseLiveScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "home" }
                                    )
                                }

                                "horse" -> {
                                    HorseScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "home" }
                                    )
                                }

                                "soccer" -> {
                                    SoccerScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "home" },
                                        onNavigateToScores = { currentScreen = "soccer_scores" },
                                        onNavigateToOdds = { currentScreen = "soccer_odds" }
                                    )
                                }

                                "soccer_scores" -> {
                                    SoccerScoresScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "soccer" }
                                    )
                                }



                                "soccer_odds" -> {
                                    SoccerOddsScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "soccer" }
                                    )
                                }

                                "lottery" -> {
                                    LotteryScreen(
                                        selectedLanguage = selectedLanguage,
                                        onLanguageChange = onLanguageChange,
                                        onBack = { currentScreen = "home" }
                                    )
                                }
                            }
                        }
                        NetworkStatusAlert(networkStatus, selectedLanguage)
                    }
                }
            }
        }

    @Composable
    fun NetworkStatusAlert(status: NetworkObserver.Status, selectedLanguage: String) {
        val isProbing = status is NetworkObserver.Status.Probing
        // 1. 使用 animateColorAsState 实现平滑颜色过渡
        val themeColor by animateColorAsState(
            targetValue = if (isProbing) Color(0xFFFFA000) else Color(0xFFE53935),
            animationSpec = tween(durationMillis = 600),
            label = "NetworkThemeColor"
        )

        AnimatedVisibility(
            visible = status !is NetworkObserver.Status.Available,
            enter = fadeIn() + scaleIn(initialScale = 0.92f), // 微缩放进入
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            // 全屏锁定蒙层：强制拦截所有交互，确保断网期间无法操作业务
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 彻底拦截所有点击事件 */ }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isProbing) Icons.Default.SignalWifiConnectedNoInternet4 else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = themeColor, // 应用动画颜色
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = when {
                                isProbing -> if (selectedLanguage == "ENG") "Restricted Network" else "网络受限"
                                else -> if (selectedLanguage == "ENG") "No Connection" else "网络不可用"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                isProbing -> if (selectedLanguage == "ENG") "Validating internet access..." else "正在尝试拨测互联网..."
                                else -> if (selectedLanguage == "ENG") "Please check your network settings." else "请检查移动数据或 WiFi 是否开启"
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        if (isProbing) {
                            Spacer(modifier = Modifier.height(24.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = themeColor, // 进度条颜色也同步动画
                                trackColor = Color(0xFFF5F5F5)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InitialAuthScreen(
        networkStatus: NetworkObserver.Status,
        androidId: String,
        savedUser: String?,
        selectedLanguage: String,
        onResult: (String?) -> Unit
    ) {
        val context = LocalContext.current
        val database = Firebase.database
        val userDetailsRef = database.getReference("UserDetails")

        // 使用 rememberUpdatedState 确保 LaunchedEffect 内的 snapshotFlow 总是能读取到最新的网络状态
        val currentStatus by rememberUpdatedState(networkStatus)

        LaunchedEffect(Unit) {
            // 1. 等待网络可用
            snapshotFlow { currentStatus }.first { it is NetworkObserver.Status.Available }

            // 视觉平滑缓冲，避免由于执行过快导致的闪烁
            delay(500)

            try {
                if (savedUser != null) {
                    // --- 路径 A: 验证已知用户 ---
                    val snapshot = userDetailsRef.child(savedUser).get().await()
                    val dbDeviceId = snapshot.child("deviceId").value?.toString()

                    if (snapshot.exists() && dbDeviceId == androidId) {
                        onResult(savedUser)
                    } else {
                        if (dbDeviceId != null && dbDeviceId != androidId) {
                            val msg = if (selectedLanguage == "ENG") "Device changed" else "设备已更改"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        onResult(null)
                    }
                } else {
                    // --- 路径 B: 根据设备 ID 寻找用户 ---
                    val snapshot = withTimeoutOrNull(8000L) {
                        userDetailsRef.get().await()
                    }

                    if (snapshot == null) {
                        if (currentStatus is NetworkObserver.Status.Available) {
                            val slowMsg = if (selectedLanguage == "ENG") "Slow network response..." else "当前网络连接缓慢..."
                            Toast.makeText(context, slowMsg, Toast.LENGTH_SHORT).show()
                        }
                        onResult(null)
                    } else {
                        var foundUser: String? = null
                        for (userSnapshot in snapshot.children) {
                            val deviceId = userSnapshot.child("deviceId").value?.toString()
                            if (deviceId == androidId) {
                                foundUser = userSnapshot.key
                                break
                            }
                        }
                        onResult(foundUser)
                    }
                }
            } catch (e: Exception) {
                Log.e("Auth", "Initial check failed: ${e.message}")
                onResult(null)
            }
        }

        val loadingText = if (selectedLanguage == "ENG") "Logging in..." else "登录中..."

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFD30000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.Yellow)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = loadingText,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }

    @Composable
    fun LoginScreen(
        networkStatus: NetworkObserver.Status,
        initialUsername: String,
        androidId: String,
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onLoginSuccess: (String) -> Unit
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var username by remember { mutableStateOf(initialUsername) }
        var password by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }

        val isNetAvailable = networkStatus is NetworkObserver.Status.Available

        val database = Firebase.database
        val loginRef = database.getReference("Login")
        val userDetailsRef = database.getReference("UserDetails")

        LaunchedEffect(username) {
            val trimmedUser = username.trim()
            if (trimmedUser.isNotEmpty()) {
                try {
                    // 加入 5 秒超时，避免自动填充卡死
                    val snapshot = withTimeoutOrNull(5000L) {
                        loginRef.child(trimmedUser).get().await()
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val rawPassword = snapshot.getValue(String::class.java) ?: ""
                        password = rawPassword.replace("\"", "")
                    } else {
                        password = ""
                    }
                } catch (_: Exception) {
                    password = ""
                }
            } else {
                password = ""
            }
        }

        val isEng = selectedLanguage == "ENG"
        val usernameHint = if (isEng) "Username" else "用户名"
        val passwordHint = if (isEng) "Password" else "密码"
        val signInText = if (isEng) "Sign in" else "登录"
        val userNotFound = if (isEng) "User not found" else "找不到该用户"
        val deviceMismatch =
            if (isEng) "Account bound to another device" else "此账号已绑定其他设备"
        val brandName = "TURF MOBILE"
        val bottomAnnouncement =
            if (isEng) "$brandName do not accept any illegal betting activities" else "$brandName 不接受任何非法博彩活动"

        val bgColor = Color(0xFFD30000)
        val cardColor = Color(0xFFF0A51E)
        val buttonColor = Color(0xFFB71C1C)
        val radioSelectedColor = Color.Cyan

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.15f))

                // Logo Section: Ingot background with Text
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTablet) 320.dp else 240.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Form Card
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = if (isTablet) 600.dp else 480.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(if (isTablet) 40.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) },
                            placeholder = { Text(usernameHint, color = Color.Gray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))
                        TextField(
                            value = password,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
                            placeholder = { Text(passwordHint, color = Color.Gray) },
                            singleLine = true,
                            readOnly = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val radioColors = RadioButtonDefaults.colors(
                                selectedColor = radioSelectedColor,
                                unselectedColor = Color.White
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onLanguageChange("ENG") }
                            ) {
                                RadioButton(
                                    selected = selectedLanguage == "ENG",
                                    onClick = { onLanguageChange("ENG") },
                                    colors = radioColors
                                )
                                Text(
                                    "ENG",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = if (isTablet) 20.sp else 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onLanguageChange("中文") }
                            ) {
                                RadioButton(
                                    selected = selectedLanguage == "中文",
                                    onClick = { onLanguageChange("中文") },
                                    colors = radioColors
                                )
                                Text(
                                    "中文",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = if (isTablet) 20.sp else 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val finalUser = username.trim()
                                if (finalUser.isEmpty()) {
                                    Toast.makeText(context, userNotFound, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isVerifying = true

                                    val loginSuccess = withTimeoutOrNull(10000L) {
                                        try {
                                            val loginSnapshot = loginRef.child(finalUser).get().await()
                                            if (!loginSnapshot.exists()) {
                                                Toast.makeText(context, userNotFound, Toast.LENGTH_SHORT).show()
                                                return@withTimeoutOrNull false
                                            }

                                            val deviceSnapshot = userDetailsRef.child(finalUser).child("deviceId").get().await()
                                            val boundId = deviceSnapshot.getValue(String::class.java)

                                            if (boundId == null || boundId == androidId) {
                                                if (boundId == null) {
                                                    userDetailsRef.child(finalUser).child("deviceId").setValue(androidId).await()
                                                }
                                                true
                                            } else {
                                                Toast.makeText(context, deviceMismatch, Toast.LENGTH_LONG).show()
                                                false
                                            }
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }

                                    isVerifying = false
                                    if (loginSuccess == null) {
                                        if (networkStatus is NetworkObserver.Status.Available) {
                                            val timeoutMsg = if (selectedLanguage == "ENG") "Network Timeout" else "网络超时"
                                            Toast.makeText(context, timeoutMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    } else if (loginSuccess) {
                                        onLoginSuccess(finalUser)
                                    }
                                }
                            },
                            enabled = !isVerifying && isNetAvailable,
                            modifier = Modifier
                                .align(Alignment.End)
                                .width(if (isTablet) 160.dp else 120.dp)
                                .height(if (isTablet) 64.dp else 50.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White,
                                disabledContainerColor = buttonColor.copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                if (isVerifying) "..." else signInText,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isTablet) 20.sp else 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.3f))

                val noteLabel = if (isEng) "Note: " else "注意: "
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(noteLabel)
                        }
                        append(bottomAnnouncement)
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                )

                Spacer(modifier = Modifier.weight(0.1f))

                // 底部动态占位 (Spacer)
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }

    @Composable
    fun HomeScreen(
        networkStatus: NetworkObserver.Status,
        username: String,
        androidId: String,
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onLogout: () -> Unit,
        onNavigateToHorse: () -> Unit,
        onNavigateToSoccer: () -> Unit,
        onNavigateToLottery: () -> Unit,
        onNavigateToHorseLive: () -> Unit
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        
        val context = LocalContext.current
        val activity = context as? Activity
        var dueDate by remember { mutableStateOf("Loading...") }
        val isEng = selectedLanguage == "ENG"
        val userDetailsRef = Firebase.database.getReference("UserDetails").child(username)

        val isNetAvailable = networkStatus is NetworkObserver.Status.Available

        val logoutText = if (isEng) "Logout" else "注销"
        val duedateLabel = if (isEng) "Expiry Date: " else "到期日期: "
        val announcementTitle = if (isEng) "Special announcement:" else "特别公告:"

        var showExitDialog by remember { mutableStateOf(false) }

        BackHandler {
            showExitDialog = true
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEng) "Exit App" else "退出应用",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng) "Are you sure you want to exit?" else "确定要退出软件吗？",
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExitDialog = false },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isEng) "Cancel" else "取消",
                                color = Color.Black,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Button(
                            onClick = { activity?.finish() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isEng) "Exit" else "退出",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                },
                dismissButton = null,
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 400.dp)
            )
        }
        val announcementText = if (isEng) {
            "Turf Mobile provides horse racing, soccer and lottery info for personal use only. We do not offer gambling or betting services."
        } else {
            "Turf Mobile及其附属网站仅提供赛马、足球和彩票相关信息供个人使用。Turf Mobile不会在平台上提供博彩或投注服务。"
        }

        // --- Subscription Check Logic ---
        val daysRemaining =
            if (dueDate != "Loading...") SubscriptionManager.getDaysRemaining(dueDate) else 999
        val isExpired = daysRemaining < 0 && dueDate != "Loading..."
        val showReminder = daysRemaining in 0..3 && dueDate != "Loading..."
        var reminderDismissed by remember { mutableStateOf(false) }

        if (isExpired) {
            AlertDialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEng) "Subscription Expired" else "会员已到期",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng)
                                "Your subscription has expired. Please contact support to renew and continue using the service."
                            else
                                "您的会员已到期，请联系客服续费以继续使用服务。",
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { onLogout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEng) "Back to Login" else "返回登录", color = Color.White)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 400.dp)
            )
        } else if (showReminder && !reminderDismissed) {
            AlertDialog(
                onDismissRequest = { reminderDismissed = true },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEng) "Subscription Reminder" else "续费提醒",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFE65100),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng)
                                "Your subscription will expire in $daysRemaining day(s). Please renew in time to avoid service interruption."
                            else
                                "您的会员还有 $daysRemaining 天到期，请及时续费以免影响使用。",
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { reminderDismissed = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEng) "Understood" else "知道了", color = Color.White)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 400.dp)
            )
        }

        LaunchedEffect(Unit) {
            if (username.isBlank()) {
                onLogout()
                return@LaunchedEffect
            }
            userDetailsRef.child("deviceId").get().addOnSuccessListener { snapshot ->
                try {
                    val boundId = snapshot.value?.toString()
                    if (boundId != null && boundId != androidId) {
                        val msg =
                            if (isEng) "Account bound to another device" else "此账号已绑定其他设备"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onLogout()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "deviceId check error: ${e.message}")
                }
            }

            userDetailsRef.child("dueDate").get().addOnSuccessListener { snapshot ->
                try {
                    if (snapshot.exists()) {
                        val rawDate = snapshot.value?.toString() ?: ""
                        // 只取日期部分，过滤掉时间
                        dueDate = if (rawDate.contains(" ")) rawDate.split(" ")[0] else rawDate
                    } else {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.MONTH, 1)
                        calendar.set(Calendar.DAY_OF_MONTH, 6)
                        // 使用 yyyy-MM-dd 格式，不含时间
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val newDueDate = formatter.format(calendar.time)
                        userDetailsRef.child("dueDate").setValue(newDueDate)
                        dueDate = newDueDate
                    }
                } catch (e: Exception) {
                    dueDate = "Error"
                    android.util.Log.e("HomeScreen", "dueDate fetch error: ${e.message}")
                }
            }
        }

        val colorRed = Color(0xFFD32F2F)
        val colorYellow = Color(0xFFFFEB3B)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Top Bar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorRed)
                    .statusBarsPadding()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Logout Left
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onLogout() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = colorYellow,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isEng) "Logout" else "注销",
                        color = colorYellow,
                        fontSize = if (isTablet) 18.sp else 14.sp
                    )
                }

                // Language Right
                Text(
                    text = if (isEng) "中文" else "ENG",
                    color = colorYellow,
                    fontSize = if (isTablet) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onLanguageChange(if (isEng) "中文" else "ENG") }
                )
            }

            Spacer(modifier = Modifier.weight(0.8f))
            Spacer(modifier = Modifier.height(24.dp))

            // --- Grid Area with Red Lines ---
            Box(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 680.dp else 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1.1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        HomeGridItem(
                            Modifier.weight(1f),
                            R.drawable.horse,
                            if (isEng) "Horse" else "赛马",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = isNetAvailable,
                            showBackground = false
                        ) { onNavigateToHorse() }

                        // Vertical Divider
                        Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(colorRed))

                        HomeGridItem(
                            Modifier.weight(1f),
                            R.drawable.soccer,
                            if (isEng) "Soccer" else "足球",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = isNetAvailable,
                            showBackground = false
                        ) { onNavigateToSoccer() }
                    }

                    // Horizontal Divider
                    Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(colorRed))

                    Row(modifier = Modifier.weight(1f)) {
                        HomeGridItem(
                            Modifier.weight(1f),
                            R.drawable.lottery,
                            if (isEng) "Lottery" else "彩票",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = isNetAvailable,
                            showBackground = false
                        ) { onNavigateToLottery() }

                        // Vertical Divider
                        Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(colorRed))

                        HomeGridItem(
                            Modifier.weight(1f),
                            R.drawable.replay,
                            if (isEng) "Horse Playback" else "马赛重播",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = isNetAvailable,
                            showBackground = false,
                            onClick = { onNavigateToHorseLive() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ID and DueDate ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 26.dp else 18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ID: ",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 24.sp else 19.sp
                    )
                    Text(
                        text = username,
                        color = colorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 24.sp else 19.sp
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Duedate: ",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 24.sp else 19.sp
                    )
                    Text(
                        text = dueDate,
                        color = colorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 24.sp else 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Announcement ---
            Column(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 720.dp else 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp)
                    .heightIn(min = 160.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = announcementTitle,
                    color = colorRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 22.sp else 15.sp
                )
                Text(
                    text = announcementText,
                    color = Color.Black,
                    fontSize = if (isTablet) 20.sp else 15.sp,
                    lineHeight = if (isTablet) 26.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部动态占位 (Spacer)
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }






    @Composable
    fun HorseLiveScreen(
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
            if (isEng) "https://horsereplay.netlify.app/?lang=en" else "https://horsereplay.netlify.app/?lang=zh"
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
                                if (view.url != targetUrl && !targetUrl.startsWith("javascript:")) {
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




    @Composable
    fun HorseScreen(
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var webView by remember { mutableStateOf<WebView?>(null) }
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
        val colorPrimary = Color(0xFFD32F2F)

        val url = remember(selectedLanguage) {
            if (isEng) "https://netmobile.me/app/horse/horse_info" else "https://netmobile.me/app/horse/horse_info?lang=zh-hk"
        }

        var isWebLoading by remember { mutableStateOf(true) }
        var needsClearHistory by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (isWebLoading) 0f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        text = if (isEng) "Horse Racing" else "赛马信息",
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
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
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
                                        view?.evaluateJavascript(script, null)
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
                                }
                                loadUrl(url)
                            }
                        },
                        update = { view ->
                            if (view.url != url && !url.startsWith("javascript:")) {
                                needsClearHistory = true
                                view.loadUrl(url)
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
    }

    @Composable
    fun SoccerMenuButton(
        title: String,
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        }
    }

    @Composable
    fun SoccerScreen(
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onBack: () -> Unit,
        onNavigateToScores: () -> Unit,
        onNavigateToOdds: () -> Unit
    ) {
        BackHandler(onBack = onBack)
        val isEng = selectedLanguage == "ENG"
        val colorRed = Color(0xFFD32F2F)
        val colorYellow = Color(0xFFFFEB3B)
        val colorPrimary = Color(0xFFB71C1C)
        val colorBackground = Color(0xFFF5F7FA)

        Surface(modifier = Modifier.fillMaxSize(), color = colorBackground) {
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
                        text = if (isEng) "Soccer Info" else "足球资讯",
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    SoccerMenuButton(
                        title = if (isEng) "Soccer Scores" else "足球比分",
                        icon = Icons.Default.Score,
                        onClick = onNavigateToScores
                    )

                    SoccerMenuButton(
                        title = if (isEng) "Soccer Odds" else "足球赔率",
                        icon = Icons.Default.Timeline,
                        onClick = onNavigateToOdds
                    )
                }
            }
        }
    }

    @Composable
    fun SoccerScoresScreen(
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

        // 拦截返回键逻辑
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
        val colorPrimary = Color(0xFFB71C1C)
        val colorBackground = Color(0xFFF5F7FA)

        // 指定目标网址
        val targetUrl = remember(selectedLanguage) {
            if (isEng) "https://www.sofascore.com/" else "https://www.boti.net/football/"
        }

        var isWebLoading by remember { mutableStateOf(true) }
        var needsClearHistory by remember { mutableStateOf(false) }

        // 内容淡入动画
        val contentAlpha by animateFloatAsState(
            targetValue = if (isWebLoading) 0f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                // --- 顶部导航栏 ---
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

                // --- WebView 容器 ---
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

                                        // 注入 JS 优化移动端适配并隐藏广告
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
                                        view?.evaluateJavascript(script, null)
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
                                }
                                loadUrl(targetUrl)
                            }
                        },
                        update = { view ->
                            if (view.url != targetUrl && !targetUrl.startsWith("javascript:")) {
                                needsClearHistory = true
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
    }

    @Composable
    fun SoccerLiveScreen(
        selectedLanguage: String,
        onBack: () -> Unit
    ) {
        val context = LocalContext.current
        val activity = context as? Activity
        val window = (context as? Activity)?.window

        var webView by remember { mutableStateOf<WebView?>(null) }
        var customView by remember { mutableStateOf<View?>(null) }
        var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

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

        val hideCustomView = {
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            toggleSystemBars(true)
        }

        BackHandler {
            if (customView != null) {
                hideCustomView()
            } else if (webView?.canGoBack() == true) {
                webView?.goBack()
            } else {
                onBack()
            }
        }

        val isEng = selectedLanguage == "ENG"
        val colorRed = Color(0xFFD32F2F)
        val colorYellow = Color(0xFFFFEB3B)
        val colorPrimary = Color(0xFFB71C1C)

        val targetUrl = "https://m.yyzb1.tv/match.html"

        var isWebLoading by remember { mutableStateOf(true) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (isWebLoading) 0f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
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
                            text = if (isEng) "Soccer Live" else "足球直播",
                            color = colorYellow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webView = this
                                setBackgroundColor(android.graphics.Color.WHITE)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    textZoom = 100
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                                }

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

                                        // 优化后的全屏自动关闭弹幕逻辑（观察者模式）
                                        val observerScript = """
                                            (function() {
                                                function autoClose() {
                                                    var selectors = ['.danmu-switch', '.barrage-switch', '.btn-danmu', '.icon-danmu'];
                                                    selectors.forEach(function(s) {
                                                        var btn = document.querySelector(s);
                                                        if (btn && btn.innerText.indexOf('关') === -1 && btn.offsetHeight > 0) {
                                                            btn.click();
                                                        }
                                                    });
                                                }
                                                autoClose();
                                                var observer = new MutationObserver(autoClose);
                                                observer.observe(document.body, { childList: true, subtree: true });
                                                setTimeout(autoClose, 500);
                                            })();
                                        """.trimIndent()

                                        postDelayed({
                                            evaluateJavascript(observerScript, null)
                                        }, 100)
                                    }

                                    override fun onHideCustomView() {
                                        hideCustomView()
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
                                        val hideElementsScript = """
                                            (function() {
                                                var style = document.createElement('style');
                                                style.innerHTML = `
                                                    .header-down, .top-download, .download-app, [class*="down-app"],
                                                    .footer, .nav-bottom, .bottom-nav, [class*="footer-ad"],
                                                    .mask, .modal, .pop-ups, .announcement-mask,
                                                    .hd-down, .match-ad, #match-ad, .ad-banner, [id*="ad-"] { display: none !important; }
                                                    
                                                    /* 提前隐藏弹幕层 */
                                                    .danmu-box, .barrage-box, .dm-canvas, .danmu-item,
                                                    [class*="danmu-layer"], [id*="danmu"], [class*="barrage"],
                                                    .player-danmaku, .video-danmaku { 
                                                        display: none !important; 
                                                        visibility: hidden !important; 
                                                        opacity: 0 !important;
                                                        pointer-events: none !important;
                                                    }

                                                    body { padding-top: 0 !important; padding-bottom: 0 !important; }
                                                `;
                                                document.head.appendChild(style);
                                                
                                                var closeBtn = document.querySelector('.close, .know-btn, .confirm-btn, .×');
                                                if(closeBtn) closeBtn.click();
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(hideElementsScript, null)
                                        postDelayed({ isWebLoading = false }, 800)
                                    }
                                }
                                loadUrl(targetUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }
                    )

                    if (isWebLoading) {
                        CircularProgressIndicator(color = Color(0xFFB71C1C))
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

    @Composable
    fun SoccerOddsScreen(
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var webView by remember { mutableStateOf<WebView?>(null) }
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
        val colorPrimary = Color(0xFFB71C1C)
        val colorBackground = Color(0xFFF5F7FA)

        val targetUrl = remember(selectedLanguage) {
            if (isEng) "https://netmobile.me/app/soccer/main2" else "https://netmobile.me/app/soccer/main2?lang=2"
        }

        var isWebLoading by remember { mutableStateOf(true) }
        var needsClearHistory by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (isWebLoading) 0f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        text = if (isEng) "Soccer Odds" else "足球赔率",
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
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
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
                                        view?.evaluateJavascript(script, null)
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
                                }
                                loadUrl(targetUrl)
                            }
                        },
                        update = { view ->
                            if (view.url != targetUrl && !targetUrl.startsWith("javascript:")) {
                                needsClearHistory = true
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
    }

    @Composable
    fun LotteryScreen(
        selectedLanguage: String,
        onLanguageChange: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var webView by remember { mutableStateOf<WebView?>(null) }
        val adKeywords = remember {
            listOf(
                "googleads", "doubleclick", "adsbygoogle", "amazon-adsystem",
                "popads", "adservice", "analytics", "facebook.com/tr",
                "adsystem", "adnxs", "smartadserver"
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
        val colorPrimary = Color(0xFFB71C1C)
        val colorBackground = Color(0xFFF5F7FA)

        val url = remember(selectedLanguage) {
            if (isEng) "https://4dlotteryresults.netlify.app/?lang=en" else "https://4dlotteryresults.netlify.app/?lang=zh"
        }

        var isWebLoading by remember { mutableStateOf(true) }
        var needsClearHistory by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (isWebLoading) 0f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "ContentAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        text = if (isEng) "Lottery Results" else "开奖结果",
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
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
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
                                        val script = """
                                        (function() {
                                            var meta = document.querySelector('meta[name="viewport"]');
                                            if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; document.head.appendChild(meta); }
                                            
                                            var style = document.createElement('style');
                                            style.innerHTML = `
                                                .adsbygoogle, ins.adsbygoogle, [id*="google_ads"], 
                                                iframe[src*="ads"], div[class*="ad-"], div[id*="ad-"] { 
                                                    display: none !important; 
                                                }
                                                html, body { overflow-x: hidden !important; width: auto !important; transform-origin: top left; }
                                            `;
                                            
                                            function hideAds() {
                                                var ads = document.querySelectorAll('.adsbygoogle, ins.adsbygoogle, [id*="google_ads"]');
                                                ads.forEach(function(el) { 
                                                    el.style.setProperty('display', 'none', 'important'); 
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
                                        view?.evaluateJavascript(script, null)
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
                                }
                                loadUrl(url)
                            }
                        },
                        update = { view ->
                            if (view.url != url && !url.startsWith("javascript:")) {
                                needsClearHistory = true
                                view.loadUrl(url)
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
    }


    // --- 专属更新数据模型 ---
    data class NetmobileUpdateInfo(
        val newVersionCode: Int = 0,
        val apkUrl: String = "",
        val isForce: Boolean = true
    )

    // --- 获取当前 App 版本号工具 ---
    private fun getCurrentVersionCode(context: android.content.Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION") packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    // --- 专属更新检测组件 ---
    @Composable
    fun NetmobileUpdateChecker(selectedLanguage: String) {
        val context = LocalContext.current
        val database = Firebase.database
        val updateRef = database.getReference("NetmobileUpdate")

        var updateData by remember { mutableStateOf<NetmobileUpdateInfo?>(null) }
        var showDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            updateRef.get().addOnSuccessListener { snapshot ->
                try {
                    if (snapshot.exists()) {
                        val rawVersion = snapshot.child("newVersionCode").value
                        val newVersionCode = when (rawVersion) {
                            is Long -> rawVersion.toInt()
                            is String -> rawVersion.split(".")[0].toIntOrNull() ?: 0
                            else -> 0
                        }
                        val apkUrl = snapshot.child("apkUrl").value?.toString() ?: ""
                        val isForce = when (val f = snapshot.child("isForce").value) {
                            is Boolean -> f
                            is Long -> f == 1L
                            is String -> f.lowercase() == "true"
                            else -> true // 默认开启强制更新
                        }

                        val info = NetmobileUpdateInfo(newVersionCode, apkUrl, isForce)
                        val currentVersion = getCurrentVersionCode(context)

                        // 最终判定逻辑：只有当云端版本明确大于本地版本，且本地读取成功时才弹窗
                        if (newVersionCode > currentVersion && currentVersion > 0) {
                            updateData = info
                            showDialog = true
                        } else {
                            showDialog = false // 强制对齐，防止缓存弹窗
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetmobileUpdate", "Update check failed: ${e.message}")
                }
            }
        }

        if (showDialog && updateData != null) {
            UpdateDialog(
                apkUrl = updateData!!.apkUrl,
                selectedLanguage = selectedLanguage,
                isForce = updateData!!.isForce,
                onDismiss = { if (!updateData!!.isForce) showDialog = false }
            )
        }
    }

    @Composable
    fun HomeGridItem(
        modifier: Modifier,
        imageRes: Int,
        text: String,
        imageSize: androidx.compose.ui.unit.Dp = 80.dp,
        enabled: Boolean = true,
        showBackground: Boolean = true,
        fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
        maxLines: Int = Int.MAX_VALUE,
        softWrap: Boolean = true,
        onClick: () -> Unit
    ) {
        val alpha by animateFloatAsState(
            targetValue = if (enabled) 1f else 0.4f,
            animationSpec = tween(durationMillis = 300),
            label = "GridItemAlpha"
        )
        Column(
            modifier = modifier
                .then(if (showBackground) Modifier.padding(4.dp) else Modifier)
                .graphicsLayer { this.alpha = alpha }
                .then(
                    if (showBackground)
                        Modifier.background(Color(0xFFFFD600), shape = RoundedCornerShape(12.dp))
                    else
                        Modifier
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(imageSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = text,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = Color.Black,
                maxLines = 2,
                softWrap = true,
                lineHeight = 22.sp,
                overflow = TextOverflow.Visible
            )
        }
    }
}

