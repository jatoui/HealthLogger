package com.jadatoui.healthlogger.net

import com.jadatoui.healthlogger.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** What the user is logging. Mirrors the !food / !poop / !med commands. */
enum class EntryType(val wire: String, val label: String) {
    FOOD("food", "Food"),
    POOP("poop", "Poop"),
    MED("med", "Med"),
    PISS("piss", "Piss"),
}

/** Cell-fill colours supported by the workflow's palette. */
enum class EntryColor(val wire: String?, val label: String) {
    NONE(null, "No colour"),
    RED("red", "Red"),
    BROWN("brown", "Brown"),
    GREEN("green", "Green"),
    YELLOW("yellow", "Yellow"),
    BLUE("blue", "Blue"),
    ORANGE("orange", "Orange"),
    GREY("grey", "Grey"),
}

@Serializable
private data class LogRequest(
    val type: String,
    val entry: String? = null,
    val color: String? = null,
)

@Serializable
private data class DiaryRequest(val text: String)

/** Shape produced by the "Build App Response" node in n8n. */
@Serializable
data class LogResponse(
    val ok: Boolean = false,
    val action: String? = null,
    val type: String? = null,
    val range: String? = null,
    val value: String? = null,
    val color: String? = null,
    @SerialName("colorInvalid") val colorInvalid: String? = null,
    val reason: String? = null,
    val message: String = "",
)

sealed interface LogResult {
    data class Success(val response: LogResponse) : LogResult
    /** Reached the workflow, but it reported a problem (e.g. no column for today). */
    data class Rejected(val response: LogResponse) : LogResult
    /** Never reached the workflow, or the workflow errored. Safe to retry. */
    data class Failure(val message: String, val retryable: Boolean) : LogResult
}

/** Shape produced by the "Build Insights Response" node in n8n. */
@Serializable
data class InsightsResponse(
    val ok: Boolean = false,
    val period: String? = null,
    val intel: String? = null,
    val reason: String? = null,
    val message: String = "",
)

sealed interface InsightsResult {
    data class Success(val period: String?, val intel: String) : InsightsResult
    /** Reached the workflow, but it couldn't produce a summary (e.g. no data logged yet). */
    data class Rejected(val message: String) : InsightsResult
    /** Never reached the workflow, or the workflow errored. Safe to retry. */
    data class Failure(val message: String, val retryable: Boolean) : InsightsResult
}

class HealthLogClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun log(
        settings: Settings,
        type: EntryType,
        entry: String? = null,
        color: EntryColor = EntryColor.NONE,
    ): LogResult {
        if (!settings.isConfigured) {
            return LogResult.Failure(
                "Set the webhook URL and auth header in Settings first.",
                retryable = false,
            )
        }
        return post(
            url = settings.webhookUrl,
            settings = settings,
            body = json.encodeToString(
                LogRequest.serializer(),
                LogRequest(
                    type = type.wire,
                    entry = entry?.trim()?.ifBlank { null },
                    color = color.wire,
                ),
            ),
        )
    }

    suspend fun undo(settings: Settings): LogResult {
        if (!settings.isConfigured) {
            return LogResult.Failure(
                "Set the webhook URL and auth header in Settings first.",
                retryable = false,
            )
        }
        return post(
            url = settings.webhookUrl,
            settings = settings,
            body = json.encodeToString(LogRequest.serializer(), LogRequest(type = "undo")),
        )
    }

    suspend fun logDiary(settings: Settings, text: String): LogResult {
        if (!settings.isDiaryConfigured) {
            return LogResult.Failure(
                "Set the diary webhook URL in Settings first.",
                retryable = false,
            )
        }
        return post(
            url = settings.diaryUrl,
            settings = settings,
            body = json.encodeToString(DiaryRequest.serializer(), DiaryRequest(text.trim())),
        )
    }

    suspend fun undoDiary(settings: Settings): LogResult {
        if (!settings.isDiaryConfigured) {
            return LogResult.Failure(
                "Set the diary webhook URL in Settings first.",
                retryable = false,
            )
        }
        return post(
            url = settings.diaryUrl,
            settings = settings,
            body = json.encodeToString(LogRequest.serializer(), LogRequest(type = "undo")),
        )
    }

    suspend fun fetchInsights(settings: Settings): InsightsResult {
        if (!settings.isInsightsConfigured) {
            return InsightsResult.Failure(
                "Set the insights webhook URL in Settings first.",
                retryable = false,
            )
        }
        return when (val raw = postRaw(settings.insightsUrl, settings, "{}")) {
            is RawResponse.Body -> {
                val parsed = runCatching {
                    json.decodeFromString(InsightsResponse.serializer(), raw.text)
                }.getOrNull()
                    ?: return InsightsResult.Failure(
                        "Could not read the workflow's response.",
                        retryable = false,
                    )

                if (parsed.ok) {
                    InsightsResult.Success(parsed.period, parsed.intel ?: parsed.message)
                } else {
                    InsightsResult.Rejected(parsed.message.ifBlank { "No insights available yet." })
                }
            }
            is RawResponse.Failure -> InsightsResult.Failure(raw.message, raw.retryable)
        }
    }

    private suspend fun post(url: String, settings: Settings, body: String): LogResult =
        when (val raw = postRaw(url, settings, body)) {
            is RawResponse.Body -> {
                val parsed = runCatching {
                    json.decodeFromString(LogResponse.serializer(), raw.text)
                }.getOrNull()
                    ?: return LogResult.Failure(
                        "Could not read the workflow's response.",
                        retryable = false,
                    )

                if (parsed.ok) LogResult.Success(parsed) else LogResult.Rejected(parsed)
            }
            is RawResponse.Failure -> LogResult.Failure(raw.message, raw.retryable)
        }

    private sealed interface RawResponse {
        data class Body(val text: String) : RawResponse
        data class Failure(val message: String, val retryable: Boolean) : RawResponse
    }

    private suspend fun postRaw(url: String, settings: Settings, body: String): RawResponse =
        withContext(Dispatchers.IO) {
            val requestBody = body.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader(settings.headerName, settings.headerValue)
                .post(requestBody)
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val hint = when (response.code) {
                            401, 403 -> "Auth rejected (${response.code}). Check the header name and value."
                            404 -> "Webhook not found (404). Is the workflow active, and is the URL the production one?"
                            else -> "Server returned ${response.code}."
                        }
                        return@use RawResponse.Failure(hint, retryable = response.code >= 500)
                    }

                    RawResponse.Body(text)
                }
            } catch (e: IOException) {
                RawResponse.Failure(
                    e.message?.let { "Network error: $it" } ?: "No connection.",
                    retryable = true,
                )
            }
        }
}
