package com.sentup.android.ui.video

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.data.VideoChapter
import com.sentup.android.data.VttCue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoPowerToolsSheet(
    videoId: String,
    videoTitle: String,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    focusModeEnabled: Boolean,
    subtitles: List<VttCue>,
    chapters: List<VideoChapter>,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    onSetAbLoopStart: (Long?) -> Unit,
    onSetAbLoopEnd: (Long?) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleFocusMode: (Boolean) -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember(videoId) { mutableStateOf(VideoNotesStore.getNotes(context, videoId)) }
    var noteDraft by remember { mutableStateOf("") }
    var transcriptQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                ) {
                    Text(
                        text = "YouTube Güç Araçları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("A-B Döngü") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Notlar (${notes.size})") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Transkript") },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Hız & Odak") },
                )
            }

            when (selectedTab) {
                0 -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                val rangeLabel = if (abLoopStartMs != null && abLoopEndMs != null) {
                                    "Aktif aralık: ${VideoToolsEngine.formatTimestampMs(abLoopStartMs)} → ${VideoToolsEngine.formatTimestampMs(abLoopEndMs)}"
                                } else {
                                    "Şu anki konum: ${VideoToolsEngine.formatTimestampMs(currentPositionMs)}"
                                }
                                Text(
                                    text = rangeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { onSetAbLoopStart(currentPositionMs) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        if (abLoopStartMs == null) "A Noktasını Seç"
                                        else "A: ${VideoToolsEngine.formatTimestampMs(abLoopStartMs)}"
                                    )
                                }
                                Button(
                                    onClick = {
                                        val start = abLoopStartMs ?: (currentPositionMs - 10_000L).coerceAtLeast(0L)
                                        if (abLoopStartMs == null) onSetAbLoopStart(start)
                                        onSetAbLoopEnd(currentPositionMs.coerceAtLeast(start + 1_000L))
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        if (abLoopEndMs == null) "B Noktasını Seç"
                                        else "B: ${VideoToolsEngine.formatTimestampMs(abLoopEndMs)}"
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val end = currentPositionMs.coerceAtLeast(1_000L)
                                        val start = (end - 15_000L).coerceAtLeast(0L)
                                        onSetAbLoopStart(start)
                                        onSetAbLoopEnd(end)
                                        onSeekTo(start)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Son 15 sn Tekrarla")
                                }
                                TextButton(
                                    onClick = {
                                        onSetAbLoopStart(null)
                                        onSetAbLoopEnd(null)
                                    },
                                    enabled = abLoopStartMs != null || abLoopEndMs != null,
                                ) {
                                    Text("Temizle")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            placeholder = {
                                Text("${VideoToolsEngine.formatTimestampMs(currentPositionMs)} için not ekle…")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                notes = VideoNotesStore.addNote(
                                    context = context,
                                    videoId = videoId,
                                    positionMs = currentPositionMs,
                                    text = noteDraft,
                                )
                                noteDraft = ""
                            },
                            enabled = noteDraft.isNotBlank(),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                    }

                    if (notes.isEmpty()) {
                        Text(
                            text = "Bu video için henüz zaman damgalı not yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(notes, key = { it.id }) { note ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSeekTo(note.positionMs)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = VideoToolsEngine.formatTimestampMs(note.positionMs),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = note.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp),
                                        )
                                        IconButton(
                                            onClick = {
                                                notes = VideoNotesStore.deleteNote(context, videoId, note.id)
                                            }
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    OutlinedTextField(
                        value = transcriptQuery,
                        onValueChange = { transcriptQuery = it },
                        placeholder = { Text("Transkriptte veya bölümlerde ara…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filteredCues = remember(subtitles, transcriptQuery) {
                        VideoToolsEngine.searchTranscriptCues(subtitles, transcriptQuery)
                    }
                    when {
                        filteredCues.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(filteredCues.take(150)) { cue ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSeekTo(cue.startMs)
                                                onDismiss()
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = VideoToolsEngine.formatTimestampMs(cue.startMs),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = cue.text,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        chapters.isNotEmpty() -> {
                            val q = transcriptQuery.trim().lowercase()
                            val filteredChapters = chapters.filter {
                                q.isEmpty() || it.title.lowercase().contains(q)
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(filteredChapters) { chapter ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSeekTo(chapter.startMs)
                                                onDismiss()
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = VideoToolsEngine.formatTimestampMs(chapter.startMs),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = chapter.title,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bu videoda aranabilir altyazı veya bölüm bulunamadı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Süper Hız (YouTube 2x sınırının ötesi)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { speed ->
                                    val selected = kotlin.math.abs(playbackSpeed - speed) < 0.03f
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetPlaybackSpeed(speed) },
                                        label = { Text("${speed}x") },
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Odak Modu (Önerilenleri Gizle)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Video altındaki öneri karmaşasını gizler; ders/çalışma için ideal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = focusModeEnabled,
                                onCheckedChange = onToggleFocusMode,
                            )
                        }
                    }

                    val shareUrl = VideoToolsEngine.buildShareTimestampUrl(videoId, currentPositionMs)
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(shareUrl))
                            Toast.makeText(context, "Zaman damgalı link kopyalandı: $shareUrl", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Şu Anki Saniyeden Kopyala (${VideoToolsEngine.formatTimestampMs(currentPositionMs)})")
                    }
                }
            }
        }
    }
}
