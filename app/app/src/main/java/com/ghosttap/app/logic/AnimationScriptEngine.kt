package com.ghosttap.app.logic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import kotlin.math.*
import java.util.Calendar

class AnimationScriptEngine {
    private val paint = Paint().apply {
        color = Color.White
        style = PaintingStyle.Stroke
        strokeWidth = 1f
    }
    
    private val fillPaint = Paint().apply {
        color = Color.White
        style = PaintingStyle.Fill
    }

    // Parses and executes a C-style script frame
    fun renderFrame(
        canvas: Canvas,
        scriptCode: String,
        tick: Int,
        width: Int,
        height: Int
    ): List<String> {
        val variables = mutableMapOf<String, Float>()
        variables["t"] = tick.toFloat()
        variables["w"] = width.toFloat()
        variables["h"] = height.toFloat()

        // Time variables
        val now = Calendar.getInstance()
        variables["hour"] = now.get(Calendar.HOUR_OF_DAY).toFloat()
        variables["minute"] = now.get(Calendar.MINUTE).toFloat()
        variables["second"] = now.get(Calendar.SECOND).toFloat()

        val bleCommands = mutableListOf<String>()

        // Pre-process: remove comments and main() wrapper if present
        var code = scriptCode
            .replace(Regex("//.*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("void\\s+main\\s*\\(\\s*\\)\\s*\\{"), "")
            .replace(Regex("\\}\\s*$"), "")
            .trim()

        // Split into statements by semicolon
        val statements = code.split(";")

        statements.forEach { stmt ->
            val line = stmt.trim()
            if (line.isEmpty()) return@forEach

            // 1. Handle Function Calls: drawCircle(x, y, r)
            val funcMatch = Regex("(\\w+)\\s*\\((.*)\\)").find(line)
            if (funcMatch != null) {
                val funcName = funcMatch.groupValues[1]
                val argsStr = funcMatch.groupValues[2]
                val args = if (argsStr.isEmpty()) emptyList() else argsStr.split(",").map { it.trim() }

                when (funcName) {
                    "clear" -> bleCommands.add("CLEAR")
                    "drawLine" -> {
                        if (args.size >= 4) {
                            val x1 = eval(args[0], variables)
                            val y1 = eval(args[1], variables)
                            val x2 = eval(args[2], variables)
                            val y2 = eval(args[3], variables)
                            canvas.drawLine(Offset(x1, y1), Offset(x2, y2), paint)
                            bleCommands.add("LINE:${x1.toInt()}:${y1.toInt()}:${x2.toInt()}:${y2.toInt()}:1.0")
                        }
                    }
                    "drawRect" -> {
                        if (args.size >= 4) {
                            val x = eval(args[0], variables)
                            val y = eval(args[1], variables)
                            val w = eval(args[2], variables)
                            val h = eval(args[3], variables)
                            canvas.drawRect(x, y, x + w, y + h, paint)
                            bleCommands.add("RECT:${x.toInt()}:${y.toInt()}:${w.toInt()}:${h.toInt()}:1.0")
                        }
                    }
                    "drawCircle" -> {
                        if (args.size >= 3) {
                            val x = eval(args[0], variables)
                            val y = eval(args[1], variables)
                            val r = eval(args[2], variables)
                            canvas.drawCircle(Offset(x, y), r, paint)
                            bleCommands.add("CIRCLE:${x.toInt()}:${y.toInt()}:${r.toInt()}:1.0")
                        }
                    }
                    "drawPixel" -> {
                        if (args.size >= 2) {
                            val x = eval(args[0], variables)
                            val y = eval(args[1], variables)
                            canvas.drawRect(x, y, x + 1, y + 1, fillPaint)
                            bleCommands.add("PIXEL:${x.toInt()}:${y.toInt()}:1")
                        }
                    }
                }
                return@forEach
            }

            // 2. Handle Assignments: float x = 10; or x = 10;
            val assignMatch = Regex("(?:\\w+\\s+)?(\\w+)\\s*=\\s*(.*)").find(line)
            if (assignMatch != null) {
                val varName = assignMatch.groupValues[1]
                val expr = assignMatch.groupValues[2]
                variables[varName] = eval(expr, variables)
            }
        }
        
        bleCommands.add("UPDATE")
        return bleCommands
    }

    private fun eval(expr: String, vars: Map<String, Float>): Float {
        var evalStr = expr
        
        // Handle time functions: hour(), minute(), second()
        evalStr = evalStr.replace("hour()", vars["hour"].toString())
        evalStr = evalStr.replace("minute()", vars["minute"].toString())
        evalStr = evalStr.replace("second()", vars["second"].toString())

        vars.forEach { (k, v) ->
            evalStr = evalStr.replace(Regex("\\b$k\\b"), v.toString())
        }
        
        return try {
            MathEvaluator(evalStr).parse().toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    private class MathEvaluator(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else {
                    x = 0.0 // Should not happen with current logic
                }
                x = when (func) {
                    "sin" -> sin(x)
                    "cos" -> cos(x)
                    "abs" -> abs(x)
                    "random" -> Math.random() * x
                    else -> x
                }
            } else {
                x = 0.0
            }
            return x
        }
    }
}
