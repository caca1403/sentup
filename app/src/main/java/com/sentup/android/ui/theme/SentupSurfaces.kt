package com.sentup.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sentüp's own surface language: depth instead of outlines.
 *
 * Material's flat surfaces put a page and a card one shade apart. That is
 * legible and completely flat - it is what every screen looks like, and it is
 * most of why a good app on a good phone still feels cheap. iOS gets its depth
 * from three things stacked on one object rather than from a different colour:
 * a *tinted* shadow underneath it, a *light* hairline along its top edge, and
 * a trace of what is behind it in its own fill. Together they make a card look
 * like it is floating a few millimetres above the page rather than printed on
 * it, and the hairline is what keeps that reading on a dark background, where a
 * shadow alone disappears into it.
 *
 * Three pieces, used together:
 *
 * - [sentupPage] behind everything: the page tone, lifted toward the top so a
 *   dark theme has a bright end instead of being one black rectangle.
 * - [sentupCard] for anything on the page - shadow, hairline, tint.
 * - [SentupRadii] for the corners: large and continuous, so a card reads as a
 *   rounded solid rather than as a cut-out.
 *
 * Every colour is read from the theme, so this follows whichever palette is
 * chosen; only the *depth* is fixed here, which is the point.
 */
object SentupRadii {
    /** Cards and list rows. */
    val card: Dp = 26.dp

    /** Sheets and panels. */
    val panel: Dp = 34.dp

    /** Pills, chips, badges. */
    val pill: Dp = 50.dp

    /** Small tiles: artwork, thumbnails. */
    val tile: Dp = 18.dp
}

/**
 * The page behind a screen: [SentupRadii.card] corners on the content, on a
 * wash that gets slightly deeper towards the bottom.
 *
 * A vertical gradient rather than a flat fill for the same reason a glass of
 * water is not a rectangle of paint: the eye reads a gradient as a surface
 * with an orientation, and a page that gets darker towards the bottom reads as
 * a page you are looking *down* rather than a wall of colour.
 */
@Composable
fun Modifier.sentupPage(): Modifier {
    val scheme = MaterialTheme.colorScheme
    return this.background(
        Brush.verticalGradient(
            0.0f to scheme.surface,
            0.45f to scheme.surfaceContainerLow,
            1.0f to scheme.surfaceContainerLowest,
        )
    )
}

/**
 * A card that sits on the page.
 *
 * @param elevation how far it floats. Three steps is the whole vocabulary: 0
 *   for a row inside a card, 1 for a card, 2 for a sheet or the player. Larger
 *   numbers do not look better, they look like a bug.
 * @param tinted whether the card picks up a trace of the page behind it. On
 *   for anything over content - the player, a sheet - off for a flat list,
 *   where a gradient across a 56dp row is only noise.
 */
@Composable
fun Modifier.sentupCard(
    shape: Shape = RoundedCornerShape(SentupRadii.card),
    elevation: Int = 1,
    tinted: Boolean = true,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val base = when (elevation) {
        0 -> scheme.surfaceContainerHigh
        1 -> scheme.surfaceContainer
        else -> scheme.surfaceContainerLow
    }
    return this
        .shadow(
            elevation = when (elevation) {
                0 -> 0.dp
                1 -> 12.dp
                else -> 26.dp
            },
            shape = shape,
            // A grey shadow is haze. Tinting it with the primary is what makes
            // it read as coloured light falling on the card rather than smoke
            // drifting behind it.
            ambientColor = scheme.primary.copy(alpha = 0.30f),
            spotColor = scheme.primary.copy(alpha = 0.45f),
        )
        .clip(shape)
        .background(
            if (tinted) {
                Brush.verticalGradient(
                    listOf(
                        base,
                        androidx.compose.ui.graphics.lerp(base, scheme.surfaceContainerLowest, 0.30f),
                    )
                )
            } else {
                androidx.compose.ui.graphics.SolidColor(base)
            }
        )
        .border(
            width = 1.dp,
            color = scheme.onSurface.copy(alpha = if (elevation == 0) 0.05f else 0.09f),
            shape = shape,
        )
}

/**
 * A bright hairline along the top edge - the specular line that says "this is a
 * solid catching light". For surfaces with nothing behind them to cast a
 * shadow against, such as the player, where a shadow has no page to fall on.
 */
@Composable
fun Modifier.sentupSpecularEdge(shape: Shape = RoundedCornerShape(SentupRadii.card)): Modifier =
    this.border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0f),
            )
        ),
        shape = shape,
    )