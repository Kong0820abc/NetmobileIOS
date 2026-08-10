package com.xyz.netmobile

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import netmobileios.shared.generated.resources.Res
import netmobileios.shared.generated.resources.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import com.russhwolf.settings.Settings
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebContent
import kotlinx.datetime.*
import com.xyz.netmobile.CommonBackHandler

@Composable
fun App() {
    val platform = getPlatform()
    val settings = remember { createSettings() }
    
    val networkObserver = remember { NetworkObserver() }
    DisposableEffect(networkObserver) {
        onDispose { networkObserver.clear() }
    }
    val networkStatus = networkObserver.status

    var selectedLanguage by remember {
        mutableStateOf(settings.getString("selected_lang", "ENG"))
    }
    
    var currentScreen by remember { mutableStateOf("initial_auth") }
    var loggedInUser by remember { 
        mutableStateOf(settings.getString("logged_in_user", "")) 
    }
    
    val onLanguageChange = { lang: String ->
        selectedLanguage = lang
        settings.putString("selected_lang", lang)
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 400),
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "initial_auth" -> {
                        InitialAuthScreen(
                            platform = platform,
                            networkStatus = networkStatus,
                            selectedLanguage = selectedLanguage,
                            onResult = { user ->
                                if (user != null) {
                                    loggedInUser = user
                                    settings.putString("logged_in_user", user)
                                    currentScreen = "home"
                                } else {
                                    currentScreen = "login"
                                }
                            }
                        )
                    }

                    "login" -> {
                        LoginScreen(
                            platform = platform,
                            networkStatus = networkStatus,
                            initialUsername = loggedInUser,
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = onLanguageChange,
                            onLoginSuccess = { user ->
                                settings.putString("logged_in_user", user)
                                loggedInUser = user
                                currentScreen = "home"
                            }
                        )
                    }

                    "home" -> {
                        HomeScreen(
                            platform = platform,
                            networkStatus = networkStatus,
                            username = loggedInUser,
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = onLanguageChange,
                            onLogout = { 
                                settings.remove("logged_in_user")
                                currentScreen = "login" 
                            },
                            onNavigateToHorse = { currentScreen = "horse" },
                            onNavigateToSoccer = { currentScreen = "soccer" },
                            onNavigateToLottery = { currentScreen = "lottery" },
                            onNavigateToHorseLive = { currentScreen = "horse_live" }
                        )
                    }

                    "horse_live" -> {
                        key(selectedLanguage) {
                            HorseLiveScreen(
                                selectedLanguage = selectedLanguage,
                                onLanguageChange = onLanguageChange,
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                    
                    "horse" -> {
                        key(selectedLanguage) {
                            HorseScreen(
                                selectedLanguage = selectedLanguage,
                                onLanguageChange = onLanguageChange,
                                onBack = { currentScreen = "home" }
                            )
                        }
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
                        key(selectedLanguage) {
                            SoccerScoresScreen(
                                selectedLanguage = selectedLanguage,
                                onLanguageChange = onLanguageChange,
                                onBack = { currentScreen = "soccer" }
                            )
                        }
                    }

                    "soccer_odds" -> {
                        key(selectedLanguage) {
                            SoccerOddsScreen(
                                selectedLanguage = selectedLanguage,
                                onLanguageChange = onLanguageChange,
                                onBack = { currentScreen = "soccer" }
                            )
                        }
                    }
                    
                    "lottery" -> {
                        key(selectedLanguage) {
                            LotteryScreen(
                                selectedLanguage = selectedLanguage,
                                onLanguageChange = onLanguageChange,
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
            
            NetworkStatusAlert(networkStatus, selectedLanguage)
            
            NetmobileUpdateChecker(platform, selectedLanguage)
        }
    }
}

@Composable
fun NetworkStatusAlert(status: NetworkStatus, selectedLanguage: String) {
    val isProbing = status is NetworkStatus.Probing
    val themeColor by animateColorAsState(
        targetValue = if (isProbing) Color(0xFFFFA000) else Color(0xFFE53935),
        animationSpec = tween(durationMillis = 600),
        label = "NetworkThemeColor"
    )

    AnimatedVisibility(
        visible = status !is NetworkStatus.Available,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
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
                    val isEng = selectedLanguage == "ENG"
                    Icon(
                        imageVector = if (isProbing) Icons.Default.SignalWifiConnectedNoInternet4 else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = when {
                            isProbing -> if (isEng) "Restricted Network" else "网络受限"
                            else -> if (isEng) "No Connection" else "网络不可用"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            isProbing -> if (isEng) "Validating internet access..." else "正在尝试拨测互联网..."
                            else -> if (isEng) "Please check your network settings." else "请检查移动数据或 WiFi 是否开启"
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
                            color = themeColor,
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
    platform: Platform,
    networkStatus: NetworkStatus,
    selectedLanguage: String,
    onResult: (String?) -> Unit
) {
    val database = Firebase.database
    val userDetailsRef = database.reference("UserDetails")
    val deviceId = platform.getDeviceId()
    val settings = remember { createSettings() }
    val savedUser = settings.getStringOrNull("logged_in_user")

    val currentStatus by rememberUpdatedState(networkStatus)

    LaunchedEffect(Unit) {
        // 1. 等待网络可用
        snapshotFlow { currentStatus }.first { it is NetworkStatus.Available }

        // 视觉平滑缓冲，避免由于执行过快导致的闪烁
        delay(500)

        try {
            if (savedUser != null) {
                // --- 路径 A: 验证已知用户 ---
                val snapshot = userDetailsRef.child(savedUser).valueEvents.first()
                val dbDeviceId = snapshot.child("deviceId").value<String?>()

                if (snapshot.exists && dbDeviceId == deviceId) {
                    onResult(savedUser)
                } else {
                    if (dbDeviceId != null && dbDeviceId != deviceId) {
                        val msg = if (selectedLanguage == "ENG") "Account bound to another device" else "此账号已绑定其他设备"
                        platform.showToast(msg)
                    }
                    onResult(null)
                }
            } else {
                // --- 路径 B: 根据设备 ID 寻找用户 ---
                val snapshot = withTimeoutOrNull(8000L) {
                    userDetailsRef.valueEvents.first()
                }

                if (snapshot == null) {
                    if (currentStatus is NetworkStatus.Available) {
                        val slowMsg = if (selectedLanguage == "ENG") "Slow network response..." else "当前网络连接缓慢..."
                        platform.showToast(slowMsg)
                    }
                    onResult(null)
                } else {
                    var foundUser: String? = null
                    for (userSnapshot in snapshot.children) {
                        val dbDeviceId = userSnapshot.child("deviceId").value<String?>()
                        if (dbDeviceId == deviceId) {
                            foundUser = userSnapshot.key
                            break
                        }
                    }
                    onResult(foundUser)
                }
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFD30000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.Yellow)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedLanguage == "ENG") "Logging in..." else "登录中...",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun LoginScreen(
    platform: Platform,
    networkStatus: NetworkStatus,
    initialUsername: String,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        
        var username by remember { mutableStateOf(initialUsername) }
        var password by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val deviceId = platform.getDeviceId()

        val database = Firebase.database
        val loginRef = database.reference("Login")
        val userDetailsRef = database.reference("UserDetails")

        LaunchedEffect(username) {
            val trimmedUser = username.trim()
            if (trimmedUser.isNotEmpty()) {
                try {
                    val snapshot = withTimeoutOrNull(5000L) {
                        loginRef.child(trimmedUser).valueEvents.first()
                    }
                    if (snapshot != null && snapshot.exists) {
                        password = snapshot.value<String?>()?.replace("\"", "") ?: ""
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
        val userNotFound = if (isEng) "User not found" else "找不到该用户"
        val deviceMismatch = if (isEng) "Account bound to another device" else "此账号已绑定其他设备"
        
        val bgColor = Color(0xFFD30000)
        val cardColor = Color(0xFFF0A51E)
        val buttonColor = Color(0xFFB71C1C)
        val radioSelectedColor = Color.Cyan

        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.15f))
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(if (isTablet) 320.dp else 240.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.logo_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

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
                            placeholder = { Text(if (isEng) "Username" else "用户名", color = Color.Gray) },
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
                            placeholder = { Text(if (isEng) "Password" else "密码", color = Color.Gray) },
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
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val radioColors = RadioButtonDefaults.colors(
                                selectedColor = radioSelectedColor,
                                unselectedColor = Color.White
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLanguageChange("ENG") } ) {
                                RadioButton(
                                    selected = selectedLanguage == "ENG",
                                    onClick = { onLanguageChange("ENG") },
                                    colors = radioColors
                                )
                                Text("ENG", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = if (isTablet) 20.sp else 16.sp)
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLanguageChange("中文") } ) {
                                RadioButton(
                                    selected = selectedLanguage == "中文",
                                    onClick = { onLanguageChange("中文") },
                                    colors = radioColors
                                )
                                Text("中文", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = if (isTablet) 20.sp else 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val finalUser = username.trim()
                                if (finalUser.isEmpty()) {
                                    platform.showToast(userNotFound)
                                    return@Button
                                }
                                scope.launch {
                                    isVerifying = true
                                    val loginSuccess = withTimeoutOrNull(10000L) {
                                        try {
                                            val loginSnapshot = loginRef.child(finalUser).valueEvents.first()
                                            if (!loginSnapshot.exists) {
                                                platform.showToast(userNotFound)
                                                return@withTimeoutOrNull false
                                            }
                                            val deviceSnapshot = userDetailsRef.child(finalUser).child("deviceId").valueEvents.first()
                                            val boundId = deviceSnapshot.value<String?>()

                                            if (boundId == null || boundId == deviceId) {
                                                if (boundId == null) {
                                                    userDetailsRef.child(finalUser).child("deviceId").setValue(deviceId)
                                                }
                                                true
                                            } else {
                                                platform.showToast(deviceMismatch)
                                                false
                                            }
                                        } catch (_: Exception) { null }
                                    }
                                    isVerifying = false
                                    if (loginSuccess == null) {
                                        if (networkStatus is NetworkStatus.Available) {
                                            platform.showToast(if (isEng) "Network Timeout" else "网络超时")
                                        }
                                    } else if (loginSuccess) {
                                        onLoginSuccess(finalUser)
                                    }
                                }
                            },
                            enabled = !isVerifying && networkStatus is NetworkStatus.Available,
                            modifier = Modifier.align(Alignment.End).width(if (isTablet) 160.dp else 120.dp).height(if (isTablet) 64.dp else 50.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                disabledContainerColor = buttonColor.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(if (isVerifying) "..." else if (isEng) "Sign in" else "登录", fontWeight = FontWeight.Bold, fontSize = if (isTablet) 20.sp else 16.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(0.3f))
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(if (isEng) "Note: " else "注意: ")
                        }
                        append(if (isEng) "NETMOBILE do not accept any illegal betting activities" else "NETMOBILE 不接受任何非法博彩活动")
                    },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp)
                )
                Spacer(modifier = Modifier.weight(0.1f))
            }
        }
    }
}

@Composable
fun HomeScreen(
    platform: Platform,
    networkStatus: NetworkStatus,
    username: String,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToHorse: () -> Unit,
    onNavigateToSoccer: () -> Unit,
    onNavigateToLottery: () -> Unit,
    onNavigateToHorseLive: () -> Unit
) {
    var dueDate by remember { mutableStateOf("Loading...") }
    val isEng = selectedLanguage == "ENG"
    val userDetailsRef = Firebase.database.reference("UserDetails").child(username)
    val deviceId = platform.getDeviceId()

    val daysRemaining = if (dueDate != "Loading...") SubscriptionManager.getDaysRemaining(platform, dueDate) else 999
    val isExpired = daysRemaining < 0 && dueDate != "Loading..."
    val showReminder = daysRemaining in 0..3 && dueDate != "Loading..."
    var reminderDismissed by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    CommonBackHandler {
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
                        Text(text = if (isEng) "Cancel" else "取消", color = Color.Black, fontSize = 14.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { platform.exitApp() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(text = if (isEng) "Exit" else "退出", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
            modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 400.dp)
        )
    }

    if (isExpired) {
        AlertDialog(
            onDismissRequest = { },
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
            modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 400.dp)
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
            modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 400.dp)
        )
    }

    LaunchedEffect(Unit) {
        try {
            val boundIdSnapshot = userDetailsRef.child("deviceId").valueEvents.first()
            val boundId = boundIdSnapshot.value<String?>()
            if (boundId != null && boundId != deviceId) {
                val msg = if (isEng) "Account bound to another device" else "此账号已绑定其他设备"
                platform.showToast(msg)
                onLogout()
                return@LaunchedEffect
            }

            val dueDateSnapshot = userDetailsRef.child("dueDate").valueEvents.first()
            if (dueDateSnapshot.exists) {
                val rawDate = dueDateSnapshot.value<String?>() ?: ""
                dueDate = if (rawDate.contains(" ")) rawDate.split(" ")[0] else rawDate
            } else {
                val nextDueDate = SubscriptionManager.calculateNextDueDate(platform, null)
                userDetailsRef.child("dueDate").setValue(nextDueDate)
                dueDate = nextDueDate
            }
        } catch (e: Exception) {
            dueDate = "Error"
        }
    }

    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val isTablet = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().background(colorRed).statusBarsPadding().padding(vertical = 4.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).clickable { onLogout() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = colorYellow, modifier = Modifier.size(26.dp))
                    Text(text = if (isEng) "Logout" else "注销", color = colorYellow, fontSize = if (isTablet) 18.sp else 14.sp)
                }
                Text(
                    text = if (isEng) "中文" else "ENG",
                    color = colorYellow, fontSize = if (isTablet) 22.sp else 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable { onLanguageChange(if (isEng) "中文" else "ENG") }
                )
            }

            Spacer(modifier = Modifier.weight(0.8f))
            Spacer(modifier = Modifier.height(24.dp))

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
                            Res.drawable.horse,
                            if (isEng) "Horse" else "赛马",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = networkStatus is NetworkStatus.Available,
                            showBackground = false
                        ) { onNavigateToHorse() }

                        Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(colorRed))

                        HomeGridItem(
                            Modifier.weight(1f),
                            Res.drawable.soccer,
                            if (isEng) "Soccer" else "足球",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = networkStatus is NetworkStatus.Available,
                            showBackground = false
                        ) { onNavigateToSoccer() }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(colorRed))
                    Row(modifier = Modifier.weight(1f)) {
                        HomeGridItem(
                            Modifier.weight(1f),
                            Res.drawable.lottery,
                            if (isEng) "Lottery" else "彩票",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = networkStatus is NetworkStatus.Available,
                            showBackground = false
                        ) { onNavigateToLottery() }

                        Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(colorRed))

                        HomeGridItem(
                            Modifier.weight(1f),
                            Res.drawable.replay,
                            if (isEng) "Horse Playback" else "马赛重播",
                            imageSize = if (isTablet) 110.dp else 80.dp,
                            fontSize = if (isTablet) 24.sp else 19.sp,
                            enabled = networkStatus is NetworkStatus.Available,
                            showBackground = false
                        ) { onNavigateToHorseLive() }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(if (isTablet) 26.dp else 18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ID: ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 24.sp else 19.sp)
                    Text(username, color = colorRed, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 24.sp else 19.sp)
                }
                Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Duedate: ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 24.sp else 19.sp)
                    Text(dueDate, color = colorRed, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 24.sp else 19.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.widthIn(max = if (isTablet) 720.dp else 600.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp).heightIn(min = 160.dp)) {
                Text(if (isEng) "Special announcement:" else "特别公告:", color = colorRed, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 22.sp else 15.sp)
                Text(
                    if (isEng) "Netmobile provides horse racing, soccer and lottery info for personal use only. We do not offer gambling or betting services."
                    else "Netmobile及其附属网站仅提供赛马、足球和彩票相关信息供个人使用。Netmobile不会在平台上提供博彩或投注服务。",
                    color = Color.Black, fontSize = if (isTablet) 20.sp else 15.sp, fontWeight = FontWeight.Bold, lineHeight = if (isTablet) 26.sp else 22.sp, textAlign = TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun HomeGridItem(
    modifier: Modifier,
    imageRes: org.jetbrains.compose.resources.DrawableResource,
    text: String,
    imageSize: androidx.compose.ui.unit.Dp = 80.dp,
    enabled: Boolean = true,
    showBackground: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
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
                painter = painterResource(imageRes),
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

@Composable
expect fun HorseScreen(selectedLanguage: String, onLanguageChange: (String) -> Unit, onBack: () -> Unit)

@Composable
expect fun HorseLiveScreen(selectedLanguage: String, onLanguageChange: (String) -> Unit, onBack: () -> Unit)

@Composable
fun SoccerScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToScores: () -> Unit,
    onNavigateToOdds: () -> Unit
) {
    CommonBackHandler {
        onBack()
    }
    val isEng = selectedLanguage == "ENG"
    val colorRed = Color(0xFFD32F2F)
    val colorYellow = Color(0xFFFFEB3B)
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().background(colorRed).statusBarsPadding().padding(vertical = 4.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.align(Alignment.CenterStart).clickable { onBack() }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colorYellow, modifier = Modifier.size(24.dp))
                Text(if (isEng) "Back" else "返回", color = colorYellow, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text(if (isEng) "Soccer Info" else "足球资讯", color = colorYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isEng) "中文" else "ENG",
                color = colorYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).clickable { onLanguageChange(if (isEng) "中文" else "ENG") }
            )
        }
        
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SoccerMenuButton(if (isEng) "Soccer Scores" else "足球比分", Icons.Default.Score, onNavigateToScores)
            SoccerMenuButton(if (isEng) "Soccer Odds" else "足球赔率", Icons.Default.Timeline, onNavigateToOdds)
        }
    }
}

