package com.sentup.android.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.FolderAutoRule
import com.sentup.android.data.FolderEqPreset
import com.sentup.android.data.LibraryHealthReport
import com.sentup.android.data.MusicFolder
import com.sentup.android.data.Song
import com.sentup.android.data.SongBookmark
import com.sentup.android.data.TrackTrim
import com.sentup.android.ui.theme.SentupRadii
import java.util.Locale

private val SpeedOptions = listOf(0.85f, 1.0f, 1.15f, 1.25f, 1.5f)

@Composable
fun eqPresetLabel(preset: FolderEqPreset): String = when (preset) {
    FolderEqPreset.Default -> stringResource(R.string.lib_folder_eq_default)
    FolderEqPreset.BassBoost -> stringResource(R.string.lib_folder_eq_bass)
    FolderEqPreset.Vocal -> stringResource(R.string.lib_folder_eq_vocal)
    FolderEqPreset.NightLoFi -> stringResource(R.string.lib_folder_eq_lofi)
    FolderEqPreset.Rock -> stringResource(R.string.lib_folder_eq_rock)
    FolderEqPreset.Acoustic -> stringResource(R.string.lib_folder_eq_acoustic)
}

@Composable
fun FolderAudioProfileDialog(
    folder: MusicFolder,
    onDismiss: () -> Unit,
    onConfirm: (playbackSpeed: Float?, eqPreset: FolderEqPreset?) -> Unit
) {
    var selectedSpeed by remember(folder.id) { mutableFloatStateOf(folder.playbackSpeed ?: 1.0f) }
    var selectedEq by remember(folder.id) { mutableStateOf(folder.eqPreset ?: FolderEqPreset.Default) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
        title = { Text(stringResource(R.string.lib_folder_audio_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.lib_folder_audio_profile_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.lib_folder_speed_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedOptions.forEach { speed ->
                        FilterChip(
                            selected = kotlin.math.abs(selectedSpeed - speed) < 0.01f,
                            onClick = { selectedSpeed = speed },
                            label = { Text("${speed}x") }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.lib_folder_eq_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FolderEqPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedEq == preset,
                            onClick = { selectedEq = preset },
                            label = { Text(eqPresetLabel(preset)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val speedArg = selectedSpeed.takeIf { kotlin.math.abs(it - 1.0f) >= 0.01f }
                    val eqArg = selectedEq.takeIf { it != FolderEqPreset.Default }
                    onConfirm(speedArg, eqArg)
                }
            ) {
                Text(stringResource(R.string.lib_custom_folder_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun FolderAutoRuleDialog(
    folder: MusicFolder,
    onDismiss: () -> Unit,
    onConfirm: (FolderAutoRule?) -> Unit
) {
    var keyword by remember(folder.id) { mutableStateOf(folder.autoRule?.keyword.orEmpty()) }
    var minPlays by remember(folder.id) { mutableIntStateOf(folder.autoRule?.minPlays ?: 0) }
    var downloadedOnly by remember(folder.id) { mutableStateOf(folder.autoRule?.downloadedOnly ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lib_folder_auto_rule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.lib_folder_auto_rule_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.lib_folder_rule_keyword)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.lib_folder_rule_min_plays),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (minPlays > 0) minPlays -= 1 }) {
                            Icon(Icons.Rounded.Remove, contentDescription = null)
                        }
                        Text(
                            text = minPlays.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { minPlays += 1 }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.lib_folder_rule_downloaded),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = downloadedOnly,
                        onCheckedChange = { downloadedOnly = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rule = FolderAutoRule(
                        keyword = keyword.trim(),
                        minPlays = minPlays,
                        downloadedOnly = downloadedOnly
                    )
                    onConfirm(rule.takeUnless { it.isEmpty })
                }
            ) {
                Text(stringResource(R.string.lib_custom_folder_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (folder.autoRule != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(stringResource(R.string.lib_folder_rule_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackTrimAndBookmarksSheet(
    song: Song,
    currentTrim: TrackTrim?,
    bookmarks: List<SongBookmark>,
    onSaveTrim: (startMs: Long, endMs: Long?) -> Unit,
    onClearTrim: () -> Unit,
    onAddBookmark: (positionMs: Long, note: String) -> Unit,
    onDeleteBookmark: (bookmarkId: String) -> Unit,
    onJumpToTimestamp: (Long) -> Unit,
    allSongs: List<Song> = emptyList(),
    onPlaySimilar: ((List<Song>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var startSecText by remember(song.id, currentTrim) {
        mutableStateOf(((currentTrim?.startMs ?: 0L) / 1000L).takeIf { it > 0 }?.toString().orEmpty())
    }
    var endSecText by remember(song.id, currentTrim) {
        mutableStateOf(currentTrim?.endMs?.let { (it / 1000L).toString() }.orEmpty())
    }
    var bookmarkSecText by remember(song.id) { mutableStateOf("") }
    var bookmarkNoteText by remember(song.id) { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = stringResource(R.string.lib_track_trim_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.lib_track_trim_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startSecText,
                    onValueChange = { startSecText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.lib_track_trim_start)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endSecText,
                    onValueChange = { endSecText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.lib_track_trim_end)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val startMs = (startSecText.toLongOrNull() ?: 0L).coerceAtLeast(0L) * 1000L
                        val endMs = endSecText.toLongOrNull()?.takeIf { it > 0 }?.let { it * 1000L }
                        onSaveTrim(startMs, endMs)
                    },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Text(stringResource(R.string.lib_custom_folder_save))
                }
                    if (currentTrim != null) {
                        OutlinedButton(
                            onClick = {
                                startSecText = ""
                                endSecText = ""
                                onClearTrim()
                            },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Text(stringResource(R.string.lib_track_trim_clear))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val auto = com.sentup.android.ml.AudioFeatureAndDspEngine.estimateSmartTrim(song)
                            startSecText = (auto.startMs / 1000L).takeIf { it > 0L }?.toString().orEmpty()
                            endSecText = auto.endMs?.let { (it / 1000L).toString() }.orEmpty()
                            onSaveTrim(auto.startMs, auto.endMs)
                        },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Text(stringResource(R.string.lib_track_auto_trim))
                    }
                }

            val songDna = remember(song.id) { com.sentup.android.ml.AudioFeatureAndDspEngine.extractSongDna(song) }
            val camelot = remember(song.id) { com.sentup.android.ml.AudioFeatureAndDspEngine.camelotKey(song) }
            val eqProfile = remember(song.id) { com.sentup.android.ml.AudioFeatureAndDspEngine.computeAdaptiveEqCurve(song) }
            val hookMs = remember(song.id) { com.sentup.android.ml.AudioFeatureAndDspEngine.detectHookSegmentMs(song) }

            HorizontalDivider()
            Text(
                text = "${stringResource(R.string.lib_track_dna_title)} · $camelot · LUFS ${String.format(Locale.US, "%+.1f dB", eqProfile.lufsOffsetDb)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            SongDnaRadarChart(dna = songDna)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onJumpToTimestamp(hookMs) },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Text(stringResource(R.string.lib_track_jump_hook))
                }
                if (onPlaySimilar != null && allSongs.size > 1) {
                    OutlinedButton(
                        onClick = {
                            val sim = com.sentup.android.ml.AudioFeatureAndDspEngine.findSimilarTracks(
                                anchor = song,
                                pool = allSongs,
                                embeddingsById = emptyMap(),
                                limit = 15
                            )
                            if (sim.isNotEmpty()) {
                                onPlaySimilar(listOf(song) + sim)
                            }
                        },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Text(stringResource(R.string.lib_track_similar))
                    }
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.lib_track_bookmarks_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = bookmarkSecText,
                    onValueChange = { bookmarkSecText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.lib_track_bookmark_sec_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(104.dp)
                )
                OutlinedTextField(
                    value = bookmarkNoteText,
                    onValueChange = { bookmarkNoteText = it },
                    label = { Text(stringResource(R.string.lib_track_bookmark_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val posMs = (bookmarkSecText.toLongOrNull() ?: 0L).coerceAtLeast(0L) * 1000L
                    onAddBookmark(posMs, bookmarkNoteText.trim())
                    bookmarkSecText = ""
                    bookmarkNoteText = ""
                },
                enabled = bookmarkSecText.isNotBlank() || bookmarkNoteText.isNotBlank(),
                shape = RoundedCornerShape(SentupRadii.pill)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.lib_track_bookmark_add))
            }

            if (bookmarks.isEmpty()) {
                Text(
                    text = stringResource(R.string.lib_track_bookmarks_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                bookmarks.forEach { bm ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpToTimestamp(bm.positionMs) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatBookmarkTime(bm.positionMs),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = bm.note.ifBlank { formatBookmarkTime(bm.positionMs) },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCleanerSheet(
    report: LibraryHealthReport,
    onPlaySong: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = stringResource(R.string.lib_cleaner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.lib_cleaner_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (report.totalIssues == 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.lib_cleaner_healthy),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (report.duplicateGroups.isNotEmpty()) {
                        item(key = "cleaner_dup_header") {
                            Text(
                                text = stringResource(R.string.lib_cleaner_duplicates, report.duplicateGroups.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(report.duplicateGroups, key = { "dup_${it.normalizedKey}" }) { group ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.songs.first().title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${group.songs.first().artist} · ${stringResource(R.string.lib_cleaner_copies, group.songs.size)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onPlaySong(group.songs.first()) }) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }

                    if (report.missingArtistSongs.isNotEmpty()) {
                        item(key = "cleaner_artist_header") {
                            Text(
                                text = stringResource(R.string.lib_cleaner_missing_artist, report.missingArtistSongs.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(report.missingArtistSongs, key = { "no_artist_${it.id}" }) { song ->
                            CleanerOrphanRow(song = song, onPlay = { onPlaySong(song) })
                        }
                    }

                    if (report.missingAlbumSongs.isNotEmpty()) {
                        item(key = "cleaner_album_header") {
                            Text(
                                text = stringResource(R.string.lib_cleaner_missing_album, report.missingAlbumSongs.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(report.missingAlbumSongs, key = { "no_album_${it.id}" }) { song ->
                            CleanerOrphanRow(song = song, onPlay = { onPlaySong(song) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanerOrphanRow(
    song: Song,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            }
        }
    }
}

private fun formatBookmarkTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

@Composable
fun FolderExportImportDialog(
    exportedJson: String,
    onImportJson: (String) -> Int,
    onDismiss: () -> Unit
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var importText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val copiedMsg = stringResource(R.string.lib_folder_copied_json)
    val invalidMsg = stringResource(R.string.lib_folder_import_invalid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lib_folder_export_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.lib_folder_export_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(exportedJson))
                        statusMessage = copiedMsg
                    },
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.lib_folder_copy_json))
                }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text(stringResource(R.string.lib_folder_import_hint)) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            val importedFmt = stringResource(R.string.lib_folder_imported_count, 0)
            Button(
                onClick = {
                    val count = onImportJson(importText)
                    if (count < 0) {
                        statusMessage = invalidMsg
                    } else {
                        statusMessage = importedFmt.replace("0", count.toString())
                        importText = ""
                    }
                },
                enabled = importText.isNotBlank()
            ) {
                Text(stringResource(R.string.lib_folder_import_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
