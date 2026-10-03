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