@Composable
expect fun SoccerScoresScreen(selectedLanguage: String, onLanguageChange: (String) -> Unit, onBack: () -> Unit)

@Composable
expect fun SoccerOddsScreen(selectedLanguage: String, onLanguageChange: (String) -> Unit, onBack: () -> Unit)

@Composable
expect fun LotteryScreen(selectedLanguage: String, onLanguageChange: (String) -> Unit, onBack: () -> Unit)

@Composable
fun SoccerMenuButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun NetmobileUpdateChecker(platform: Platform, selectedLanguage: String) {
    val database = Firebase.database
    val updateRef = database.reference("NetmobileUpdate")
    var updateData by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = updateRef.valueEvents.first()
            if (snapshot.exists) {
                val newVersionCode = snapshot.child("newVersionCode").value<Long?>() ?: 0L
                val newVersion = snapshot.child("version").value<String?>() ?: ""
                val apkUrl = snapshot.child("apkUrl").value<String?>() ?: ""
                val isForce = snapshot.child("isForce").value<Boolean?>() ?: true
                
                val shouldUpdate = if (newVersionCode > 0) {
                    newVersionCode > platform.versionCode
                } else {
                    newVersion.isNotEmpty() && newVersion != platform.appVersion
                }

                if (shouldUpdate) {
                    updateData = UpdateInfo(if (newVersion.isNotEmpty()) newVersion else "New Version", apkUrl, isForce)
                    showDialog = true
                }
            }
        } catch (_: Exception) {}
    }

    if (showDialog && updateData != null) {
        UpdateDialog(apkUrl = updateData!!.apkUrl, selectedLanguage = selectedLanguage, isForce = updateData!!.isForce, onDismiss = { showDialog = false })
    }
}

data class UpdateInfo(val version: String, val apkUrl: String, val isForce: Boolean)
