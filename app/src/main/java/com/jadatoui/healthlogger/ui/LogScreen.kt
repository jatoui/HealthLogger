package com.jadatoui.healthlogger.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadatoui.healthlogger.FeedEntry
import com.jadatoui.healthlogger.LogMode
import com.jadatoui.healthlogger.UiState
import com.jadatoui.healthlogger.net.EntryColor
import com.jadatoui.healthlogger.net.EntryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    state: UiState,
    onLog: (EntryType, String?, EntryColor) -> Unit,
    onUndo: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onModeChange: (LogMode) -> Unit,
    onLogDiary: (String) -> Unit,
    onUndoDiary: () -> Unit,
    onFetchInsights: () -> Unit,
) {
    var entry by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EntryType.FOOD) }
    var color by remember { mutableStateOf(EntryColor.NONE) }
    var colorMenuOpen by remember { mutableStateOf(false) }
    var medMenuOpen by remember { mutableStateOf(false) }

    var diaryText by remember { mutableStateOf("") }
    var awaitingDiarySend by remember { mutableStateOf(false) }
    var awaitingHealthSend by remember { mutableStateOf(false) }

    LaunchedEffect(state.sending) {
        if (!state.sending && awaitingDiarySend) {
            awaitingDiarySend = false
            if (state.retry == null) {
                diaryText = ""
            }
        }
        if (!state.sending && awaitingHealthSend) {
            awaitingHealthSend = false
            if (state.retry == null) {
                type = EntryType.FOOD
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Logger") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == LogMode.HEALTH,
                    onClick = { onModeChange(LogMode.HEALTH) },
                    label = { Text("Health") },
                )
                FilterChip(
                    selected = state.mode == LogMode.DIARY,
                    onClick = { onModeChange(LogMode.DIARY) },
                    label = { Text("Diary") },
                )
                FilterChip(
                    selected = state.mode == LogMode.INSIGHTS,
                    onClick = { onModeChange(LogMode.INSIGHTS) },
                    label = { Text("Insights") },
                )
            }

            if (!state.settings.isConfigured) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Not connected",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Add your webhook URL and auth header before logging.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = onOpenSettings) { Text("Open settings") }
                    }
                }
            }

            if (state.mode == LogMode.INSIGHTS) {
                InsightsPanel(
                    state = state,
                    onFetchInsights = onFetchInsights,
                    onOpenSettings = onOpenSettings,
                )
            } else if (state.mode == LogMode.HEALTH) {
                Text("Quick log", style = MaterialTheme.typography.titleSmall)

                QuickActionGrid(
                    items = listOf(
                        // Food
                        QuickActionSpec("Coffee", Icons.Default.Coffee) {
                            onLog(EntryType.FOOD, "coffee", EntryColor.NONE)
                        },
                        QuickActionSpec("PB & Jelly", Icons.Default.LunchDining) {
                            onLog(EntryType.FOOD, "pb jelly", EntryColor.NONE)
                        },
                        QuickActionSpec("Kashi + Milk", Icons.Default.BreakfastDining) {
                            onLog(EntryType.FOOD, "kashi soy milk", EntryColor.NONE)
                        },
                        QuickActionSpec("Oatmeal", Icons.Default.BreakfastDining) {
                            onLog(EntryType.FOOD, "oatmeal", EntryColor.NONE)
                        },
                        QuickActionSpec("Boiled Eggs", Icons.Default.Egg) {
                            onLog(EntryType.FOOD, "hard boiled eggs", EntryColor.NONE)
                        },
                        QuickActionSpec("Banana", emoji = "🍌") {
                            onLog(EntryType.FOOD, "banana", EntryColor.NONE)
                        },
                        QuickActionSpec("Turkey Sandwich", Icons.Default.LunchDining) {
                            onLog(EntryType.FOOD, "turkey sandwich", EntryColor.NONE)
                        },
                        QuickActionSpec("Mandarin", emoji = "🍊") {
                            onLog(EntryType.FOOD, "mandarin", EntryColor.NONE)
                        },
                        QuickActionSpec("Waffles", emoji = "🧇") {
                            onLog(EntryType.FOOD, "waffles", EntryColor.NONE)
                        },
                        // Poop
                        QuickActionSpec("Poop", Icons.Default.Wc) {
                            onLog(EntryType.POOP, null, EntryColor.NONE)
                        },
                        QuickActionSpec("Diarrhea", Icons.Default.Wc) {
                            onLog(EntryType.POOP, "diarrhea", EntryColor.RED)
                        },
                        // Piss
                        QuickActionSpec("Piss", Icons.Default.Wc) {
                            onLog(EntryType.PISS, null, EntryColor.NONE)
                        },
                    ),
                    enabled = !state.sending,
                )

                HorizontalDivider()

                Text("Meds", style = MaterialTheme.typography.titleSmall)

                ExposedDropdownMenuBox(
                    expanded = medMenuOpen,
                    onExpandedChange = { if (!state.sending) medMenuOpen = it },
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.sending,
                        placeholder = { Text("Tap a medication to log it") },
                        leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = medMenuOpen)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = medMenuOpen,
                        onDismissRequest = { medMenuOpen = false },
                    ) {
                        MED_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    medMenuOpen = false
                                    onLog(EntryType.MED, option.wire, option.color)
                                },
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text("Custom entry", style = MaterialTheme.typography.titleSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                        )
                    }
                }

                OutlinedTextField(
                    value = entry,
                    onValueChange = { entry = it },
                    label = { Text(if (type == EntryType.FOOD) "What did you have?" else "Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = colorMenuOpen,
                    onExpandedChange = { colorMenuOpen = it },
                ) {
                    OutlinedTextField(
                        value = color.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cell colour") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorMenuOpen)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = colorMenuOpen,
                        onDismissRequest = { colorMenuOpen = false },
                    ) {
                        EntryColor.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    color = option
                                    colorMenuOpen = false
                                },
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            awaitingHealthSend = true
                            onLog(type, entry.ifBlank { null }, color)
                            entry = ""
                            color = EntryColor.NONE
                        },
                        enabled = !state.sending &&
                            (type != EntryType.FOOD || entry.isNotBlank()),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Log it")
                        }
                    }

                    OutlinedButton(onClick = onUndo, enabled = !state.sending) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Text("Undo", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            } else {
                Text("Quick log", style = MaterialTheme.typography.titleSmall)

                QuickActionGrid(
                    items = listOf(
                        QuickActionSpec("F45", Icons.Default.FitnessCenter) { onLogDiary("f45") },
                        QuickActionSpec("Run", Icons.AutoMirrored.Filled.DirectionsRun) { onLogDiary("run") },
                        QuickActionSpec("Therapy", Icons.Default.Psychology) { onLogDiary("therapy") },
                    ),
                    enabled = !state.sending,
                )

                HorizontalDivider()

                Text("Diary entry", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = diaryText,
                    onValueChange = { diaryText = it },
                    label = { Text("What happened?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            awaitingDiarySend = true
                            onLogDiary(diaryText)
                        },
                        enabled = !state.sending && diaryText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Log entry")
                        }
                    }

                    OutlinedButton(onClick = onUndoDiary, enabled = !state.sending) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Text("Undo", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            if (state.mode != LogMode.INSIGHTS) {
                if (state.retry != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "That didn't send.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }

                HorizontalDivider()

                Text(
                    "Today's log (${state.feed.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FeedList(
                    items = state.feed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun InsightsPanel(
    state: UiState,
    onFetchInsights: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val insights = state.insights

    Text("AI insights", style = MaterialTheme.typography.titleSmall)
    Text(
        "Compares this month's log against last month's using Claude, based on " +
            "food, poop, med, and piss entries.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (!state.settings.isInsightsConfigured) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Not connected", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Add the insights webhook URL in Settings first.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenSettings) { Text("Open settings") }
            }
        }
        return
    }

    Button(
        onClick = onFetchInsights,
        enabled = !insights.loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (insights.loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(if (insights.intel != null) "Refresh insights" else "Generate insights")
        }
    }

    if (insights.error != null) {
        Text(
            insights.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (insights.intel != null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (insights.period != null) {
                    Text(insights.period, style = MaterialTheme.typography.labelMedium)
                }
                Text(insights.intel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Meds offered by the Meds dropdown; [wire] is what the workflow expects. */
private data class MedOption(
    val label: String,
    val wire: String,
    val color: EntryColor = EntryColor.NONE,
)

private val MED_OPTIONS = listOf(
    MedOption("ADHD", "adhd"),
    MedOption("Enema", "enema", EntryColor.RED),
    MedOption("Ibsrela", "ibsrela"),
    MedOption("Linzess", "linzess"),
    MedOption("Miralax", "miralax"),
    MedOption("Motility Activator", "motility activator"),
    MedOption("Prucalopride", "prucalopride"),
    MedOption("Suppository", "suppository", EntryColor.RED),
    MedOption("Trulance", "trulance"),
).sortedBy { it.label.lowercase() }

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector? = null,
    val emoji: String? = null,
    val onClick: () -> Unit,
)

private const val QUICK_ACTION_COLUMNS = 4

@Composable
private fun QuickActionGrid(
    items: List<QuickActionSpec>,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(QUICK_ACTION_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    QuickAction(
                        label = item.label,
                        icon = item.icon,
                        emoji = item.emoji,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = item.onClick,
                    )
                }
                repeat(QUICK_ACTION_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector?,
    emoji: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedIconButton(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            } else {
                Text(emoji.orEmpty(), fontSize = 18.sp)
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeedList(items: List<FeedEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items) { item -> FeedRow(item) }
        }

        val needsScrollbar by remember(items.size) {
            derivedStateOf {
                val info = listState.layoutInfo
                info.totalItemsCount > info.visibleItemsInfo.size
            }
        }
        if (needsScrollbar) {
            FeedScrollbar(
                state = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(6.dp),
            )
        }
    }
}

@Composable
private fun FeedScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val targetAlpha = if (state.isScrollInProgress) 0.9f else 0.4f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = if (state.isScrollInProgress) 100 else 500),
        label = "feedScrollbarAlpha",
    )
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        if (totalItems == 0 || visibleItems.isEmpty()) return@Canvas

        val averageItemHeight = layoutInfo.visibleItemsInfo.sumOf { it.size } / visibleItems.size.toFloat()
        val estimatedContentHeight = averageItemHeight * totalItems
        val firstVisible = visibleItems.first()
        val scrolledPastPx = firstVisible.index * averageItemHeight - firstVisible.offset

        val visibleRatio = (layoutInfo.viewportSize.height / estimatedContentHeight).coerceIn(0f, 1f)
        val thumbHeight = (size.height * visibleRatio).coerceAtLeast(24.dp.toPx())
        val maxThumbOffset = size.height - thumbHeight
        val scrollRatio = (scrolledPastPx / (estimatedContentHeight - layoutInfo.viewportSize.height))
            .coerceIn(0f, 1f)
        val thumbOffset = if (maxThumbOffset > 0f) maxThumbOffset * scrollRatio else 0f

        drawRoundRect(
            color = color,
            alpha = alpha,
            topLeft = Offset(0f, thumbOffset),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(size.width / 2),
        )
    }
}

@Composable
private fun FeedRow(item: FeedEntry) {
    val tint = when (item.status) {
        FeedEntry.Status.OK -> MaterialTheme.colorScheme.onSurfaceVariant
        FeedEntry.Status.WARN -> MaterialTheme.colorScheme.tertiary
        FeedEntry.Status.ERROR -> MaterialTheme.colorScheme.error
    }
    val marker = when (item.status) {
        FeedEntry.Status.OK -> "OK "
        FeedEntry.Status.WARN -> "!  "
        FeedEntry.Status.ERROR -> "x  "
    }

    Text(
        text = marker + item.text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = tint,
    )
}
