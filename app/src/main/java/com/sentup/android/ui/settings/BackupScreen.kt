package com.sentup.android.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentup.android.R
import androidx.annotation.StringRes
import com.sentup.android.data.BackupApplyFailure
import com.sentup.android.data.BackupManifest
import com.sentup.android.data.BackupReadFailure
import com.sentup.android.data.BackupRepository
import com.sentup.android.data.BackupSnapshot
import com.sentup.android.data.BackupTransfer
import com.sentup.android.data.FolderStatus
import com.sentup.android.data.ProfileKind
import com.sentup.android.data.RestoreResult
import com.sentup.android.data.ScheduledBackupRepository
import com.sentup.android.data.ScheduledBackupResult
import com.sentup.android.data.ScheduledBackupStatus
import com.sentup.android.data.UnsupportedBackupException
import com.sentup.android.ui.components.DismissibleSnackbarHost
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.util.KLog
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Writing this install to a file, and putting one back.
 *
 * A screen rather than two rows on a settings page, because a restore is not a
 * setting: it replaces everything the user has, so it has to be able to say
 * what is in the file *before* they agree to it, and it has to end somewhere
 * that explains what just happened. A row cannot do either.
 *
 * The whole flow works offline and signed out - nothing here makes a request -
 * which is the point of it existing at all for an app people sideload from
 * GitHub.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    /**
     * True while a file is being read, written or restored. Settings hosts
     * this as one of its pages, and beside the hub on a wide window it has to
     * know not to swap the page out from under that work.
     */
    onBusyChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { BackupRepository(context) }
    val scheduledRepo = remember { ScheduledBackupRepository(context) }

    val scheduledEnabled by scheduledRepo.enabled.collectAsState()
    val scheduledFolderUri by scheduledRepo.folderUri.collectAsState()
    val scheduledFolderName by scheduledRepo.folderDisplayName.collectAsState()
    val scheduledFrequencyHours by scheduledRepo.frequencyHours.collectAsState()
    val scheduledRetentionLimit by scheduledRepo.retentionLimit.collectAsState()
    val scheduledRequiresCharging by scheduledRepo.requiresCharging.collectAsState()
    val scheduledRequiresDeviceIdle by scheduledRepo.requiresDeviceIdle.collectAsState()
    val scheduledRequiresUnmetered by scheduledRepo.requiresUnmetered.collectAsState()
    val scheduledLastRunAt by scheduledRepo.lastRunAt.collectAsState()
    val scheduledLastRunStatus by scheduledRepo.lastRunStatus.collectAsState()
    val scheduledLastRunError by scheduledRepo.lastRunError.collectAsState()

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var folderStatus by remember { mutableStateOf(FolderStatus.VALID) }

    LaunchedEffect(scheduledFolderUri, scheduledEnabled) {
        folderStatus = scheduledRepo.checkFolderStatus()
    }

    var phase by remember { mutableStateOf(BackupPhase.IDLE) }
    var lastBackupAt by remember { mutableStateOf(BackupRepository.lastBackupAt(context)) }
    var preview by remember { mutableStateOf<BackupSnapshot?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var restored by remember { mutableStateOf<RestoreResult?>(null) }

    val busy = phase != BackupPhase.IDLE
    LaunchedEffect(busy) { onBusyChange(busy) }

    // A restore is mid-write for as long as this is up. Leaving the screen
    // cannot stop it, but it can leave the user somewhere that will not tell
    // them how it went, so back is held until it lands.
    BackHandler(enabled = phase == BackupPhase.RESTORING) {}

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupTransfer.MIME_TYPE)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        phase = BackupPhase.BACKING_UP
        scope.launch {
            val manifest = repository.writeTo(uri)
            phase = BackupPhase.IDLE
            if (manifest == null) {
                failure = context.getString(R.string.bk_write_failed)
            } else {
                lastBackupAt = manifest.createdAt
                snackbarHostState.showSnackbar(backupWrittenMessage(context, manifest))
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scheduledRepo.setFolder(uri)
            scheduledRepo.setEnabled(true)
            folderStatus = FolderStatus.VALID
        }
    }

    // The filter stays wide for the reason the subscription importer's does:
    // providers report a zip as octet-stream, as x-zip-compressed, or as
    // nothing at all. The file is recognised by its contents either way.
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        phase = BackupPhase.READING
        scope.launch {
            val snapshot = try {
                repository.peek(uri)
            } catch (e: UnsupportedBackupException) {
                phase = BackupPhase.IDLE
                failure = context.getString(backupReadFailureMessage(e.failure))
                return@launch
            } catch (e: Exception) {
                // The read failed in some way the format layer did not name.
                // The generic sentence is shown; the reason that made this
                // report worth reading is in the log.
                KLog.e("BackupScreen", "Could not read the chosen backup file", e)
                null
            }
            phase = BackupPhase.IDLE
            if (snapshot == null) {
                failure = context.getString(R.string.bk_read_failed)
            } else {
                preview = snapshot
            }
        }
    }

    Scaffold(
        modifier = Modifier.sentupPage(),
        containerColor = Color.Transparent,
        snackbarHost = { DismissibleSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // The same title the other settings pages wear, so
                        // this reads as one of them rather than a screen apart.
                        Text(
                            text = stringResource(R.string.settings_backup_and_restore),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = lastBackupLine(context, lastBackupAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    // Beside the hub there is nothing to go back to.
                    if (LocalSettingsDetailShowsBack.current) IconButton(
                        onClick = onBack,
                        enabled = phase != BackupPhase.RESTORING,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(44.dp)
                    ) {
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
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "progress") {
                AnimatedVisibility(
                    visible = busy,
                    enter = fadeIn() + expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                    ),
                    exit = fadeOut() + shrinkVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                    )
                ) {
                    BusyCard(phase)
                }
            }

            item(key = "scheduled") {
                SettingsSection(title = stringResource(R.string.bk_scheduled_title)) {
                    SettingsCard {
                        SettingsToggleRow(
                            icon = Icons.Rounded.Schedule,
                            title = stringResource(R.string.bk_scheduled_title),
                            subtitle = stringResource(R.string.bk_scheduled_sub),
                            enabled = scheduledEnabled,
                            onToggle = { enable ->
                                if (enable && scheduledFolderUri == null) {
                                    folderLauncher.launch(null)
                                } else {
                                    scheduledRepo.setEnabled(enable)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = scheduledEnabled,
                            enter = fadeIn() + expandVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ),
                            exit = fadeOut() + shrinkVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                        ) {
                            Column {
                                if (folderStatus == FolderStatus.PERMISSION_REVOKED || folderStatus == FolderStatus.FOLDER_MISSING) {
                                    FolderWarningCard(
                                        status = folderStatus,
                                        onFix = { folderLauncher.launch(null) }
                                    )
                                }

                                SettingsDivider()
                                SettingsRow(
                                    icon = Icons.Rounded.Folder,
                                    title = stringResource(R.string.bk_folder_title),
                                    subtitle = scheduledFolderName ?: stringResource(R.string.bk_folder_none),
                                    onClick = { folderLauncher.launch(null) },
                                    showChevron = true
                                )

                                SettingsDivider()
                                SettingsRow(
                                    icon = Icons.Rounded.Update,
                                    title = stringResource(R.string.bk_freq_title),
                                    subtitle = frequencyLabel(scheduledFrequencyHours),
                                    onClick = { showFrequencyDialog = true },
                                    showChevron = true
                                )

                                SettingsDivider()
                                SettingsRow(
                                    icon = Icons.Rounded.Layers,
                                    title = stringResource(R.string.bk_retention_title),
                                    subtitle = retentionLabel(scheduledRetentionLimit),
                                    onClick = { showRetentionDialog = true },
                                    showChevron = true
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Power,
                                    title = stringResource(R.string.bk_constraint_charging),
                                    subtitle = stringResource(R.string.bk_constraint_charging_sub),
                                    enabled = scheduledRequiresCharging,
                                    onToggle = { scheduledRepo.setRequiresCharging(it) }
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Bedtime,
                                    title = stringResource(R.string.bk_constraint_idle),
                                    subtitle = stringResource(R.string.bk_constraint_idle_sub),
                                    enabled = scheduledRequiresDeviceIdle,
                                    onToggle = { scheduledRepo.setRequiresDeviceIdle(it) }
                                )

                                SettingsDivider()
                                SettingsToggleRow(
                                    icon = Icons.Rounded.Wifi,
                                    title = stringResource(R.string.bk_constraint_unmetered),
                                    subtitle = stringResource(R.string.bk_constraint_unmetered_sub),
                                    enabled = scheduledRequiresUnmetered,
                                    onToggle = { scheduledRepo.setRequiresUnmetered(it) }
                                )

                                if (scheduledFolderUri != null && folderStatus == FolderStatus.VALID) {
                                    SettingsDivider()
                                    SettingsRow(
                                        icon = Icons.Rounded.PlayArrow,
                                        title = stringResource(R.string.bk_run_now),
                                        subtitle = stringResource(R.string.bk_run_now_sub),
                                        onClick = {
                                            if (!busy) {
                                                phase = BackupPhase.BACKING_UP
                                                scope.launch {
                                                    when (val result = scheduledRepo.executeBackup()) {
                                                        is ScheduledBackupResult.Success -> {
                                                            phase = BackupPhase.IDLE
                                                            lastBackupAt = result.manifest.createdAt
                                                            val baseMsg = context.getString(
                                                                R.string.bk_backup_saved_to_folder,
                                                                scheduledFolderName ?: ""
                                                            )
                                                            val fullMsg = if (result.prunedCount > 0) {
                                                                baseMsg + " • " + context.getString(
                                                                    R.string.bk_pruned_count,
                                                                    result.prunedCount
                                                                )
                                                            } else {
                                                                baseMsg
                                                            }
                                                            snackbarHostState.showSnackbar(fullMsg)
                                                        }
                                                        is ScheduledBackupResult.Failure -> {
                                                            phase = BackupPhase.IDLE
                                                            failure = result.message
                                                            folderStatus = scheduledRepo.checkFolderStatus()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        showChevron = false
                                    )

                                    val statusCaption = when {
                                        scheduledLastRunAt > 0L && scheduledLastRunStatus == ScheduledBackupStatus.SUCCESS -> {
                                            stringResource(
                                                R.string.bk_last_run_success,
                                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                                    .format(Date(scheduledLastRunAt))
                                            )
                                        }
                                        scheduledLastRunStatus != ScheduledBackupStatus.IDLE && scheduledLastRunError != null -> {
                                            stringResource(R.string.bk_last_run_failed, scheduledLastRunError ?: "")
                                        }
                                        else -> stringResource(R.string.bk_last_run_never)
                                    }

                                    Text(
                                        text = statusCaption,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (scheduledLastRunStatus == ScheduledBackupStatus.SUCCESS || scheduledLastRunStatus == ScheduledBackupStatus.IDLE) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "actions") {
                SettingsSection(title = stringResource(R.string.bk_this_install)) {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Rounded.Save,
                            title = stringResource(R.string.bk_create),
                            subtitle = stringResource(R.string.bk_create_sub),
                            onClick = {
                                if (!busy) createLauncher.launch(BackupRepository.suggestedFileName())
                            },
                            showChevron = true
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Restore,
                            title = stringResource(R.string.bk_restore),
                            subtitle = stringResource(R.string.bk_restore_sub),
                            onClick = {
                                if (!busy) {
                                    openLauncher.launch(
                                        arrayOf(
                                            "application/zip",
                                            "application/x-zip-compressed",
                                            "application/octet-stream"
                                        )
                                    )
                                }
                            },
                            tint = SettingsRowDefaults.destructiveTint,
                            showChevron = true
                        )
                    }
                }
            }

            item(key = "included") {
                SettingsSection(title = stringResource(R.string.bk_holds)) {
                    SettingsCard {
                        InfoBlock(
                            icon = Icons.Rounded.Info,
                            lines = listOf(
                                stringResource(R.string.bk_hold_1),
                                stringResource(R.string.bk_hold_2),
                                stringResource(R.string.bk_hold_3),
                                stringResource(R.string.bk_hold_4),
                                stringResource(R.string.bk_hold_5),
                                stringResource(R.string.bk_hold_6)
                            )
                        )
                    }
                }
            }

            item(key = "excluded") {
                SettingsSection(title = stringResource(R.string.bk_not_holds)) {
                    SettingsCard {
                        InfoBlock(
                            icon = Icons.Rounded.CloudOff,
                            lines = listOf(
                                stringResource(R.string.bk_not_1),
                                stringResource(R.string.bk_not_2),
                                stringResource(R.string.bk_not_3)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showFrequencyDialog) {
        FrequencyDialog(
            currentHours = scheduledFrequencyHours,
            onSelect = { scheduledRepo.setFrequencyHours(it) },
            onDismiss = { showFrequencyDialog = false }
        )
    }

    if (showRetentionDialog) {
        RetentionDialog(
            currentLimit = scheduledRetentionLimit,
            onSelect = { scheduledRepo.setRetentionLimit(it) },
            onDismiss = { showRetentionDialog = false }
        )
    }

    preview?.let { snapshot ->
        RestoreConfirmDialog(
            snapshot = snapshot,
            onDismiss = { preview = null },
            onConfirm = {
                preview = null
                phase = BackupPhase.RESTORING
                scope.launch {
                    val result = repository.apply(snapshot)
                    phase = BackupPhase.IDLE
                    if (result.success) restored = result
                    else failure = context.getString(backupApplyFailureMessage(result.failure))
                }
            }
        )
    }

    restored?.let { result ->
        RestartDialog(result = result, onRestart = { restartApp(context) })
    }

    failure?.let { message ->
        FailureDialog(message = message, onDismiss = { failure = null })
    }
}

private enum class BackupPhase { IDLE, BACKING_UP, READING, RESTORING }

/**
 * The sentence for a refusal from the reader.
 *
 * [BackupTransfer] cannot say anything in the reader's language - it is the
 * format half and holds no `Context` - so it names the reason and this turns it
 * into the two strings there are. An unknown reason is treated as the generic
 * read failure rather than swallowed: a new code added to the data layer without
 * its string here should still say something true.
 */
@StringRes
private fun backupReadFailureMessage(failure: BackupReadFailure?): Int = when (failure) {
    BackupReadFailure.NOT_A_BACKUP -> R.string.bk_err_not_a_backup
    BackupReadFailure.FROM_NEWER_VERSION -> R.string.bk_err_newer_version
    null -> R.string.bk_read_failed
}

/** The sentence for a restore that stopped short. See [backupReadFailureMessage]. */
@StringRes
private fun backupApplyFailureMessage(failure: BackupApplyFailure?): Int = when (failure) {
    BackupApplyFailure.INCOMPLETE, null -> R.string.bk_err_restore_failed
}

// ---------------- Pieces ----------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BusyCard(phase: BackupPhase) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingIndicator(modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = when (phase) {
                        BackupPhase.BACKING_UP -> stringResource(R.string.bk_writing)
                        BackupPhase.READING -> stringResource(R.string.bk_reading)
                        BackupPhase.RESTORING -> stringResource(R.string.bk_restoring)
                        BackupPhase.IDLE -> ""
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (phase == BackupPhase.RESTORING) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.bk_dont_leave),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    lines: List<String>
) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEach { line ->
                Text(
                    text = line,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * What is in the file, and what agreeing to it costs.
 *
 * The counts come out of the backup's own manifest rather than out of the
 * snapshot that was just parsed, so a file written by a build that knew about
 * a store this one does not still describes itself honestly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreConfirmDialog(
    snapshot: BackupSnapshot,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val accounts = snapshot.profiles.count { it.kind == ProfileKind.YOUTUBE.name }
    val dialogContext = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.bk_confirm_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = manifestLine(dialogContext, snapshot.manifest),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val contents = describeContents(dialogContext, snapshot.manifest)
                if (contents.isEmpty()) {
                    // A valid backup of an install with nothing in it yet. Worth
                    // saying out loud: restoring it would wipe this device for
                    // nothing, and that is exactly the mistake to catch here.
                    Text(
                        text = stringResource(R.string.bk_empty_warn),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    contents.forEach { line ->
                        Text(
                            text = "•  $line",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.bk_replace_warn),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
                if (accounts > 0) {
                    Text(
                        text = stringResource(
                            if (accounts == 1) R.string.bk_no_signin_one else R.string.bk_no_signin_many,
                            accounts
                        ),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.bk_restarts),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.bk_replace_restore), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * The end of a restore.
 *
 * Deliberately not dismissible and with no way out but the button: the process
 * has to go before any of this is visible, and an app left running on top of
 * data it has already replaced would show a mix of the two.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestartDialog(result: RestoreResult, onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.bk_restored),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.bk_restart_needed),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.signInNeeded > 0) {
                    Text(
                        text = stringResource(R.string.bk_sign_in_again),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRestart) { Text(stringResource(R.string.bk_restart_now)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FailureDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.bk_failed_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } }
    )
}

// ---------------- Text ----------------

/** The backup row, in the language the phone is set to. */
internal fun lastBackupLine(context: android.content.Context, lastBackupAt: Long?): String {
    if (lastBackupAt == null) return context.getString(R.string.bk_never_backed_up)
    return context.getString(R.string.bk_last_backup_at, relativeDay(context, lastBackupAt))
}

/**
 * The hub's copy of the same line. SettingsScreen builds it inside `remember { }`,
 * which has no CompositionContext to read resources from and no Context to pass
 * one in, so this stays as it was until that call site takes a Context parameter.
 * Every screen in this package uses the [lastBackupLine] overload above.
 */
internal fun lastBackupLine(lastBackupAt: Long?): String {
    if (lastBackupAt == null) return "Never backed up from this device"
    val days = ((System.currentTimeMillis() - lastBackupAt) / 86_400_000L).toInt()
    val when1 = when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        days < 30 -> "$days days ago"
        else -> "on " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastBackupAt))
    }
    return "Last backup $when1"
}

private fun relativeDay(context: android.content.Context, timestamp: Long): String {
    val days = ((System.currentTimeMillis() - timestamp) / 86_400_000L).toInt()
    return when {
        days <= 0 -> context.getString(R.string.bk_day_today)
        days == 1 -> context.getString(R.string.bk_day_yesterday)
        days < 30 -> context.getString(R.string.bk_day_days_ago, days)
        else -> context.getString(
            R.string.bk_day_on_date,
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
        )
    }
}

private fun manifestLine(context: android.content.Context, manifest: BackupManifest): String {
    val date = if (manifest.createdAt > 0) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(manifest.createdAt))
    } else {
        context.getString(R.string.bk_manifest_unknown_date)
    }
    return context.getString(R.string.bk_manifest_taken, date) +
        (manifest.device?.let { context.getString(R.string.bk_manifest_from, it) }.orEmpty()) +
        (manifest.appVersionName?.let { context.getString(R.string.bk_manifest_version, it) }.orEmpty())
}

private fun backupWrittenMessage(context: android.content.Context, manifest: BackupManifest): String {
    val lines = describeContents(context, manifest)
    return if (lines.isEmpty()) {
        context.getString(R.string.bk_backup_saved)
    } else {
        context.getString(R.string.bk_backup_saved_counts, lines.joinToString(", "))
    }
}

/**
 * The manifest's counts as lines, skipping anything at zero.
 *
 * Zero-suppressed rather than shown as "0 playlists": a list of zeroes reads
 * like something failed, when the honest meaning is simply that the install had
 * none. An empty result is handled separately by the caller, which says so.
 */
private fun describeContents(
    context: android.content.Context,
    manifest: BackupManifest
): List<String> {
    fun count(key: String) = manifest.contents[key] ?: 0
    fun line(key: String, @androidx.annotation.PluralsRes plural: Int): String? {
        val value = count(key)
        if (value <= 0) return null
        return context.resources.getQuantityString(plural, value, value)
    }
    val profiles = count(BackupRepository.COUNT_PROFILES)
    return listOfNotNull(
        line(BackupRepository.COUNT_PLAYLISTS, R.plurals.bk_count_playlists),
        line(BackupRepository.COUNT_VIDEO_PLAYLISTS, R.plurals.bk_count_video_playlists),
        line(BackupRepository.COUNT_SAVED_PLAYLISTS, R.plurals.bk_count_saved_playlists),
        line(BackupRepository.COUNT_LIKED_SONGS, R.plurals.bk_count_liked_songs),
        line(BackupRepository.COUNT_PLAY_HISTORY, R.plurals.bk_count_plays),
        line(BackupRepository.COUNT_WATCH_HISTORY, R.plurals.bk_count_watch_history),
        line(BackupRepository.COUNT_SUBSCRIPTIONS, R.plurals.bk_count_subscriptions),
        line(BackupRepository.COUNT_BLOCKED, R.plurals.bk_count_blocked),
        line(BackupRepository.COUNT_HIDDEN, R.plurals.bk_count_hidden),
        line(BackupRepository.COUNT_SETTINGS, R.plurals.bk_count_settings),
        // Only worth a line when there is more than one identity in the file;
        // "1 profile" is true of every install and tells nobody anything.
        profiles.takeIf { it > 1 }?.let {
            context.resources.getQuantityString(R.plurals.bk_count_profiles, it, it)
        }
    )
}

// ---------------- Restart ----------------

/**
 * Relaunch the app as a cold start.
 *
 * A restore rewrites preference files and stored JSON underneath a process
 * that has already read all of it: `SharedPreferences` serves from an
 * in-memory map, five stores seed process-wide caches once, and `MusicService`
 * holds a repository and a media session of its own. Rather than reloading
 * each of those by hand - a list with no compiler behind it, where one
 * omission is a half-restored install - the process goes and comes back.
 *
 * The launch intent is started before the exit so there is something to return
 * to; `exit` rather than `killProcess` so the VM shuts down through the normal
 * path. If the launch intent cannot be resolved, the process is left alone
 * rather than killed into nothing.
 */
private fun restartApp(context: android.content.Context) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ?: return
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}


@Composable
private fun FolderWarningCard(
    status: FolderStatus,
    onFix: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (status == FolderStatus.PERMISSION_REVOKED) {
                        stringResource(R.string.bk_folder_permission_lost)
                    } else {
                        stringResource(R.string.bk_folder_not_found)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onFix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.bk_folder_pick), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FrequencyDialog(
    currentHours: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.bk_freq_title),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                com.sentup.android.ui.components.SegmentedGroup(
                    ScheduledBackupRepository.FREQUENCY_OPTIONS
                ) { index, count, option ->
                    SegmentedChoiceRow(
                        selected = option.hours == currentHours,
                        onClick = {
                            haptics.confirm()
                            onSelect(option.hours)
                            onDismiss()
                        },
                        index = index,
                        count = count,
                        label = stringResource(option.labelRes)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun RetentionDialog(
    currentLimit: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.bk_retention_dialog_title),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.bk_retention_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                com.sentup.android.ui.components.SegmentedGroup(
                    ScheduledBackupRepository.RETENTION_OPTIONS
                ) { index, rowCount, count ->
                    SegmentedChoiceRow(
                        selected = count == currentLimit,
                        onClick = {
                            haptics.confirm()
                            onSelect(count)
                            onDismiss()
                        },
                        index = index,
                        count = rowCount,
                        label = when (count) {
                            3 -> stringResource(R.string.bk_retention_keep_3)
                            5 -> stringResource(R.string.bk_retention_keep_5)
                            10 -> stringResource(R.string.bk_retention_keep_10)
                            20 -> stringResource(R.string.bk_retention_keep_20)
                            else -> stringResource(R.string.bk_retention_format, count)
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun frequencyLabel(hours: Long): String {
    val opt = ScheduledBackupRepository.FREQUENCY_OPTIONS.firstOrNull { it.hours == hours }
    return if (opt != null) stringResource(opt.labelRes) else "${hours}h"
}

@Composable
private fun retentionLabel(limit: Int): String {
    return when (limit) {
        3 -> stringResource(R.string.bk_retention_keep_3)
        5 -> stringResource(R.string.bk_retention_keep_5)
        10 -> stringResource(R.string.bk_retention_keep_10)
        20 -> stringResource(R.string.bk_retention_keep_20)
        else -> stringResource(R.string.bk_retention_format, limit)
    }
}
