package com.sentup.android.ui.video

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupSpecularEdge

/**
 * The floating chrome the player is built from.
 *
 * **What this replaces.** Every player control used to sit on a full-bleed
 * gradient - a black band along the top edge, another along the bottom, both
 * fading into the picture. That is the idiom of a video *app* from ~2015: the
 * controls are a wash of grey painted over the frame, the buttons inside them
 * are tonal squares because a square is cheaper to lay out than a pill, and
 * pressing one flashes a ripple.
 *
 * **What this does instead.** The controls are objects floating *above* the
 * video, with the depth vocabulary the rest of the app uses: a pill-shaped
 * surface, [sentupCard] carrying its shadow and hairline, [sentupSpecularEdge]
 * catching light along the top so the surface still reads on a bright frame.
 * Nothing touches the screen edge, so the picture keeps its whole rectangle and
 * the chrome reads as something you could pick up.
 *
 * Press feedback is a scale ([sentupPressScale], 0.97) rather than a ripple:
 * these surfaces sit over moving video, and a ripple spreading across a panel
 * repaints the frame behind it - a flicker precisely where the eye already is.
 */

/** The pill shape every floating control cluster uses. */
internal fun playerPillShape(): Shape = RoundedCornerShape(SentupRadii.pill)

/** The shape of the wider floating panels: the bottom bar, the readout. */
internal fun playerPanelShape(): Shape = RoundedCornerShape(SentupRadii.card)

/**
 * Press feedback for the player's chrome: the object moves inward, nothing
 * flashes. Pair with `clickable(interactionSource, indication = null)`.
 */
@Composable
fun Modifier.sentupPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "chromePress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A floating pill of controls - back plus title, a cluster of viewing controls,
 * a live badge. Depth on the outside, flat targets inside.
 */
@Composable
fun PlayerChromePill(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
    content: @Composable () -> Unit
) {
    val shape = playerPillShape()
    Box(
        modifier = modifier
            .sentupCard(shape = shape, elevation = 2)
            .sentupSpecularEdge(shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * A wide floating panel: the timeline bar, the readout row. Spans the window
 * minus [com.sentup.android.ui.theme.SentupGroup.inset] on each side, which is
 * what makes it read as a panel rather than a border on the frame.
 */
@Composable
fun PlayerChromePanel(
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
    content: @Composable () -> Unit
) {
    // Flat timeline (Koda original look): the raised rounded card is gone. Only a
    // soft bottom scrim so the readout stays legible over any frame - the seek
    // line itself sits directly on the picture like YouTube's.
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                )
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * An icon target inside a floating surface.
 *
 * Deliberately flat: the pill behind it is already the object, so a button with
 * its own container inside it draws a box inside a box. [active] is the one
 * state that gets a fill - captions on, a filter engaged - and it is filled with
 * the palette rather than with white, so "on" is emerald and not a brighter grey.
 */
@Composable
fun PlayerChromeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .sentupPressScale(interactionSource)
            .size(size)
            .clip(CircleShape)
            .background(if (active) scheme.primary else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> scheme.onSurface.copy(alpha = 0.35f)
                active -> scheme.onPrimary
                else -> scheme.onSurface
            },
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * The primary transport control: an emerald disc with a specular edge.
 *
 * It is the only control on the frame with its own fill, because it is the only
 * one the thumb is looking for. The morphing squircle it used to be is gone - a
 * control that changes shape under a finger reads as unstable, and the spring
 * made the most-used button on the screen the slowest thing to answer.
 */
@Composable
fun PlayerPrimaryPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
    size: Dp = 72.dp,
    contentDescription: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .sentupPressScale(interactionSource, pressedScale = 0.94f)
            .size(size)
            .sentupCard(shape = shape, elevation = 2)
            .sentupSpecularEdge(shape)
            .background(scheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering && !isPlaying) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.42f),
                color = scheme.onPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = contentDescription,
                tint = scheme.onPrimary,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

/**
 * A small floating label over the frame: a chapter name, a live offset, the
 * "screen locked" confirmation. Same surface as everything else, sized to its
 * text, and tappable only when it leads somewhere.
 */
@Composable
fun PlayerChromeChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: ImageVector? = null,
    leadingSlot: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val shape = playerPillShape()
    val pressModifier = if (onClick != null) {
        Modifier
            .sentupPressScale(interactionSource)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .sentupCard(shape = shape, elevation = 2)
            .then(pressModifier)
            .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingSlot != null) leadingSlot()
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                maxLines = 1
            )
            if (trailing != null) {
                Icon(
                    imageVector = trailing,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * A single labelled pill in the palette - "Abone ol", "İzleyeceğim", and the
 * one affirmative action on any card.
 *
 * One card gets one of these. An outline (`filled = false`) is the *negative*
 * state of the same control - subscribed, saved - rather than a second style of
 * button: the label changes, the shape and place do not, so the thumb keeps
 * finding it in the same spot either way.
 */
@Composable
fun SentupActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = playerPillShape()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .sentupPressScale(interactionSource, pressedScale = 0.96f)
            .clip(shape)
            .background(if (filled) scheme.primary else scheme.surfaceContainerHighest)
            .then(
                if (filled) Modifier else Modifier.border(1.dp, scheme.outlineVariant, shape)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (filled) scheme.onPrimary else scheme.onSurface,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (filled) scheme.onPrimary else scheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/** A check mark in the palette, for the selected row of a sheet. */
@Composable
fun SentupSelectedMark(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(20.dp)
    )
}
