package com.sentup.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentup.android.data.Song
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard

/**
 * The content objects every list screen is built from: the song row, the
 * album/playlist/artist card, and the playing mark they share.
 *
 * **One geometry, read once.** The type scale and the slots live in the
 * private vals below rather than at each call site, because the difference
 * between a screen that looks designed and one that looks assembled is that
 * the same 16sp semibold title and the same 56dp cover appear everywhere.
 * Screens may pass a shape, but the defaults here are the house defaults:
 * [SentupRadii.tile] corners, [sentupCard] depth, hairline edges on covers.
 *
 * **Presses are transforms, not tints.** A row shrinks to 0.97 and pales a
 * little while held; it never flashes a colour. A tint repaints the artwork
 * thumbnail inside the row, which reads as the row changing content rather
 * than being touched - and a ripple on a row that already holds a picture is
 * two things moving under one finger.
 *
 * **No full-bleed hairlines.** Where a divider exists it starts at the text,
 * aligned with the title it sits under, never edge to edge.
 */

/* The shared scale. Private on purpose: screens tune spacing and content,
 * not the size of the type. */
private val ROW_ART_SIZE = 56.dp
private val ROW_ART_GAP = 14.dp
private val ROW_MIN_HEIGHT = 68.dp
private val ROW_TITLE_SIZE = 16.sp
private val ROW_SUBTITLE_SIZE = 13.sp

private val CARD_TITLE_SIZE = 15.sp
private val CARD_TITLE_GAP = 10.dp
private val CARD_SUBTITLE_SIZE = 13.sp

/** The resting shape of cover art everywhere: rows, cards, search results. */
private val CONTENT_ART_SHAPE = RoundedCornerShape(SentupRadii.tile)

/**
 * A row's pressed state: scale 0.97 and a slight fade while held, springing
 * back through the same tween. Transforms only - see the file header.
 */
@Composable
private fun Modifier.sentupPressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "contentPressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "contentPressAlpha"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            } else {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            }
        )
}

/**
 * The hairline a cover carries so it reads as a solid object on the page
 * rather than a hole cut in it. Drawn above the picture, at the cover's own
 * corner radius.
 */
@Composable
private fun Modifier.sentupArtHairline(shape: Shape): Modifier =
    border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        shape = shape
    )

/**
 * What a song's cover looks like in this language: the picture at
 * [CONTENT_ART_SHAPE] (or a circle for artists, which stay round), a hairline
 * above it, and the playing mark over the picture when it is the playing one.
 */
@Composable
private fun ContentArtwork(
    song: Song?,
    artwork: (@Composable () -> Unit)?,
    circular: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (circular) CircleShape else CONTENT_ART_SHAPE
    Box(
        modifier = modifier
            .sentupCard(shape = shape, elevation = 1, tinted = false)
            .sentupArtHairline(shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            artwork != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) { artwork() }
            song != null -> SongArtwork(
                song = song,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            else -> MusicPlaceholder()
        }
        if (playing) {
            // The picture darkens just enough for the bars to hold, the same
            // scrim the duration badge uses: the cover stays recognisable.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center
            ) {
                PlayingBars(
                    playing = true,
                    color = Color.White,
                    modifier = Modifier.size(if (circular) 18.dp else 16.dp)
                )
            }
        }
    }
}

/**
 * The song row: cover, one-line title over one line of secondary text, one
 * secondary control at the trailing edge. Height, type and the divider rule
 * are fixed here - see the file header.
 *
 * A search result, a queue neighbour and a library row are all this object
 * with different trailing content, so all three draw through it.
 *
 * @param song Draws its cover through [SongArtwork]; ignored when [artwork]
 *   is given.
 * @param playing Defaults to what [LocalNowPlaying] says for [song], so a
 *   list under the player marks its row without any wiring at the call site.
 * @param showDivider The hairline above this row, inset to the title. Off by
 *   default: rows that stand in cards of their own want none at all.
 * @param trailingIcon The single secondary icon at the end (an overflow
 *   menu, a download state). Null draws nothing.
 */
@Composable
fun SentupSongRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
    artwork: (@Composable () -> Unit)? = null,
    playing: Boolean? = null,
    showDivider: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingTint: Color? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val nowPlaying = LocalNowPlaying.current
    val isPlaying = playing
        ?: (song != null && nowPlaying.songId == song.id && nowPlaying.isPlaying)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .sentupPressable(
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (showDivider) {
            // Inset past the cover so the hairline begins at the title it
            // separates, never edge to edge.
            SentupDivider(Modifier.padding(start = ROW_ART_SIZE + ROW_ART_GAP))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContentArtwork(
                song = song,
                artwork = artwork,
                circular = false,
                playing = isPlaying,
                modifier = Modifier.size(ROW_ART_SIZE)
            )
            Spacer(modifier = Modifier.width(ROW_ART_GAP))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = ROW_TITLE_SIZE,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = ROW_SUBTITLE_SIZE
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            } else if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                val iconTint = trailingTint
                    ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                if (onTrailingClick != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTrailingClick
                            )
                            .padding(10.dp)
                    )
                } else {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** The overflow icon a row's trailing slot usually holds. */
val SentupOverflowIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Rounded.MoreVert

/**
 * A cover object on the grid: the square picture, its title in at most two
 * lines, one line of secondary text. Album, playlist and search-result cards
 * are this; [circularArtwork] turns it into the artist card, where the round
 * avatar stays round.
 */
@Composable
fun SentupMediaCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
    artwork: (@Composable () -> Unit)? = null,
    circularArtwork: Boolean = false,
    playing: Boolean? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val nowPlaying = LocalNowPlaying.current
    val isPlaying = playing
        ?: (song != null && nowPlaying.songId == song.id && nowPlaying.isPlaying)

    Column(
        modifier = modifier
            .sentupPressable(
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ContentArtwork(
            song = song,
            artwork = artwork,
            circular = circularArtwork,
            playing = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Spacer(modifier = Modifier.height(CARD_TITLE_GAP))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = CARD_TITLE_SIZE,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = CARD_SUBTITLE_SIZE
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
