package com.example.mediscannerai.data.remote



import com.example.mediscannerai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Thin wrapper around the Gemini REST API's generateContent endpoint.
 * Uses plain OkHttp + JSONObject rather than the official SDK, keeping
 * the dependency surface small and the request/response shape fully visible.
 */
class GeminiExplanationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.8-flash"

    private val maxRetries = 5
    private val baseDelayMillis = 1500L
    private val maxDelayMillis = 15000L

    suspend fun generateExplanation(systemInstruction: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            for (attempt in 0..maxRetries) {
                try {
                    return@withContext performRequest(systemInstruction, userPrompt)
                } catch (e: RetryableException) {
                    if (attempt == maxRetries) {
                        // TEMPORARY: showing the full error body so we can see
                        // exactly which quota was hit (per-minute vs per-day).
                        throw IllegalStateException("HTTP ${e.code}: ${e.body.take(500)}")
                    }
                    delay(backoffDelay(attempt))
                } catch (e: IOException) {
                    if (attempt == maxRetries) {
                        throw IllegalStateException(
                            "Couldn't reach the AI service. Please check your internet connection and try again."
                        )
                    }
                    delay(backoffDelay(attempt))
                }
            }
            error("generateExplanation: retry loop exited unexpectedly")
        }

    private fun backoffDelay(attempt: Int): Long {
        val exponential = baseDelayMillis * (1 shl attempt) // 1.5s, 3s, 6s, 12s, ...
        val capped = exponential.coerceAtMost(maxDelayMillis)
        val jitter = Random.nextLong(0, capped / 3 + 1)
        return capped + jitter
    }

    private fun performRequest(systemInstruction: String, userPrompt: String): String {
        enforceMinimumGap()

        val requestJson = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                }
            ))
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (response.code == 503 || response.code == 429) {
                throw RetryableException(response.code, responseBody)
            }

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Gemini request failed (${response.code}): ${responseBody.take(300)}"
                )
            }

            return parseExplanationText(responseBody)
        }
    }

    /**
     * Ensures at least MIN_GAP_MILLIS passes between any two calls to this
     * service, app-wide — not just within one screen. This directly prevents
     * the common pattern of one screen's call succeeding, then another
     * screen (e.g. AI Explanation, then Doctor Questions) firing its own
     * call moments later and tripping the free tier's per-minute limit.
     */
    private fun enforceMinimumGap() {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTimeMillis
            if (elapsed < MIN_GAP_MILLIS) {
                Thread.sleep(MIN_GAP_MILLIS - elapsed)
            }
            lastRequestTimeMillis = System.currentTimeMillis()
        }
    }

    private fun parseExplanationText(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("No response was returned. This may be due to the content safety filters — try rephrasing or check the raw text.")

        val firstCandidate = candidates.getJSONObject(0)
        val parts = firstCandidate.getJSONObject("content").getJSONArray("parts")

        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            builder.append(parts.getJSONObject(i).optString("text"))
        }

        val text = builder.toString().trim()
        if (text.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response. Please try again.")
        }
        return text
    }

    private class RetryableException(val code: Int, val body: String = "") : Exception("Retryable response ($code)")

    companion object {
        @Volatile private var lastRequestTimeMillis = 0L
        private const val MIN_GAP_MILLIS = 6000L
    }
}