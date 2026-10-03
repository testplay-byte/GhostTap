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

// Material 3 Expressive - Big & Bold Theme
val HackerRed = Color(0xFFFF3B30)
val HackerGreen = Color(0xFF34C759)
val HackerBlue = Color(0xFF007AFF)
val HackerBlack = Color(0xFF000000)
val HackerDarkGray = Color(0xFF1C1C1E)

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
@Composable
fun HackerDashboard(
    bleManager: BleManager,
    onAttackStateChange: (Boolean) -> Unit,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    selectedAttackType: String?,
    onAttackTypeChange: (String?) -> Unit,
    selectedNetworks: List<BleManager.ScanResultItem>,
    onNetworksChange: (List<BleManager.ScanResultItem>) -> Unit,
    isAllMode: Boolean,
    onAllModeChange: (Boolean) -> Unit
) {
    var attackDuration by remember { mutableFloatStateOf(60f) } // Default 60 seconds
    var selectedMethod by remember { mutableIntStateOf(1) } // Default to Broadcast (1) for most attacks
    var isSearching by remember { mutableStateOf(false) }
    
    // Handshake specific data
    val handshakeStatus by bleManager.handshakeStatus.collectAsState()
    
    // Countdown timer state
    var timeLeft by remember { mutableIntStateOf(60) }
    val isInfinite = attackDuration >= 300f

    val statusMessage by bleManager.statusMessage.collectAsState()
    val scanResults by bleManager.scanResults.collectAsState()
    val isReady by bleManager.isReady.collectAsState()
    val temperature by bleManager.temperature.collectAsState()
    
    val selectedNetwork = selectedNetworks.firstOrNull()
    
    // Notify parent about attack state
    LaunchedEffect(currentStep) {
        onAttackStateChange(currentStep == 3 || currentStep == 10)
        if (currentStep == 3) {
            timeLeft = if (isInfinite) -1 else attackDuration.roundToInt()
            if (selectedAttackType == "HANDSHAKE") {
                bleManager.clearHandshakeStatus()
            }
        }
    }

    // Countdown Timer Logic
    LaunchedEffect(currentStep, timeLeft) {
        if (currentStep == 3 && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
            if (timeLeft == 0) {
                bleManager.sendCommand("STOP")
            }
        }
    }
    
    // Auto-close attack page when finished
    LaunchedEffect(statusMessage) {
        if (statusMessage == "ATTACK_COMPLETED" || 
            statusMessage == "ATTACK_SUCCESS" || 
            statusMessage == "ATTACK_TIMEOUT") {
            if (currentStep == 3) {
                delay(1500) // Show result for 1.5 seconds
                if (selectedAttackType == "HANDSHAKE") {
                    onStepChange(4) // Move to results screen for handshake
                } else {
                    onStepChange(0)
                    onNetworksChange(emptyList())
                    onAllModeChange(false)
                }
            }
        }
    }

    // Reset isSearching when results change or command finished
    LaunchedEffect(scanResults, isReady) {
        if (scanResults.isNotEmpty() || isReady) isSearching = false
    }
    
    // Reset method when attack type changes
    LaunchedEffect(selectedAttackType) {
        selectedMethod = when(selectedAttackType) {
            "DOS" -> 1 // Broadcast
            "HANDSHAKE" -> 1 // Broadcast
            else -> 0
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attack Flow Container
        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                0 -> { // Step 0: Select Attack Type
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "SELECT OPERATION",
                            color = Color.Gray,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                        AttackTypeCard(
                            "DEAUTH", 
                            "Disconnect users from a network",
                            Icons.Default.WifiOff, 
                            HackerRed
                        ) {
                            onAttackTypeChange("DOS")
                            if (selectedNetworks.isNotEmpty() || isAllMode) {
                                onStepChange(2) // Skip scan if network already selected
                            } else {
                                onStepChange(1)
                                isSearching = true
                                bleManager.clearScanResults()
                                bleManager.sendCommand("SCAN")
                            }
                        }
                        AttackTypeCard(
                            "BEACON SWARM", 
                            "Create multiple fake networks",
                            Icons.Default.Wifi, 
                            HackerBlue
                        ) {
                            onAttackTypeChange("BEACON")
                            onStepChange(2) // Skip network selection for beacon
                        }
                        AttackTypeCard(
                            "HANDSHAKE", 
                            "Intercept WPA/WPA2 handshakes",
                            Icons.Default.Security, 
                            HackerGreen
                        ) {
                            onAttackTypeChange("HANDSHAKE")
                            if (selectedNetworks.isNotEmpty() || isAllMode) {
                                onStepChange(2) // Skip scan if network already selected
                            } else {
                                onStepChange(1)
                                isSearching = true
                                bleManager.clearScanResults()
                                bleManager.sendCommand("SCAN")
                            }
                        }
                        
                        // New Advanced Handshake Mode Button
                        AttackTypeCard(
                            "ADV HANDSHAKE", 
                            "Auto-Target Active Capture Mode",
                            Icons.Default.Autorenew, 
                            Color(0xFFFFD700) // Gold color
                        ) {
                            onStepChange(10) // Special step for Advanced Handshake
                        }
                    }
                }
                1 -> { // Step 1: Select Target Network
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onStepChange(0) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Text(
                                "SELECT TARGET",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // "All" Mode Button
                            if (selectedAttackType != "HANDSHAKE") {
                                Surface(
                                    modifier = Modifier.clickable { 
                                        onAllModeChange(true)
                                        onNetworksChange(emptyList())
                                        onStepChange(2) // Navigate to configuration immediately
                                    },
                                    color = if (isAllMode) HackerRed.copy(alpha = 0.2f) else HackerDarkGray,
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isAllMode) BorderStroke(1.dp, HackerRed) else null
                                ) {
                                    Text(
                                        "ALL",
                                        color = if (isAllMode) HackerRed else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        val isBleScanning by bleManager.isScanning.collectAsState()
                        val isActuallySearching = isBleScanning || isSearching
                        
                        if (scanResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isActuallySearching) {
                                        var progress by remember { mutableFloatStateOf(0f) }
                                        var statusText by remember { mutableStateOf("Initializing search...") }

                                        LaunchedEffect(isActuallySearching) {
                                            if (isActuallySearching) {
                                                val startTime = System.currentTimeMillis()
                                                val baseDuration = 10000f // 10 seconds for initial sweep
                                                
                                                while (isActuallySearching && scanResults.isEmpty()) {
                                                    val elapsed = System.currentTimeMillis() - startTime
                                                    
                                                    // Dynamic status text based on progress
                                                    statusText = when {
                                                        elapsed < 3000 -> "Broadcasting SCAN..."
                                                        elapsed < 8000 -> "Scanning frequencies..."
                                                        elapsed < 15000 -> "Waiting for ESP32..."
                                                        else -> "Still waiting for data..."
                                                    }

                                                    if (elapsed < baseDuration) {
                                                        // Linear-ish progress up to 90%
                                                        progress = (elapsed / baseDuration) * 0.9f
                                                    } else {
                                                        // Asymptotically approach 99% if still searching
                                                        val overflow = (elapsed - baseDuration) / 20000f
                                                        progress = 0.9f + (0.09f * (1f - 1f / (1f + overflow)))
                                                    }
                                                    delay(16) // ~60fps smooth animation
                                                    
                                                    // Hard safety timeout: if 45 seconds pass and no results, stop animation
                                                    if (elapsed > 45000) break
                                                }
                                                
                                                if (scanResults.isNotEmpty()) {
                                                    statusText = "Data received!"
                                                    // Quick jump to 100% when data arrives
                                                    val endStart = progress
                                                    val endDuration = 300f
                                                    val endStartTime = System.currentTimeMillis()
                                                    while (System.currentTimeMillis() - endStartTime < endDuration) {
                                                        val t = (System.currentTimeMillis() - endStartTime) / endDuration
                                                        progress = endStart + (1f - endStart) * t
                                                        delay(16)
                                                    }
                                                    progress = 1f
                                                    delay(100)
                                                }
                                            } else {
                                                progress = 0f
                                                statusText = ""
                                            }
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            // Glow effect
                                            Surface(
                                                modifier = Modifier.size(120.dp),
                                                color = HackerBlue.copy(alpha = 0.05f),
                                                shape = CircleShape
                                            ) {}
                                            
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                color = HackerBlue,
                                                modifier = Modifier.size(90.dp),
                                                strokeWidth = 4.dp,
                                                trackColor = Color.White.copy(alpha = 0.05f)
                                            )
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "${(progress * 100).toInt()}%",
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    "SCAN",
                                                    color = HackerBlue,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(32.dp))
                                        
                                        Text(
                                            statusText,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = 2.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text("PLEASE WAIT FOR RESULTS", color = Color.Gray, fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.WifiFind, null, tint = HackerDarkGray, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No networks found", color = Color.Gray)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { 
                                                isSearching = true
                                                bleManager.sendCommand("SCAN") 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HackerBlue),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("START DISCOVERY", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(scanResults) { network ->
                                    val isSelected = selectedNetworks.any { it.index == network.index }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().combinedClickable(
                                            onClick = {
                                                if (selectedAttackType == "HANDSHAKE") {
                                                    onNetworksChange(listOf(network))
                                                    onAllModeChange(false)
                                                    onStepChange(2)
                                                } else {
                                                    if (selectedNetworks.isEmpty()) {
                                                        onNetworksChange(listOf(network))
                                                        onAllModeChange(false)
                                                        onStepChange(2)
                                                    } else {
                                                        val newList = if (isSelected) {
                                                            selectedNetworks.filter { it.index != network.index }
                                                        } else {
                                                            selectedNetworks + network
                                                        }
                                                        onNetworksChange(newList)
                                                        onAllModeChange(false)
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (selectedAttackType != "HANDSHAKE") {
                                                    val newList = if (isSelected) {
                                                        selectedNetworks.filter { it.index != network.index }
                                                    } else {
                                                        selectedNetworks + network
                                                    }
                                                    onNetworksChange(newList)
                                                    onAllModeChange(false)
                                                }
                                            }
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) HackerBlue.copy(alpha = 0.2f) else HackerDarkGray
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (network.isSecure) Icons.Default.Lock else Icons.Default.LockOpen, 
                                                null, 
                                                tint = if (network.isSecure) HackerRed else HackerGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(network.ssid, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(network.bssid, color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(Icons.Default.CheckCircle, null, tint = HackerBlue)
                                            }
                                        }
                                    }
                                }
                                
                                // Show "NEXT" button if multiple are selected
                                if (selectedNetworks.size > 1) {
                                    item {
                                        Button(
                                            onClick = { onStepChange(2) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = HackerGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("NEXT (${selectedNetworks.size} SELECTED)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                // Add Scan button at the bottom of the list
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { 
                                            isSearching = true
                                            bleManager.sendCommand("SCAN") 
                                        },
                                        enabled = isReady && !isActuallySearching,
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HackerBlue,
                                            disabledContainerColor = HackerDarkGray
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isActuallySearching) {
                                            var progress by remember { mutableFloatStateOf(0f) }
                                            LaunchedEffect(isActuallySearching) {
                                                if (isActuallySearching) {
                                                    val startTime = System.currentTimeMillis()
                                                    val baseDuration = 12000f // 12 seconds for first 90%
                                                    while (isActuallySearching) {
                                                        val elapsed = System.currentTimeMillis() - startTime
                                                        if (elapsed < baseDuration) {
                                                            progress = (elapsed / baseDuration) * 0.9f
                                                        } else {
                                                            // Slow crawl from 90% to 99%
                                                            val extraElapsed = elapsed - baseDuration
                                                            val extraProgress = (extraElapsed / 20000f).coerceIn(0f, 0.09f)
                                                            progress = 0.9f + extraProgress
                                                        }
                                                        delay(50)
                                                    }
                                                    progress = 1f
                                                    delay(200)
                                                } else {
                                                    progress = 0f
                                                }
                                            }
                                            
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                color = Color.White,
                                                trackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("${(progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.Radar, null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("SEARCH AGAIN", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
                2 -> { // Step 2: Configure & Start
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onStepChange(if (selectedAttackType == "BEACON") 0 else 1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Text(
                                "CONFIGURE ATTACK",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when(selectedAttackType) {
                                            "DOS" -> Icons.Default.WifiOff
                                            "HANDSHAKE" -> Icons.Default.Security
                                            else -> Icons.Default.Wifi
                                        },
                                        null,
                                        tint = when(selectedAttackType) {
                                            "DOS" -> HackerRed
                                            "HANDSHAKE" -> HackerGreen
                                            else -> HackerBlue
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        when(selectedAttackType) {
                                            "DOS" -> "DEAUTH ATTACK"
                                            "HANDSHAKE" -> "HANDSHAKE"
                                            else -> "BEACON SWARM"
                                        }, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                if (isAllMode) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Text("TARGET: ALL NETWORKS", color = HackerRed, fontWeight = FontWeight.Bold)
                                    Text("Mode: Continuous Re-auth", color = Color.Gray, fontSize = 12.sp)
                                } else if (selectedNetworks.isNotEmpty()) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    if (selectedNetworks.size == 1) {
                                        Text("TARGET: ${selectedNetworks[0].ssid}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("BSSID: ${selectedNetworks[0].bssid}", color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    } else {
                                        Text("TARGET: ${selectedNetworks.size} NETWORKS", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(selectedNetworks.joinToString(", ") { it.ssid }, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else if (selectedAttackType == "BEACON") {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("MODE: SWARM", color = HackerBlue, fontWeight = FontWeight.Bold)
                                            Text("Status: Creating multiple fake SSIDs", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { onStepChange(5) }) {
                                            Icon(Icons.Default.Settings, null, tint = HackerBlue)
                                        }
                                    }
                                }
                            }
                        }

                        // Method Selection (New)
                        if (selectedAttackType == "DOS" || selectedAttackType == "HANDSHAKE") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ATTACK METHOD", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val methods = if (selectedAttackType == "DOS") {
                                        listOf("ROGUE AP", "BROADCAST", "COMBINED")
                                    } else {
                                        listOf("ROGUE AP", "BROADCAST", "PASSIVE")
                                    }
                                    
                                    methods.forEachIndexed { index, name ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedMethod = index },
                                            color = if (selectedMethod == index) HackerBlue.copy(alpha = 0.2f) else HackerDarkGray,
                                            shape = RoundedCornerShape(12.dp),
                                            border = if (selectedMethod == index) BorderStroke(1.dp, HackerBlue) else null
                                        ) {
                                            Text(
                                                name,
                                                color = if (selectedMethod == index) HackerBlue else Color.Gray,
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                textAlign = TextAlign.Center,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Duration Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ATTACK DURATION", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    if (attackDuration >= 300f) "INFINITE" else "${attackDuration.roundToInt()} SECONDS", 
                                    color = HackerBlue, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = attackDuration,
                                onValueChange = { attackDuration = it },
                                valueRange = 10f..300f,
                                steps = 29,
                                colors = SliderDefaults.colors(
                                    thumbColor = HackerBlue,
                                    activeTrackColor = HackerBlue,
                                    inactiveTrackColor = HackerDarkGray
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("10s", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text("300s+", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { 
                                // Send 0 for infinite, otherwise the duration in seconds
                                val duration = if (isInfinite) 0 else attackDuration.roundToInt()
                                
                                if (isAllMode) {
                                    bleManager.setAllMode(true)
                                    val cmd = when(selectedAttackType) {
                                        "DOS" -> "ATTACK:DOS:$selectedMethod:ALL:$duration"
                                        "HANDSHAKE" -> "ATTACK:HANDSHAKE:$selectedMethod:ALL:$duration"
                                        else -> "ATTACK:BEACON:0:-1:$duration"
                                    }
                                    bleManager.sendCommand(cmd)
                                } else {
                                    bleManager.setTargetNetworks(selectedNetworks)
                                    val indices = selectedNetworks.joinToString(",") { it.index.toString() }
                                    val cmd = when(selectedAttackType) {
                                        "DOS" -> "ATTACK:DOS:$selectedMethod:$indices:$duration"
                                        "HANDSHAKE" -> "ATTACK:HANDSHAKE:$selectedMethod:$indices:$duration"
                                        else -> "ATTACK:BEACON:0:-1:$duration"
                                    }
                                    bleManager.sendCommand(cmd)
                                }
                                onStepChange(3) 
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HackerRed),
                            shape = RoundedCornerShape(16.dp),
                            enabled = isReady
                        ) {
                            if (!isReady) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("LAUNCH ATTACK", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
                3 -> { // Step 3: Running
                    if (selectedAttackType == "HANDSHAKE") {
                        HandshakeRunningScreen(
                            timeLeft = timeLeft,
                            statusMessage = statusMessage,
                            handshakeStatus = handshakeStatus,
                            targetSsid = selectedNetwork?.ssid,
                            isPassive = selectedMethod == 2,
                            temperature = temperature,
                            onManualDeauth = { bleManager.sendCommand("DEAUTH") },
                            onTerminate = {
                                bleManager.stopAttack()
                                onStepChange(4)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Temperature Bubble at the top right
                            TemperatureBubble(
                                temperature = temperature,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    color = HackerRed.copy(alpha = 0.1f),
                                    modifier = Modifier.size(120.dp),
                                    strokeWidth = 8.dp
                                )
                                CircularProgressIndicator(
                                    color = HackerRed, 
                                    modifier = Modifier.size(120.dp),
                                    strokeWidth = 8.dp
                                )
                                Icon(Icons.Default.Warning, null, tint = HackerRed, modifier = Modifier.size(48.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("ATTACK IN PROGRESS", color = HackerRed, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                            
                            Text(
                                if (isAllMode) "TARGET: ALL NETWORKS" 
                                else if (selectedAttackType == "BEACON") "TARGET: SWARM MODE"
                                else if (selectedNetworks.size > 1) "TARGET: ${selectedNetworks.size} NETWORKS"
                                else "TARGET: ${selectedNetwork?.ssid ?: "UNKNOWN"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            
                            if (timeLeft >= 0) {
                                Text(
                                    "TIMEOUT IN: ${timeLeft}s",
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    "MODE: INFINITE",
                                    color = HackerBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Surface(
                                color = HackerDarkGray,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    statusMessage.ifEmpty { "Waiting for response..." }, 
                                    color = Color.White, 
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(48.dp))
                            
                            Button(
                                onClick = { 
                                    bleManager.sendCommand("STOP")
                                    onStepChange(0) 
                                    onNetworksChange(emptyList())
                                    onAllModeChange(false)
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("TERMINATE OPERATION", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            4 -> { // Step 4: Handshake Results
                    HandshakeResultsScreen(
                        handshakeStatus = handshakeStatus,
                        network = selectedNetwork,
                        bleManager = bleManager,
                        onClose = {
                            onStepChange(0)
                            onNetworksChange(emptyList())
                            onAllModeChange(false)
                        }
                    )
                }
                5 -> { // Step 5: Beacon SSID Settings
                    BeaconSSIDSettings(
                        bleManager = bleManager,
                        onBack = { onStepChange(2) }
                    )
                }
                10 -> { // Step 10: Advanced Handshake Mode
                    AdvHandshakeScreen(
                        bleManager = bleManager,
                        onBack = { 
                            onStepChange(0) 
                            bleManager.sendCommand("STOP")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AttackTypeCard(name: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, color.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 0.5.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
        }
    }
}

@Composable
fun HandshakeRunningScreen(
    timeLeft: Int,
    statusMessage: String,
    handshakeStatus: BleManager.HandshakeStatus,
    targetSsid: String?,
    isPassive: Boolean = false,
    temperature: Float = 0f,
    onManualDeauth: () -> Unit = {},
    onTerminate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = HackerGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        "LIVE CAPTURE",
                        color = HackerGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    targetSsid ?: "UNKNOWN NETWORK",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            TemperatureBubble(temperature = temperature)
        }

        // Live Handshake Progress
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 1f },
                color = HackerGreen.copy(alpha = 0.05f),
                modifier = Modifier.size(130.dp),
                strokeWidth = 12.dp
            )
            CircularProgressIndicator(
                color = HackerGreen,
                modifier = Modifier.size(130.dp),
                strokeWidth = 12.dp
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${handshakeStatus.frames}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                Text("FRAMES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // EAPOL Frame Status
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HandshakeFrameBadge("M1", (handshakeStatus.eapolMask and 0x01) != 0, Modifier.weight(1f))
            HandshakeFrameBadge("M2", (handshakeStatus.eapolMask and 0x02) != 0, Modifier.weight(1f))
            HandshakeFrameBadge("M3", (handshakeStatus.eapolMask and 0x04) != 0, Modifier.weight(1f))
            HandshakeFrameBadge("M4", (handshakeStatus.eapolMask and 0x08) != 0, Modifier.weight(1f))
        }

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Devices, null, tint = HackerBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${handshakeStatus.clients}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("CLIENTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Storage, null, tint = HackerBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    val sizeKb = handshakeStatus.fileSize / 1024.0
                    Text("%.1f KB".format(sizeKb), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("FILE SIZE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Status Message
        Surface(
            color = HackerDarkGray,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            val displayMessage = when {
                statusMessage.contains("ATTACK_TIMEOUT", ignoreCase = true) -> "CAPTURE TIMED OUT"
                statusMessage.contains("ATTACK_SUCCESS", ignoreCase = true) -> "HANDSHAKE CAPTURED"
                statusMessage.contains("DEAUTH", ignoreCase = true) -> "SENDING DEAUTH PACKETS..."
                statusMessage.contains("EAPOL", ignoreCase = true) -> "INTERCEPTING EAPOL FRAME..."
                statusMessage.isEmpty() -> "MONITORING AIRWAVES..."
                else -> statusMessage.uppercase()
            }
            Text(
                displayMessage,
                color = if (displayMessage.contains("CAPTURED")) HackerGreen else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timeout and Stop button at the bottom
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (timeLeft >= 0) "TIMEOUT IN: ${timeLeft}s" else "MODE: INFINITE CAPTURE",
                color = if (timeLeft < 10 && timeLeft >= 0) HackerRed else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            if (isPassive) {
                Button(
                    onClick = onManualDeauth,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HackerBlue.copy(alpha = 0.2f), contentColor = HackerBlue),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HackerBlue)
                ) {
                    Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEND DEAUTH FRAME", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onTerminate,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("STOP CAPTURE", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HandshakeFrameBadge(label: String, captured: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (captured) HackerGreen.copy(alpha = 0.2f) else HackerDarkGray,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (captured) HackerGreen else Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (captured) HackerGreen else Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Icon(
                if (captured) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (captured) HackerGreen else Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HandshakeResultsScreen(
    handshakeStatus: BleManager.HandshakeStatus,
    network: BleManager.ScanResultItem?,
    bleManager: BleManager,
    onClose: () -> Unit
) {
    val downloadProgress by bleManager.downloadProgress.collectAsState()
    val downloadedFile by bleManager.downloadedFile.collectAsState()
    val context = LocalContext.current
    val capturedFiles by bleManager.capturedFiles.collectAsState()

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.tcpdump.pcap"),
        onResult = { uri ->
            uri?.let {
                // Find the latest captured file for this network
                val capturedFile = capturedFiles.firstOrNull { 
                    it.ssid == network?.ssid && it.bssid == network?.bssid 
                }
                
                capturedFile?.let { file ->
                    try {
                        val internalFile = bleManager.getInternalFile(file.filename)
                        if (internalFile.exists()) {
                            val data = internalFile.readBytes()
                            context.contentResolver.openOutputStream(it)?.use { stream ->
                                stream.write(data)
                                Toast.makeText(context, "Handshake saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Fallback to memory buffer
                            downloadedFile?.let { data ->
                                context.contentResolver.openOutputStream(it)?.use { stream ->
                                    stream.write(data)
                                    Toast.makeText(context, "Handshake saved successfully", Toast.LENGTH_SHORT).show()
                                }
                            } ?: Toast.makeText(context, "Handshake data not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )



    val success = handshakeStatus.eapolMask >= 0x03

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                null,
                tint = if (success) HackerGreen else HackerRed,
                modifier = Modifier.size(48.dp)
            )

            Text(
                if (success) "CAPTURE COMPLETE" else "CAPTURE FAILED",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SESSION SUMMARY", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                
                DetailItem("Target SSID", network?.ssid ?: "Unknown", Icons.Default.Wifi)
                DetailItem("Total Frames", "${handshakeStatus.frames}", Icons.Default.Analytics)
                DetailItem("Clients Found", "${handshakeStatus.clients}", Icons.Default.People)
                DetailItem("File Size", "%.1f KB".format(handshakeStatus.fileSize / 1024.0), Icons.Default.Storage)
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                
                Text("EAPOL HANDSHAKE", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HandshakeFrameBadgeCompact("M1", (handshakeStatus.eapolMask and 0x01) != 0, Modifier.weight(1f))
                    HandshakeFrameBadgeCompact("M2", (handshakeStatus.eapolMask and 0x02) != 0, Modifier.weight(1f))
                    HandshakeFrameBadgeCompact("M3", (handshakeStatus.eapolMask and 0x04) != 0, Modifier.weight(1f))
                    HandshakeFrameBadgeCompact("M4", (handshakeStatus.eapolMask and 0x08) != 0, Modifier.weight(1f))
                }
            }
        }

        val hasData = handshakeStatus.frames > 0 || handshakeStatus.fileSize > 0 || handshakeStatus.isComplete

        if (hasData) {
            // Download status indicator
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                color = HackerBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HackerBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (downloadProgress > 0f && downloadProgress < 1f) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = HackerBlue,
                            strokeWidth = 2.dp,
                            progress = { downloadProgress }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("DOWNLOADING FROM ESP...", color = HackerBlue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    } else if (downloadedFile != null) {
                        Icon(Icons.Default.CheckCircle, null, tint = HackerGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("READY TO SAVE", color = HackerGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    } else {
                        Icon(Icons.Default.CloudDownload, null, tint = HackerBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("WAITING FOR DATA...", color = HackerBlue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }

            // Manual Save Button
            Button(
                onClick = {
                    val capturedFile = bleManager.capturedFiles.value.firstOrNull { 
                        it.ssid == network?.ssid && it.bssid == network?.bssid 
                    }
                    val fileNumber = capturedFile?.number ?: 0
                    val filename = "${network?.ssid ?: "unknown"} (${network?.bssid ?: "unknown"}) ($fileNumber).pcap"
                    fileSaver.launch(filename)
                },
                enabled = downloadedFile != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HackerBlue, 
                    contentColor = Color.White,
                    disabledContainerColor = HackerDarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE TO STORAGE", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CLOSE RESULTS", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HandshakeFrameBadgeCompact(label: String, captured: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (captured) HackerGreen.copy(alpha = 0.15f) else HackerDarkGray,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (captured) HackerGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (captured) HackerGreen else Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Icon(
                if (captured) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (captured) HackerGreen else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = HackerBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SignalBars(rssi: Int, color: Color, modifier: Modifier = Modifier) {
    val bars = when {
        rssi > -50 -> 4
        rssi > -65 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + (index * 3)).dp)
                    .background(
                        if (index < bars) color else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun HackerScanner(
    bleManager: BleManager, 
    scaffoldPadding: PaddingValues,
    onConfigureTarget: (BleManager.ScanResultItem) -> Unit
) {
    val scanResults by bleManager.scanResults.collectAsState()
    val isReady by bleManager.isReady.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Int?>(null) }
    
    // Tab State
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SIMPLE", "ADVANCED")
    
    // Advanced Traffic Mode State
    var trafficMode by remember { mutableIntStateOf(0) } // 0: Channels, 1: Networks
    
    // Reset isScanning when isReady becomes true
    LaunchedEffect(isReady) {
        if (isReady) isScanning = false
    }

    // Channel Analysis Data
    val channelMap = remember(scanResults) {
        val map = IntArray(15) { 0 }
        scanResults.forEach { 
            if (it.channel in 1..14) map[it.channel]++
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        Column(modifier = Modifier.fillMaxSize().padding(top = scaffoldPadding.calculateTopPadding())) {
            // 1. Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color(0xFF101012),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) HackerBlue else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // SIMPLE VIEW (Current Implementation)
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // Interactive Visualizer
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
                        border = BorderStroke(1.dp, HackerBlue.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            // Graph
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val chartWidth = size.width - 40f
                                            val barWidth = chartWidth / 14f
                                            if (offset.x > 30f) {
                                                val index = ((offset.x - 30f) / barWidth).toInt() + 1
                                                if (index in 1..14) {
                                                    selectedChannel = if (selectedChannel == index) null else index
                                                }
                                            }
                                        }
                                    }
                            ) {
                                val chartWidth = size.width - 40f
                                val chartHeight = size.height - 20f
                                val barWidth = chartWidth / 14f
                                val maxCount = (channelMap.maxOrNull() ?: 1).coerceAtLeast(5)
                                
                                // Draw Y-Axis Grid & Labels
                                val steps = 4
                                val stepValue = maxCount / steps.toFloat()
                                val stepHeight = chartHeight / steps
                                
                                for (i in 0..steps) {
                                    val y = chartHeight - (i * stepHeight)
                                    val value = (i * stepValue).toInt()
                                    
                                    // Grid Line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.05f),
                                        start = Offset(30f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    
                                    // Label
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(
                                            value.toString(),
                                            25f,
                                            y + 10f,
                                            android.graphics.Paint().apply {
                                                color = android.graphics.Color.DKGRAY
                                                textSize = 20f
                                                textAlign = android.graphics.Paint.Align.RIGHT
                                            }
                                        )
                                    }
                                }

                                for (i in 1..14) {
                                    val count = channelMap[i]
                                    val isSelected = selectedChannel == i
                                    val heightRatio = count.toFloat() / maxCount
                                    val barHeight = chartHeight * heightRatio
                                    val x = 30f + (i - 1) * barWidth
                                    
                                    // Draw Bar
                                    if (count > 0) {
                                        if (isSelected) {
                                            drawRect(
                                                color = Color.White.copy(alpha = 0.1f),
                                                topLeft = Offset(x, 0f),
                                                size = Size(barWidth, size.height)
                                            )
                                        }

                                        drawRect(
                                            color = if (isSelected) HackerGreen else HackerBlue,
                                            topLeft = Offset(x + 4f, chartHeight - barHeight),
                                            size = Size(barWidth - 8f, barHeight)
                                        )
                                        
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.5f),
                                            topLeft = Offset(x + 4f, chartHeight - barHeight),
                                            size = Size(barWidth - 8f, 4f)
                                        )
                                    } else {
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.05f),
                                            topLeft = Offset(x + 8f, chartHeight - 4f),
                                            size = Size(barWidth - 16f, 4f)
                                        )
                                    }
                                    
                                    // X-Axis Labels
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(
                                            "$i",
                                            x + barWidth/2,
                                            size.height,
                                            android.graphics.Paint().apply {
                                                color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.GRAY
                                                textSize = 24f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                            }
                                        )
                                    }
                                }
                            }

                            // Tooltip Overlay
                            selectedChannel?.let { ch ->
                                val networksOnChannel = scanResults.filter { it.channel == ch }
                                if (networksOnChannel.isNotEmpty()) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.9f),
                                        border = BorderStroke(1.dp, HackerGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "CHANNEL $ch (${networksOnChannel.size})",
                                                color = HackerGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            networksOnChannel.take(5).forEach { net ->
                                                Text(
                                                    net.ssid.ifEmpty { "Hidden" },
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (networksOnChannel.size > 5) {
                                                Text("+ ${networksOnChannel.size - 5} more", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val secured = scanResults.count { it.isSecure }
                        val open = scanResults.count { !it.isSecure }
                        val avgRssi = if (scanResults.isNotEmpty()) scanResults.map { it.rssi }.average().toInt() else 0
                        ScannerStatBox("SECURED", "$secured", HackerGreen, Modifier.weight(1f))
                        ScannerStatBox("OPEN", "$open", HackerRed, Modifier.weight(1f))
                        ScannerStatBox("AVG SIG", "$avgRssi", HackerBlue, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // List Header & Scan Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DETECTED SIGNALS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        
                        Button(
                            onClick = { 
                                isScanning = true
                                bleManager.clearScanResults()
                                bleManager.sendCommand("SCAN") 
                                selectedChannel = null
                            },
                            enabled = isReady && !isScanning,
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HackerBlue,
                                disabledContainerColor = HackerDarkGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SCAN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Network List
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp, 
                            end = 16.dp, 
                            top = 8.dp, 
                            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val filteredList = if (selectedChannel != null) {
                            scanResults.filter { it.channel == selectedChannel }
                        } else {
                            scanResults
                        }
                        
                        items(filteredList) { network ->
                            NetworkDetailCard(network)
                        }
                        if (filteredList.isEmpty() && !isScanning) {
                            item {
                                Text(
                                    if (selectedChannel != null) "No networks on Channel $selectedChannel" else "No networks found",
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // ADVANCED VIEW
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header
                    Text(
                        "TRAFFIC ANALYZER",
                        color = HackerGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // View Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CHANNEL TRAFFIC", "NETWORK TRAFFIC").forEachIndexed { index, title ->
                            val isSelected = trafficMode == index
                            Button(
                                onClick = { trafficMode = index },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) HackerGreen else Color(0xFF1A1A1D),
                                    contentColor = if (isSelected) Color.Black else Color.Gray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (trafficMode == 0) {
                        // CHANNEL TRAFFIC VIEW
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val maxCount = (channelMap.maxOrNull() ?: 1).coerceAtLeast(1)
                            
                            items(14) { index ->
                                val channel = index + 1
                                val count = channelMap[channel]
                                val load = count.toFloat() / maxCount
                                val color = when {
                                    load > 0.7f -> HackerRed
                                    load > 0.3f -> Color(0xFFFFB703)
                                    else -> HackerBlue
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Channel Number Box
                                        Surface(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("$channel", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Traffic Bar
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("CONGESTION", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text("$count APs", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(load.coerceAtLeast(0.02f))
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(color)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // NETWORK TRAFFIC VIEW
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(scanResults) { network ->
                                val signalStrength = (network.rssi + 100).toFloat() / 50f // Normalize -100..-50 to 0..1
                                val activityColor = if (network.rssi > -60) HackerGreen else if (network.rssi > -80) Color(0xFFFFB703) else HackerRed
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                network.ssid.ifEmpty { "Hidden Network" },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                network.bssid,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Simulated Activity Bar (based on Signal)
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(80.dp)) {
                                            Text("ACTIVITY", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                repeat(5) { i ->
                                                    val h = if (signalStrength > (i * 0.2f)) (4 + i * 2).dp else 2.dp
                                                    val alpha = if (signalStrength > (i * 0.2f)) 1f else 0.2f
                                                    Box(
                                                        modifier = Modifier
                                                            .width(4.dp)
                                                            .height(h)
                                                            .background(activityColor.copy(alpha = alpha), RoundedCornerShape(1.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (scanResults.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No active traffic detected", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                isScanning = true
                                bleManager.clearScanResults()
                                bleManager.sendCommand("SCAN")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HackerBlue),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SCANNING...", color = Color.Black)
                            } else {
                                Text("REFRESH TRAFFIC", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerStatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF151517),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NetworkDetailCard(network: BleManager.ScanResultItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1D)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Row 1: SSID and Signal Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    network.ssid,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    color = (if (network.rssi > -60) HackerGreen else if (network.rssi > -80) Color(0xFFFFB703) else HackerRed).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${network.rssi} dBm",
                        color = if (network.rssi > -60) HackerGreen else if (network.rssi > -80) Color(0xFFFFB703) else HackerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Row 2: Details Grid (BSSID, Channel, Security)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BSSID
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("BSSID (MAC)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        network.bssid,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                // Channel
                Column(modifier = Modifier.weight(0.8f)) {
                    Text("CHANNEL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${network.channel}",
                        color = HackerBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Security
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (network.isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                            null,
                            tint = if (network.isSecure) HackerGreen else HackerRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (network.isSecure) "SECURE" else "OPEN",
                            color = if (network.isSecure) HackerGreen else HackerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDetailBox(value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun HackerFiles(bleManager: BleManager) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val capturedFiles by bleManager.capturedFiles.collectAsState()
    val downloadProgress by bleManager.downloadProgress.collectAsState()
    val downloadedFile by bleManager.downloadedFile.collectAsState()
    
    var expandedFileIndex by remember { mutableIntStateOf(-1) }
    var fileToSave by remember { mutableStateOf<BleManager.CapturedFile?>(null) }

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.tcpdump.pcap"),
        onResult = { uri ->
            uri?.let {
                fileToSave?.let { capturedFile ->
                    try {
                        val internalFile = bleManager.getInternalFile(capturedFile.filename)
                        if (internalFile.exists()) {
                            val data = internalFile.readBytes()
                            context.contentResolver.openOutputStream(it)?.use { stream ->
                                stream.write(data)
                                Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Fallback to latest downloaded if internal file missing
                            downloadedFile?.let { data ->
                                context.contentResolver.openOutputStream(it)?.use { stream ->
                                    stream.write(data)
                                    Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
                                }
                            } ?: Toast.makeText(context, "File data not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            fileToSave = null
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "CAPTURED FILES", 
            color = Color.White, 
            fontWeight = FontWeight.Black, 
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        if (capturedFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, tint = HackerDarkGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No files captured yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                items(capturedFiles.indices.toList()) { index ->
                    val file = capturedFiles[index]
                    val isExpanded = expandedFileIndex == index
                    // SUCCESS if at least M1 and M2 are captured (the most critical part of handshake)
                    val isSuccess = file.eapolMask >= 0x03 
                    
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .combinedClickable(
                                    onClick = { expandedFileIndex = if (isExpanded) -1 else index },
                                    onLongClick = {
                                        clipboardManager.setText(AnnotatedString("${file.ssid} (${file.bssid})"))
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) HackerGreen.copy(alpha = 0.12f) else HackerRed.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp, 
                                (if (isSuccess) HackerGreen else HackerRed).copy(alpha = if (isExpanded) 0.6f else 0.25f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                (if (isSuccess) HackerGreen else HackerRed).copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isSuccess) Icons.Default.Inventory else Icons.Default.Warning,
                                            null,
                                            tint = if (isSuccess) HackerGreen else HackerRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${file.ssid}.${file.format.lowercase()}", 
                                            color = Color.White, 
                                            fontWeight = FontWeight.ExtraBold, 
                                            fontSize = 16.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            if (isSuccess) "CAPTURED" else "FAILED", 
                                            color = (if (isSuccess) HackerGreen else HackerRed).copy(alpha = 0.7f), 
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    null,
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                            
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FileDetailItem("SOURCE", file.ssid, Icons.Default.Wifi)
                                    FileDetailItem("BSSID", file.bssid, Icons.Default.Fingerprint)
                                    FileDetailItem("FORMAT", file.format, Icons.Default.Description)
                                    FileDetailItem("SIZE", file.size, Icons.Default.Storage)
                                    FileDetailItem("TIMESTAMP", file.timestamp, Icons.Default.Schedule)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("HANDSHAKE DATA", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HandshakeFrameBadge("M1", (file.eapolMask and 0x01) != 0, Modifier.weight(1f))
                                        HandshakeFrameBadge("M2", (file.eapolMask and 0x02) != 0, Modifier.weight(1f))
                                        HandshakeFrameBadge("M3", (file.eapolMask and 0x04) != 0, Modifier.weight(1f))
                                        HandshakeFrameBadge("M4", (file.eapolMask and 0x08) != 0, Modifier.weight(1f))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Save Button
                                    Button(
                                        onClick = { 
                                            val internalFile = bleManager.getInternalFile(file.filename)
                                            val isAvailable = internalFile.exists() || (downloadedFile != null && index == 0)
                                            
                                            if (isAvailable) {
                                                fileToSave = file
                                                val filename = "${file.ssid} (${file.bssid}) (${file.number}).pcap"
                                                fileSaver.launch(filename)
                                            } else if (index < 2) {
                                                // Try to download from ESP if it's one of the last 2
                                                Toast.makeText(context, "Requesting file from ESP...", Toast.LENGTH_SHORT).show()
                                                bleManager.savePcap(index)
                                            } else {
                                                Toast.makeText(context, "File data not available. Only the last 2 captures are kept on ESP.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HackerBlue),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SAVE TO STORAGE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Delete Button
                                    OutlinedButton(
                                        onClick = { bleManager.deleteCapturedFile(file) },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        border = BorderStroke(1.dp, HackerRed.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = HackerRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("DELETE", color = HackerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                        
                    // Number Bubble Overlay
                    Surface(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .size(24.dp)
                            .align(Alignment.TopStart),
                        color = HackerBlue,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, HackerBlack),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = file.number.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
fun BeaconSSIDSettings(
    bleManager: BleManager,
    onBack: () -> Unit
) {
    val ssids by bleManager.beaconSSIDs.collectAsState()
    var newSSID by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                "SWARM NETWORKS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HackerDarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "ADD NEW NETWORK",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = newSSID,
                        onValueChange = { if (it.length <= 32) newSSID = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Network Name (SSID)", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = HackerBlue,
                            focusedIndicatorColor = HackerBlue,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = {
                            if (newSSID.isNotBlank()) {
                                bleManager.setBeaconSSIDs(ssids + newSSID)
                                newSSID = ""
                            }
                        },
                        enabled = newSSID.isNotBlank(),
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (newSSID.isNotBlank()) HackerBlue else Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "CONFIGURED NETWORKS (${ssids.size})",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            
            ssids.forEachIndexed { index, ssid ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HackerDarkGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = HackerBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(ssid, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        
                        IconButton(onClick = {
                            bleManager.setBeaconSSIDs(ssids.toMutableList().apply { removeAt(index) })
                        }) {
                            Icon(Icons.Default.Delete, null, tint = HackerRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            if (ssids.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No custom networks added", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
