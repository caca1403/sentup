package com.sentup.android.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.CustomFolderTree
import com.sentup.android.data.MusicFolder
import com.sentup.android.data.Song
import com.sentup.android.ml.AudioFeatureAndDspEngine
import com.sentup.android.ml.EnvironmentAudioMode
import com.sentup.android.ml.MoodAndDiscoveryEngine
import com.sentup.android.ml.SensorAndEnvironmentEngine
import com.sentup.android.ml.SequenceAndBanditEngine
import com.sentup.android.ui.theme.SentupRadii
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class NeuralLabTab { Map, Clusters, Drift, Sensors }

@Composable
fun SongDnaRadarChart(
    dna: AudioFeatureAndDspEngine.SongDnaProfile,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val values = remember(dna) {
        listOf(dna.energy, dna.tempoIndex, dna.vocalness, dna.bass, dna.brightness, dna.dynamicRange)
    }
    val labels = listOf("Energy", "Pace", "Vocal", "Bass", "Bright", "Dyn")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.42f
            val axes = 6

            // Grid rings
            listOf(0.33f, 0.66f, 1.0f).forEach { scale ->
                val ringPath = Path()
                for (i in 0 until axes) {
                    val angle = (-PI / 2.0 + 2.0 * PI * i / axes).toFloat()
                    val px = cx + cos(angle) * radius * scale
                    val py = cy + sin(angle) * radius * scale
                    if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
                }
                ringPath.close()
                drawPath(ringPath, color = accent.copy(alpha = 0.22f), style = Stroke(width = 1.5f))
            }

            // Data polygon
            val dataPath = Path()
            values.forEachIndexed { i, v ->
                val angle = (-PI / 2.0 + 2.0 * PI * i / axes).toFloat()
                val r = radius * v.coerceIn(0.05f, 1f)
                val px = cx + cos(angle) * r
                val py = cy + sin(angle) * r
                if (i == 0) dataPath.moveTo(px, py) else dataPath.lineTo(px, py)
            }
            dataPath.close()
            drawPath(dataPath, color = accent.copy(alpha = 0.28f))
            drawPath(dataPath, color = accent, style = Stroke(width = 3f))
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEachIndexed { idx, label ->
                Text(
                    text = "$label ${(values[idx] * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralLabSheet(
    songs: List<Song>,
    customFolders: List<MusicFolder>,
    playCounts: Map<String, Int>,
    recentlyPlayed: List<Song>,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(NeuralLabTab.Map) }
    val context = LocalContext.current

    val embeddingsById = remember(songs) {
        songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }
    }
    val mapPoints = remember(songs) {
        MoodAndDiscoveryEngine.projectTo2DMusicMap(songs)
    }

    var targetValence by remember { mutableFloatStateOf(0.55f) }
    var targetEnergy by remember { mutableFloatStateOf(0.65f) }
    val mapSelection = remember(mapPoints, targetValence, targetEnergy) {
        MoodAndDiscoveryEngine.pickTracksNearMapCoordinate(mapPoints, targetValence, targetEnergy, limit = 15)
    }

    val sensorEngine = remember(context) { SensorAndEnvironmentEngine(context) }
    DisposableEffect(activeTab) {
        if (activeTab == NeuralLabTab.Sensors) sensorEngine.startMonitoring()
        onDispose { sensorEngine.stopMonitoring() }
    }
    val spm by sensorEngine.estimatedSpm.collectAsState()
    val stationary by sensorEngine.isStationaryForSleep.collectAsState()
    val envMode by sensorEngine.environmentMode.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = stringResource(R.string.lib_neural_lab_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.lib_neural_lab_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    NeuralLabTab.Map to R.string.lib_neural_tab_map,
                    NeuralLabTab.Clusters to R.string.lib_neural_tab_clusters,
                    NeuralLabTab.Drift to R.string.lib_neural_tab_drift,
                    NeuralLabTab.Sensors to R.string.lib_neural_tab_sensors
                )
                tabs.forEachIndexed { idx, (tab, labelRes) ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size)
                    ) {
                        Text(stringResource(labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            when (activeTab) {
                NeuralLabTab.Map -> {
                    Text(
                        text = stringResource(R.string.lib_neural_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceColor)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        targetValence = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                                        targetEnergy = (1f - offset.y / size.height).coerceIn(0.05f, 0.95f)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        targetValence = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                                        targetEnergy = (1f - change.position.y / size.height).coerceIn(0.05f, 0.95f)
                                    }
                                }
                        ) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.2f),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = primaryColor.copy(alpha = 0.2f),
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 2f
                            )
                            mapPoints.take(160).forEach { pt ->
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.45f),
                                    radius = 6f,
                                    center = Offset(pt.valence * size.width, (1f - pt.energy) * size.height)
                                )
                            }
                            val selCenter = Offset(targetValence * size.width, (1f - targetEnergy) * size.height)
                            drawCircle(color = primaryColor.copy(alpha = 0.22f), radius = 38f, center = selCenter)
                            drawCircle(color = primaryColor, radius = 12f, center = selCenter)
                        }
                    }
                    Button(
                        onClick = {
                            if (mapSelection.isNotEmpty()) {
                                onPlayQueue(mapSelection, mapSelection.first())
                                onDismiss()
                            }
                        },
                        enabled = mapSelection.isNotEmpty(),
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.lib_neural_map_play, mapSelection.size))
                    }
                }

                NeuralLabTab.Clusters -> {
                    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                    val nowMs = remember { System.currentTimeMillis() }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val predicted = SequenceAndBanditEngine.predictNextTracks(
                                    recentSongs = recentlyPlayed,
                                    candidates = songs,
                                    embeddingsById = embeddingsById,
                                    banditStats = emptyMap(),
                                    recentEvents = emptyList(),
                                    hourOfDay = hour,
                                    nowMs = nowMs
                                )
                                if (predicted.isNotEmpty()) {
                                    onPlayQueue(predicted, predicted.first())
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.lib_neural_rnn_next))
                        }
                        OutlinedButton(
                            onClick = {
                                val diverse = SequenceAndBanditEngine.rerankWithDppDiversity(
                                    rankedSongs = songs.shuffled(),
                                    embeddingsById = embeddingsById
                                )
                                if (diverse.isNotEmpty()) {
                                    onPlayQueue(diverse, diverse.first())
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.lib_neural_dpp_shuffle))
                        }
                        OutlinedButton(
                            onClick = {
                                val arc = AudioFeatureAndDspEngine.sortFolderByEnergyArc(songs.take(30))
                                if (arc.isNotEmpty()) {
                                    onPlayQueue(arc, arc.first())
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.lib_neural_arc_play))
                        }
                    }

                    val clusters = remember(songs, embeddingsById) {
                        AudioFeatureAndDspEngine.clusterUnorganizedSongs(songs, embeddingsById, k = 4)
                    }
                    Text(
                        text = stringResource(R.string.lib_neural_clusters_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    clusters.forEach { cluster ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayQueue(cluster.songs, cluster.songs.firstOrNull())
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cluster.label, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${cluster.songs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    onPlayQueue(cluster.songs, cluster.songs.firstOrNull())
                                    onDismiss()
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                NeuralLabTab.Drift -> {
                    val drift = remember(songs, recentlyPlayed, playCounts, embeddingsById) {
                        val recentIds = recentlyPlayed.take(15).map { it.id }
                        val baseIds = playCounts.entries.sortedByDescending { it.value }.take(40).map { it.key }
                        MoodAndDiscoveryEngine.detectTasteDrift(songs, recentIds, baseIds, embeddingsById)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.lib_neural_drift_score, (drift.driftScore * 100).roundToInt()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    if (drift.isShifting) R.string.lib_neural_drift_shifting
                                    else R.string.lib_neural_drift_steady
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (customFolders.size >= 2) {
                        val f1 = customFolders[0]
                        val f2 = customFolders[1]
                        val s1 = CustomFolderTree.allDescendantSongs(customFolders, f1.id, songs)
                        val s2 = CustomFolderTree.allDescendantSongs(customFolders, f2.id, songs)
                        val overlap = AudioFeatureAndDspEngine.computeFolderOverlapPercent(s1, s2, embeddingsById)
                        Text(
                            text = stringResource(R.string.lib_neural_overlap_result, f1.name, f2.name, overlap),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val gems = remember(songs, playCounts) {
                        MoodAndDiscoveryEngine.rankForgottenGems(
                            songs = songs,
                            playCounts = playCounts,
                            lastPlayedAtMs = emptyMap(),
                            nowMs = System.currentTimeMillis()
                        )
                    }
                    if (gems.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.lib_neural_gems_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        gems.take(5).forEach { gem ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayQueue(gems, gem)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(gem.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                        Text(gem.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                NeuralLabTab.Sensors -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(R.string.lib_neural_cadence_spm, spm),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (stationary) R.string.lib_neural_sleep_stationary
                                    else R.string.lib_neural_sleep_moving
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    val matched = sensorEngine.matchSongsToCadence(songs, spm)
                                    if (matched.isNotEmpty()) {
                                        onPlayQueue(matched, matched.first())
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(SentupRadii.pill)
                            ) {
                                Text(stringResource(R.string.lib_neural_cadence_play))
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.lib_neural_env_mode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EnvironmentAudioMode.entries.forEach { mode ->
                            FilterChip(
                                selected = envMode == mode,
                                onClick = { sensorEngine.setEnvironmentMode(mode) },
                                label = { Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }
    }
}
