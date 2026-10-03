package com.ghosttap.app.data

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

class GeminiService {
    private val baseClient = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun generateAnimation(
        apiKey: String,
        model: String,
        prompt: String,
        timeoutSeconds: Long = 30L
    ): String = withContext(Dispatchers.IO) {
        val client = baseClient.newBuilder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()

        val normalizedModel = if (model.startsWith("models/")) model.removePrefix("models/") else model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$normalizedModel:generateContent?key=$apiKey"
        
        android.util.Log.d("GeminiService", "Requesting animation from model: $normalizedModel with timeout: ${timeoutSeconds}s")
        
        val systemPrompt = """
            You are an expert C/C++ animation generator for a 72x40 pixel monochrome OLED display.
            Your task is to generate a C-style function body that draws a frame of an animation.
            
            Available API Functions:
            - clear(): Clears the display.
            - drawLine(x1, y1, x2, y2): Draws a line.
            - drawRect(x, y, w, h): Draws a rectangle.
            - drawCircle(x, y, r): Draws a circle.
            - drawPixel(x, y): Draws a single pixel.
            
            Available Variables:
            - t: Current frame tick (starts at 0).
            - w: Display width (72).
            - h: Display height (40).
            - hour(), minute(), second(): Current system time.
            
            Math Functions:
            - sin(val), cos(val), abs(val), random(max).
            
            Example (Bouncing Ball with Time):
            void main() {
              clear();
              float x = 36 + 20 * sin(t * 0.1);
              float y = 20 + 10 * cos(t * 0.1);
              drawCircle(x, y, 5);
              // Draw small dots for seconds
              for(int i=0; i<second(); i++) {
                drawPixel(i, 0);
              }
            }
            
            Output ONLY the raw C code within the main() function or just the code block. No markdown, no explanations.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", "$systemPrompt\n\nUser Request: $prompt")
                ))
            ))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                android.util.Log.e("GeminiService", "API Error ${response.code}: $errorBody")
                throw Exception("API Error ${response.code}: ${response.message}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            // Parse Gemini response to extract text
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""
            
            // Clean up code blocks if present
            text.replace("```c", "").replace("```cpp", "").replace("```json", "").replace("```", "").trim()
        }
    }
}
