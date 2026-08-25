package com.jadatoui.healthlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "health_logger_settings")

/**
 * Connection settings for the n8n webhook.
 *
 * [headerName] / [headerValue] must match the Header Auth credential attached to
 * the "App Webhook" node in n8n. Without them the request is rejected with 403.
 */
data class Settings(
    val webhookUrl: String = "",
    val diaryUrl: String = "",
    val headerName: String = "",
    val headerValue: String = "",
) {
    val isConfigured: Boolean
        get() = webhookUrl.startsWith("http") && headerName.isNotBlank() && headerValue.isNotBlank()

    val isDiaryConfigured: Boolean
        get() = diaryUrl.startsWith("http") && headerName.isNotBlank() && headerValue.isNotBlank()
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val URL = stringPreferencesKey("webhook_url")
        val DIARY_URL = stringPreferencesKey("diary_url")
        val HEADER_NAME = stringPreferencesKey("header_name")
        val HEADER_VALUE = stringPreferencesKey("header_value")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            webhookUrl = prefs[Keys.URL].orEmpty(),
            diaryUrl = prefs[Keys.DIARY_URL].orEmpty(),
            headerName = prefs[Keys.HEADER_NAME].orEmpty(),
            headerValue = prefs[Keys.HEADER_VALUE].orEmpty(),
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.URL] = settings.webhookUrl.trim()
            prefs[Keys.DIARY_URL] = settings.diaryUrl.trim()
            prefs[Keys.HEADER_NAME] = settings.headerName.trim()
            prefs[Keys.HEADER_VALUE] = settings.headerValue.trim()
        }
    }
}
