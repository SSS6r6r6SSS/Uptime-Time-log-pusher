package com.example

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LogEntry
import com.example.ui.LogViewModel
import com.example.ui.theme.ElegantBackground
import com.example.ui.theme.ElegantButtonGray
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantSurface
import com.example.ui.theme.ElegantTerminalBg
import com.example.ui.theme.ElegantTerminalHeader
import com.example.ui.theme.ElegantTextGray
import com.example.ui.theme.ElegantTextLight
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LightGreenText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    LogMonitorScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogMonitorScreen(
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val logs by viewModel.filteredLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val pollingInterval by viewModel.pollingInterval.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoScrollEnabled by viewModel.autoScrollEnabled.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val logUrl by viewModel.logUrl.collectAsState()
    val hideFromRecents by viewModel.hideFromRecents.collectAsState()

    // Dynamically exclude the app from recent tasks list when enabled
    LaunchedEffect(hideFromRecents) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.appTasks.forEach { task ->
                    task.setExcludeFromRecents(hideFromRecents)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting excludeFromRecents to $hideFromRecents", e)
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Logs, 1: History & Stats, 2: Config

    // Handle system notification permission on Android 13+ (API 33+)
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "通知权限已启用", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "将无法在收到新日志时向您发送通知", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        // Automatically fetch initial logs on start
        viewModel.refreshLogsManually()
        
        // Request notifications permission if missing and on Android 13+
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Auto-scroll list state
    val listState = rememberLazyListState()
    LaunchedEffect(logs, autoScrollEnabled) {
        if (autoScrollEnabled && logs.isNotEmpty() && selectedTab == 0) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantBackground)
    ) {
        // Main Scrollable Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header / App Bar (Material 3 style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Uptime logpush",
                        color = ElegantTextLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alphaPulse by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) SuccessGreen else ElegantTextGray)
                                .alpha(alphaPulse)
                        )
                        Text(
                            text = if (isServiceActive) "LIVE STREAM ACTIVE" else "LIVE STREAM INACTIVE",
                            color = if (isServiceActive) LightGreenText else ElegantTextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Header Settings Button: Toggles settings tab directly
                IconButton(
                    onClick = { 
                        selectedTab = if (selectedTab == 2) 0 else 2
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElegantButtonGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quick Config",
                        tint = ElegantPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Status Dashboard Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Events Received",
                            color = ElegantTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = String.format("%,d", logs.size),
                            color = ElegantPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Notifications",
                            color = ElegantTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = if (notificationsEnabled) "Enabled" else "Disabled",
                            color = if (notificationsEnabled) ElegantPrimary else ElegantTextGray,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. Permission Banner & Network Error Alert
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "未授予系统通知权限",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFFB4AB)
                            )
                            Text(
                                text = "当产生新日志时，系统将无法弹窗通知",
                                fontSize = 11.sp,
                                color = Color(0xFFFFB4AB).copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("授权", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFFB4AB),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearErrorMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Error",
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 4. Tab Views
            when (selectedTab) {
                0 -> {
                    // Logs Terminal View
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp))
                            .background(ElegantTerminalBg)
                    ) {
                        // Terminal Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ElegantTerminalHeader)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = logUrl.replace("https://", "").replace("http://", ""),
                                color = ElegantPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                            }
                        }

                        // Search Bar Inside Console view
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { 
                                    Text(
                                        "grep keyword...", 
                                        fontSize = 11.sp, 
                                        color = ElegantTextGray, 
                                        fontFamily = FontFamily.Monospace
                                    ) 
                                },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = ElegantTextGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = ElegantTextGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElegantPrimary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                    focusedTextColor = ElegantTextLight,
                                    unfocusedTextColor = ElegantTextLight,
                                    focusedContainerColor = ElegantSurface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = ElegantSurface.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("search_input")
                            )

                            IconButton(
                                onClick = { viewModel.refreshLogsManually() },
                                modifier = Modifier
                                    .background(ElegantSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .size(44.dp)
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ElegantPrimary,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = ElegantTextLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Terminal logs scrolling area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (logs.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = "Console Empty",
                                        tint = ElegantTextGray,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .alpha(0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No matching entries" else "Console is empty",
                                        color = ElegantTextLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Try clearing your grep search filter" else "Waiting for incoming webhook requests...",
                                        color = ElegantTextGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(logs, key = { it.id }) { logItem ->
                                        LogLineItem(
                                            log = logItem,
                                            onLineClick = {
                                                clipboardManager.setText(AnnotatedString(logItem.content))
                                                Toast.makeText(context, "Copied log line", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Auto Scroll Floating indicator
                            if (logs.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.toggleAutoScroll() },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .background(
                                            if (autoScrollEnabled) ElegantPrimary else ElegantSurface,
                                            CircleShape
                                        )
                                        .size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Auto Scroll",
                                        tint = if (autoScrollEnabled) ElegantOnPrimary else ElegantTextLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Terminal bottom controls card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ElegantTerminalHeader)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.setNotificationsEnabled(!notificationsEnabled) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (notificationsEnabled) ElegantPrimary else ElegantButtonGray
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = "Toggle Notifications Alert",
                                        tint = if (notificationsEnabled) ElegantOnPrimary else ElegantPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (notificationsEnabled) "Alerts Active" else "Alerts Off",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (notificationsEnabled) ElegantOnPrimary else ElegantTextLight
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.clearLogs()
                                    Toast.makeText(context, "Local database cleared", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(ElegantButtonGray, RoundedCornerShape(16.dp))
                                    .size(44.dp)
                                    .testTag("clear_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear all database logs",
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // History & Stats View
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Pipeline Status History",
                                color = ElegantTextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatDetailItem(label = "Primary Endpoint", value = logUrl)
                                StatDetailItem(label = "Local Data Store", value = "SQLite / Room DB")
                                StatDetailItem(label = "Total Logs Cached", value = "${logs.size} lines")
                                StatDetailItem(
                                    label = "Background Daemon State", 
                                    value = if (isServiceActive) "ACTIVE (DATA_SYNC)" else "PAUSED"
                                )
                                StatDetailItem(
                                    label = "Last Connection Sync", 
                                    value = if (lastUpdated > 0) {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated))
                                    } else {
                                        "No connection established yet"
                                    }
                                )
                            }

                            // Simple helper instruction box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ElegantTerminalBg, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info icon",
                                        tint = ElegantPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "How to generate webhooks:",
                                            color = ElegantTextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Send standard HTTP POST or GET requests to the registered webhook endpoint. The polling daemon automatically captures and parses incoming logs in real time.",
                                            color = ElegantTextGray,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Config settings View
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "System Configurations",
                                color = ElegantTextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Log Source URL Configuration
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "日志拉取源 URL",
                                    color = ElegantTextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "设置日志监控拉取的数据源 Webhook 地址 (支持标准 txt 或 Uptime Kuma Webhook)",
                                    color = ElegantTextGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                var urlInputText by remember(logUrl) { mutableStateOf(logUrl) }
                                val isEditing = urlInputText != logUrl

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = urlInputText,
                                        onValueChange = { urlInputText = it },
                                        textStyle = TextStyle(
                                            color = ElegantPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElegantPrimary,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedContainerColor = ElegantTerminalBg,
                                            unfocusedContainerColor = ElegantTerminalBg,
                                            disabledContainerColor = ElegantTerminalBg
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("log_url_input"),
                                        trailingIcon = {
                                            if (urlInputText.isNotEmpty()) {
                                                IconButton(onClick = { urlInputText = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear text",
                                                        tint = ElegantTextGray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    if (isEditing) {
                                        Button(
                                            onClick = {
                                                viewModel.setLogUrl(urlInputText)
                                                Toast.makeText(context, "URL 已保存并清空缓存", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Text(
                                                text = "保存",
                                                color = ElegantOnPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                urlInputText = com.example.service.LogPollingService.DEFAULT_LOG_URL
                                                viewModel.setLogUrl(com.example.service.LogPollingService.DEFAULT_LOG_URL)
                                                Toast.makeText(context, "已重置为默认地址", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElegantButtonGray),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Text(
                                                text = "重置",
                                                color = ElegantTextLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Background daemon service toggle
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "后台实时监控",
                                            color = ElegantTextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "启用前台数据同步服务(FOREGROUND_SERVICE)，退到后台依然能实时拉取并通知",
                                            color = ElegantTextGray,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Switch(
                                        checked = isServiceActive,
                                        onCheckedChange = { viewModel.toggleService() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ElegantOnPrimary,
                                            checkedTrackColor = ElegantPrimary,
                                            uncheckedThumbColor = ElegantTextGray,
                                            uncheckedTrackColor = ElegantButtonGray
                                        ),
                                        modifier = Modifier.scale(0.85f).testTag("service_toggle")
                                    )
                                }
                            }

                            // Polling interval selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "实时拉取轮询间隔",
                                    color = ElegantTextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "设置后台轮询接口的更新速度，高频率更实时但略微消耗电量",
                                    color = ElegantTextGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                var isDropdownOpen by remember { mutableStateOf(false) }
                                val activeLabel = when (pollingInterval) {
                                    3000L -> "极速轮询 (3 秒)"
                                    5000L -> "高频轮询 (5 秒)"
                                    10000L -> "标准平衡 (10 秒)"
                                    30000L -> "均衡省电 (30 秒)"
                                    60000L -> "超级省电 (1 分钟)"
                                    else -> "自定义 (${pollingInterval / 1000} 秒)"
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ElegantTerminalBg,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isDropdownOpen = true }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(ElegantTerminalBg),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = activeLabel,
                                                color = ElegantPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "选择预设 ▾",
                                                color = ElegantTextGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isDropdownOpen,
                                        onDismissRequest = { isDropdownOpen = false },
                                        modifier = Modifier.background(ElegantSurface)
                                    ) {
                                        listOf(3000L, 5000L, 10000L, 30000L, 60000L).forEach { ms ->
                                            val label = when (ms) {
                                                3000L -> "极速轮询 (3 秒)"
                                                5000L -> "高频轮询 (5 秒)"
                                                10000L -> "标准平衡 (10 秒)"
                                                30000L -> "均衡省电 (30 秒)"
                                                60000L -> "超级省电 (1 分钟)"
                                                else -> "${ms / 1000}s"
                                            }
                                            DropdownMenuItem(
                                                text = { 
                                                    Text(
                                                        text = label, 
                                                        fontFamily = FontFamily.Monospace, 
                                                        fontSize = 12.sp,
                                                        color = ElegantTextLight
                                                    ) 
                                                },
                                                onClick = {
                                                    viewModel.setPollingInterval(ms)
                                                    isDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Custom custom interval text input field
                                var customSecondsText by remember(pollingInterval) { 
                                    mutableStateOf((pollingInterval / 1000).toString()) 
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customSecondsText,
                                        onValueChange = { newValue ->
                                            if (newValue.all { it.isDigit() }) {
                                                customSecondsText = newValue
                                            }
                                        },
                                        label = { Text("自定义时间 (秒)", fontSize = 11.sp, color = ElegantTextGray) },
                                        textStyle = TextStyle(
                                            color = ElegantPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElegantPrimary,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                            focusedContainerColor = ElegantTerminalBg,
                                            unfocusedContainerColor = ElegantTerminalBg,
                                            focusedTextColor = ElegantTextLight,
                                            unfocusedTextColor = ElegantTextLight
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .testTag("custom_interval_input")
                                    )

                                    val isCustomSaveEnabled = customSecondsText.toLongOrNull()?.let { it * 1000L != pollingInterval && it > 0 } ?: false

                                    Button(
                                        onClick = {
                                            val sec = customSecondsText.toLongOrNull()
                                            if (sec != null && sec > 0) {
                                                viewModel.setPollingInterval(sec * 1000L)
                                                Toast.makeText(context, "轮询间隔已自定义为 $sec 秒", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "请输入有效的秒数", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = isCustomSaveEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElegantPrimary,
                                            disabledContainerColor = ElegantButtonGray
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text(
                                            text = "应用",
                                            color = if (isCustomSaveEnabled) ElegantOnPrimary else ElegantTextGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Notification alert switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "有新日志行时推送系统通知",
                                        color = ElegantTextLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "在设备通知栏接收实时的 webhook 更新日志消息",
                                        color = ElegantTextGray,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElegantOnPrimary,
                                        checkedTrackColor = ElegantPrimary,
                                        uncheckedThumbColor = ElegantTextGray,
                                        uncheckedTrackColor = ElegantButtonGray
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            // Hide from recent tasks switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "在最近任务列表中隐藏",
                                        color = ElegantTextLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "开启后，本应用将不会显示在系统的最近任务列表中，更安全隐私",
                                        color = ElegantTextGray,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                                Switch(
                                    checked = hideFromRecents,
                                    onCheckedChange = { viewModel.setHideFromRecents(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElegantOnPrimary,
                                        checkedTrackColor = ElegantPrimary,
                                        uncheckedThumbColor = ElegantTextGray,
                                        uncheckedTrackColor = ElegantButtonGray
                                    ),
                                    modifier = Modifier.scale(0.85f).testTag("hide_from_recents_toggle")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Bottom Navigation Bar (Matching Elegant Dark style precisely)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(ElegantBackground)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logs tab
            BottomNavItem(
                icon = Icons.Default.ListAlt,
                label = "Logs",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )

            // History tab
            BottomNavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )

            // Config tab
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Config",
                isSelected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            )
        }
    }
}

@Composable
fun StatDetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .background(ElegantTerminalBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ElegantTextGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = ElegantTextLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) Color(0xFF394458) else Color.Transparent
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFD2E4FF) else ElegantTextGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color(0xFFD2E4FF) else ElegantTextGray
        )
    }
}

// Helper class to parse and extract Uptime Kuma payload fields
data class KumaPayload(
    val monitorName: String? = null,
    val status: Int? = null, // 1 = UP, 0 = DOWN, 2 = PENDING
    val msg: String? = null,
    val ping: Int? = null,
    val time: String? = null,
    val url: String? = null,
    val type: String? = null,
    val hostname: String? = null,
    val port: String? = null,
    val isKuma: Boolean = false
) {
    companion object {
        fun parse(content: String): KumaPayload {
            try {
                val json = org.json.JSONObject(content)
                // Uptime Kuma payloads have "heartbeat" or "monitor" or "msg"
                if (json.has("heartbeat") || json.has("monitor") || json.has("msg")) {
                    val monitorObj = if (json.has("monitor")) json.getJSONObject("monitor") else null
                    val heartbeatObj = if (json.has("heartbeat")) json.getJSONObject("heartbeat") else null
                    
                    val monitorName = monitorObj?.optString("name") ?: json.optString("msg")?.substringBefore("]")?.trim('[', ' ')
                    val status = heartbeatObj?.optInt("status", -1) ?: (
                        if (json.optString("msg").contains("Up", ignoreCase = true) || json.optString("msg").contains("✅")) 1 
                        else if (json.optString("msg").contains("Down", ignoreCase = true) || json.optString("msg").contains("🔴") || json.optString("msg").contains("❌")) 0 
                        else -1
                    )
                    val msg = heartbeatObj?.optString("msg") ?: json.optString("msg")
                    val ping = heartbeatObj?.optInt("ping", -1) ?: -1
                    val time = heartbeatObj?.optString("time")
                    val url = monitorObj?.optString("url") ?: json.optString("url")
                    val type = monitorObj?.optString("type") ?: json.optString("type")
                    val hostname = monitorObj?.optString("hostname") ?: json.optString("hostname")
                    val portVal = monitorObj?.optString("port") ?: json.optString("port")
                    
                    return KumaPayload(
                        monitorName = if (monitorName?.isNotEmpty() == true) monitorName else null,
                        status = if (status != -1) status else null,
                        msg = if (msg?.isNotEmpty() == true) msg else null,
                        ping = if (ping != -1) ping else null,
                        time = if (time?.isNotEmpty() == true) time else null,
                        url = if (url?.isNotEmpty() == true) url else null,
                        type = if (type?.isNotEmpty() == true) type else null,
                        hostname = if (hostname?.isNotEmpty() == true) hostname else null,
                        port = if (portVal?.isNotEmpty() == true) portVal else null,
                        isKuma = true
                    )
                }
            } catch (e: Exception) {
                // Ignore, treat as standard text log
            }
            return KumaPayload(isKuma = false)
        }
    }
}

private fun getPrettyJson(rawJson: String): String {
    return try {
        val json = org.json.JSONObject(rawJson)
        json.toString(2)
    } catch (e: Exception) {
        try {
            val jsonArray = org.json.JSONArray(rawJson)
            jsonArray.toString(2)
        } catch (ex: Exception) {
            rawJson
        }
    }
}

@Composable
fun LogLineItem(
    log: LogEntry,
    onLineClick: () -> Unit
) {
    val kumaPayload = remember(log.content) { KumaPayload.parse(log.content) }
    var isExpanded by remember { mutableStateOf(false) }

    if (kumaPayload.isKuma) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = ElegantSurface.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp, 
                when (kumaPayload.status) {
                    1 -> SuccessGreen.copy(alpha = 0.2f)
                    0 -> ErrorCrimson.copy(alpha = 0.2f)
                    else -> ElegantPrimary.copy(alpha = 0.15f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Colored Status Indicator Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (kumaPayload.status) {
                                        1 -> SuccessGreen.copy(alpha = 0.15f)
                                        0 -> ErrorCrimson.copy(alpha = 0.15f)
                                        else -> WarningAmber.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (kumaPayload.status) {
                                    1 -> "🟢 UP"
                                    0 -> "🔴 DOWN"
                                    else -> "🟡 PENDING"
                                },
                                color = when (kumaPayload.status) {
                                    1 -> LightGreenText
                                    0 -> ErrorCrimson
                                    else -> WarningAmber
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Monitor Name
                        Text(
                            text = kumaPayload.monitorName ?: "Uptime Kuma Event",
                            color = ElegantTextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Optional Ping / Response time
                    if (kumaPayload.ping != null && kumaPayload.ping > 0) {
                        Text(
                            text = "${kumaPayload.ping} ms",
                            color = ElegantPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Event message
                Text(
                    text = kumaPayload.msg ?: log.content,
                    color = ElegantTextLight.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // Render secondary info (URL, Hostname/Port, or Type) if present
                if (kumaPayload.url != null || kumaPayload.hostname != null || kumaPayload.port != null || kumaPayload.type != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        if (kumaPayload.type != null) {
                            Text(
                                text = "type: ${kumaPayload.type}",
                                color = ElegantTextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (kumaPayload.url != null) {
                            Text(
                                text = "url: ${kumaPayload.url}",
                                color = ElegantPrimary.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (kumaPayload.hostname != null || kumaPayload.port != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (kumaPayload.hostname != null) {
                                    Text(
                                        text = "hostname: ${kumaPayload.hostname}",
                                        color = ElegantPrimary.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (kumaPayload.port != null) {
                                    Text(
                                        text = "port: ${kumaPayload.port}",
                                        color = ElegantPrimary.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Raw JSON payload block
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(ElegantTerminalBg, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAW WEBHOOK JSON",
                                color = ElegantPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Click to Copy",
                                color = ElegantTextGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable { onLineClick() }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = getPrettyJson(log.content),
                            color = ElegantTextLight,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable { onLineClick() }
                .padding(vertical = 4.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Line number tag e.g. [0032] in elegant purple color
            Text(
                text = String.format("[%04d]", log.lineIndex),
                color = ElegantPrimary.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(46.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            // Highlight-formatted actual log text
            Text(
                text = parseLogWithHighlighters(log.content),
                color = ElegantTextLight,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Custom color parsing on individual text words for stunning terminal display.
 * Highlight-renders errors, warning keywords, and query types.
 */
fun parseLogWithHighlighters(text: String): AnnotatedString {
    return buildAnnotatedString {
        val words = text.split(" ")
        words.forEachIndexed { index, word ->
            val cleanedWord = word.trim('[', ']', '(', ')', '{', '}', '"', '\'', ',', ';')
            val isUpper = cleanedWord.uppercase(Locale.getDefault())

            when {
                // Errors
                isUpper.contains("ERROR") || isUpper.contains("FAIL") || isUpper.contains("CRITICAL") || isUpper.contains("EXCEPTION") -> {
                    withStyle(style = SpanStyle(color = ErrorCrimson, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                // Warnings
                isUpper.contains("WARN") || isUpper.contains("WARNING") -> {
                    withStyle(style = SpanStyle(color = WarningAmber, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                // Success / OK
                isUpper.contains("SUCCESS") || isUpper == "OK" || isUpper == "TRUE" || isUpper == "200" -> {
                    withStyle(style = SpanStyle(color = LightGreenText, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                // Network Methods
                isUpper == "GET" || isUpper == "POST" || isUpper == "PUT" || isUpper == "DELETE" || isUpper == "PATCH" -> {
                    withStyle(style = SpanStyle(color = ElegantPrimary, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                // Log Info levels
                isUpper == "INFO" || isUpper == "DEBUG" -> {
                    withStyle(style = SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Normal)) {
                        append(word)
                    }
                }
                // Default word
                else -> {
                    append(word)
                }
            }

            if (index < words.size - 1) {
                append(" ")
            }
        }
    }
}
