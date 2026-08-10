package com.xyz.netmobile

import androidx.compose.runtime.Composable

@Composable
expect fun CommonBackHandler(enabled: Boolean = true, onBack: () -> Unit)
