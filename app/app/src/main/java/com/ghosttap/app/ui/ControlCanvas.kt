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

