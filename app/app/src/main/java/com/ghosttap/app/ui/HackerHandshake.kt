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

