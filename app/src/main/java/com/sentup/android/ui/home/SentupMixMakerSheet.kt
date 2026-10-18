package com.sentup.android.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.ArtistItem
import com.sentup.android.data.PlaylistRepository
import com.sentup.android.data.Song
import com.sentup.android.data.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The autonomous-list maker. Artists come from a real YouTube Music search
 * (anything, not only what has been played) plus a quick row from the play
 * history; each picked artist contributes its popular songs - the artist
 * page's own top list, with local history as the fallback - so the built
 * playlist is real catalogue, not a replay of one's own queue. The result is
 * an ordinary playlist: Library, share, delete, everything behaves as usual.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SentupMixMakerSheet(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var historyArtists by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var picked by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var perArtistText by remember { mutableStateOf("15") }
    val perArtist = perArtistText.toIntOrNull()?.coerceIn(1, 500) ?: 15
    var windowDays by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.mixmaker_default_name)

    LaunchedEffect(windowDays) {
        val cutoff =
            if (windowDays == 0) 0L else System.currentTimeMillis() - windowDays * 86_400_000L
        val plays = withContext(Dispatchers.IO) { StatsRepository(context).loadHistory() }
        // Group case-insensitively and show the most-used spelling: "gripin"
        // and "Gripin" are one artist, and listing both was the "same artist
        // three times" bug. The chosen window trims how far back to look.
        historyArtists = plays.asSequence()
            .filter { it.artist.isNotBlank() && it.timestamp >= cutoff }
            .groupBy { it.artist.trim().lowercase() }
            .map { (_, entries) ->
                val spelling = entries.groupingBy { it.artist.trim() }.eachCount()
                    .maxByOrNull { it.value }?.key.orEmpty()
                spelling to entries.size
            }
            .sortedByDescending { it.second }
            .take(12)
    }

    // Debounced so every keystroke is not a search request.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        results = runCatching { viewModel.searchArtists(query.trim()) }.getOrDefault(emptyList())
        searching = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.mixmaker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.mixmaker_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.mixmaker_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (searching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            results.forEach { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (picked.none { it.id == artist.id }) picked = picked + artist
                            query = ""
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).then(
                            Modifier.padding(0.dp)
                        ),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Column {
                        Text(artist.name, style = MaterialTheme.typography.bodyLarge)
                        artist.subscriberCount?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (historyArtists.isNotEmpty()) {
                Text(
                    stringResource(R.string.mixmaker_artists_recent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    historyArtists.forEach { (artist, plays) ->
                        MakerChip(
                            selected = picked.any { it.name == artist },
                            label = artist,
                            badge = "$plays",
                            onClick = {
                                picked = if (picked.any { it.name == artist })
                                    picked.filterNot { it.name == artist }
                                else picked + ArtistItem(id = "", name = artist)
                            }
                        )
                    }
                }
            }

            if (picked.isNotEmpty()) {
                Text(
                    stringResource(R.string.mixmaker_picked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    picked.forEach { artist ->
                        MakerChip(
                            selected = true,
                            label = artist.name,
                            badge = null,
                            trailing = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            onClick = { picked = picked.filterNot { it.id == artist.id && it.name == artist.name } }
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.mixmaker_window),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(7, 30, 90, 0).forEach { days ->
                    MakerChip(
                        selected = windowDays == days,
                        label = if (days == 0) stringResource(R.string.mixmaker_window_all)
                        else String.format(stringResource(R.string.mixmaker_window_days), days),
                        badge = null,
                        onClick = { windowDays = days }
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.mixmaker_per_artist),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(10, 15, 25, 50).forEach { n ->
                    MakerChip(
                        selected = perArtist == n,
                        label = "$n",
                        badge = null,
                        onClick = { perArtistText = n.toString() }
                    )
                }
                // The count is freely settable, not just the presets; a number
                // larger than an artist's own songs simply takes them all.
                OutlinedTextField(
                    value = perArtistText,
                    onValueChange = { v -> perArtistText = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.mixmaker_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (creating) return@Button
                    creating = true
                    val chosen = picked
                    val chosenPer = perArtist
                    val chosenWindow = windowDays
                    val chosenName = name.trim().ifBlank {
                        defaultName +
                            if (chosen.isEmpty()) ""
                            else " · " + chosen.take(2).joinToString(" / ") { it.name }
                    }
                    scope.launch {
                        val cutoff =
                            if (chosenWindow == 0) 0L
                            else System.currentTimeMillis() - chosenWindow * 86_400_000L
                        val plays = withContext(Dispatchers.IO) {
                            StatsRepository(context).loadHistory()
                        }.filter { it.timestamp >= cutoff }
                        val queue: List<Song> = if (chosen.isEmpty()) {
                            plays.asSequence()
                                .groupBy { it.songId }
                                .values
                                .map { group -> group.first() to group.size }
                                .sortedByDescending { it.second }
                                .shuffled()
                                .take(50)
                                .map { (play, _) ->
                                    Song.fromYouTube(
                                        play.songId, play.title, play.artist, play.album,
                                        play.duration, play.thumbnailUrl
                                    )
                                }
                                .toList()
                        } else {
                            val per = mutableListOf<Song>()
                            chosen.forEach { artist ->
                                val page = if (artist.id.startsWith("UC"))
                                    viewModel.getArtistPage(artist.id) else null
                                val pageSongs = page?.songs.orEmpty()
                                val historySongs = plays
                                    .filter { it.artist.equals(artist.name, ignoreCase = true) }
                                    .map {
                                        Song.fromYouTube(
                                            it.songId, it.title, it.artist, it.album,
                                            it.duration, it.thumbnailUrl
                                        )
                                    }
                                per += (pageSongs + historySongs)
                                    .distinctBy { it.id }
                                    .take(chosenPer)
                            }
                            per.distinctBy { it.id }.shuffled()
                        }
                        if (queue.isEmpty()) {
                            Toast.makeText(
                                context, R.string.mixmaker_empty, Toast.LENGTH_SHORT
                            ).show()
                            creating = false
                        } else {
                            val repo = PlaylistRepository(context)
                            val id = repo.createPlaylist(chosenName, null)
                            repo.addSongsToPlaylist(id, queue)
                            Toast.makeText(
                                context,
                                String.format(
                                    context.getString(R.string.mixmaker_created),
                                    chosenName
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        }
                    }
                },
                enabled = !creating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(stringResource(R.string.mixmaker_create))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

/** One tappable pill. The M3 chip in this dependency tree has no supporting
 * text, so the pill is drawn here in the sheet's own language. */
@Composable
private fun MakerChip(
    selected: Boolean,
    label: String,
    badge: String?,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.labelSmall)
            }
            if (trailing != null) trailing()
        }
    }
}
