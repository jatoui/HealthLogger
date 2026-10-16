package com.jadatoui.healthlogger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jadatoui.healthlogger.data.FeedStore
import com.jadatoui.healthlogger.data.InsightsStore
import com.jadatoui.healthlogger.data.Settings
import com.jadatoui.healthlogger.data.SettingsStore
import com.jadatoui.healthlogger.data.StoredFeedEntry
import com.jadatoui.healthlogger.data.StoredInsights
import com.jadatoui.healthlogger.net.EntryColor
import com.jadatoui.healthlogger.net.EntryType
import com.jadatoui.healthlogger.net.HealthLogClient
import com.jadatoui.healthlogger.net.InsightsResult
import com.jadatoui.healthlogger.net.LogResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One line in the on-screen activity feed. */
data class FeedEntry(
    val text: String,
    val status: Status,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    enum class Status { OK, WARN, ERROR }
}

/** A send that failed and can be re-attempted with one tap. */
data class PendingRetry(
    val type: EntryType?,
    val entry: String?,
    val color: EntryColor,
    val isUndo: Boolean,
    val isDiary: Boolean = false,
)

enum class LogMode { HEALTH, DIARY, INSIGHTS }

/** Result of the last AI insights request, shown on the Insights tab. */
data class InsightsState(
    val loading: Boolean = false,
    val period: String? = null,
    val intel: String? = null,
    val error: String? = null,
)

data class UiState(
    val settings: Settings = Settings(),
    val sending: Boolean = false,
    val feed: List<FeedEntry> = emptyList(),
    val retry: PendingRetry? = null,
    val mode: LogMode = LogMode.HEALTH,
    val insights: InsightsState = InsightsState(),
    /** Today's diary sends, kept separate from [feed] and shown only on the Diary tab. */
    val diaryFeed: List<FeedEntry> = emptyList(),
)

class LogViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val feedStore = FeedStore(app)
    private val diaryFeedStore = FeedStore(app, key = "diary_entries_json")
    private val insightsStore = InsightsStore(app)
    private val client = HealthLogClient()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.settings.collect { loaded ->
                _state.update { it.copy(settings = loaded) }
            }
        }
        viewModelScope.launch {
            val restored = feedStore.todaysEntries.first().map { stored ->
                FeedEntry(stored.text, FeedEntry.Status.valueOf(stored.status), stored.timestampMillis)
            }
            val restoredDiary = diaryFeedStore.todaysEntries.first().map { stored ->
                FeedEntry(stored.text, FeedEntry.Status.valueOf(stored.status), stored.timestampMillis)
            }
            if (restored.isNotEmpty() || restoredDiary.isNotEmpty()) {
                _state.update { it.copy(feed = restored, diaryFeed = restoredDiary) }
            }
        }
        viewModelScope.launch {
            insightsStore.lastInsights.first()?.let { restored ->
                _state.update {
                    it.copy(insights = it.insights.copy(period = restored.period, intel = restored.intel))
                }
            }
        }
    }

    fun saveSettings(settings: Settings) {
        viewModelScope.launch { store.save(settings) }
    }

    fun log(type: EntryType, entry: String?, color: EntryColor = EntryColor.NONE) {
        dispatch(PendingRetry(type, entry, color, isUndo = false))
    }

    fun undo() {
        dispatch(PendingRetry(null, null, EntryColor.NONE, isUndo = true))
    }

    fun undoDiary() {
        dispatch(PendingRetry(null, null, EntryColor.NONE, isUndo = true, isDiary = true))
    }

    fun setMode(mode: LogMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun logDiary(text: String) {
        dispatch(PendingRetry(null, text, EntryColor.NONE, isUndo = false, isDiary = true))
    }

    fun fetchInsights() {
        if (_state.value.insights.loading) return
        _state.update { it.copy(insights = it.insights.copy(loading = true, error = null)) }

        viewModelScope.launch {
            val settings = _state.value.settings
            val current = _state.value.insights
            val next = when (val result = client.fetchInsights(settings)) {
                is InsightsResult.Success -> {
                    insightsStore.save(StoredInsights(result.period, result.intel))
                    InsightsState(period = result.period, intel = result.intel)
                }
                is InsightsResult.Rejected ->
                    current.copy(loading = false, error = result.message)
                is InsightsResult.Failure ->
                    current.copy(loading = false, error = result.message)
            }
            _state.update { it.copy(insights = next) }
        }
    }

    fun retry() {
        _state.value.retry?.let { dispatch(it) }
    }

    fun dismissRetry() {
        _state.update { it.copy(retry = null) }
    }

    private fun dispatch(action: PendingRetry) {
        if (_state.value.sending) return

        _state.update { it.copy(sending = true, retry = null) }

        viewModelScope.launch {
            val settings = _state.value.settings
            val result = when {
                action.isDiary && action.isUndo -> client.undoDiary(settings)
                action.isDiary -> client.logDiary(settings, action.entry.orEmpty())
                action.isUndo -> client.undo(settings)
                else -> client.log(settings, action.type!!, action.entry, action.color)
            }

            val (entry, retry) = when (result) {
                is LogResult.Success -> {
                    val warn = result.response.colorInvalid
                    val text = if (warn != null) {
                        "${result.response.message}  (unknown colour \"$warn\" ignored)"
                    } else {
                        result.response.message
                    }
                    FeedEntry(text, if (warn != null) FeedEntry.Status.WARN else FeedEntry.Status.OK) to null
                }

                is LogResult.Rejected ->
                    FeedEntry(result.response.message, FeedEntry.Status.WARN) to null

                is LogResult.Failure ->
                    FeedEntry(result.message, FeedEntry.Status.ERROR) to
                        (action.takeIf { result.retryable })
            }

            if (action.isDiary) {
                val newDiaryFeed = listOf(entry) + _state.value.diaryFeed
                _state.update { it.copy(sending = false, diaryFeed = newDiaryFeed, retry = retry) }
                persistFeed(diaryFeedStore, newDiaryFeed)
                return@launch
            }

            val newFeed = listOf(entry) + _state.value.feed
            _state.update {
                it.copy(
                    sending = false,
                    feed = newFeed,
                    retry = retry,
                )
            }
            persistFeed(feedStore, newFeed)
        }
    }

    private fun persistFeed(target: FeedStore, feed: List<FeedEntry>) {
        viewModelScope.launch {
            val today = FeedStore.todayString()
            target.save(
                feed.map { StoredFeedEntry(it.text, it.status.name, it.timestampMillis, today) },
            )
        }
    }
}
