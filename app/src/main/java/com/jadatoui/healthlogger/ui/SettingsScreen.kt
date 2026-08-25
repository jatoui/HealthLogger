package com.jadatoui.healthlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jadatoui.healthlogger.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    current: Settings,
    onSave: (Settings) -> Unit,
    onBack: () -> Unit,
) {
    var url by remember(current) { mutableStateOf(current.webhookUrl) }
    var diaryUrl by remember(current) { mutableStateOf(current.diaryUrl) }
    var headerName by remember(current) { mutableStateOf(current.headerName) }
    var headerValue by remember(current) { mutableStateOf(current.headerValue) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "These must match the App Webhook node and its Header Auth " +
                    "credential in n8n. The health and diary webhooks share the " +
                    "same auth header.",
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Webhook URL") },
                placeholder = { Text("https://…/webhook/health-log") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = diaryUrl,
                onValueChange = { diaryUrl = it },
                label = { Text("Diary webhook URL") },
                placeholder = { Text("https://…/webhook/daily-log") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = headerName,
                onValueChange = { headerName = it },
                label = { Text("Auth header name") },
                placeholder = { Text("e.g. X-Api-Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = headerValue,
                onValueChange = { headerValue = it },
                label = { Text("Auth header value") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(Settings(url, diaryUrl, headerName, headerValue))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            Text(
                "Use the Production URL from the webhook node, not the Test URL — " +
                    "the test one only listens while the n8n editor is open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
