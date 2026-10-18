package com.sentup.android.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shared shapes every screen in the app is built from.
 *
 * **Where this comes from.** iOS did not invent the inset grouped list, but it
 * is the clearest expression of a principle Android screens keep forgetting:
 * *a screen is a page with objects on it, not a column of text*. A list of
 * settings is a stack of floating cards, each row a whole target rather than a
 * slice of one, with the label and its value on one line and the value right
 * aligned. The header above a group is small, bold, uppercase and indented to
 * the card's own edge - not a 32sp title that pushes the content down. And the
 * row presses inward instead of flashing a colour.
 *
 * Android's equivalents are all worse for this: `ListItem` is a flat row inside
 * one big container (so a 4-row group and a 40-row group look identical, and the
 * first and last rows have no rounded corners of their own), the divider is
 * full-bleed instead of inset, and the group header is a headline.
 *
 * These are the parts worth copying. Nothing here imitates a platform's
 * *look* - the colours come from this app's emerald palette and the type from
 * its type scale - because a copy that only borrows the furniture still reads
 * as somebody else's app.
 */
object SentupGroup {
    /**
     * The spacing above a group that follows another one.
     *
     * Generous on purpose: the gap between cards is what separates them. Too
     * tight and two groups read as one list with a stray divider in it.
     */
    val gap: Dp = 28.dp

    /** Inset from the screen edge. One value, so every page aligns. */
    val inset: Dp = 16.dp

    /** Inset from the card's left edge to the hairline that separates rows. */
    val dividerInset: Dp = 16.dp

    /** The tallest a row is allowed to be before its own text is clipped. */
    val maxRowHeight: Dp = 64.dp
}

/**
 * A group header: small, bold, sentence-cased, inset to the card's text.
 *
 * Not uppercase. Uppercase small text is a typographic tic that reads as
 * "settings screen" at a glance; sentence case reads as part of the content,
 * and the bold weight alone is enough to separate it from what follows.
 */
@Composable
fun SentupGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SentupGroup.inset + SentupGroup.dividerInset,
                end = SentupGroup.inset + SentupGroup.dividerInset,
                bottom = 8.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * A floating card of rows, with the hairlines inset to the text rather than
 * running edge to edge.
 *
 * [showDividers] is off for cards of one row, where a line would only underline
 * the item's own label, and for cards whose rows are tall enough to be separate
 * objects on their own (a song row with artwork).
 */
@Composable
fun SentupGroupCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SentupRadii.card),
    contentPadding: Dp = 4.dp,
    showDividers: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .sentupCard(shape = shape, elevation = 0, tinted = false),
        content = {
            Column(modifier = Modifier.padding(vertical = contentPadding)) {
                content()
            }
        }
    )
}

/**
 * One row inside a [SentupGroupCard].
 *
 * The whole row is the target and it presses *inward* - scale 0.985 for 90ms -
 * rather than tinting. A tint is a flash of colour that repaints the row and
 * everything it contains, which on a row holding an artwork thumbnail and a
 * title is visible as a flicker; a scale is a transform, costs nothing, and
 * reads as "this object is being pressed".
 *
 * @param value the right-aligned secondary text: a setting's current value, a
 *   count. Kept to one line and ellipsised, because a long value pushes the
 *   label off the row rather than truncating itself.
 */
@Composable
fun SentupRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    tint: Color? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "rowPress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(SentupRadii.tile))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick
                        )
                } else {
                    Modifier
                }
            )
            .heightIn(min = 50.dp)
            .padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint != null && icon != null) {
                    tint
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 132.dp)
            )
        }

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }

        if (showChevron) {
            Spacer(Modifier.width(8.dp))
            SentupChevron()
        }
    }
}

/** The chevron that says "this row opens something". */
@Composable
fun SentupChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = modifier.size(18.dp)
    )
}

/**
 * The hairline between two rows.
 *
 * Inset to the text, never full-bleed. A full-bleed divider says "this list is
 * cut into pieces"; an inset one says "these are separate objects in one box",
 * which is what they are, and it is what lets the row label start at the same
 * x on every row.
 */
@Composable
fun SentupDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SentupGroup.dividerInset)
            .height(1.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
    )
}

/** [SentupDivider] between the rows of a group card. */
@Composable
fun SentupRowSpacer(showDividers: Boolean) {
    if (showDividers) SentupDivider()
}