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
 * Thin wrapper around Groq's OpenAI-compatible chat completions endpoint.
 * Groq hosts open-weight models (here: Meta's Llama 3.3 70B) on custom
 * inference hardware, with a free tier far more generous than Gemini's.
 */
class GroqExplanationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelName = "openai/gpt-oss-120b"

    private val maxRetries = 4
    private val baseDelayMillis = 1000L
    private val maxDelayMillis = 10000L

    suspend fun generateExplanation(systemInstruction: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            for (attempt in 0..maxRetries) {
                try {
                    return@withContext performRequest(systemInstruction, userPrompt)
                } catch (e: RetryableException) {
                    if (attempt == maxRetries) {
                        throw IllegalStateException(
                            "The AI service is experiencing high demand right now. " +
                                    "This usually clears up quickly — please tap Retry in a moment."
                        )
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
        val exponential = baseDelayMillis * (1 shl attempt)
        val capped = exponential.coerceAtMost(maxDelayMillis)
        val jitter = Random.nextLong(0, capped / 3 + 1)
        return capped + jitter
    }

    private fun performRequest(systemInstruction: String, userPrompt: String): String {
        val requestJson = JSONObject().apply {
            put("model", modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (response.code == 503 || response.code == 429) {
                throw RetryableException(response.code)
            }

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Groq request failed (${response.code}): ${responseBody.take(300)}"
                )
            }

            return parseExplanationText(responseBody)
        }
    }

    private fun parseExplanationText(responseBody: String): String {
        val root = JSONObject(responseBody)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalStateException("No response was returned. Please try again.")

        val message = choices.getJSONObject(0).getJSONObject("message")
        val text = message.optString("content").trim()

        if (text.isBlank()) {
            throw IllegalStateException("The AI returned an empty response. Please try again.")
        }
        return text
    }

    private class RetryableException(val code: Int) : Exception("Retryable response ($code)")
}