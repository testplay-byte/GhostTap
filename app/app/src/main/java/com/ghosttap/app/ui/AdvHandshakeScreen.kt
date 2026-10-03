package com.ghosttap.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghosttap.app.BleManager
import kotlinx.coroutines.delay

@Composable
fun AdvHandshakeScreen(
    bleManager: BleManager,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf(AdvState.IDLE) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var currentTarget by remember { mutableStateOf<BleManager.ScanResultItem?>(null) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) }
    
    val scanResults by bleManager.scanResults.collectAsState()
    val capturedFiles by bleManager.capturedFiles.collectAsState()
    val handshakeStatus by bleManager.handshakeStatus.collectAsState()
    val isReady by bleManager.isReady.collectAsState()
    val temperature by bleManager.temperature.collectAsState()
    val scrollState = rememberLazyListState()

    // Pulse Animation for Active State
    val infiniteTransition = rememberInfiniteTransition(label = "adv_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Radar/Scanning Animation (Rotation)
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs = logs + "[$timestamp] $msg"
    }

    // Auto-scroll logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollToItem(logs.size - 1)
        }
    }

    // Main Logic Loop
    LaunchedEffect(Unit) {
        addLog("Starting Advanced Handshake Mode...")
        state = AdvState.SCANNING
        bleManager.clearScanResults()
        bleManager.sendCommand("SCAN")
        addLog("Scanning for networks...")
    }

    // State Transitions
    LaunchedEffect(state, scanResults) {
        if (state == AdvState.SCANNING && scanResults.isNotEmpty()) {
            delay(2000)
            state = AdvState.PROCESSING_TARGETS
        }
        
        if (state == AdvState.PROCESSING_TARGETS) {
            val availableTargets = scanResults.filter { result ->
                // Filter out networks that have already been successfully captured (M4 + M1/2/3)
                // Using BSSID for precise matching, ignore case
                val isCapturedSuccess = capturedFiles.any { file -> 
                    file.bssid.equals(result.bssid, ignoreCase = true) && 
                    (file.eapolMask and 0x08) != 0 // Has M4 = Success
                }
                !isCapturedSuccess && result.ssid != "NO_NETWORKS"
            }

            if (availableTargets.isEmpty()) {
                addLog("No new targets found. Rescanning in 5s...")
                delay(5000)
                state = AdvState.SCANNING
                bleManager.clearScanResults()
                bleManager.sendCommand("SCAN")
                bleManager.resetHandshakeStatus()
                currentTarget = null
            } else {
                val target = availableTargets.random()
                currentTarget = target
                addLog("Selected Target: ${target.ssid} (${target.bssid})")
                retryCount = 0
                state = AdvState.TARGET_PREVIEW
            }
        }

        if (state == AdvState.TARGET_PREVIEW) {
            // Show preview for 3 seconds
            delay(3000)
            state = AdvState.START_ATTACK
        }

        if (state == AdvState.START_ATTACK) {
            currentTarget?.let { target ->
                addLog("Starting Passive Attack on ${target.ssid}")
                bleManager.setTargetNetworks(listOf(target))
                bleManager.sendCommand("ATTACK:HANDSHAKE:2:${target.index}:60")
                delay(500)
                state = AdvState.DEAUTH_PHASE
            }
        }
    }

    // Timer and Phase Logic
    LaunchedEffect(state) {
        if (state == AdvState.DEAUTH_PHASE) {
            phase = "DEAUTH INJECTION"
            addLog("Phase 1: Broadcasting Deauth (5s)...")
            timerSeconds = 5
            
            // Independent Timer Loop
            val startTime = System.currentTimeMillis()
            while (timerSeconds > 0 && state == AdvState.DEAUTH_PHASE) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                timerSeconds = maxOf(0, 5 - elapsed.toInt())
                
                if (timerSeconds % 1 == 0) { // Every second
                    bleManager.sendCommand("DEAUTH")
                }
                delay(100) // Update UI frequently
            }
            
            if (state == AdvState.DEAUTH_PHASE) {
                state = AdvState.LISTEN_PHASE
            }
        }
        
        if (state == AdvState.LISTEN_PHASE) {
            phase = "LISTENING FOR HANDSHAKE"
            addLog("Phase 2: Listening (25s)...")
            timerSeconds = 25
            
            val startTime = System.currentTimeMillis()
            while (timerSeconds > 0 && state == AdvState.LISTEN_PHASE) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                timerSeconds = maxOf(0, 25 - elapsed.toInt())
                
                // Check for success
                val mask = handshakeStatus.eapolMask
                val hasM4 = (mask and 0x08) != 0
                val hasAnyM123 = (mask and 0x07) != 0
                
                if (hasM4 && hasAnyM123) {
                    addLog("SUCCESS! Handshake captured (M4 + M1/2/3).")
                    state = AdvState.SUCCESS
                    break
                }
                
                delay(100)
            }
            
            if (state == AdvState.LISTEN_PHASE) {
                // Timeout logic
                if (retryCount < 1) {
                    addLog("No handshake. Retrying (${retryCount + 1}/1)...")
                    retryCount++
                    state = AdvState.DEAUTH_PHASE
                } else {
                    addLog("Failed to capture ${currentTarget?.ssid}. Moving to next...")
                    bleManager.sendCommand("STOP")
                    delay(1000)
                    state = AdvState.PROCESSING_TARGETS
                }
            }
        }
        
        if (state == AdvState.SUCCESS) {
            bleManager.sendCommand("STOP")
            addLog("Saving capture...")
            delay(3000) // Show SUCCESS state for 3s
            bleManager.resetHandshakeStatus()
            currentTarget = null
            state = AdvState.PROCESSING_TARGETS
        }
    }

    // Session Stats
    var sessionCaptures by remember { mutableIntStateOf(0) }
    var totalScanned by remember { mutableIntStateOf(0) }
    
    // Update Stats
    LaunchedEffect(scanResults) {
        if (scanResults.isNotEmpty()) {
            totalScanned = maxOf(totalScanned, scanResults.size)
        }
    }
    
    LaunchedEffect(state) {
        if (state == AdvState.SUCCESS) {
            sessionCaptures++
        }
    }

    // New UI Layout matching HandshakeRunningScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HackerBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: HEADER (Status & Target)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = HackerDarkGray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state == AdvState.SCANNING) "STATUS: SCANNING" else "STATUS: ATTACKING",
                        color = if (state == AdvState.SCANNING) HackerBlue else HackerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    // Signal Strength Badge
                    if (currentTarget != null) {
                        Surface(
                            color = (if ((currentTarget?.rssi ?: -100) > -60) HackerGreen else HackerRed).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${currentTarget?.rssi} dBm",
                                color = if ((currentTarget?.rssi ?: -100) > -60) HackerGreen else HackerRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // SSID (Big & Bold)
                Text(
                    currentTarget?.ssid ?: "SEARCHING...",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 34.sp
                )

                // Metadata Row (Channel & BSSID)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentTarget != null) {
                        Surface(
                            color = HackerBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "CH ${currentTarget?.channel}",
                                color = HackerBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        currentTarget?.bssid ?: "WAITING FOR TARGET...",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // SECTION 2: CAPTURE VISUALIZATION (Central Circle + Corner Badges)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take available space
                .padding(horizontal = 0.dp), // Removed horizontal padding
            contentAlignment = Alignment.Center
        ) {
            // Background Container for depth
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HackerDarkGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {}

            // Inner Layout Box
            Box(
                modifier = Modifier
                    .size(280.dp) // Large enough to hold circle + badges
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Central Progress Circle
                Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(160.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        shadowElevation = 8.dp
                    ) {}
                    
                    if (currentTarget == null && state == AdvState.SCANNING) {
                         // Scanning Animation (Radar Sweep) when no target
                         CircularProgressIndicator(
                            progress = { 1f },
                            color = HackerBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(140.dp),
                            strokeWidth = 2.dp
                        )
                        Box(
                             modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { rotationZ = radarRotation }
                        ) {
                             CircularProgressIndicator(
                                progress = { 0.25f },
                                color = HackerBlue,
                                modifier = Modifier.size(140.dp),
                                strokeWidth = 12.dp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCANNING", color = HackerBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("NEARBY...", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (currentTarget == null && state == AdvState.PROCESSING_TARGETS) {
                         // No New Target Found State
                         CircularProgressIndicator(
                            progress = { 1f },
                            color = Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(140.dp),
                            strokeWidth = 4.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("NO NEW", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("TARGETS", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (state == AdvState.TARGET_PREVIEW) {
                        // Target Preview (Show SSID/Info before attack)
                        CircularProgressIndicator(
                            progress = { 1f },
                            color = HackerRed,
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { alpha = pulseAlpha },
                            strokeWidth = 4.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET", color = HackerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                currentTarget?.ssid?.take(8) ?: "...", 
                                color = Color.White, 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("LOCKED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Attack Progress / Status
                        CircularProgressIndicator(
                            progress = { 1f },
                            color = HackerGreen.copy(alpha = 0.1f),
                            modifier = Modifier.size(140.dp),
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            color = if (state == AdvState.SCANNING) HackerBlue else HackerGreen,
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { alpha = if (state != AdvState.IDLE) pulseAlpha else 1f },
                            strokeWidth = 12.dp
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${handshakeStatus.frames}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                            Text("FRAMES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Corner EAPOL Badges
                // Show "SAVED" overlay on success, or standard badges otherwise
                if (state == AdvState.SUCCESS) {
                     Surface(
                        color = HackerGreen.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("SAVED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Top Left: M1
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        HandshakeCornerBadge("M1", (handshakeStatus.eapolMask and 0x01) != 0)
                    }
                    // Top Right: M2
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        HandshakeCornerBadge("M2", (handshakeStatus.eapolMask and 0x02) != 0)
                    }
                    // Bottom Left: M3
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        HandshakeCornerBadge("M3", (handshakeStatus.eapolMask and 0x04) != 0)
                    }
                    // Bottom Right: M4
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        HandshakeCornerBadge("M4", (handshakeStatus.eapolMask and 0x08) != 0)
                    }
                }
            }
        }

        // SECTION 3: DATA GRID
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Clients, Size, Temp
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataGridItem("CLIENTS", "${handshakeStatus.clients}", Icons.Default.Devices, Modifier.weight(1f))
                DataGridItem("SIZE", "%.1f KB".format(handshakeStatus.fileSize / 1024.0), Icons.Default.Storage, Modifier.weight(1f))
                
                // Temperature with Color Coding
                val tempVal = temperature
                val tempColor = when {
                    tempVal < 40f -> HackerBlue
                    tempVal < 50f -> HackerGreen
                    tempVal < 60f -> Color(0xFFFFCC00) // Yellow
                    tempVal < 70f -> Color(0xFFFF9500) // Orange
                    else -> HackerRed
                }
                DataGridItem("TEMP", "${tempVal.toInt()}°C", Icons.Default.Thermostat, Modifier.weight(1f), iconTint = tempColor, valueColor = tempColor)
            }
            // Row 2: Scanned, Captured
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataGridItem("SCANNED", "$totalScanned", Icons.Default.Radar, Modifier.weight(1f), HackerBlue)
                DataGridItem("CAPTURED", "$sessionCaptures", Icons.Default.CheckCircle, Modifier.weight(1f), HackerGreen)
            }
        }

        // SECTION 4: FOOTER (Timer & Stop)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state == AdvState.SCANNING) "SCANNING..." else "TIMEOUT: ${timerSeconds}s",
                    color = if (timerSeconds < 5 && state != AdvState.SCANNING) HackerRed else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Button(
                onClick = { 
                    bleManager.sendCommand("STOP")
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HackerRed, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("STOP ATTACK", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun HandshakeCornerBadge(label: String, active: Boolean) {
    Surface(
        color = if (active) HackerGreen else HackerDarkGray,
        shape = CircleShape,
        border = BorderStroke(2.dp, if (active) HackerGreen else Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.size(48.dp),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (active) Color.Black else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DataGridItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, iconTint: Color = Color.Gray, valueColor: Color = Color.White) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = HackerDarkGray.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

enum class AdvState {
    IDLE,
    SCANNING,
    PROCESSING_TARGETS,
    TARGET_PREVIEW,
    START_ATTACK,
    DEAUTH_PHASE,
    LISTEN_PHASE,
    SUCCESS
}
