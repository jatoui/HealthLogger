package com.jadatoui.healthlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jadatoui.healthlogger.ui.HealthLoggerTheme
import com.jadatoui.healthlogger.ui.LogScreen
import com.jadatoui.healthlogger.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HealthLoggerTheme {
                val vm: LogViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        current = state.settings,
                        onSave = vm::saveSettings,
                        onBack = { showSettings = false },
                    )
                } else {
                    LogScreen(
                        state = state,
                        onLog = vm::log,
                        onUndo = vm::undo,
                        onRetry = vm::retry,
                        onOpenSettings = { showSettings = true },
                        onModeChange = vm::setMode,
                        onLogDiary = vm::logDiary,
                        onUndoDiary = vm::undoDiary,
                    )
                }
            }
        }
    }
}
