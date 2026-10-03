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

