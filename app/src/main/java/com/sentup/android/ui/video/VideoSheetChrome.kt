package com.sentup.android.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge

/**
 * The chrome every sheet on the video side is built from.
 *
 * These live here rather than in `ui/theme` because this is the one place in
 * the app where six sheets need the identical treatment: the options sheet,
 * the queue, comments, notifications and the download sheet are all opened
 * *over a video that is still playing*, which is a different problem from a
 * settings page. There is no page behind them to establish a floor, so every
 * row of chrome is a row of chrome over the thing the user came to watch -
 * and that is the reason all of it is one capsule bar and nothing more.
 *
 * The vocabulary is deliberately small and does the same three things
 * everywhere:
 *
 * - **The page.** [VideoSheetScaffold] puts [sentupPage] behind the sheet and
 *   opens it at the panel radius, so a sheet reads as a surface that arrived
 *   over the video rather than a grey rectangle stapled to the bottom.
 * - **One capsule bar.** [VideoSheetTitleBar] is the same object as the
 *   player's own bar: a floating pill holding the title, so the sheet names
 *   itself at a glance and the way out is a whole button rather than a drag.
 * - **Press inward.** [videoPressTarget] is the single press contract on this
 *   side of the app - scale, no ripple. A ripple repaints a thumbnail and a
 *   title in the middle of an animation; a scale is a transform and costs
 *   nothing.
 *
 * The rest are the pieces those need anyway, gathered here so that a seventh
 * sheet cannot invent its own: [VideoSegmentPill] for choosing one of a few,
 * [VideoChoiceRow] for picking one row out of a list, and
 * [VideoSheetEmptyState] for having nothing to show.
 */

/**
 * A whole sheet: the page tone behind it, the panel's own corners, and a
 * capsule bar carrying the name and the way out.
 *
 * The title is a parameter rather than something each sheet composes, because
 * every one of these has exactly one and every one of them had it as a
 * `headlineSmall` at a different inset - which is how six sheets came to look
 * like six apps.
 *
 * @param onBack set instead of relying on [onDismiss] for a pane that has
 *   something behind it: the leading control becomes a back arrow, which is
 *   what a user who pressed the system's Back meant.
 * @param trailing extra controls in the bar - a save, a refresh.
 * @param title null for a sheet that draws its own header, such as the options
 *   sheet whose two panes swap in place and each need a different title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSheetScaffold(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The page gradient's first stop. The sheet's own fill is only ever
        // seen in the few pixels behind the drag handle, and starting it at the
        // gradient's own top colour is what keeps that strip from showing a
        // seam between the handle and the content below it.
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = SentupRadii.panel, topEnd = SentupRadii.panel)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .sentupPage()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 10.dp)
        ) {
            if (title != null) {
                VideoSheetTitleBar(
                    title = title,
                    subtitle = subtitle,
                    // A back arrow replaces the close button when the pane has
                    // something behind it: leaving both would offer two exits
                    // from a sheet that only has one.
                    onClose = if (onBack == null) onDismiss else null,
                    onBack = onBack,
                    trailing = trailing
                )
            }
            content()
        }
    }
}

/**
 * The floating capsule every video sheet names itself in.
 *
 * The same object as the player's bar on purpose: opening a sheet over the
 * player should add a surface, not change the language. A pill on the page, an
 * ink-dark title taking the whole middle, one icon at each end. The title
 * ellipsises rather than wrapping, because a sheet's name is worth more than
 * its subtitle and neither is worth pushing the close button off the edge.
 */
