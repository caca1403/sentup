package com.sentup.android.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard

/**
 * Shared pieces of the Home feed, kept out of the feed file itself.
 *
 * The long-press popout lives here (and not in ui/theme) because it knows
 * what a [Song] is; [ui.theme] owns only shapes and depths that know nothing
 * about music. The music feed and anything that shows songs can both reach
 * it from here.
 */

/**
 * The long-press preview of a song: its cover, its name, and the three
 * actions a held tile is always a shortcut to - play, queue, like - each one
 * tap. A press-and-hold already picked the song; asking for a second screen
 * of choices before anything can happen is what makes popups feel like work.
 *
 * The fourth action ("more") is the full options sheet, offered only where
 * one exists. Every pill presses inward (0.97, 90 ms) like every other object
 * on the page; none of them flashes.
 */
@Composable
fun SentupSongPopout(
    song: Song,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onLike: () -> Unit,
    onMore: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.panel),
                    elevation = 2,
                    tinted = false
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(SentupRadii.tile))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = song.highResThumbnailUrl ?: song.thumbnailUrl ?: song.albumArtUri?.toString(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist.takeIf { !isUnknownArtist(it) }
                            ?: stringResource(R.string.unknown_artist),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SentupActionPill(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(R.string.cd_play),
                    primary = true,
                    onClick = onPlay,
                    modifier = Modifier.weight(1f)
                )
                SentupActionPill(
                    icon = Icons.Rounded.QueueMusic,
                    label = stringResource(R.string.song_options_add_to_queue),
                    primary = false,
                    onClick = onQueue,
                    modifier = Modifier.weight(1f)
                )
                SentupActionPill(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.like),
                    primary = false,
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else null,
                    onClick = onLike,
                    modifier = Modifier.weight(1f)
                )
                if (onMore != null) {
                    SentupActionPill(
                        icon = Icons.Rounded.MoreVert,
                        label = null,
                        primary = false,
                        onClick = onMore,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

/**
 * A pill inside [SentupSongPopout]: icon + optional label, the brand's own
 * fill for the one primary action, pressed inward rather than flashed.
 */
@Composable
private fun SentupActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "popoutPillPress"
    )
    val container = when {
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        primary -> MaterialTheme.colorScheme.onPrimary
        tint != null -> tint
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SentupRadii.pill))
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (label != null) 12.dp else 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
        if (label != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1
            )
        }
    }
}

/**
 * The quick-access capsules above the hero: one-line shortcuts, each a
 * single tap. They are capsules rather than a card because they are verbs -
 * a card invites reading, a pill invites the press.
 */
@Composable
fun SentupQuickCapsule(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "capsulePress"
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
