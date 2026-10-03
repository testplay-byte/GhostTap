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
