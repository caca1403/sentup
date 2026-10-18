package com.sentup.android.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentup.android.R
import com.sentup.android.data.LrcLine
import com.sentup.android.data.LyricsResult
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage

/**
 * Whether the lyrics screen is open, shared by every player style and by the
 * mini pill. Hoisted above the styles the way [LocalNowPlayingOptionsOpen]
 * is: one destination, so no style can keep its own in-place mode and drift
 * from the rest.
 */
internal val LocalLyricsOpen = compositionLocalOf<MutableState<Boolean>> {
    error("LocalLyricsOpen not provided")
}

/**
 * The one lyrics screen. It opens full-screen over the player - over whichever
 * style is active, and over the mini pill - so "lyrics" is one place in the
 * app instead of nine near-copies that each hide somewhere else.
 *
 * The page is the player's own language: the [sentupPage] ground, a floating
 * capsule bar with the back affordance and the song's name, and the lyrics
 * themselves in [SyncedLyricsView]. A lyric with no timing - a plain block
 * of text rather than a singable line - is not a karaoke line; it goes into a
 * [SentupGroupCard] and is read like a page, with generous line spacing.
 *
 * A lyric that cannot be fetched is not silence. Every non-success state says
 * in one line why, and offers exactly one action: try the sources again. The
 * reason differs by state - the services are unreachable is not the same as
 * no service had the song - and the retry label says which one it is.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LyricsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricsResult by viewModel.lyricsResult.collectAsState()
    val song by viewModel.currentSong.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scheme = MaterialTheme.colorScheme

    BackHandler { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The floating capsule bar: the same object the editorial player
            // already floats, so arriving at the lyrics feels like the player
            // opened a page rather than a different app.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .sentupCard(shape = CircleShape, elevation = 1)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.ps_back),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title?.takeIf { !isUnknownTitle(it) }
                            ?: stringResource(R.string.np_art_untitled),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    song?.artist?.takeIf { !isUnknownArtist(it) }?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Keeps the capsule bar's width honest without a trailing
                // action: the bar is a pill, not a menu.
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (val result = lyricsResult) {
                    is LyricsResult.Loading -> LyricsWaitingState()
                    is LyricsResult.NotFound -> LyricsMissingState(
                        title = stringResource(R.string.lyrics_none_title),
                        body = stringResource(R.string.lyrics_none_body),
                        onRetry = viewModel::retryLyrics
                    )
                    is LyricsResult.Error -> LyricsMissingState(
                        title = stringResource(R.string.lyrics_error_title),
                        body = stringResource(R.string.lyrics_error_body),
                        onRetry = viewModel::retryLyrics
                    )
                    is LyricsResult.Success -> if (result.syncType ==
                        com.sentup.android.data.LyricsSyncType.PLAIN
                    ) {
                        PlainLyricsCard(lines = result.lines)
                    } else {
                        SyncedLyricsView(
                            lyricsResult = result,
                            currentPositionMs = progress,
                            isPlaying = isPlaying,
                            onSeekTo = { viewModel.seekTo(it) },
                            modifier = Modifier.fillMaxSize(),
                            primaryColor = scheme.primary,
                            onSurfaceColor = scheme.onSurface,
                            onSurfaceVariantColor = scheme.onSurfaceVariant,
                            onRetry = viewModel::retryLyrics
                        )
                    }
                }
            }
        }
    }
}

/**
 * A lyric sheet with no timing. Read like a page: one [SentupGroupCard],
 * comfortable measure, generous leading - not the anchored karaoke column a
 * singable lyric wants.
 */
@Composable
private fun PlainLyricsCard(lines: List<LrcLine>) {
    val scroll = rememberScrollState()
    SentupGroupCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        showDividers = false,
        contentPadding = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            lines.forEach { line ->
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LyricsWaitingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                polygons = listOf(
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Pill,
                    MaterialShapes.Sunny
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lyrics_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The one shape every "there is no lyric to show" answer takes: an icon, a
 * one-line reason, and a single button that tries again. No menu of sources,
 * no settings - those live in the lyrics settings screen; from here the only
 * thing worth doing is asking once more.
 */
@Composable
private fun LyricsMissingState(
    title: String,
    body: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lyrics,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.lyrics_retry),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
