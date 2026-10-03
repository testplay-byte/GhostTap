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

