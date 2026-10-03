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

