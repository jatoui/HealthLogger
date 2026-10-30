package com.jadatoui.healthlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
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
 * phone reboot, etc.). Only the last [retainDays] local days are kept: entries tagged
 * with an older [StoredFeedEntry.day] are dropped on read, so with the default of 1
 * the feed naturally resets each day.
 */
class FeedStore(
    private val context: Context,
    key: String = "entries_json",
    private val retainDays: Long = 1,
) {

    private val entriesKey = stringPreferencesKey(key)

    private val json = Json { ignoreUnknownKeys = true }

    val recentEntries: Flow<List<StoredFeedEntry>> = context.feedDataStore.data.map { prefs ->
        val raw = prefs[entriesKey]
        if (raw.isNullOrBlank()) return@map emptyList()
        val all = runCatching { json.decodeFromString<List<StoredFeedEntry>>(raw) }.getOrDefault(emptyList())
        // ISO dates sort lexically, so a string compare is a date compare.
        val oldestKept = LocalDate.now(ZoneId.systemDefault()).minusDays(retainDays - 1).toString()
        all.filter { it.day >= oldestKept }
    }

    suspend fun save(entries: List<StoredFeedEntry>) {
        context.feedDataStore.edit { prefs ->
            prefs[entriesKey] = json.encodeToString(entries)
        }
    }

    companion object {
        /** The local day an entry was logged on, e.g. "2026-10-30". */
        fun dayOf(timestampMillis: Long): String =
            Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
