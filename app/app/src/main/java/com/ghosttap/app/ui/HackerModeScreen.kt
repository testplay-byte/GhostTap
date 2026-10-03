package com.ghosttap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.ghosttap.app.BleManager
import kotlinx.coroutines.delay
@Composable
fun TemperatureBubble(
    temperature: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        temperature < 40f -> HackerBlue
        temperature < 50f -> HackerGreen
        temperature < 60f -> Color(0xFFFFCC00) // Yellow
        temperature < 70f -> Color(0xFFFF9500) // Orange
        else -> HackerRed
    }

    val label = when {
        temperature < 40f -> "LOW"
        temperature < 50f -> "MED_LOW"
        temperature < 60f -> "MEDIUM"
        temperature < 70f -> "MED_HIGH"
        else -> "HIGH"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Glow Effect
        Surface(
            modifier = Modifier.size(60.dp),
            color = color.copy(alpha = glowAlpha * 0.2f),
            shape = CircleShape
        ) {}

        // Outer Ring
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(60.dp),
            color = color.copy(alpha = 0.1f),
            strokeWidth = 4.dp
        )
        
        // Active Ring Segment (representing value relative to 80C)
        CircularProgressIndicator(
            progress = { (temperature / 80f).coerceIn(0f, 1f) },
            modifier = Modifier.size(60.dp),
            color = color,
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round
        )

        Surface(
            modifier = Modifier.size(52.dp),
            color = HackerDarkGray,
            shape = CircleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    "${temperature.roundToInt()}°",
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    lineHeight = 16.sp
                )
                Text(
                    label,
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
 }
 
 @Composable
 fun FileDetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
     Row(verticalAlignment = Alignment.CenterVertically) {
         Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
         Spacer(modifier = Modifier.width(12.dp))
         Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
         Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
     }
 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackerModeScreen(
    bleManager: BleManager,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Scanner, 2: Files, 3: Settings
    var isAttackRunning by remember { mutableStateOf(false) }

    // Dashboard state hoisted for navigation from Scanner
    var dashboardStep by remember { mutableIntStateOf(0) }
    var selectedAttackType by remember { mutableStateOf<String?>(null) }
    var selectedNetworks by remember { mutableStateOf<List<BleManager.ScanResultItem>>(emptyList()) }
    var isAllMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bleManager.sendCommand("MODE:ADVANCED")
    }

    DisposableEffect(Unit) {
        onDispose {
            bleManager.sendCommand("MODE:NORMAL")
        }
    }

    Scaffold(
        containerColor = HackerBlack,
        topBar = {
            if (!isAttackRunning) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "THE OTHER SIDE",
                            fontWeight = FontWeight.ExtraBold,
                            color = HackerRed,
                            letterSpacing = 2.sp
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = HackerBlack
                    )
                )
            }
        },
        bottomBar = {
            if (!isAttackRunning) {
                Surface(
                    color = HackerDarkGray,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val tabs = listOf(
                            Triple(0, Icons.Default.Dashboard, "DASHBOARD"),
                            Triple(1, Icons.Default.Wifi, "SCANNER"),
                            Triple(2, Icons.Default.Folder, "FILES"),
                            Triple(3, Icons.Default.Settings, "SETTINGS")
                        )
                        
                        tabs.forEach { (index, icon, label) ->
                            val isSelected = selectedTab == index
                            val color = when(index) {
                                0 -> HackerRed
                                1 -> HackerBlue
                                2 -> HackerGreen
                                else -> Color.White
                            }
                            
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                icon = { 
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Surface(
                                                modifier = Modifier.size(42.dp),
                                                color = color.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {}
                                        }
                                        Icon(
                                            icon, 
                                            contentDescription = label,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isSelected) color else Color.Gray.copy(alpha = 0.6f)
                                        ) 
                                    }
                                },
                                label = { 
                                    Text(
                                        label, 
                                        fontSize = 10.sp, 
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = color,
                                    selectedTextColor = color,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                                    unselectedTextColor = Color.Gray.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Content with padding
            Box(modifier = Modifier.padding(if (isAttackRunning) PaddingValues(0.dp) else padding)) {
                when (selectedTab) {
                    0 -> HackerDashboard(
                        bleManager, 
                        onAttackStateChange = { isAttackRunning = it },
                        currentStep = dashboardStep,
                        onStepChange = { dashboardStep = it },
                        selectedAttackType = selectedAttackType,
                        onAttackTypeChange = { selectedAttackType = it },
                        selectedNetworks = selectedNetworks,
                        onNetworksChange = { selectedNetworks = it },
                        isAllMode = isAllMode,
                        onAllModeChange = { isAllMode = it }
                    )
                    1 -> {} // Handled below to allow popup to span bottom
                    2 -> HackerFiles(bleManager)
                    3 -> HackerSettings(onBack)
                }
            }
            
            // Scanner Tab - Special handling for popup
            if (selectedTab == 1) {
                HackerScanner(
                    bleManager, 
                    padding,
                    onConfigureTarget = { network ->
                        selectedNetworks = listOf(network)
                        isAllMode = false
                        dashboardStep = 0 // Go to select operation first
                        selectedTab = 0
                    }
                )
            }
        }
    }
}

@Composable
fun HackerSettings(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "SETTINGS",
            color = Color.Gray,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
            shape = MaterialTheme.shapes.extraLarge,
            onClick = onBack
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = HackerRed, modifier = Modifier.size(32.dp))
                Column {
                    Text("EXIT HACKER MODE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Return to main control screen", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
