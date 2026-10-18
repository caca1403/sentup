package com.sentup.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.data.AutoMixEngine
import com.sentup.android.data.TasteProfileStore
import com.sentup.android.ui.theme.SentupChevron
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.R

/**
 * The intelligence page: what the app has worked out, and the lists it builds
 * from that on its own.
 *
 * **Everything here is derived from listening that already happened.** No
 * account is involved, nothing leaves the device, and nothing here claims to
 * know anything the play history does not already say. That is a deliberate
 * constraint rather than a limitation: an inference the user can check against
 * their own history is worth trusting, and one they cannot is worth arguing
 * with.
 *
 * Three things live on the page, in the order they matter:
 *
 * 1. **The generated mixes.** Real lists with names and reasons, built from
 *    history by [AutoMixEngine]. Each says what it is made of, because a list
 *    that will not explain itself is indistinguishable from a random one.
 * 2. **The taste profile.** What the engine thinks "you" is, as plain text -
 *    the artists it has heard most, the ones it has sampled once, the genres
 *    chosen at setup.
 * 3. **The narrowing.** The one control that changes what the rest of the app
 *    recommends: artists removed from consideration, kept here rather than
 *    buried in a settings page because this is where the reasoning lives. It is
 *    stored in [TasteProfileStore] and applied by
 *    [com.sentup.android.data.RecommendationEngine] to every seed it builds, so
 *    the decision is still in force tomorrow and on the home feed, not only on
 *    this page while it is open.
 */
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPlayMix: (AutoMixEngine.AutoMix) -> Unit = {},
    videoMode: Boolean = false,
) {
    val context = LocalContext.current
    val engine = remember(context) { AutoMixEngine(context) }
    val tasteStore = remember(context) { TasteProfileStore(context) }

    // null = still building, which is drawn as a spinner; an empty list is an
    // answer ("no mixes yet"), not a loading state, and the two must not look
    // the same or the page spins forever on a fresh install.
    val mixes by produceState<List<AutoMixEngine.AutoMix>?>(initialValue = null, engine) {
        value = engine.buildMixes()
    }
    var taste by remember { mutableStateOf(tasteStore.current()) }

    // The narrowing belongs to the store, not to this page. It has to outlive
    // the screen, and RecommendationEngine reads the same list whenever it
    // seeds a feed, so tapping a name here is a setting rather than a scroll
    // position. Re-read after every change: the store holds its state in
    // memory, so current() costs nothing.
    val excluded = taste.excludedArtists

    Column(modifier = modifier.fillMaxSize().sentupPage()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SentupGroup.inset, end = SentupGroup.inset, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ai_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                // --- Generated mixes ---
                SentupGroupHeader(
                    text = stringResource(R.string.ai_mixes_header),
                    subtitle = stringResource(R.string.ai_mixes_note)
                )
            }

            val built = mixes
            when {
                built == null -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                built?.isEmpty() == true -> item {
                    SentupGroupCard {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.ai_mixes_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(((built.size + 1) / 2 * 116).dp)
                    ) {
                        items(items = built) { mix -> MixCard(mix) { onPlayMix(mix) } }
                    }
                }
            }

            item {
                Spacer(Modifier.height(SentupGroup.gap))
                SentupGroupHeader(
                    text = stringResource(R.string.ai_taste_header),
                    subtitle = stringResource(R.string.ai_taste_note)
                )
            }

            item {
                SentupGroupCard {
                    TasteRow(
                        label = stringResource(R.string.ai_taste_artists),
                        value = taste.artists.take(4).joinToString(", ") { it.name }.ifBlank {
                            stringResource(R.string.ai_none_yet)
                        }
                    )
                    TasteRow(
                        label = stringResource(R.string.ai_taste_genres),
                        value = taste.genres.joinToString(", ") { it.title }
                            .ifBlank { stringResource(R.string.ai_none_yet) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(SentupGroup.gap))
                SentupGroupHeader(
                    text = stringResource(R.string.ai_narrow_header),
                    subtitle = stringResource(R.string.ai_narrow_note)
                )
            }

            item {
                SentupGroupCard {
                    if (excluded.isEmpty()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.ai_narrow_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        excluded.forEach { artist ->
                            NarrowedRow(artist) {
                                tasteStore.allow(artist)
                                taste = tasteStore.current()
                            }
                        }
                    }
                }
            }

            // Artists heard once, offered as narrowing candidates: the point
            // of the list is that they are the ones nobody has decided about.
            if (taste.artists.isNotEmpty() && excluded.size < taste.artists.size) {
                item {
                    Spacer(Modifier.height(SentupGroup.gap))
                    SentupGroupHeader(text = stringResource(R.string.ai_narrow_add))
                }
                item {
                    SentupGroupCard {
                        taste.artists
                            .filterNot { candidate ->
                                excluded.any { it.equals(candidate.name, ignoreCase = true) }
                            }
                            .forEach { artist ->
                                NarrowedRow(artist.name, removable = false) {
                                    tasteStore.exclude(artist.name)
                                    taste = tasteStore.current()
                                }
                            }
                    }
                }
            }
        }
    }
}

/** One generated mix, as a card that says what it is. */
@Composable
private fun MixCard(mix: AutoMixEngine.AutoMix, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sentupCard(RoundedCornerShape(SentupRadii.card), elevation = 1, tinted = false)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mix.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = mix.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${mix.size} " + stringResource(R.string.ai_tracks),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Label on the left, value on the right, the way a stats row reads. */
@Composable
private fun TasteRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** One artist in the narrowing list: a chip, removable when already excluded. */
@Composable
private fun NarrowedRow(artist: String, removable: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (removable) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.ai_narrow_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        } else {
            SentupChevron()
        }
    }
}