package com.ghosttap.app.ui

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cyclone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ghosttap.app.BleManager
import com.ghosttap.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit


sealed class DeviceCommand(val command: String, val simulatorId: String) {
    data class Clock(val faceId: String) : DeviceCommand("CLOCK:$faceId", "CLOCK:$faceId")
    data class MusicViz(val mode: String) : DeviceCommand("MUSIC:VIZ:$mode", "MUSIC:VIZ:$mode")
    data class BootAnim(val name: String) : DeviceCommand("BOOT_ANIM:$name", "ANIM:$name")
    data class Snake(val mode: String) : DeviceCommand("SNAKE:$mode", "SNAKE:$mode")
    object Clear : DeviceCommand("CLEAR", "ANIM:STARS")
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val height = constraints.maxHeight.toFloat()
        val width = constraints.maxWidth.toFloat()
        val presets = listOf(1f, 2f, 3f, 5f, 10f)
        
        fun snapValue(raw: Float): Float {
            // Snap to nearest integer within the range
            return raw.roundToInt().toFloat().coerceIn(valueRange)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val position = change.position.y
                        val rawValue = valueRange.start + (1f - (position / height)) * (valueRange.endInclusive - valueRange.start)
                        onValueChange(snapValue(rawValue))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val position = offset.y
                        val rawValue = valueRange.start + (1f - (position / height)) * (valueRange.endInclusive - valueRange.start)
                        onValueChange(snapValue(rawValue))
                    }
                }
        ) {
            val trackWidth = 24.dp.toPx()
            val thumbRadius = 18.dp.toPx()
            val trackHeight = height - (thumbRadius * 2)
            
            // Draw background track
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.2f),
                topLeft = Offset((width - trackWidth) / 2, thumbRadius),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackWidth / 2)
            )

            // Draw tick marks for presets
            presets.forEach { preset ->
                val progress = (preset - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                val y = thumbRadius + trackHeight - (trackHeight * progress)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset((width - trackWidth) / 2 - 4.dp.toPx(), y),
                    end = Offset((width + trackWidth) / 2 + 4.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Draw active track
            val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val activeTrackHeight = trackHeight * progress
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                ),
                topLeft = Offset((width - trackWidth) / 2, thumbRadius + trackHeight - activeTrackHeight),
                size = Size(trackWidth, activeTrackHeight),
                cornerRadius = CornerRadius(trackWidth / 2)
            )
            
            // Draw thumb with shadow/glow effect
            drawCircle(
                color = Color.Black.copy(alpha = 0.1f),
                radius = thumbRadius + 2.dp.toPx(),
                center = Offset(width / 2, thumbRadius + trackHeight - activeTrackHeight + 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(width / 2, thumbRadius + trackHeight - activeTrackHeight)
            )
            drawCircle(
                color = Color(0xFF2196F3),
                radius = thumbRadius - 6.dp.toPx(),
                center = Offset(width / 2, thumbRadius + trackHeight - activeTrackHeight)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    bleManager: BleManager,
    onDisconnect: () -> Unit,
    onHackerMode: () -> Unit
) {
    // Implementation
    val context = LocalContext.current
    val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    
    var apiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var selectedModel by remember { mutableStateOf(prefs.getString("gemini_model", "gemini-3-flash-preview") ?: "gemini-3-flash-preview") }
    var apiTimeoutSeconds by remember { mutableStateOf(prefs.getFloat("gemini_timeout", 30f)) }
    var isEditingKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Settings Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                // API Key Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gemini API Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { 
                            apiKey = it
                            prefs.edit().putString("gemini_api_key", it).apply()
                        },
                        visualTransformation = if (isEditingKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isEditingKey = !isEditingKey }) {
                                Icon(
                                    if (isEditingKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Toggle Visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your API Key") },
                        singleLine = true
                    )
                }
                
                // Model Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    
                    // Model Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("gemini-3-flash-preview", "gemini-2.0-flash").forEach { model ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    prefs.edit().putString("gemini_model", model).apply()
                                },
                                label = { Text(model.removePrefix("gemini-")) },
                                leadingIcon = if (selectedModel == model) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    // Custom Model Typing
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { 
                            selectedModel = it
                            prefs.edit().putString("gemini_model", it).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter custom model name") },
                        label = { Text("Custom Model Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // API Timeout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("API Timeout", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("${apiTimeoutSeconds.toInt()}s", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = apiTimeoutSeconds,
                        onValueChange = { 
                            apiTimeoutSeconds = it
                            prefs.edit().putFloat("gemini_timeout", it).apply()
                        },
                        valueRange = 10f..120f,
                        steps = 11, // 10, 20, 30... 120
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Connection Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BluetoothConnected, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Device")
                }
            }
        }

        // THE OTHER SIDE Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Special Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                }
                
                Button(
                    onClick = onHackerMode,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoFixHigh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("THE OTHER SIDE", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // About Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = "IT'S AN APP",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/CONFUSED83") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Follow on GitHub",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

// Removed duplicate SettingsScreen function here

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(bleManager: BleManager, onDisconnect: () -> Unit, onHackerMode: () -> Unit) {
    val connectionState by bleManager.connectionState.collectAsState()
    val statusMessage by bleManager.statusMessage.collectAsState()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height
    val activity = LocalActivity.current
    val view = LocalView.current
    
    // Shared state moved here to persist across orientation changes
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("drawing_prefs", android.content.Context.MODE_PRIVATE) }
    
    val selectedTab = remember { mutableIntStateOf(prefs.getInt("last_tab", 0)) }

    // Text state
    var textMsg by remember { mutableStateOf(prefs.getString("text_msg", "") ?: "") }
    var textFontSize by remember { mutableIntStateOf(prefs.getInt("text_font_size", 1)) }
    var textEffect by remember { mutableIntStateOf(prefs.getInt("text_effect", 0)) }
    var textAnimMode by remember { mutableIntStateOf(prefs.getInt("text_anim", 0)) }
    var textAnimSpeed by remember { mutableFloatStateOf(prefs.getFloat("text_speed", 1.0f)) }
    var use24HourFormat by remember { mutableStateOf(prefs.getBoolean("use_24h", true)) }

    // Extras state
    var lastExtraCmd by remember { mutableStateOf(prefs.getString("last_extra_cmd", "") ?: "") }
    var currentSimAnim by remember { mutableStateOf(prefs.getString("last_sim_anim", "") ?: "") }
    var extrasSelectedSection by remember { mutableStateOf(prefs.getString("extras_section", "CLOCK") ?: "CLOCK") }
    var isVisualizerVisible by remember { mutableStateOf(false) }

    fun loadPoints(): Set<Pair<Int, Int>> {
        val saved = prefs.getString("saved_points", "") ?: ""
        if (saved.isEmpty()) return emptySet()
        return saved.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                parts[0].toIntOrNull()?.let { x ->
                    parts[1].toIntOrNull()?.let { y -> x to y }
                }
            } else null
        }.toSet()
    }

    fun savePoints(pts: Set<Pair<Int, Int>>) {
        val serialized = pts.joinToString(";") { "${it.first},${it.second}" }
        prefs.edit().putString("saved_points", serialized).apply()
    }

    var points by remember { mutableStateOf(loadPoints()) }
    var penSize by remember { mutableFloatStateOf(prefs.getFloat("saved_pen_size", 1f)) }
    var currentTool by remember { mutableStateOf(DrawTool.valueOf(prefs.getString("saved_tool", DrawTool.BRUSH.name) ?: DrawTool.BRUSH.name)) }

    // Sync tab-specific data to ESP32 when connected or tab changes
    LaunchedEffect(connectionState, selectedTab.intValue) {
        if (connectionState == BleManager.State.CONNECTED) {
            // Persist the selected tab
            prefs.edit().putInt("last_tab", selectedTab.intValue).apply()

            when (selectedTab.intValue) {
                0 -> { // Draw
                    val pts = points
                    bleManager.sendCommand("CLEAR")
                    delay(100)
                    if (pts.isNotEmpty()) {
                        // Efficient sync using ROW commands
                        for (y in 0 until 40) { // oledHeight = 40
                            var rowData = ""
                            for (i in 0 until 9) { // 72 / 8 = 9 bytes
                                var byteVal = 0
                                for (bit in 0 until 8) {
                                    val px = i * 8 + bit
                                    if (pts.contains(px to y)) {
                                        byteVal = byteVal or (1 shl (7 - bit))
                                    }
                                }
                                rowData += String.format("%02X", byteVal)
                            }
                            while (!bleManager.isReady.value) { delay(5) }
                            bleManager.sendCommand("ROW:$y:$rowData")
                            delay(5)
                        }
                    }
                    bleManager.sendCommand("UPDATE")
                }
                1 -> { // Text
                    bleManager.sendCommand("CLEAR")
                    delay(50)
                    bleManager.sendCommand("UPDATE")
                }
                2 -> { // Extras
                    bleManager.sendCommand("CLEAR")
                    delay(50)
                    if (lastExtraCmd.isNotEmpty()) {
                        bleManager.sendCommand(lastExtraCmd)
                    }
                    bleManager.sendCommand("UPDATE")
                }
                else -> {
                    bleManager.sendCommand("CLEAR")
                    bleManager.sendCommand("UPDATE")
                }
            }
        }
    }
    
    LaunchedEffect(isLandscape) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    Scaffold(
        topBar = {
            if (!isLandscape) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(40.dp) // Shortened height
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Empty spacer to center the connection status if needed, 
                        // or just use Row with SpaceBetween if actions are on the right
                        Spacer(modifier = Modifier.width(48.dp)) 

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            BleManager.State.CONNECTED -> Color(0xFF4CAF50)
                                            BleManager.State.CONNECTING -> Color(0xFFFFC107)
                                            BleManager.State.FAILED -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (connectionState) {
                                    BleManager.State.CONNECTED -> "CONNECTED"
                                    BleManager.State.CONNECTING -> "CONNECTING..."
                                    BleManager.State.FAILED -> "FAILED"
                                    else -> "DISCONNECTED"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = when (connectionState) {
                                    BleManager.State.CONNECTED -> Color(0xFF4CAF50)
                                    BleManager.State.CONNECTING -> Color(0xFFFFC107)
                                    BleManager.State.FAILED -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            )

                            if (selectedTab.intValue == 2) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isVisualizerVisible = !isVisualizerVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVisualizerVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }
        },
        bottomBar = {
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 6.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
                ) {
                    Surface(
                        tonalElevation = 12.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            val tabs = listOf(
                                Triple(0, Icons.Default.Brush, "Draw"),
                                Triple(1, Icons.Default.TextFields, "Text"),
                                Triple(2, Icons.Default.AutoAwesome, "Extras"),
                                Triple(3, Icons.Default.Settings, "Settings")
                            )

                            tabs.forEach { (index, icon, label) ->
                                val selected = selectedTab.intValue == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab.intValue = index },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(if (selected) 42.dp else 28.dp)
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                icon, 
                                                contentDescription = label,
                                                modifier = Modifier.size(if (selected) 24.dp else 22.dp)
                                            )
                                        }
                                    },
                                    label = null,
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(if (isLandscape) PaddingValues(0.dp) else padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = if (isLandscape) Modifier.fillMaxSize() else Modifier.weight(1f)) {
                if (isLandscape) {
                    DrawingCanvas(
                        bleManager = bleManager, 
                        isLandscape = true,
                        points = points,
                        onPointsChange = { 
                            points = it
                            savePoints(it)
                        },
                        penSize = penSize,
                        onPenSizeChange = { 
                            penSize = it
                            prefs.edit().putFloat("saved_pen_size", it).apply()
                        },
                        currentTool = currentTool,
                        onToolChange = { 
                            currentTool = it
                            prefs.edit().putString("saved_tool", it.name).apply()
                        }
                    )
                } else {
                    when (selectedTab.intValue) {
                        0 -> DrawingCanvas(
                            bleManager = bleManager, 
                            isLandscape = false,
                            points = points,
                            onPointsChange = { 
                                points = it 
                                savePoints(it)
                            },
                            penSize = penSize,
                            onPenSizeChange = { 
                                penSize = it
                                prefs.edit().putFloat("saved_pen_size", it).apply()
                            },
                            currentTool = currentTool,
                            onToolChange = { 
                                currentTool = it
                                prefs.edit().putString("saved_tool", it.name).apply()
                            }
                        )
                        1 -> TextControl(
                            bleManager = bleManager,
                            text = textMsg, onTextChange = { textMsg = it; prefs.edit().putString("text_msg", it).apply() },
                            fontSize = textFontSize, onFontSizeChange = { textFontSize = it; prefs.edit().putInt("text_font_size", it).apply() },
                            effect = textEffect, onEffectChange = { textEffect = it; prefs.edit().putInt("text_effect", it).apply() },
                            animationMode = textAnimMode, onAnimationModeChange = { textAnimMode = it; prefs.edit().putInt("text_anim", it).apply() },
                            animSpeed = textAnimSpeed, onAnimSpeedChange = { textAnimSpeed = it; prefs.edit().putFloat("text_speed", it).apply() }
                        )
                        2 -> ExtrasScreen(
                            bleManager = bleManager,
                            isVisualizerVisible = isVisualizerVisible,
                            onSetVisualizerVisible = { isVisualizerVisible = it },
                            onCommandSent = { 
                                lastExtraCmd = it
                                prefs.edit().putString("last_extra_cmd", it).apply() 
                            },
                            currentSimAnim = currentSimAnim,
                            onSimAnimChange = { 
                                currentSimAnim = it
                                prefs.edit().putString("last_sim_anim", it).apply()
                            },
                            selectedSection = extrasSelectedSection,
                            onSectionChange = { 
                                extrasSelectedSection = it
                                prefs.edit().putString("extras_section", it).apply()
                            },
                            use24HourFormat = use24HourFormat,
                            onTimeFormatChange = {
                                use24HourFormat = it
                                prefs.edit().putBoolean("use_24h", it).apply()
                            }
                        )
                        3 -> SettingsScreen(
                            bleManager = bleManager,
                            onDisconnect = onDisconnect,
                            onHackerMode = onHackerMode
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

enum class DrawTool { BRUSH, ERASER, LINE, RECT, CIRCLE }

@Composable
fun OledCanvas(
    bleManager: BleManager,
    points: Set<Pair<Int, Int>>,
    onPointsChange: (Set<Pair<Int, Int>>) -> Unit,
    penSize: Float,
    currentTool: DrawTool,
    scale: Float,
    oledWidth: Int,
    oledHeight: Int,
    pendingPoints: MutableList<Triple<Int, Int, Boolean>>
) {
    val density = LocalDensity.current
    val currentPointsState by rememberUpdatedState(points)
    val currentOnPointsChangeState by rememberUpdatedState(onPointsChange)
    val currentPenSizeState by rememberUpdatedState(penSize)
    val currentToolState by rememberUpdatedState(currentTool)
    
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }
    var lastPixel by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun getMappedSize(size: Float): Float {
        return when (size.toInt()) {
            2 -> 1.5f
            4 -> 3.5f
            else -> size.toInt().toFloat()
        }
    }

    fun getBrushPoints(x: Int, y: Int, size: Float): Set<Pair<Int, Int>> {
        val pts = mutableSetOf<Pair<Int, Int>>()
        val mappedSize = getMappedSize(size)
        if (mappedSize <= 1.1f) {
            pts.add(x to y)
        } else if (mappedSize <= 2.1f) {
            pts.add(x to y); pts.add(x + 1 to y); pts.add(x to y + 1); pts.add(x + 1 to y + 1)
        } else {
            val radius = mappedSize / 2f
            val ir = (radius + 0.5f).toInt()
            for (ix in -ir..ir) {
                for (iy in -ir..ir) {
                    if (ix*ix + iy*iy <= radius*radius + radius) {
                        pts.add((x + ix) to (y + iy))
                    }
                }
            }
        }
        return pts
    }

    fun getLinePoints(x0: Int, y0: Int, x1: Int, y1: Int, size: Float): Set<Pair<Int, Int>> {
        val pts = mutableSetOf<Pair<Int, Int>>()
        val dx = abs(x1 - x0); val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx - dy; var currX = x0; var currY = y0
        while (true) {
            pts.addAll(getBrushPoints(currX, currY, size))
            if (currX == x1 && currY == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; currX += sx }
            if (e2 < dx) { err += dx; currY += sy }
        }
        return pts
    }

    fun interpolateLine(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        val pts = mutableListOf<Pair<Int, Int>>()
        val dx = abs(x1 - x0); val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx - dy; var currX = x0; var currY = y0
        while (true) {
            pts.add(currX to currY)
            if (currX == x1 && currY == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; currX += sx }
            if (e2 < dx) { err += dx; currY += sy }
        }
        return pts
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Canvas(
            modifier = Modifier
                .size((oledWidth * scale).dp, (oledHeight * scale).dp)
                .pointerInput(currentTool) {
                    detectTapGestures { offset ->
                        val sx = (offset.x / (scale * density.density)).toInt()
                        val sy = (offset.y / (scale * density.density)).toInt()
                        
                        if (currentToolState == DrawTool.BRUSH || currentToolState == DrawTool.ERASER) {
                            val pts = getBrushPoints(sx, sy, currentPenSizeState)
                            if (currentToolState == DrawTool.ERASER) {
                                val toRemove = currentPointsState.intersect(pts)
                                if (toRemove.isNotEmpty()) {
                                    currentOnPointsChangeState(currentPointsState - toRemove)
                                    toRemove.forEach { pendingPoints.add(Triple(it.first, it.second, false)) }
                                }
                            } else {
                                val toAdd = pts.subtract(currentPointsState)
                                if (toAdd.isNotEmpty()) {
                                    currentOnPointsChangeState(currentPointsState + toAdd)
                                    toAdd.forEach { pendingPoints.add(Triple(it.first, it.second, true)) }
                                }
                            }
                        }
                    }
                }
                .pointerInput(currentTool) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startPoint = offset
                            currentPoint = offset
                            lastPixel = null
                        },
                        onDragEnd = {
                            val start = startPoint
                            val end = currentPoint
                            if (start != null && end != null) {
                                val sx = (start.x / (scale * density.density)).toInt()
                                val sy = (start.y / (scale * density.density)).toInt()
                                val ex = (end.x / (scale * density.density)).toInt()
                                val ey = (end.y / (scale * density.density)).toInt()
                                
                                when (currentToolState) {
                                    DrawTool.LINE -> {
                                        val pts = getLinePoints(sx, sy, ex, ey, currentPenSizeState)
                                        currentOnPointsChangeState(currentPointsState + pts)
                                        // Optimize: Send single command instead of pixels
                                        bleManager.sendCommand("LINE:$sx:$sy:$ex:$ey:$currentPenSizeState")
                                    }
                                    DrawTool.RECT -> {
                                        val x = min(sx, ex); val y = min(sy, ey)
                                        val w = abs(ex - sx); val h = abs(ey - sy)
                                        val pts = mutableSetOf<Pair<Int, Int>>()
                                        for (i in x..x+w) { 
                                            pts.addAll(getBrushPoints(i, y, currentPenSizeState))
                                            pts.addAll(getBrushPoints(i, y+h, currentPenSizeState))
                                        }
                                        for (i in y..y+h) { 
                                            pts.addAll(getBrushPoints(x, i, currentPenSizeState))
                                            pts.addAll(getBrushPoints(x+w, i, currentPenSizeState))
                                        }
                                        currentOnPointsChangeState(currentPointsState + pts)
                                        // Optimize: Send single command
                                        bleManager.sendCommand("RECT:$x:$y:$w:$h:$currentPenSizeState")
                                    }
                                    DrawTool.CIRCLE -> {
                                        val radiusVal = hypot((ex - sx).toDouble(), (ey - sy).toDouble()).toInt()
                                        val pts = mutableSetOf<Pair<Int, Int>>()
                                        var x = radiusVal; var y = 0; var err = 0
                                        while (x >= y) {
                                            pts.addAll(getBrushPoints(sx+x, sy+y, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx+y, sy+x, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx-y, sy+x, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx-x, sy+y, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx-x, sy-y, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx-y, sy-x, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx+y, sy-x, currentPenSizeState))
                                            pts.addAll(getBrushPoints(sx+x, sy-y, currentPenSizeState))
                                            y += 1
                                            if (err <= 0) err += 2*y + 1
                                            else { x -= 1; err += 2*(y - x) + 1 }
                                        }
                                        currentOnPointsChangeState(currentPointsState + pts)
                                        // Optimize: Send single command
                                        bleManager.sendCommand("CIRCLE:$sx:$sy:$radiusVal:$currentPenSizeState")
                                    }
                                    else -> {}
                                }
                            }
                            startPoint = null
                            currentPoint = null
                        },
                        onDragCancel = {
                            startPoint = null
                            currentPoint = null
                            lastPixel = null
                        }
                    ) { change, _ ->
                        val x = (change.position.x / (scale * density.density)).toInt()
                        val y = (change.position.y / (scale * density.density)).toInt()
                        currentPoint = change.position

                        if (currentToolState == DrawTool.BRUSH || currentToolState == DrawTool.ERASER) {
                            if (x in 0 until oledWidth && y in 0 until oledHeight) {
                                val currentPixel = x to y
                                val pixelsToDraw = if (lastPixel != null) {
                                    interpolateLine(lastPixel!!.first, lastPixel!!.second, x, y)
                                } else {
                                    listOf(currentPixel)
                                }
                                
                                val newPointsToModify = mutableSetOf<Pair<Int, Int>>()
                                pixelsToDraw.forEach { (px, py) ->
                                    newPointsToModify.addAll(getBrushPoints(px, py, currentPenSizeState))
                                }

                                if (currentToolState == DrawTool.ERASER) {
                                    val toRemove = currentPointsState.intersect(newPointsToModify)
                                    if (toRemove.isNotEmpty()) {
                                        currentOnPointsChangeState(currentPointsState - toRemove)
                                        toRemove.forEach { pendingPoints.add(Triple(it.first, it.second, false)) }
                                    }
                                } else {
                                    val toAdd = newPointsToModify.subtract(currentPointsState)
                                    if (toAdd.isNotEmpty()) {
                                        currentOnPointsChangeState(currentPointsState + toAdd)
                                        toAdd.forEach { pendingPoints.add(Triple(it.first, it.second, true)) }
                                    }
                                }
                                lastPixel = currentPixel
                            }
                        }
                    }
                }
        ) {
            // Draw Grid
            for (i in 0..oledWidth) {
                val x = i * scale * density.density
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(x, 0f),
                    end = Offset(x, oledHeight * scale * density.density),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            for (j in 0..oledHeight) {
                val y = j * scale * density.density
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(oledWidth * scale * density.density, y),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // Draw Points
            points.forEach { (px, py) ->
                drawRect(
                    color = Color.White,
                    topLeft = Offset(px * scale * density.density, py * scale * density.density),
                    size = Size(scale * density.density, scale * density.density)
                )
            }
            
            // Preview
            val start = startPoint
            val end = currentPoint
            if (start != null && end != null) {
                val strokeWidthPx = getMappedSize(penSize) * scale * density.density
                when (currentTool) {
                    DrawTool.LINE -> drawLine(Color.White.copy(alpha = 0.5f), start, end, strokeWidth = strokeWidthPx)
                    DrawTool.RECT -> {
                        val left = min(start.x, end.x); val top = min(start.y, end.y)
                        val right = max(start.x, end.x); val bottom = max(start.y, end.y)
                        drawRect(Color.White.copy(alpha = 0.5f), Offset(left, top), Size(right - left, bottom - top), style = Stroke(width = strokeWidthPx))
                    }
                    DrawTool.CIRCLE -> {
                        val radiusVal = hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).toFloat()
                        drawCircle(Color.White.copy(alpha = 0.5f), radiusVal, start, style = Stroke(width = strokeWidthPx))
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun DrawingCanvas(
    bleManager: BleManager, 
    isLandscape: Boolean,
    points: Set<Pair<Int, Int>>,
    onPointsChange: (Set<Pair<Int, Int>>) -> Unit,
    penSize: Float,
    onPenSizeChange: (Float) -> Unit,
    currentTool: DrawTool,
    onToolChange: (DrawTool) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current
    
    // Use rememberUpdatedState to ensure pointerInput blocks always have the latest values
    val currentPointsState by rememberUpdatedState(points)
    val currentOnPointsChangeState by rememberUpdatedState(onPointsChange)
    val currentPenSizeState by rememberUpdatedState(penSize)
    val currentToolState by rememberUpdatedState(currentTool)
    
    val pendingPoints = remember { mutableStateListOf<Triple<Int, Int, Boolean>>() }
    val isConnected by bleManager.isConnected.collectAsState()
    
    val oledWidth = 72
    val oledHeight = 40
    
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    
    val scale = if (isLandscape) {
        val containerSize = windowInfo.containerSize
        // Precise calculation: sidebar (120) + slider (60) + horizontal padding (12*2) + gaps (16*2) + card border (4) = 240dp
        val availableWidth = with(density) { containerSize.width.toDp().value } - 120 - 60 - 24 - 32 - 4
        val availableHeight = with(density) { containerSize.height.toDp().value } - 32 // vertical padding (12*2) + safety
        min(availableWidth / oledWidth, availableHeight / oledHeight)
    } else {
        val containerSize = windowInfo.containerSize
        val availableWidth = with(density) { containerSize.width.toDp().value } - 32
        val availableHeight = with(density) { containerSize.height.toDp().value } - 250 // Leave room for tools
        min(availableWidth / oledWidth, availableHeight / oledHeight)
    }
    
    LaunchedEffect(Unit) {
        if (points.isEmpty()) {
            pendingPoints.clear()
            bleManager.sendCommand("CLEAR")
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream)
            if (decodedBitmap != null) {
                val originalBitmap = if (decodedBitmap.width < decodedBitmap.height) {
                    val matrix = Matrix().apply { postRotate(90f) }
                    Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                } else {
                    decodedBitmap
                }

                val scaleFactor = min(
                    oledWidth.toFloat() / originalBitmap.width,
                    oledHeight.toFloat() / originalBitmap.height
                )
                val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
                val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
                val scaledBitmap = originalBitmap.scale(scaledWidth, scaledHeight, true)

                val finalBitmap = createBitmap(oledWidth, oledHeight, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(finalBitmap)
                canvas.drawColor(AndroidColor.BLACK)
                val left = (oledWidth - scaledWidth) / 2f
                val top = (oledHeight - scaledHeight) / 2f
                canvas.drawBitmap(scaledBitmap, left, top, null)

                val newPoints = mutableSetOf<Pair<Int, Int>>()
                
                scope.launch {
                    pendingPoints.clear()
                    bleManager.sendCommand("CLEAR")
                    
                    // Wait for READY after CLEAR to ensure ESP is ready for the flood
                    var retryCount = 0
                    while (!bleManager.isReady.value && retryCount < 10) {
                        delay(20)
                        retryCount++
                    }

                    // Extract grayscale and apply Floyd-Steinberg dithering
                    val grayValues = Array(oledHeight) { FloatArray(oledWidth) }
                    for (y in 0 until oledHeight) {
                        for (x in 0 until oledWidth) {
                            val pixel = finalBitmap[x, y]
                            grayValues[y][x] = (AndroidColor.red(pixel) * 0.299f + AndroidColor.green(pixel) * 0.587f + AndroidColor.blue(pixel) * 0.114f)
                        }
                    }

                    for (y in 0 until oledHeight) {
                        for (x in 0 until oledWidth) {
                            val oldPixel = grayValues[y][x]
                            val newPixel = if (oldPixel > 128f) 255f else 0f
                            grayValues[y][x] = newPixel
                            val error = oldPixel - newPixel
                            
                            if (x + 1 < oledWidth) grayValues[y][x + 1] += error * 7/16f
                            if (y + 1 < oledHeight) {
                                if (x > 0) grayValues[y + 1][x - 1] += error * 3/16f
                                grayValues[y + 1][x] += error * 5/16f
                                if (x + 1 < oledWidth) grayValues[y + 1][x + 1] += error * 1/16f
                            }
                        }
                    }

                    for (y in 0 until oledHeight) {
                        var rowData = ""
                        for (i in 0 until 9) {
                            var byteVal = 0
                            for (bit in 0 until 8) {
                                val px = i * 8 + bit
                                if (px < oledWidth) {
                                    if (grayValues[y][px] > 128f) {
                                        byteVal = byteVal or (1 shl (7 - bit))
                                        newPoints.add(px to y)
                                    }
                                }
                            }
                            rowData += String.format("%02X", byteVal)
                        }
                        
                        // Wait for flow control
                        while (!bleManager.isReady.value) {
                            delay(5)
                        }
                        bleManager.sendCommand("ROW:$y:$rowData")
                        delay(5) // Small pacing delay
                    }
                    onPointsChange(newPoints)
                    bleManager.sendCommand("UPDATE")
                }
            }
        }
    }
    
    // Cleanup: Single consolidated sync loop for drawing data and heartbeats
    LaunchedEffect(isConnected) {
        if (!isConnected) return@LaunchedEffect
        var lastSyncTime = System.currentTimeMillis()
        while (true) {
            val currentTime = System.currentTimeMillis()
            
            // Priority 1: High-speed drawing data
            if (pendingPoints.isNotEmpty() && bleManager.isReady.value) {
                // Optimized Binary Encoding: [0x01] [x1, y1, on1] [x2, y2, on2] ...
                val batchSize = 40 
                val batch = mutableListOf<Triple<Int, Int, Boolean>>()
                repeat(min(batchSize, pendingPoints.size)) {
                    batch.add(pendingPoints.removeAt(0))
                }
                
                val binaryData = ByteArray(1 + batch.size * 3)
                binaryData[0] = 0x01 // PX_BIN type
                batch.forEachIndexed { index, triple ->
                    val offset = 1 + index * 3
                    binaryData[offset] = triple.first.toByte()
                    binaryData[offset + 1] = triple.second.toByte()
                    binaryData[offset + 2] = (if (triple.third) 1 else 0).toByte()
                }
                bleManager.sendBinaryCommand(binaryData)
                lastSyncTime = currentTime
            } 
            // Priority 2: Heartbeat
            else if (currentTime - lastSyncTime > 5000) {
                if (bleManager.isReady.value) {
                    bleManager.sendCommand("HB")
                    lastSyncTime = currentTime
                }
            }
            
            // Stability delay
            delay(20) 
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Tools and Actions with refined layout and depth
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(140.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tools Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LabelWithIcon(Icons.Default.Brush, "TOOLS")
                        
                        // Pen & Eraser
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = { onToolChange(DrawTool.BRUSH) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (currentTool == DrawTool.BRUSH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (currentTool == DrawTool.BRUSH) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Brush, null)
                            }
                            FilledTonalIconButton(
                                onClick = { onToolChange(DrawTool.ERASER) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (currentTool == DrawTool.ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (currentTool == DrawTool.ERASER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(painterResource(id = R.drawable.inkeraser), null, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Shapes Row
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val shapesList = listOf(
                                DrawTool.LINE to Icons.Default.HorizontalRule,
                                DrawTool.RECT to Icons.Default.Rectangle,
                                DrawTool.CIRCLE to Icons.Default.RadioButtonUnchecked
                            )
                            shapesList.forEach { (tool, icon) ->
                                val selected = currentTool == tool
                                IconButton(
                                    onClick = { onToolChange(tool) },
                                    modifier = Modifier.size(34.dp).background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Icon(
                                        icon, 
                                        null, 
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Image Picker
                        FilledTonalIconButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp))
                                Text("Image", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Actions Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LabelWithIcon(Icons.Default.Bolt, "ACTIONS")
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    onPointsChange(emptySet())
                                    pendingPoints.clear()
                                    bleManager.sendCommand("CLEAR")
                                }, 
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                            
                            FilledTonalIconButton(
                                onClick = { 
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Icon(Icons.Default.ScreenRotation, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }

            // Stroke Size Adjuster: Refined with Surface and stretched
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Removed LineWeight icon indicator at the top
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalSlider(
                            value = penSize,
                            onValueChange = { onPenSizeChange(it) },
                            valueRange = 1f..10f,
                            modifier = Modifier
                                .width(50.dp)
                                .fillMaxHeight()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.clickable {
                            val presets = listOf(1f, 2f, 3f, 5f, 10f)
                            val currentIndex = presets.indexOf(penSize.toInt().toFloat())
                            val nextSize = if (currentIndex == -1 || currentIndex == presets.size - 1) {
                                presets[0]
                            } else {
                                presets[currentIndex + 1]
                            }
                            onPenSizeChange(nextSize)
                        }
                    ) {
                        Text(
                            "${penSize.toInt()}px", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Repositioned OLED Canvas with better frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                OledCanvas(
                    bleManager = bleManager,
                    points = points,
                    onPointsChange = onPointsChange,
                    penSize = penSize,
                    currentTool = currentTool,
                    scale = scale,
                    oledWidth = oledWidth,
                    oledHeight = oledHeight,
                    pendingPoints = pendingPoints
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Drawing Canvas Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp), // Height to fit content (~184dp content)
                        contentAlignment = Alignment.Center
                    ) {
                        OledCanvas(
                            bleManager = bleManager,
                            points = points,
                            onPointsChange = onPointsChange,
                            penSize = penSize,
                            currentTool = currentTool,
                            scale = scale,
                            oledWidth = oledWidth,
                            oledHeight = oledHeight,
                            pendingPoints = pendingPoints
                        )
                    }
                }
            }
            
            // 1. Stroke Size Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelWithIcon(Icons.Default.LineWeight, "STROKE SIZE")
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.clickable {
                                val presets = listOf(1f, 2f, 3f, 5f, 10f)
                                val currentIndex = presets.indexOf(penSize.toInt().toFloat())
                                val nextSize = if (currentIndex == -1 || currentIndex == presets.size - 1) {
                                    presets[0]
                                } else {
                                    presets[currentIndex + 1]
                                }
                                onPenSizeChange(nextSize)
                            }
                        ) {
                            Text(
                                "${penSize.toInt()} px", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Slider(
                        value = penSize,
                        onValueChange = { onPenSizeChange(it) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // 2. Tools & Shapes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LabelWithIcon(Icons.Default.Brush, "TOOLS & SHAPES")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main Tools: Pen, Eraser, Image
                        val mainTools = listOf(
                            DrawTool.BRUSH to Icons.Default.Brush,
                            DrawTool.ERASER to null // Special case for painterResource
                        )
                        
                        mainTools.forEach { (tool, icon) ->
                            val selected = currentTool == tool
                            FilledTonalIconButton(
                                onClick = { onToolChange(tool) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                if (icon != null) {
                                    Icon(icon, null)
                                } else {
                                    Icon(painterResource(id = R.drawable.inkeraser), null, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        
                        // Image Picker
                        FilledTonalIconButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(24.dp))
                        }
                    }
                    
                    // Shapes Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val shapes = listOf(
                            DrawTool.LINE to Icons.Default.HorizontalRule,
                            DrawTool.RECT to Icons.Default.Rectangle,
                            DrawTool.CIRCLE to Icons.Default.RadioButtonUnchecked
                        )
                        
                        shapes.forEach { (tool, icon) ->
                            val selected = currentTool == tool
                            FilterChip(
                                selected = selected,
                                onClick = { onToolChange(tool) },
                                label = { 
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Icon(icon, null, modifier = Modifier.size(20.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            // 3. Quick Actions Row (Clear, Rotate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Clear Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    onClick = {
                        onPointsChange(emptySet())
                        pendingPoints.clear()
                        bleManager.sendCommand("CLEAR")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Canvas", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
                
                // Rotate Card
                Card(
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    onClick = {
                        activity?.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ScreenRotation, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun applyDithering(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val gray = FloatArray(width * height)
    for (i in pixels.indices) {
        val r = (pixels[i] shr 16) and 0xFF
        val g = (pixels[i] shr 8) and 0xFF
        val b = pixels[i] and 0xFF
        gray[i] = (r * 0.299f + g * 0.587f + b * 0.114f)
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val oldPixel = gray[y * width + x]
            val newPixel = if (oldPixel > 128f) 255f else 0f
            gray[y * width + x] = newPixel
            val quantError = oldPixel - newPixel

            if (x + 1 < width) gray[y * width + x + 1] += quantError * 7f / 16f
            if (y + 1 < height) {
                if (x > 0) gray[(y + 1) * width + x - 1] += quantError * 3f / 16f
                gray[(y + 1) * width + x] += quantError * 5f / 16f
                if (x + 1 < width) gray[(y + 1) * width + x + 1] += quantError * 1f / 16f
            }
        }
    }

    val resultPixels = IntArray(width * height)
    for (i in gray.indices) {
        val g = gray[i].toInt().coerceIn(0, 255)
        resultPixels[i] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
    }
    
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(resultPixels, 0, width, 0, 0, width, height)
    return result
}

fun renderFullTextToBitmap(
    text: String,
    fontSize: Int,
    effect: Int,
    useDithering: Boolean = true
): Bitmap {
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.WHITE
        textSize = when (fontSize) {
            0 -> 11f
            1 -> 20f
            else -> 32f
        }
        isAntiAlias = !useDithering
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
    }

    // Measure text width
    val textWidth = paint.measureText(text).toInt().coerceAtLeast(1)
    val oledHeight = 40
    
    // Create a bitmap that fits the entire text
    val bitmap = Bitmap.createBitmap(textWidth, oledHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.BLACK)

    if (effect == 1) { // Inverse
        canvas.drawColor(AndroidColor.WHITE)
        paint.color = AndroidColor.BLACK
    }

    val yPos = (oledHeight / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(text, 0f, yPos, paint)

    if (effect == 2) { // Underline
        val startX = 0f
        val endX = textWidth.toFloat()
        val lineY = yPos + 3f
        paint.strokeWidth = 2f
        canvas.drawLine(startX, lineY, endX, lineY, paint)
    }

    return if (useDithering) applyDithering(bitmap) else {
        // Thresholding
        val pixels = IntArray(textWidth * oledHeight)
        bitmap.getPixels(pixels, 0, textWidth, 0, 0, textWidth, oledHeight)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val gray = (r + g + b) / 3
            pixels[i] = if (gray > 128) AndroidColor.WHITE else AndroidColor.BLACK
        }
        bitmap.setPixels(pixels, 0, textWidth, 0, 0, textWidth, oledHeight)
        bitmap
    }
}

suspend fun sendAnimDataToOled(
    bleManager: BleManager, 
    bitmap: Bitmap, 
    mode: Int, 
    speed: Float
) {
    val width = bitmap.width
    val height = bitmap.height
    val oledPages = height / 8
    
    // speed is 0.2 to 3.0. Map to 1-255 (100 is baseline)
    val speedByte = (speed * 100).toInt().coerceIn(1, 255).toByte()
    
    // Original buffer content for FULL_ANIM_BIN (0x03)
    // 1 mode + 1 speed + 2 width + 1 height + data
    val totalDataSize = 5 + width * oledPages
    val fullData = ByteArray(totalDataSize)
    fullData[0] = mode.toByte()
    fullData[1] = speedByte
    fullData[2] = (width shr 8).toByte()
    fullData[3] = (width and 0xFF).toByte()
    fullData[4] = height.toByte()
    
    for (x in 0 until width) {
        for (page in 0 until oledPages) {
            var byteVal = 0
            for (bit in 0 until 8) {
                val y = page * 8 + bit
                if (y < height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = (r + g + b) / 3
                    if (brightness > 128) {
                        byteVal = byteVal or (1 shl bit)
                    }
                }
            }
            fullData[5 + x * oledPages + page] = byteVal.toByte()
        }
    }

    // Fragmentation Logic
    val maxFragmentSize = 240 // Total packet size ~244 with header
    val totalFragments = ((totalDataSize + maxFragmentSize - 1) / maxFragmentSize).coerceAtLeast(1)
    
    for (f in 0 until totalFragments) {
        val start = f * maxFragmentSize
        val end = (start + maxFragmentSize).coerceAtMost(totalDataSize)
        val fragmentSize = end - start
        
        // Packet: [0x04 (FragCmd), FragID, TotalFrags, DataType(0x03), ... data ...]
        val packet = ByteArray(4 + fragmentSize)
        packet[0] = 0x04 
        packet[1] = f.toByte()
        packet[2] = totalFragments.toByte()
        packet[3] = 0x03 // We are fragmenting a 0x03 command
        System.arraycopy(fullData, start, packet, 4, fragmentSize)
        
        // Wait for flow control if needed
        while (!bleManager.isReady.value) {
            delay(1)
        }
        
        // For the last packet, we might want a small delay or check READY after
        bleManager.sendBinaryCommand(packet)
        
        // If there are more fragments, wait for a small delay to not overwhelm the ESP32
        // even though we have flow control, fragmentation needs to be careful
        if (f < totalFragments - 1) {
            delay(10) 
        }
    }
}

fun renderTextToBitmap(
    text: String, 
    fontSize: Int, 
    effect: Int, 
    xOffset: Int = 0, 
    charLimit: Int = -1,
    useDithering: Boolean = false
): Bitmap {
    val oledWidth = 72
    val oledHeight = 40
    val bitmap = Bitmap.createBitmap(oledWidth, oledHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.BLACK)

    val paint = android.graphics.Paint().apply {
        color = AndroidColor.WHITE
        textSize = when (fontSize) {
            0 -> 11f
            1 -> 20f
            else -> 32f // Big bold feel
        }
        isAntiAlias = !useDithering
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
    }

    if (effect == 1) { // Inverse
        canvas.drawColor(AndroidColor.WHITE)
        paint.color = AndroidColor.BLACK
    }

    val displayText = if (charLimit >= 0 && charLimit <= text.length) text.substring(0, charLimit) else text
    val xPos = (oledWidth / 2f) + xOffset
    val yPos = (oledHeight / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(displayText, xPos, yPos, paint)

    if (effect == 2) { // Underline
        val textWidth = paint.measureText(displayText)
        val startX = xPos - textWidth / 2f
        val endX = xPos + textWidth / 2f
        val lineY = yPos + 3f
        paint.strokeWidth = 2f
        canvas.drawLine(startX, lineY, endX, lineY, paint)
    }

    return if (useDithering) applyDithering(bitmap) else {
        // Crisp thresholding
        val pixels = IntArray(oledWidth * oledHeight)
        bitmap.getPixels(pixels, 0, oledWidth, 0, 0, oledWidth, oledHeight)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val gray = (r + g + b) / 3
            pixels[i] = if (gray > 128) AndroidColor.WHITE else AndroidColor.BLACK
        }
        bitmap.setPixels(pixels, 0, oledWidth, 0, 0, oledWidth, oledHeight)
        bitmap
    }
}

suspend fun sendBitmapToOled(bleManager: BleManager, bitmap: Bitmap) {
    val oledWidth = 72
    val oledHeight = 40
    val oledPages = oledHeight / 8
    
    // Create binary buffer: 1 byte for command type (0x02) + 360 bytes for data
    val buffer = ByteArray(1 + oledWidth * oledPages)
    buffer[0] = 0x02 // FULL_FRAME_BIN command type
    
    for (page in 0 until oledPages) {
        for (x in 0 until oledWidth) {
            var byteVal = 0
            for (bit in 0 until 8) {
                val y = page * 8 + bit
                if (y < oledHeight) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = (r + g + b) / 3
                    if (brightness > 128) {
                        byteVal = byteVal or (1 shl bit)
                    }
                }
            }
            buffer[1 + page * oledWidth + x] = byteVal.toByte()
        }
    }
    
    // Wait for flow control
    while (!bleManager.isReady.value) {
        delay(1)
    }
    bleManager.sendBinaryCommand(buffer)
}

@Composable
fun OledSimulatedPreview(
    text: String,
    fontSize: Int,
    effect: Int,
    animationMode: Int,
    speed: Float = 1.0f
) {
    val oledWidth = 72
    val oledHeight = 40
    
    // Live preview animation states
    var previewOffset by remember { mutableIntStateOf(0) }
    var previewCharLimit by remember { mutableIntStateOf(-1) }
    
    // Pre-calculate full text bitmap for smooth motion and accurate dithering
    val fullTextBitmap = remember(text, fontSize, effect) {
        renderFullTextToBitmap(text, fontSize, effect, useDithering = true)
    }
    
    val textWidth = fullTextBitmap.width

    LaunchedEffect(text, animationMode, speed, textWidth) {
        previewOffset = oledWidth
        previewCharLimit = -1
        
        // Calibrated to match OLED hardware timings provided by user:
        // Fastest (3.0): App 5.4s -> OLED 6.4s
        // Slowest (0.2): App 46s -> OLED 80s
        // Derived formula: delay = (28.5 / speed) - 3.1
        val movingDelay = ((28.5f / speed) - 3.1f).toLong().coerceAtLeast(1L)
        val typingDelay = (235L / speed).toLong().coerceAtLeast(5L)
        
        when (animationMode) {
            1 -> { // Moving (Right to Left)
                while (true) {
                    // Start from right (oledWidth) and go to left (-textWidth)
                    for (offset in oledWidth downTo -textWidth) {
                        previewOffset = offset
                        delay(movingDelay)
                    }
                    delay(500) // Small pause before restart
                }
            }
            2 -> { // Typing
                while (true) {
                    for (i in 0..text.length) {
                        previewCharLimit = i
                        delay(typingDelay)
                    }
                    delay(1000) // Pause at end
                }
            }
            else -> {}
        }
    }

    val displayBitmap = remember(previewOffset, previewCharLimit, animationMode, fullTextBitmap) {
        val bmp = Bitmap.createBitmap(oledWidth, oledHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        canvas.drawColor(AndroidColor.BLACK)
        
        if (animationMode == 1) { // Moving
            // Draw the full text bitmap at the current offset
            canvas.drawBitmap(fullTextBitmap, previewOffset.toFloat(), 0f, null)
        } else if (animationMode == 2) { // Typing
            // For typing, we still need to render partial text
            val partialBmp = renderTextToBitmap(
                text = text,
                fontSize = fontSize,
                effect = effect,
                xOffset = 0,
                charLimit = previewCharLimit,
                useDithering = true
            )
            canvas.drawBitmap(partialBmp, 0f, 0f, null)
        } else { // Static
            val staticBmp = renderTextToBitmap(
                text = text,
                fontSize = fontSize,
                effect = effect,
                xOffset = 0,
                charLimit = -1,
                useDithering = true
            )
            canvas.drawBitmap(staticBmp, 0f, 0f, null)
        }
        bmp
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle glow background
        Box(
            modifier = Modifier
                .size((oledWidth * 4.5).dp, (oledHeight * 4.5).dp)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Canvas(modifier = Modifier.size((oledWidth * 4).dp, (oledHeight * 4).dp)) {
                val pixelSize = size.width / oledWidth
                for (y in 0 until oledHeight) {
                    for (x in 0 until oledWidth) {
                        val pixel = displayBitmap.getPixel(x, y)
                        val color = Color(pixel)
                        if (color != Color.Black) {
                            // Only draw glow for non-black pixels
                            drawRect(
                                color = color.copy(alpha = 0.3f),
                                topLeft = Offset(x * pixelSize - 1f, y * pixelSize - 1f),
                                size = Size(pixelSize + 2f, pixelSize + 2f)
                            )
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(x * pixelSize, y * pixelSize),
                            size = Size(pixelSize, pixelSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GenericOledSimulator(
    modifier: Modifier = Modifier,
    animationType: String = "STARS",
    use24HourFormat: Boolean = true,
    customScript: String = ""
) {
    val oledWidth = 72
    val oledHeight = 40
    
    // Animation state
    var tick by remember { mutableIntStateOf(0) }
    
    // Script engine
    val engine = remember { com.ghosttap.app.logic.AnimationScriptEngine() }
    
    LaunchedEffect(animationType) {
        while (true) {
            tick++
            delay(40) // ~25 FPS for smoother look
        }
    }

    val displayBitmap = remember(tick, animationType, customScript) {
        val bmp = Bitmap.createBitmap(oledWidth, oledHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        
        // For custom script, we don't clear black immediately as the engine handles logic
        // But for consistency with other modes, let's start black
        canvas.drawColor(AndroidColor.BLACK)
        
        val paint = android.graphics.Paint().apply { 
            color = AndroidColor.WHITE
            isAntiAlias = false
        }

        when {
            animationType == "CUSTOM_AI" -> {
                // Use the Compose Canvas wrapper for the engine if possible, 
                // but our engine expects Compose Canvas.
                // We need to bridge Android Canvas to Compose Canvas or adapt engine.
                // Adapting engine to use Android Canvas is easier or wrap it.
                val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
                engine.renderFrame(composeCanvas, customScript, tick, oledWidth, oledHeight)
            }
            animationType.startsWith("CLOCK:") -> {
                val id = animationType.removePrefix("CLOCK:")
                val time = java.util.Calendar.getInstance()
                val h24 = time.get(java.util.Calendar.HOUR_OF_DAY)
                val m = time.get(java.util.Calendar.MINUTE)
                val s = time.get(java.util.Calendar.SECOND)
                
                val hDisplay = if (use24HourFormat) h24 else {
                    val h12 = h24 % 12
                    if (h12 == 0) 12 else h12
                }
                
                val cx = oledWidth / 2f
                val cy = oledHeight / 2f
                
                when(id) {
                    "MINIMAL" -> { // Minimal Analog - Fixed Edges
                        // Hour Indicators on Edges
                        paint.style = android.graphics.Paint.Style.FILL
                        for (i in 0 until 12) {
                            val angle = (i * Math.PI / 6) - Math.PI / 2
                            val dx = Math.cos(angle)
                            val dy = Math.sin(angle)
                            
                            // Project to rectangle edges
                            val xEdge: Float
                            val yEdge: Float
                            
                            // Check which edge it hits
                            if (Math.abs(dx * 19.5) > Math.abs(dy * 35.5)) {
                                // Hits left or right edge
                                xEdge = if (dx > 0) 71f else 0f
                                yEdge = (cy + (dy * (if (dx > 0) 35.5 else -35.5) / dx)).toFloat()
                            } else {
                                // Hits top or bottom edge
                                yEdge = if (dy > 0) 39f else 0f
                                xEdge = (cx + (dx * (if (dy > 0) 19.5 else -19.5) / dy)).toFloat()
                            }
                            
                            // Draw a small line inward from the edge with a gap
                            // Gap of 2 pixels from the edge, length of 6 pixels
                            val xStart = (xEdge - 2 * dx).toFloat()
                            val yStart = (yEdge - 2 * dy).toFloat()
                            val xEnd = (xEdge - 8 * dx).toFloat()
                            val yEnd = (yEdge - 8 * dy).toFloat()
                            canvas.drawLine(xStart, yStart, xEnd, yEnd, paint)
                        }

                        // Hands - Long and Hour is Bold
                        val ma = (m * Math.PI / 30) - Math.PI / 2
                        val ha = ((hDisplay % 12) * Math.PI / 6) + (m * Math.PI / 360) - Math.PI / 2
                        
                        // Minute hand (Long)
                        canvas.drawLine(cx, cy, (cx + 18 * Math.cos(ma)).toFloat(), (cy + 18 * Math.sin(ma)).toFloat(), paint)
                        
                        // Hour hand (Bold and Long-ish)
                        val oldWidth = paint.strokeWidth
                        paint.strokeWidth = 3f
                        canvas.drawLine(cx, cy, (cx + 12 * Math.cos(ha)).toFloat(), (cy + 12 * Math.sin(ha)).toFloat(), paint)
                        paint.strokeWidth = oldWidth

                        // Seconds Filling/Disappearing Border Trail (2-minute cycle)
                        val perimeter = 220f
                        val totalLen = if (m % 2 == 0) {
                            (s / 60f) * perimeter
                        } else {
                            (1f - (s / 60f)) * perimeter
                        }

                        var currentLen = 0f
                        // Top (0,0) to (71,0)
                        if (currentLen < totalLen) {
                            val len = Math.min(71f, totalLen - currentLen)
                            canvas.drawLine(0f, 0f, len, 0f, paint)
                            currentLen += 71
                        }
                        // Right (71,0) to (71,39)
                        if (currentLen < totalLen) {
                            val len = Math.min(39f, totalLen - currentLen)
                            canvas.drawLine(71f, 0f, 71f, len, paint)
                            currentLen += 39
                        }
                        // Bottom (71,39) to (0,39)
                        if (currentLen < totalLen) {
                            val len = Math.min(71f, totalLen - currentLen)
                            canvas.drawLine(71f, 39f, 71f - len, 39f, paint)
                            currentLen += 71
                        }
                        // Left (0,39) to (0,0)
                        if (currentLen < totalLen) {
                            val len = Math.min(39f, totalLen - currentLen)
                            canvas.drawLine(0f, 39f, 0f, 39f - len, paint)
                        }
                    }
                    "CYBER" -> { // Cyberpunk - Fixed Formatting
                        paint.style = android.graphics.Paint.Style.STROKE
                        // Border on very outer pixels
                        canvas.drawRect(0f, 0f, 71f, 39f, paint)
                        canvas.drawLine(0f, 24f, 71f, 24f, paint)
                        
                        paint.style = android.graphics.Paint.Style.FILL
                        // Reduced spacing and size to prevent overflow
                        paint.textSize = 18f 
                        val hStr = "${hDisplay}"
                        val mStr = "${m}"
                        val sep = ":"
                        
                        // Manual positioning to control gaps
                        canvas.drawText(hStr, 8f, 20f, paint)
                        canvas.drawText(sep, 30f, 20f, paint)
                        canvas.drawText(mStr, 45f, 20f, paint)
                        
                        paint.textSize = 8f
                        canvas.drawText("SEC ${String.format("%02d", s)}", 35f, 34f, paint)
                        if (tick % 20 < 2) {
                            val ry = (0..40).random().toFloat()
                            canvas.drawLine(0f, ry, 72f, ry, paint)
                        }
                    }
                    "BIG_HOUR" -> { // Big Hour - Bold
                        paint.isFakeBoldText = true
                        paint.textSize = 28f
                        canvas.drawText("${hDisplay}", 5f, 24f, paint)
                        paint.isFakeBoldText = false
                        paint.textSize = 18f
                        canvas.drawText(String.format("%02d", m), 45f, 36f, paint)
                        // Diagonal line
                        canvas.drawLine(0f, 40f, 72f, 0f, paint)
                    }
                    "MODERN" -> { // Modernist - Dynamic Size & Bold
                        val timeStr = String.format("%d:%02d", hDisplay, m)
                        val baseSize = if (timeStr.length > 4) 28f else 34f
                        paint.textSize = baseSize
                        paint.isFakeBoldText = true
                        
                        val bounds = android.graphics.Rect()
                        paint.getTextBounds(timeStr, 0, timeStr.length, bounds)
                        canvas.drawText(timeStr, (oledWidth - bounds.width()) / 2f, 32f, paint)
                        paint.isFakeBoldText = false
                    }
                    else -> {
                        paint.textSize = 12f
                        val timeStr = String.format("%02d:%02d", hDisplay, m)
                        canvas.drawText(timeStr, cx - 15f, cy + 4f, paint)
                    }
                }
            }
            animationType.startsWith("MUSIC:VIZ:") -> {
                val modeId = animationType.removePrefix("MUSIC:VIZ:")
                val bars = 12
                val barW = 4
                val spacing = 2
                
                // Use a stable random for flicker but keep it dynamic
                val rand = java.util.Random(tick.toLong() / 2)
                
                for (i in 0 until bars) {
                    val target = when(modeId) {
                        "BARS" -> rand.nextInt(30)
                        "WAVE" -> (15 + 15 * Math.sin(i * 0.5 + tick * 0.2)).toInt()
                        "PEAK" -> { // Peak meter
                            var t = (tick + i * 5) % 35
                            if (t > 30) t = 30
                            t
                        }
                        else -> rand.nextInt(30)
                    }
                    
                    // Simple smoothing in the app simulation
                    val h = target 
                    
                    val x = i * (barW + spacing)
                    if (modeId == "PEAK") { // Centered
                        canvas.drawRect(x.toFloat(), (oledHeight - h) / 2f, (x + barW).toFloat(), (oledHeight + h) / 2f, paint)
                    } else {
                        canvas.drawRect(x.toFloat(), (oledHeight - h).toFloat(), (x + barW).toFloat(), oledHeight.toFloat(), paint)
                    }
                }
            }
            else -> {
                val name = animationType.removePrefix("ANIM:")
                when(name) {
                    "STARS" -> {
                        val rand = java.util.Random(42)
                        for (i in 0 until 20) {
                            val speed = 1.5f
                            val z = ((100 - (tick * speed + i * 5)) % 100).coerceAtLeast(1f)
                            val x = (oledWidth / 2) + ((rand.nextInt(100) - 50) * 50 / z).toInt()
                            val y = (oledHeight / 2) + ((rand.nextInt(100) - 50) * 50 / z).toInt()
                            if (x in 0 until oledWidth && y in 0 until oledHeight) {
                                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                            }
                        }
                    }
                    "WAVES" -> {
                        for (x in 0 until oledWidth) {
                            val phase = tick * 0.1f
                            val y1 = 20 + (8 * sin(x * 0.15 + phase)).toInt()
                            val y2 = 20 + (6 * sin(x * 0.1 - phase * 0.8)).toInt()
                            canvas.drawPoint(x.toFloat(), y1.toFloat(), paint)
                            canvas.drawPoint(x.toFloat(), y2.toFloat(), paint)
                            if (x % 2 == 0) canvas.drawLine(x.toFloat(), y1.toFloat(), x.toFloat(), y2.toFloat(), paint)
                        }
                    }
                    "DNA" -> {
                        for (y in 0 until oledHeight step 2) {
                            val angle = y * 0.2 + tick * 0.15
                            val x1 = (oledWidth / 2) + (15 * sin(angle)).toInt()
                            val x2 = (oledWidth / 2) - (15 * sin(angle)).toInt()
                            canvas.drawPoint(x1.toFloat(), y.toFloat(), paint)
                            canvas.drawPoint(x2.toFloat(), y.toFloat(), paint)
                            if (y % 8 == 0) canvas.drawLine(x1.toFloat(), y.toFloat(), x2.toFloat(), y.toFloat(), paint)
                        }
                    }
                    "FIREWORKS" -> {
                        val rand = java.util.Random(tick.toLong() / 20)
                        val fx = rand.nextInt(oledWidth).toFloat()
                        val fy = rand.nextInt(oledHeight).toFloat()
                        val phase = tick % 20
                        for (i in 0 until 12) {
                            val a = i * PI / 6 + tick * 0.1
                            val r = phase * 1.5
                            canvas.drawPoint((fx + r * cos(a)).toFloat(), (fy + r * sin(a)).toFloat(), paint)
                        }
                    }
                    "TUNNEL" -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        for (i in 0 until 4) {
                            val r = ((tick + i * 15) % 60).toFloat()
                            if (r < 40) canvas.drawCircle(oledWidth / 2f, oledHeight / 2f, r, paint)
                        }
                    }
                    "SNOW" -> {
                        val rand = java.util.Random(42)
                        for (i in 0 until 15) {
                            val x = (rand.nextInt(oledWidth) + (if (tick % 2 == 0) rand.nextInt(3) - 1 else 0)) % oledWidth
                            val y = (tick / 2 + rand.nextInt(oledHeight)) % oledHeight
                            canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                        }
                    }
                    "VORTEX" -> {
                        for (i in 0 until 20) {
                            val a = i * 0.5 + tick * 0.2
                            val r = i * 1.5
                            canvas.drawPoint((oledWidth / 2 + r * cos(a)).toFloat(), (oledHeight / 2 + r * sin(a)).toFloat(), paint)
                        }
                    }
                    "CUBE" -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        val cx = oledWidth / 2f
                        val cy = oledHeight / 2f
                        val a = tick * 0.05
                        val sz = 12f
                        val pts = arrayOf(
                            floatArrayOf(-1f,-1f,-1f), floatArrayOf(1f,-1f,-1f), floatArrayOf(1f,1f,-1f), floatArrayOf(-1f,1f,-1f),
                            floatArrayOf(-1f,-1f,1f), floatArrayOf(1f,-1f,1f), floatArrayOf(1f,1f,1f), floatArrayOf(-1f,1f,1f)
                        )
                        val x = IntArray(8)
                        val y = IntArray(8)
                        for (i in 0 until 8) {
                            val px = pts[i][0]
                            val py = pts[i][1]
                            val pz = pts[i][2]
                            val nx = px * cos(a) - pz * sin(a)
                            val nz = px * sin(a) + pz * cos(a)
                            val ny = py * cos(a * 0.7) - nz * sin(a * 0.7)
                            x[i] = (cx + nx * sz).toInt()
                            y[i] = (cy + ny * sz).toInt()
                        }
                        for (i in 0 until 4) {
                            canvas.drawLine(x[i].toFloat(), y[i].toFloat(), x[(i + 1) % 4].toFloat(), y[(i + 1) % 4].toFloat(), paint)
                            canvas.drawLine(x[i + 4].toFloat(), y[i + 4].toFloat(), x[((i + 1) % 4) + 4].toFloat(), y[((i + 1) % 4) + 4].toFloat(), paint)
                            canvas.drawLine(x[i].toFloat(), y[i].toFloat(), x[i + 4].toFloat(), y[i + 4].toFloat(), paint)
                        }
                    }
                    "RINGS" -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        for (i in 0 until 3) {
                            val r = 10 + i * 8 + (3 * sin(tick * 0.1 + i)).toFloat()
                            canvas.drawCircle(oledWidth / 2f, oledHeight / 2f, r, paint)
                        }
                    }
                    "GLITCH" -> {
                        if (tick % 5 == 0) {
                            val rand = java.util.Random()
                            for (i in 0 until 5) {
                                canvas.drawRect(rand.nextInt(oledWidth).toFloat(), rand.nextInt(oledHeight).toFloat(), (rand.nextInt(oledWidth) + 20).toFloat(), (rand.nextInt(oledHeight) + 2).toFloat(), paint)
                            }
                        } else {
                            paint.textSize = 14f
                            canvas.drawText("ERROR", 15f, 25f, paint)
                            if (tick % 2 == 0) {
                                val ry = (0 until oledHeight).random().toFloat()
                                canvas.drawLine(0f, ry, oledWidth.toFloat(), ry, paint)
                            }
                        }
                    }
                    "RAIN" -> {
                        val rand = java.util.Random(42)
                        for (i in 0 until 12) {
                            val x = i * 6 + 2
                            val startY = (tick + rand.nextInt(40)) % 44 - 4
                            canvas.drawLine(x.toFloat(), startY.toFloat(), x.toFloat(), (startY + 4).toFloat(), paint)
                        }
                    }
                    "PLASMA" -> {
                        for (x in 0 until oledWidth step 2) {
                            for (y in 0 until oledHeight step 2) {
                                val v = Math.sin(x * 0.1 + tick * 0.1) + Math.sin(y * 0.1 + tick * 0.1) + Math.sin((x + y) * 0.1 + tick * 0.1)
                                if (v > 1.0) canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                            }
                        }
                    }
                    "CIRCLES" -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        for (i in 0 until 4) {
                            val r = ((tick + i * 10) % 40).toFloat()
                            canvas.drawCircle(36f, 20f, r, paint)
                        }
                    }
                    "NOISE" -> {
                        val rand = java.util.Random()
                        for (i in 0 until 100) {
                            canvas.drawPoint(rand.nextInt(oledWidth).toFloat(), rand.nextInt(oledHeight).toFloat(), paint)
                        }
                    }
                    "SWARM" -> {
                        val tx = 36 + 20 * Math.cos(tick * 0.05)
                        val ty = 20 + 15 * Math.sin(tick * 0.07)
                        canvas.drawPoint(tx.toFloat(), ty.toFloat(), paint)
                        val rand = java.util.Random(42)
                        for (i in 0 until 10) {
                            val px = (tx + Math.sin(tick * 0.1 + i) * 10 + rand.nextInt(3) - 1).toFloat()
                            val py = (ty + Math.cos(tick * 0.1 + i) * 5 + rand.nextInt(3) - 1).toFloat()
                            canvas.drawPoint(px, py, paint)
                        }
                    }
                    "WAVE2" -> {
                        for (x in 0 until oledWidth) {
                            val y = 20 + 10 * Math.sin(x * 0.1 + tick * 0.2) * Math.cos(tick * 0.05)
                            canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                        }
                    }
                    "METABALLS" -> {
                        val x1 = 36 + 15 * Math.cos(tick * 0.1)
                        val y1 = 20 + 10 * Math.sin(tick * 0.15)
                        val x2 = 36 + 15 * Math.sin(tick * 0.08)
                        val y2 = 20 + 10 * Math.cos(tick * 0.12)
                        for (x in 0 until oledWidth step 3) {
                            for (y in 0 until oledHeight step 3) {
                                val d1 = (x - x1) * (x - x1) + (y - y1) * (y - y1)
                                val d2 = (x - x2) * (x - x2) + (y - y2) * (y - y2)
                                if (100.0 / d1 + 100.0 / d2 > 0.5) {
                                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + 2).toFloat(), (y + 2).toFloat(), paint)
                                }
                            }
                        }
                    }
                    else -> {
                        val r = (10 + Math.abs(Math.sin(tick * 0.1) * 15)).toFloat()
                        paint.style = android.graphics.Paint.Style.FILL
                        canvas.drawCircle(oledWidth / 2f, oledHeight / 2f, r, paint)
                    }
                }
            }
        }
        bmp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(top = 0.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(300.dp, 160.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        
        // The "Screen" container
        Surface(
            color = Color(0xFF050505),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(4.dp)
        ) {
            // Draw the pixel grid
            Canvas(modifier = Modifier.size(252.dp, 140.dp)) { // 72*3.5, 40*3.5
                val cols = 72
                val rows = 40
                val gap = 1f
                val cellW = (size.width - (cols - 1) * gap) / cols
                val cellH = (size.height - (rows - 1) * gap) / rows
                
                // Draw background pixels (the "off" state)
                for (y in 0 until rows) {
                    for (x in 0 until cols) {
                        val px = displayBitmap.getPixel(x, y)
                        val isHigh = px != AndroidColor.BLACK
                        
                        val color = if (isHigh) {
                            Color(0xFFE0E0E0) // Bright OLED Blue/White
                        } else {
                            Color(0xFF1A1A1A) // Very dim "off" pixel
                        }
                        
                        // Add glow for "on" pixels
                        if (isHigh) {
                            drawRect(
                                color = color.copy(alpha = 0.3f),
                                topLeft = Offset(x * (cellW + gap) - 1f, y * (cellH + gap) - 1f),
                                size = Size(cellW + 2f, cellH + 2f)
                            )
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(x * (cellW + gap), y * (cellH + gap)),
                            size = Size(cellW, cellH)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextControl(
    bleManager: BleManager,
    text: String, onTextChange: (String) -> Unit,
    fontSize: Int, onFontSizeChange: (Int) -> Unit,
    effect: Int, onEffectChange: (Int) -> Unit,
    animationMode: Int, onAnimationModeChange: (Int) -> Unit,
    animSpeed: Float, onAnimSpeedChange: (Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }
    var sendStatusMessage by remember { mutableStateOf("") }
    
    // Character limit logic
    val charLimit = if (animationMode == 0) {
        when (fontSize) {
            2 -> 4
            1 -> 6
            else -> 10
        }
    } else Int.MAX_VALUE

    // Auto-send timer
    LaunchedEffect(text, fontSize, effect, animationMode, animSpeed) {
        if (text.isNotEmpty() && !isSending) {
            delay(5000) // 5 seconds delay
            if (!isSending && text.isNotEmpty()) { // Check again after delay
                scope.launch {
                    isSending = true
                    sendStatusMessage = ""
                    try {
                        val fullBitmap = renderFullTextToBitmap(text, fontSize, effect, useDithering = true)
                        sendAnimDataToOled(bleManager, fullBitmap, animationMode, animSpeed)
                        sendStatusMessage = "SENT"
                        delay(1500)
                        sendStatusMessage = ""
                    } catch (e: Exception) {
                        sendStatusMessage = "FAILED"
                        delay(1500)
                        sendStatusMessage = ""
                    } finally {
                        isSending = false
                    }
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Pixelated Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp), // Height to fit content (~184dp content)
                    contentAlignment = Alignment.Center
                ) {
                    OledSimulatedPreview(text, fontSize, effect, animationMode, speed = animSpeed)
                }
            }
        }

        // Input Field Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelWithIcon(Icons.AutoMirrored.Filled.Message, "TEXT MESSAGE")
                    if (animationMode == 0) {
                        Surface(
                            color = if (text.length > charLimit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                "${text.length}/$charLimit",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (text.length > charLimit) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { onTextChange(it) },
                    visualTransformation = { 
                        val annotatedString = buildAnnotatedString {
                            for (i in text.indices) {
                                if (animationMode == 0 && i >= charLimit) {
                                    withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                        append(text[i])
                                    }
                                } else {
                                    append(text[i])
                                }
                            }
                        }
                        androidx.compose.ui.text.input.TransformedText(
                            annotatedString,
                            androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    },
                    placeholder = { Text("Type something fun!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { onTextChange("") }) {
                                Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                )
            }
        }

        // Configuration Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Size Column
            Card(
                modifier = Modifier.weight(0.4f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabelWithIcon(Icons.Default.TextFields, "SIZE")
                    listOf("S", "M", "H").forEachIndexed { index, label ->
                        val selected = fontSize == index
                        Surface(
                            onClick = { onFontSizeChange(index) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Anim & Effects Column
            Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabelWithIcon(Icons.Default.Animation, "ANIMATION")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Icons.Default.Stop, Icons.AutoMirrored.Filled.CompareArrows, Icons.Default.Keyboard).forEachIndexed { index, icon ->
                                val selected = animationMode == index
                                IconButton(
                                    onClick = { onAnimationModeChange(index) },
                                    modifier = Modifier.weight(1f).height(48.dp).background(
                                        if (selected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                ) {
                                    Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabelWithIcon(Icons.Default.AutoFixHigh, "EFFECTS")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Icons.Default.Block, Icons.Default.InvertColors, Icons.Default.FormatUnderlined).forEachIndexed { index, icon ->
                                val selected = effect == index
                                IconButton(
                                    onClick = { onEffectChange(index) },
                                    modifier = Modifier.weight(1f).height(48.dp).background(
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                ) {
                                    Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (animationMode != 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelWithIcon(Icons.Default.Speed, "SPEED")
                        Text(
                            "${(animSpeed * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = animSpeed,
                        onValueChange = onAnimSpeedChange,
                        valueRange = 0.2f..3.0f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }

        // Send & Clear Buttons
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isSending) return@Button
                    scope.launch {
                        isSending = true
                        sendStatusMessage = ""
                        try {
                            val fullBitmap = renderFullTextToBitmap(text, fontSize, effect, useDithering = true)
                            sendAnimDataToOled(bleManager, fullBitmap, animationMode, animSpeed)
                            
                            sendStatusMessage = "SENT"
                            delay(1500)
                            sendStatusMessage = ""
                        } catch (e: Exception) {
                            sendStatusMessage = "FAILED"
                            delay(1500)
                            sendStatusMessage = ""
                        } finally {
                            isSending = false
                        }
                    }
                },
                enabled = text.isNotEmpty(),
                modifier = Modifier.weight(3f).fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (sendStatusMessage.isNotEmpty()) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(28.dp))
                        Text(sendStatusMessage, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                    } else if (isSending) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp), strokeWidth = 4.dp)
                        Text("SENDING...", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(32.dp))
                        Text("SEND", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp))
                    }
                }
            }

            // Clear Button
            Button(
                onClick = { bleManager.sendCommand("CLEAR") },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LabelWithIcon(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    bleManager: BleManager,
    isVisualizerVisible: Boolean,
    onSetVisualizerVisible: (Boolean) -> Unit,
    onCommandSent: (String) -> Unit,
    currentSimAnim: String,
    onSimAnimChange: (String) -> Unit,
    selectedSection: String,
    onSectionChange: (String) -> Unit,
    use24HourFormat: Boolean,
    onTimeFormatChange: (Boolean) -> Unit
) {
    var selectedAnimation by remember { mutableStateOf("") }
    var selectedWatchFace by remember { mutableStateOf("") }
    var selectedMusicViz by remember { mutableStateOf("") }
    var selectedSpecial by remember { mutableStateOf("") }
    
    // Sync item selection with current simulator state to ensure persistence
    LaunchedEffect(currentSimAnim) {
        if (currentSimAnim.isEmpty()) {
            selectedAnimation = ""
            selectedWatchFace = ""
            selectedMusicViz = ""
            selectedSpecial = ""
            return@LaunchedEffect
        }
        
        when {
            currentSimAnim.startsWith("CLOCK:") -> {
                val faceId = currentSimAnim.removePrefix("CLOCK:")
                selectedWatchFace = faceId
                selectedMusicViz = ""
                selectedAnimation = ""
                selectedSpecial = ""
                onSectionChange("CLOCK")
            }
            currentSimAnim.startsWith("MUSIC:VIZ:") -> {
                val vizId = currentSimAnim.removePrefix("MUSIC:VIZ:")
                selectedMusicViz = vizId
                selectedWatchFace = ""
                selectedAnimation = ""
                selectedSpecial = ""
                onSectionChange("MUSIC VIZ")
            }
            currentSimAnim.startsWith("SNAKE:") -> {
                val snakeId = currentSimAnim.removePrefix("SNAKE:")
                selectedSpecial = snakeId
                selectedAnimation = ""
                selectedWatchFace = ""
                selectedMusicViz = ""
                onSectionChange("ANIMATIONS")
            }
            currentSimAnim == "CUSTOM_AI" -> {
                selectedAnimation = ""
                selectedWatchFace = ""
                selectedMusicViz = ""
                selectedSpecial = ""
                onSectionChange("AI")
            }
            else -> {
                // If it starts with "ANIM:", remove it to get the ID for highlighting
                val animId = if (currentSimAnim.startsWith("ANIM:")) {
                    currentSimAnim.removePrefix("ANIM:")
                } else {
                    currentSimAnim
                }
                selectedAnimation = animId
                selectedWatchFace = ""
                selectedMusicViz = ""
                selectedSpecial = ""
                onSectionChange("ANIMATIONS")
            }
        }
    }

    // Load AI scripts
    val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val gson = com.google.gson.Gson()
    var savedScripts by remember {
        mutableStateOf<List<Pair<String, String>>>(emptyList())
    }
    var currentScript by remember { mutableStateOf<String>("") }
    
    // Load saved scripts initially
    LaunchedEffect(Unit) {
        val json = prefs.getString("saved_ai_scripts", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<Pair<String, String>>>() {}.type
        savedScripts = try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Collapsible Visualizer (GenericOledSimulator)
        AnimatedVisibility(
            visible = isVisualizerVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            GenericOledSimulator(
                animationType = currentSimAnim,
                use24HourFormat = use24HourFormat,
                customScript = currentScript,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // Horizontal Tabs Row Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sections = listOf("CLOCK", "MUSIC VIZ", "ANIMATIONS", "AI")
                sections.forEach { section ->
                    val isSelected = selectedSection == section
                    Surface(
                        onClick = { onSectionChange(section) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Content Area based on selected section
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedSection) {
                    "CLOCK" -> {
                        // 12/24h Format Toggle
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            onClick = {
                                val newVal = !use24HourFormat
                                onTimeFormatChange(newVal)
                                bleManager.sendCommand(if (newVal) "TIME:FMT:24" else "TIME:FMT:12")
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WatchLater,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Time Format",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            if (use24HourFormat) "24-Hour Clock" else "12-Hour Clock",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = use24HourFormat,
                                    onCheckedChange = {
                                        onTimeFormatChange(it)
                                        bleManager.sendCommand(if (it) "TIME:FMT:24" else "TIME:FMT:12")
                                    }
                                )
                            }
                        }

                        val faces = listOf(
                            "MINIMAL" to "Minimal Analog",
                            "CYBER" to "Cyber Tech",
                            "BIG_HOUR" to "Big Hour",
                            "MODERN" to "Modernist"
                        )

                        AdaptiveGrid(items = faces, columns = 2) { (id, name) ->
                            WatchFaceItem(
                                id = id,
                                name = name,
                                isSelected = selectedWatchFace == id,
                                onClick = { 
                                    if (selectedWatchFace == id) return@WatchFaceItem
                                    val cmd = DeviceCommand.Clock(id)
                                    onSimAnimChange(cmd.simulatorId)
                                    bleManager.sendCommand(cmd.command)
                                    onCommandSent(cmd.command)
                                }
                            )
                        }
                    }

                    "MUSIC VIZ" -> {
                        val vizItems = listOf(
                            "BARS" to "Classic Bars",
                            "WAVE" to "Fluid Wave",
                            "PEAK" to "Peak Meter"
                        )

                        AdaptiveGrid(items = vizItems, columns = 2) { (id, name) ->
                            MusicVizItem(
                                name = name,
                                id = id,
                                isSelected = selectedMusicViz == id,
                                onClick = { 
                                    if (selectedMusicViz == id) return@MusicVizItem
                                    val cmd = DeviceCommand.MusicViz(id)
                                    onSimAnimChange(cmd.simulatorId)
                                    bleManager.sendCommand(cmd.command)
                                    onCommandSent(cmd.command)
                                }
                            )
                        }
                    }

                    "ANIMATIONS" -> {
                        val animations = listOf(
                            "STARS" to "Starfield",
                            "WAVES" to "Ocean Waves",
                            "DNA" to "DNA Helix",
                            "FIREWORKS" to "Fireworks",
                            "TUNNEL" to "Warp Tunnel",
                            "SNOW" to "Snow Fall",
                            "VORTEX" to "Vortex",
                            "CUBE" to "3D Cube",
                            "RINGS" to "Sonic Rings",
                            "GLITCH" to "Glitch Art",
                            "RAIN" to "Matrix Rain",
                            "PLASMA" to "Plasma",
                            "CIRCLES" to "Orbital",
                            "NOISE" to "Static Noise",
                            "SWARM" to "Swarm",
                            "WAVE2" to "Sinusoid",
                            "METABALLS" to "Metaballs"
                        )

                        AdaptiveGrid(items = animations, columns = 1) { (id, name) ->
                            AnimationItem(
                                id = id,
                                name = name,
                                isSelected = selectedAnimation == id,
                                onClick = { 
                                    if (selectedAnimation == id) return@AnimationItem
                                    val cmd = DeviceCommand.BootAnim(id)
                                    onSimAnimChange(cmd.simulatorId)
                                    bleManager.sendCommand(cmd.command)
                                    onCommandSent(cmd.command)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Special",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )

                        val specialItems = listOf(
                            "1X1" to "Snake AI (1x1)",
                            "2X2" to "Snake AI (2x2)"
                        )

                        AdaptiveGrid(items = specialItems, columns = 1) { (id, name) ->
                            Card(
                                onClick = {
                                    if (selectedSpecial == id) return@Card
                                    val cmd = DeviceCommand.Snake(id)
                                    onSimAnimChange(cmd.simulatorId)
                                    bleManager.sendCommand(cmd.command)
                                    onCommandSent(cmd.command)
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedSpecial == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = if (selectedSpecial == id) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (selectedSpecial == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (id == "1X1") Icons.Default.Timeline else Icons.Default.TrackChanges,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = if (selectedSpecial == id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSpecial == id) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (id == "1X1") "Classic Snake AI" else "Rectangle Snake AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedSpecial == id) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "AI" -> {
                        // AI Tab Logic
                        var aiSubTab by remember { mutableStateOf(0) } // 0: Created, 1: Create
                        var prompt by remember { mutableStateOf("") }
                        var isGenerating by remember { mutableStateOf(false) }
                        var generatedScript by remember { mutableStateOf<String?>(null) }
                        
                        val scope = rememberCoroutineScope()
                        val context = LocalContext.current
                        val geminiService = remember { com.ghosttap.app.data.GeminiService() }
                        
                        // Settings (API Key & Model)
                        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                        val model = prefs.getString("gemini_model", "gemini-3-flash-preview") ?: "gemini-3-flash-preview"
                        val timeoutSeconds = prefs.getFloat("gemini_timeout", 30f).toLong()
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Sub-tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Created", "Create").forEachIndexed { index, title ->
                                    val isSelected = aiSubTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { aiSubTab = index }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (aiSubTab == 0) {
                                // Created List
                                if (savedScripts.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No AI animations created yet.\nGo to 'Create' tab to make one!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    val clipboardManager = LocalClipboardManager.current
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        savedScripts.forEach { (name, script) ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        // 1. Preview on Phone
                                                        currentScript = script
                                                        onSimAnimChange("CUSTOM_AI")
                                                        onSetVisualizerVisible(true)

                                                        // 2. Play on Device (Smooth)
                                                        scope.launch {
                                                            try {
                                                                bleManager.sendCommand("SCRIPT_START")
                                                                delay(100)
                                                                // Split script into meaningful lines for the ESP parser
                                                                val lines = script.replace("{", ";{")
                                                                    .replace("}", ";}")
                                                                    .split(";")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotEmpty() }
                                                                
                                                                lines.forEach { line ->
                                                                    while (!bleManager.isReady.value) { delay(5) }
                                                                    bleManager.sendCommand("SCRIPT_LINE:$line")
                                                                    delay(10)
                                                                }
                                                                bleManager.sendCommand("SCRIPT_RUN")
                                                                android.widget.Toast.makeText(context, "Playing '$name' on device...", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "Failed to send script", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Surface(
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                                shape = CircleShape,
                                                                modifier = Modifier.size(44.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column {
                                                                Text(
                                                                    name, 
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    "C-Style Animation", 
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                        
                                                        Row {
                                                            IconButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString(script))
                                                                    android.widget.Toast.makeText(context, "Code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                                                },
                                                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                                            ) {
                                                                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val newList = savedScripts.toMutableList()
                                                                    newList.removeIf { it.first == name && it.second == script }
                                                                    savedScripts = newList
                                                                    val jsonStr = gson.toJson(newList)
                                                                    prefs.edit().putString("saved_ai_scripts", jsonStr).apply()
                                                                },
                                                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                                            ) {
                                                                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    
                                                    // Code snippet preview
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            script.take(150) + (if(script.length > 150) "..." else ""),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(12.dp),
                                                            maxLines = 3,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Create Tab
                                if (apiKey.isEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Gemini API Key Missing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Please go to Settings -> AI Settings to configure your API key.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = prompt,
                                    onValueChange = { prompt = it },
                                    placeholder = { Text("e.g. A bouncing ball with time display at bottom") },
                                    label = { Text("Describe your animation") },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    enabled = !isGenerating,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                
                                Button(
                                    onClick = {
                                        if (prompt.isBlank()) return@Button
                                        isGenerating = true
                                        scope.launch {
                                            try {
                                                val script = geminiService.generateAnimation(apiKey, model, prompt, timeoutSeconds)
                                                generatedScript = script
                                                
                                                // Auto-preview
                                                currentScript = script
                                                onSimAnimChange("CUSTOM_AI")
                                                onSetVisualizerVisible(true) 
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                            } finally {
                                                isGenerating = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    enabled = !isGenerating && apiKey.isNotEmpty(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("AI is thinking...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate with AI", fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                generatedScript?.let { script ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Animation Generated!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val name = if (prompt.length > 20) prompt.take(20) + "..." else prompt
                                                        val newList = savedScripts + (name to script)
                                                        savedScripts = newList
                                                        val jsonStr = gson.toJson(newList)
                                                        prefs.edit().putString("saved_ai_scripts", jsonStr).apply()
                                                        aiSubTab = 0 
                                                        generatedScript = null
                                                        prompt = ""
                                                        android.widget.Toast.makeText(context, "Saved to your library", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Save")
                                                }
                                                
                                                var isSendingScript by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            isSendingScript = true
                                                            try {
                                                                bleManager.sendCommand("SCRIPT_START")
                                                                delay(100)
                                                                val lines = script.replace("{", ";{")
                                                                    .replace("}", ";}")
                                                                    .split(";")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotEmpty() }
                                                                
                                                                lines.forEach { line ->
                                                                    while (!bleManager.isReady.value) { delay(5) }
                                                                    bleManager.sendCommand("SCRIPT_LINE:$line")
                                                                    delay(10)
                                                                }
                                                                bleManager.sendCommand("SCRIPT_RUN")
                                                                android.widget.Toast.makeText(context, "Playing on device!", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "Failed to send script", android.widget.Toast.LENGTH_SHORT).show()
                                                            } finally {
                                                                isSendingScript = false
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    enabled = !isSendingScript,
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    if (isSendingScript) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Text("Play on ESP")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    columns: Int,
    content: @Composable (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                if (rowItems.size < columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationItem(
    id: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(id) {
                            "STARS" -> Icons.Default.AutoAwesome
                            "WAVES" -> Icons.Default.Waves
                            "DNA" -> Icons.Default.Cyclone
                            "FIREWORKS" -> Icons.Default.Celebration
                            "TUNNEL" -> Icons.Default.ScreenRotation
                            "SNOW" -> Icons.Default.AcUnit
                            "VORTEX" -> Icons.Default.Refresh
                            "CUBE" -> Icons.Default.ViewInAr
                            "RINGS" -> Icons.Default.RadioButtonUnchecked
                            "GLITCH" -> Icons.Default.Grain
                            "RAIN" -> Icons.Default.Opacity
                            "PLASMA" -> Icons.Default.InvertColors
                            "CIRCLES" -> Icons.Default.TrackChanges
                            "NOISE" -> Icons.Default.Texture
                            "SWARM" -> Icons.Default.BugReport
                            "WAVE2" -> Icons.Default.Timeline
                            "METABALLS" -> Icons.Default.BubbleChart
                            else -> Icons.Default.AutoAwesome
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchFaceItem(
    id: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(id) {
                            "MINIMAL" -> Icons.Default.WatchLater
                            "CYBER" -> Icons.Default.Terminal
                            "BIG_HOUR" -> Icons.Default.BarChart
                            "MODERN" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Timer
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicVizItem(
    name: String,
    id: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(id) {
                            "BARS" -> Icons.Default.BarChart
                            "WAVE" -> Icons.Default.Waves
                            "PEAK" -> Icons.Default.GraphicEq
                            else -> Icons.Default.GraphicEq
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
