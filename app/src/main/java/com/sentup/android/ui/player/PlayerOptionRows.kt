package com.sentup.android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.data.Song
import com.sentup.android.ui.components.SongArtwork
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.sentupSpecularEdge
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.SentupRadii

/**
 * The row language shared by music mode's two option sheets - the long-press
 * [SongOptionsSheet] and the now-playing [NowPlayingOptionsSheet].
 *
 * One file rather than a copy each, for the reason `QueueReorder.kt` is one
 * file: the two sheets are opened by different gestures on different screens
 * and must not drift into two different-looking menus, but only the *rows* are
 * the same - what goes in them is each sheet's own business. It matches
 * `VideoOptionsSheet`'s rows on the video side, so the same gesture produces a
 * recognisably similar sheet in both modes.
 *
 * It holds three shapes now, not one, and which a sheet reaches for is about
 * what the sheet is. [OptionRow] is a list line: a long-press menu of one-shot
 * actions on some row in a list. [OptionTile] and [OptionUtility] are a control
 * panel: the now-playing sheet is opened *from* the player, on the song already
 * playing, and the things in it are pressed and toggled rather than read - so
 * the four common actions are filled tiles under the thumb, and the occasional
 * ones sit unfilled on the same columns beneath them. The destinations there
 * are [OptionRow]s, because a name is the label. The vocabulary stays shared so
 * the two sheets cannot drift into two different-looking menus.
 */

/**
 * A run of [OptionRow]s as one floating card, inset from the sheet's edges.
 *
 * This used to be a Material segmented column: a full-bleed grey container with
 * segment gaps and per-row ripples. It is now [SentupGroupCard], so the same
 * call site gets the app's group card - depth instead of an outline, rows
 * pressed inward instead of flashing, and the card held off the page - and every
 * sheet that draws one (the long-press song menu, the now-playing overflow, the
 * settings and profile menus that share these rows) gets it without a call site
 * changing.
 */
@Composable
internal fun OptionGroup(content: @Composable ColumnScope.() -> Unit) {
    SentupGroupCard(content = content)
}

/**
 * The hairline between two rows in a group.
 *
 * Inset to the text rather than running edge to edge, which is the whole reason
 * it moved off [HorizontalDivider]: a full-bleed rule says the list is cut into
 * pieces, an inset one says these are separate objects in one box.
 */
@Composable
internal fun OptionRowDivider() {
    SentupDivider()
}

/** Whether the row goes somewhere, is working, has already happened, or acts. */
internal enum class OptionRowTrailing { NONE, CHEVRON, CHECK, LOADING }

/**
 * One action inside a group.
 *
 * This is [SentupRow] wearing the menu's vocabulary, and the reason it is a
 * wrapper rather than a second implementation: a menu row and a settings row
 * have to look like the same object, or the app reads as three design systems.
 * So the whole press contract is delegated - the whole row is the target, it
 * scales inward over 90ms instead of rippling, and it is disabled at 40% rather
 * than going flat - and all this adds is the trailing glyph, which the group
 * rows of a sheet need and the rest of the app does not.
 *
 * It used to be its own Row with a 56dp minimum, a ripple and a full-bleed
 * divider. A menu of six standalone cards is what overflows a bottom sheet, and
 * a ripple on a row holding an icon and two lines is visible as a flicker, so
 * both were dropped in favour of the shared row.
 */
@Composable
internal fun OptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: OptionRowTrailing = OptionRowTrailing.NONE,
    enabled: Boolean = true,
    /** Overrides the accent, for a row whose state is the icon (liked). */
    iconTint: Color? = null
) {
    SentupRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        tint = iconTint,
        showChevron = trailing == OptionRowTrailing.CHEVRON,
        onClick = onClick,
        trailing = when (trailing) {
            OptionRowTrailing.NONE, OptionRowTrailing.CHEVRON -> null
            // The emerald tick: a thing already done says so in the accent,
            // not in a neutral grey.
            OptionRowTrailing.CHECK -> {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            OptionRowTrailing.LOADING -> {
                {
                    LoadingIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

/**
 * One action as a tile: a 24dp icon over a one-word label, sized to sit four
 * across in a row.
 *
 * **A tile is for an action that is used, not read.** The rows above are a list
 * you scan once; these are the four things someone opens the player's overflow
 * to *press*, and at four across they are all reachable with one thumb without
 * the sheet growing. [selected] is the state of a toggle - liked, downloaded -
 * carried by the container rather than a tick, so the state is legible from
 * across the sheet; the shape squares off as it fills, the same selection
 * language as the app's connected [androidx.compose.material3.ToggleButton]
 * groups.
 *
 * The label sits on two lines at most, because the tile's width is a quarter of
 * the sheet and a large display scale is exactly where one line would clip.
 */
@Composable
internal fun OptionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    /** Replaces the icon while the action is in flight (a download starting). */
    loading: Boolean = false,
) {
    val corner by animateDpAsState(
        targetValue = if (selected) 16.dp else 26.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "optionTileCorner"
    )
    val container by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "optionTileContainer"
    )
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val accent = if (selected) content else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 86.dp),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (loading) {
                LoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accent
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = content,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A secondary action: an icon over a short label, straight on the sheet with no
 * container of its own.
 *
 * **Laid on the same columns as the [OptionTile]s above it.** The sheet's
 * actions form one grid - four filled tiles, then a row of these - so every
 * icon centre lines up with the one above it and the eye reads two rows of
 * one panel rather than a second, differently shaped widget. The difference in
 * emphasis is carried by the fill alone: tiles are the things this sheet is
 * opened to press, these are the ones reached for now and then, and a second
 * row of filled containers would give the panel two loud bands. The press
 * still has a rounded ripple, so the touch target is the whole column.
 *
 * [quiet] mutes the icon for the one action that takes something away.
 */
@Composable
internal fun OptionUtility(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Read instead of [label] by accessibility services, where it says more. */
    contentDescription: String? = null,
    quiet: Boolean = false,
) {
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.clearAndSetSemantics {
                        this.contentDescription = contentDescription
                        role = Role.Button
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (quiet) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // Two lines, for the same reason as a tile's: a quarter of the
            // sheet at a large display scale is where one line would clip.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Artwork, title and artist, so a sheet says which song it is acting on.
 *
 * @param modifier the caller passes the page inset, because the group cards
 *   below carry their own and the header has to line up with their *text*, not
 *   with the sheet edge.
 */
@Composable
internal fun SongOptionsHeader(song: Song, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The 8dp below joins the sheet column's own 8dp gap, so the header
            // stands further off the first group than the groups do off each
            // other - it is a caption, not another action.
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The cover is a card like any other: at elevation 1 with the page's
        // specular edge, so it is not the one flat patch left in the sheet.
        Box(
            modifier = Modifier
                .size(56.dp)
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.tile),
                    elevation = 1,
                    tinted = false
                )
                .sentupSpecularEdge(RoundedCornerShape(SentupRadii.tile))
                .clip(RoundedCornerShape(SentupRadii.tile))
        ) {
            SongArtwork(
                song = song,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                // Two lines: a long track name truncated to one is the same
                // ellipsis for every song in an album, which tells the user
                // nothing about which one they pressed.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
