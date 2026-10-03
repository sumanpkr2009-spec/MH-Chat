package com.mh.chat

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
 * Minimal client for an OpenAI-compatible chat API (OmniRoute gateway).
 * Generous timeouts: the Render free tier can cold-start for 30s+.
 * The API key is only ever sent in the Authorization header — never logged.
 */
object ChatClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(endpoint: String) = endpoint.trim().trimEnd('/')

    /** Non-streaming chat completion. Sends the full conversation history. */
    suspend fun chat(
        endpoint: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()
            for (m in history) {
                if (m.role == "system") continue
                messages.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .toString()

            val req = Request.Builder()
                .url("${baseUrl(endpoint)}/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body.toRequestBody(JSON))
                .build()

            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("API error ${resp.code}: ${text.take(300)}")
                    )
                }
                val content = JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    /** Returns the sorted list of model ids from GET /v1/models. */
    suspend fun loadModels(endpoint: String, apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("${baseUrl(endpoint)}/v1/models")
                    .header("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("API error ${resp.code}: ${text.take(300)}")
                        )
                    }
                    val ids = mutableListOf<String>()
                    val data = JSONObject(text).optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            data.optJSONObject(i)?.optString("id")?.let {
                                if (it.isNotBlank()) ids.add(it)
                            }
                        }
                    }
                    Result.success(ids.sorted())
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
}
