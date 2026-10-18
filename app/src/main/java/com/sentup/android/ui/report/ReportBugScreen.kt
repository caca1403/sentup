package com.sentup.android.ui.report
import com.sentup.android.ui.components.DismissibleSnackbarHost
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentup.android.data.CrashReporter
import com.sentup.android.data.DiagnosticsCollector
import com.sentup.android.data.ThemePreferences
import com.sentup.android.ui.settings.SettingsCard
import com.sentup.android.ui.settings.SettingsDivider
import com.sentup.android.ui.settings.SettingsSection
import com.sentup.android.ui.settings.SettingsToggleRow
import com.sentup.android.util.KLog
import kotlinx.coroutines.launch

/**
 * The bug reporter: one screen that builds the whole report in front of the
 * user, shows exactly what will be copied, and hands it off to wherever they
 * want to take it.
 *
 * Nothing leaves the device by itself. Every section has a visible switch,
 * the preview below is byte-for-byte what lands on the clipboard, and the
 * three actions all copy first and only then open Telegram or GitHub - so the
 * report survives even when neither app is installed or the paste fails.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReportBugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { ThemePreferences(context) }

    val diagnostics = remember { DiagnosticsCollector.collect(context) }
    val logEntries = remember { KLog.snapshot() }
    val crashText = remember { CrashReporter.readPendingCrash(context) }

    var description by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var includeCrash by remember { mutableStateOf(crashText != null) }
    var logFilter by remember {
        mutableStateOf(
            if (prefs.getReportVerboseLogs()) LogLevelFilter.ALL else LogLevelFilter.WARNINGS
        )
    }
    // The crash file is deleted the moment its content has been copied into a
    // report - reporting it twice would read as two crashes.
    var crashConsumed by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun filteredEntries(): List<KLog.Entry> = logEntries.filter { entry ->
        when (logFilter) {
            LogLevelFilter.ERRORS -> entry.level == KLog.Level.ERROR
            LogLevelFilter.WARNINGS -> entry.level != KLog.Level.DEBUG
            LogLevelFilter.ALL -> true
        }
    }

    fun buildReport(): String = buildString {
        appendLine("Sentüp bug report")
        appendLine()
        appendLine("What happened:")
        appendLine(description.ifBlank { "(not described)" })
        if (includeDiagnostics) {
            appendLine()
            append(diagnostics.trimEnd())
            appendLine()
        }
        if (includeCrash && crashText != null && !crashConsumed) {
            appendLine()
            appendLine("[Last crash]")
            append(crashText.trimEnd())
            appendLine()
        }
        appendLine()
        append(KLog.render(filteredEntries(), header = "[Recent logs (${filteredEntries().size})]"))
    }

    fun copyAnd(reportText: String, then: (() -> Unit)? = null) {
        val clipboard =
            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Sentüp bug report", reportText))
        if (includeCrash && crashText != null && !crashConsumed) {
            CrashReporter.clearPendingCrash(context)
            crashConsumed = true
        }
        then?.invoke()
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.rb_copied))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { DismissibleSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.sp_report_bug), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.rb_session_logs, logEntries.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "description") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rb_what_happened)) },
                    placeholder = { Text(stringResource(R.string.rb_placeholder)) },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (crashText != null) {
                item(key = "crash") {
                    SettingsSection(title = stringResource(R.string.rb_last_crash)) {
                        SettingsCard {
                            SettingsToggleRow(
                                icon = Icons.Rounded.BugReport,
                                title = stringResource(R.string.rb_attach_crash),
                                subtitle = crashPreviewLine(crashText),
                                enabled = includeCrash,
                                onToggle = { includeCrash = it }
                            )
                            if (includeCrash && !crashConsumed) {
                                SettingsDivider()
                                MonospaceBlock(text = crashText.lines().take(12).joinToString("\n"))
                            }
                        }
                    }
                }
            }

            item(key = "diagnostics") {
                SettingsSection(title = stringResource(R.string.rb_device_info)) {
                    SettingsCard {
                        SettingsToggleRow(
                            icon = Icons.Rounded.Troubleshoot,
                            title = stringResource(R.string.rb_attach_device),
                            subtitle = stringResource(R.string.rb_attach_device_sub),
                            enabled = includeDiagnostics,
                            onToggle = { includeDiagnostics = it }
                        )
                        if (includeDiagnostics) {
                            SettingsDivider()
                            MonospaceBlock(text = diagnostics.trimEnd())
                        }
                    }
                }
            }

            item(key = "log-level") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.rb_log_detail),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LogLevelFilter.entries.forEach { filter ->
                            val selected = logFilter == filter
                            LogPill(
                                label = when (filter) {
                                    LogLevelFilter.ERRORS -> stringResource(R.string.rb_errors)
                                    LogLevelFilter.WARNINGS -> stringResource(R.string.rb_warnings)
                                    LogLevelFilter.ALL -> stringResource(R.string.rb_everything)
                                },
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    logFilter = filter
                                    prefs.setReportVerboseLogs(filter == LogLevelFilter.ALL)
                                }
                            )
                        }
                    }
                }
            }

            item(key = "logs") {
                val filtered = filteredEntries()
                SettingsSection(title = stringResource(R.string.rb_recent_logs)) {
                    SettingsCard {
                        if (filtered.isEmpty()) {
                            Text(
                                text = if (logEntries.isEmpty()) {
                                    stringResource(R.string.rb_nothing_logged)
                                } else {
                                    stringResource(R.string.rb_no_entries)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        } else {
                            MonospaceBlock(
                                text = KLog.render(filtered, header = "").trimEnd(),
                                modifier = Modifier.height(240.dp),
                                newestFirst = false
                            )
                        }
                    }
                }
            }

            item(key = "preview") {
                SettingsSection(title = stringResource(R.string.rb_preview)) {
                    SettingsCard {
                        Text(
                            text = stringResource(R.string.rb_preview_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                        MonospaceBlock(
                            text = buildReport(),
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }

            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val report = buildReport()
                    Button(
                        onClick = { copyAnd(report) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.rb_copy_report))
                    }
                    Text(
                        text = stringResource(R.string.rb_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/** First meaningful line of the crash file, for the toggle's subtitle. */
@Composable
private fun crashPreviewLine(crashText: String): String =
    crashText.lineSequence().firstOrNull { it.startsWith("Version:") }
        ?.let { version -> stringResource(R.string.rb_crash_subtitle, version) }
        ?: stringResource(R.string.rb_default_attachment)

/**
 * A monospace log/report text block that scrolls internally.
 *
 * `heightIn(max = ...)` is load-bearing, not cosmetic: several call sites
 * hand this no height of its own, and a vertically scrollable Text measured
 * with unbounded max height crashes outright ("infinity maximum height
 * constraints") - the exact failure mode documented in ROADMAP's notes on
 * sheets and nested scrollers. Callers may still pin an exact height; the
 * cap then simply passes through.
 */
@Composable
private fun MonospaceBlock(
    text: String,
    modifier: Modifier = Modifier,
    newestFirst: Boolean = true
) {
    val scrollState = rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(text, newestFirst) {
        if (newestFirst) scrollState.scrollTo(scrollState.maxValue)
    }
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .verticalScroll(scrollState)
    )
}

private enum class LogLevelFilter { ERRORS, WARNINGS, ALL }

/**
 * One choice pill of the log-detail strip: emerald fill when selected,
 * tonal otherwise, press answered by a dip.
 */
@Composable
private fun LogPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "logPillPress"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1
        )
    }
}
