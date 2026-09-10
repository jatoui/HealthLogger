package com.jadatoui.healthlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.feedDataStore by preferencesDataStore(name = "health_logger_feed")

/** A feed row as persisted to disk, tagged with the local day it belongs to. */
@Serializable
data class StoredFeedEntry(
    val text: String,
    val status: String,
    val timestampMillis: Long,
    val day: String,
)

/**
 * Persists the on-screen activity feed so it survives process death (backgrounding,
 * phone reboot, etc.) for the rest of the current day. Entries tagged with an older
 * [StoredFeedEntry.day] are dropped on read, so the feed naturally resets each day.
 */
class FeedStore(private val context: Context) {

    private object Keys {
        val ENTRIES = stringPreferencesKey("entries_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val todaysEntries: Flow<List<StoredFeedEntry>> = context.feedDataStore.data.map { prefs ->
        val raw = prefs[Keys.ENTRIES]
        if (raw.isNullOrBlank()) return@map emptyList()
        val all = runCatching { json.decodeFromString<List<StoredFeedEntry>>(raw) }.getOrDefault(emptyList())
        val today = todayString()
        all.filter { it.day == today }
    }

    suspend fun save(entries: List<StoredFeedEntry>) {
        context.feedDataStore.edit { prefs ->
            prefs[Keys.ENTRIES] = json.encodeToString(entries)
        }
    }

    companion object {
        fun todayString(): String = LocalDate.now(ZoneId.systemDefault()).toString()
    }
}
