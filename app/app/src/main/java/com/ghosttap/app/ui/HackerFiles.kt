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
