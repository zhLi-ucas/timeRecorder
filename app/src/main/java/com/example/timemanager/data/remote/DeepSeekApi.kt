package com.example.timemanager.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeepSeekApi(private val config: DeepSeekConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val content: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun chat(
        messages: JSONArray,
        jsonMode: Boolean = false,
        maxTokens: Int = 2000
    ): Result = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) return@withContext Result.Error("未配置 API key")
        try {
            val body = JSONObject().apply {
                put("model", config.model)
                put("messages", messages)
                put("max_tokens", maxTokens)
                put("temperature", 0.7)
                put("stream", false)
                if (jsonMode) {
                    put("response_format", JSONObject().put("type", "json_object"))
                }
            }
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    return@withContext Result.Error("HTTP ${response.code}：${errBody.take(200)}")
                }
                val raw = response.body?.string()
                    ?: return@withContext Result.Error("空响应")
                val content = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                if (content.isBlank()) Result.Error("AI 返回了空内容")
                else Result.Success(content)
            }
        } catch (e: IOException) {
            Result.Error("网络错误：${e.message ?: "未知"}")
        } catch (e: Exception) {
            Result.Error("解析错误：${e.message ?: "未知"}")
        }
    }

    suspend fun ping(): Result {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "user").put("content", "ping"))
        }
        return chat(messages, jsonMode = false, maxTokens = 5)
    }

    companion object {
        private const val ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