@Composable
fun VideoSheetTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClose: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val leading = onBack ?: onClose

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .padding(top = 4.dp, bottom = 12.dp)
            .sentupCard(shape = CircleShape, elevation = 1)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            IconButton(onClick = leading) {
                Icon(
                    imageVector = if (onBack != null) {
                        Icons.AutoMirrored.Rounded.ArrowBack
                    } else {
                        Icons.Rounded.Close
                    },
                    contentDescription = stringResource(
                        if (onBack != null) R.string.cd_back else R.string.cd_close
                    ),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * A group header for a header that sits *inside* a card.
 *
 * [com.sentup.android.ui.theme.SentupGroupHeader] is inset for a header on the
 * page, above a card that is itself inset from the edge. A header between two
 * rows of one card has already been inset by the card, and the same padding on
 * it would step it 16dp right of the text it labels - which reads as a
 * mistake rather than as a level.
 */
@Composable
fun VideoCardHeader(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SentupGroup.dividerInset,
                end = SentupGroup.dividerInset,
                top = 12.dp,
                bottom = 4.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Nothing to show, said once.
 *
 * One sentence and a mark, centred, with no card and no button. An empty state
 * is not an invitation to go somewhere else - it is the absence of a thing, and
 * the honest way to draw that is small and quiet. YouTube's own are a headline,
 * a paragraph and a full-width button, which on a sheet over a playing video
 * covers a third of the screen to say nothing happened.
 */
@Composable
fun VideoSheetEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset + 8.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/**
 * One option of a few, as a capsule.
 *
 * Not a segmented button. A segmented control draws a connected bar with a
 * travelling selection and corners of its own, which is a lot of machinery
 * for "which of two comment orders" - and on a sheet over video it reads as a
 * control with settings in it. A capsule strip says the same thing with less
 * ink: the selected one is the filled one, which on the app's palette is
 * emerald, and that is what makes the current choice findable without any
 * label change.
 *
 * @param trailing a count or a mark on the far end - how many comments each
 *   order would show, for instance.
 */
@Composable
fun VideoSegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.pill)
    Row(
        modifier = modifier
            .videoPressTarget(shape = shape, onClick = onClick)
            .background(if (selected) scheme.primary else scheme.surfaceContainerHighest, shape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    scheme.onPrimary.copy(alpha = 0.78f)
                } else {
                    scheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                maxLines = 1
            )
        }
    }
}

/**
 * One row of a single-choice list, with the current pick ticked.
 *
 * A check rather than a radio dot: a dot inside a filled circle and a dot
 * inside an unfilled one are the same glyph at two opacities and the eye has to
 * check both, whereas a check appears - and the row it appeared in is the
 * answer. It is the theme's primary, so it costs no new colour.
 *
 * @param value the right-aligned secondary: for a quality ladder, how big the
 *   file will be. It belongs beside its own row rather than in one block under
 *   the list, which is where it used to live and where nobody could connect it
 *   to the rung they had just picked.
 *
 * Built on [SentupRow], so it inherits the house's press-inward and two-line
 * label without restating either.
 */
@Composable
fun VideoChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SentupRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        value = value,
        enabled = enabled,
        tint = if (selected) MaterialTheme.colorScheme.primary else null,
        onClick = onClick,
        trailing = {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    )
}

/**
 * The one press contract on the video side: scale inward, no ripple.
 *
 * [SentupRow] already does this for rows inside a group card. This is for
 * everything else - pills, queue rows, comment rows, notification rows - so a
 * press means the same thing on every surface a user can touch while a video
 * plays, instead of a ripple on one sheet and a tint on the next.
 *
 * @param scaleTo how far in. 0.97 for a full-width row; closer to 1 for a
 *   small one, because a 34dp pill scaled to 0.94 loses a visible sliver of
 *   its own label.
 */
@Composable
fun Modifier.videoPressTarget(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SentupRadii.tile),
    scaleTo: Float = 0.97f,
    enabled: Boolean = true,
    onClickLabel: String? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "videoPress"
    )
    return this
        .then(modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}

/**
 * The one loud control on a sheet: a filled capsule.
 *
 * A full-width rectangle with 20dp corners reads as a form control. A capsule
 * reads as a button, and there is only ever one per sheet, which is what makes
 * it legible without having to shout.
 */
@Composable
fun VideoSheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.pill)
    Row(
        modifier = modifier
            .videoPressTarget(
                onClick = onClick,
                shape = shape,
                scaleTo = 0.975f,
                enabled = enabled
            )
            .background(
                if (enabled) scheme.primary else scheme.surfaceContainerHighest,
                shape
            )
            .sentupSpecularEdge(shape)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The quieter capsule, for the control beside the primary one.
 *
 * Tonal rather than outlined. An outline is a border the eye has to follow
 * around, and on a page that already carries a group card of hairline rows more
 * borders is the one thing that makes it look like a form.
 */
@Composable
fun VideoSheetTonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.pill)
    Row(
        modifier = modifier
            .videoPressTarget(onClick = onClick, shape = shape, scaleTo = 0.96f, enabled = enabled)
            .background(scheme.surfaceContainerHigh, shape)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A thumbnail sitting on the page like the page's other objects.
 *
 * [height] tall, tile corners, a specular hairline along the top. The old
 * sheets drew a flat 12dp-cornered rectangle in a grey box, which at 96x54 was
 * a chip of nothing that did not really tell the user which video they had
 * opened a menu for. This is the sheet's second-largest object and it is
 * finished like one.
 */
@Composable
fun VideoSheetThumbnail(
    url: String?,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
    corner: Dp = SentupRadii.tile,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .height(height)
            .sentupCard(shape = shape, elevation = 1, tinted = false)
            .sentupSpecularEdge(shape)
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentScale = ContentScale.Crop
            )
        }
    }
}