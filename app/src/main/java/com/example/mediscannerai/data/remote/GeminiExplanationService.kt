package com.example.mediscannerai.data.remote

import com.example.mediscannerai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    suspend fun generateExplanation(systemInstruction: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
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

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Gemini request failed (${response.code}): ${responseBody.take(300)}"
                    )
                }

                parseExplanationText(responseBody)
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
}

