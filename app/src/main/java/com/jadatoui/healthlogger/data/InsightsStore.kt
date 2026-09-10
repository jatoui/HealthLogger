package com.jadatoui.healthlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.insightsDataStore by preferencesDataStore(name = "health_logger_insights")

/** The last successfully generated AI insights, as persisted to disk. */
data class StoredInsights(
    val period: String?,
    val intel: String,
)

/**
 * Persists the last AI insights result so it survives process death and still shows
 * on the Insights tab until the user generates a fresh one.
 */
class InsightsStore(private val context: Context) {

    private object Keys {
        val PERIOD = stringPreferencesKey("period")
        val INTEL = stringPreferencesKey("intel")
    }

    val lastInsights: Flow<StoredInsights?> = context.insightsDataStore.data.map { prefs ->
        val intel = prefs[Keys.INTEL]
        if (intel.isNullOrBlank()) null else StoredInsights(prefs[Keys.PERIOD], intel)
    }

    suspend fun save(insights: StoredInsights) {
        context.insightsDataStore.edit { prefs ->
            prefs[Keys.INTEL] = insights.intel
            if (insights.period != null) {
                prefs[Keys.PERIOD] = insights.period
            } else {
                prefs.remove(Keys.PERIOD)
            }
        }
    }
}
