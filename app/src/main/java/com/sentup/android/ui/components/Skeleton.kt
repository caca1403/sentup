package com.sentup.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard

/**
 * Loading placeholders that stand in for content at its real size and shape.
 *
 * The point is that a screen's chrome - top bar, section titles, navigation -
 * has nothing to wait for and should never be replaced by a spinner. Only the
 * regions actually blocked on data get covered, and they keep their layout, so
 * nothing jumps when the data lands. That includes the *surface*: the cards
 * here wear the same [sentupCard] depth as the content they replace, so the
 * swap is a fill and not a change of furniture.
 *
 * A slow alpha pulse rather than a travelling gradient: it reads as "not ready"
 * without competing with the staggered entrance animations the real content
 * plays on arrival. Time-driven, so tween rather than spring.
 */
private const val SKELETON_PULSE_MS = 900

/** The pulse tone: the same container the content plates would use. */
@Composable
private fun skeletonTone(alpha: Float) =
    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)

/**
 * The shared pulse. Hoist this once per screen and pass it down so every
 * placeholder breathes in step - separate transitions drift apart and the
 * screen starts to twinkle.
 */
@Composable
fun rememberSkeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(SKELETON_PULSE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return alpha
}

/**
 * A single placeholder block. Size it with [modifier] to match whatever it is
 * standing in for; [shape] should match too, so the swap to real content is a
 * fill rather than a reflow.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = rememberSkeletonAlpha()
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(skeletonTone(alpha))
    )
}

/** Placeholder for a line of text, sized like the type it replaces. */
@Composable
fun SkeletonTextLine(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = rememberSkeletonAlpha()
) {
    SkeletonBox(
        modifier = modifier.size(width = width, height = height),
        shape = RoundedCornerShape(height / 2),
        alpha = alpha
    )
}

/** The cover radius a placeholder art block carries: the house tile. */
private val SKELETON_ART_SHAPE = RoundedCornerShape(SentupRadii.tile)

/**
 * Stand-in for a [SentupSongRow]: the 56dp cover tile and two lines of type
 * at the row's own height, on the page rather than in a card - the same
 * furniture a loaded row will wear.
 */
@Composable
fun SongRowSkeleton(
    modifier: Modifier = Modifier,
    alpha: Float = rememberSkeletonAlpha()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(
                start = SentupGroup.dividerInset,
                end = SentupGroup.dividerInset,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBox(
            modifier = Modifier.size(56.dp),
            shape = SKELETON_ART_SHAPE,
            alpha = alpha
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title line at the title's own cap height, subtitle shorter -
            // a placeholder that guesses the type wrong is felt as a jump.
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.72f).height(15.dp),
                shape = RoundedCornerShape(8.dp),
                alpha = alpha
            )
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.44f).height(12.dp),
                shape = RoundedCornerShape(6.dp),
                alpha = alpha
            )
        }
    }
}

/**
 * Stand-in for a `VideoCard`: 16:9 thumbnail, then a 40dp channel avatar beside
 * a two-line title and one line of metadata. Mirrors the real card's shape,
 * colors and padding so the swap does not reflow.
 */
@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier,
    alpha: Float = rememberSkeletonAlpha()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sentupCard(elevation = 1, tinted = false)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(
                topStart = SentupRadii.card,
                topEnd = SentupRadii.card
            ),
            alpha = alpha
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                alpha = alpha
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Two title lines, the second short, the way a wrapped
                // title actually lands.
                SkeletonBox(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    shape = RoundedCornerShape(7.dp),
                    alpha = alpha
                )
                SkeletonBox(
                    modifier = Modifier.fillMaxWidth(0.6f).height(14.dp),
                    shape = RoundedCornerShape(7.dp),
                    alpha = alpha
                )
                SkeletonTextLine(width = 120.dp, height = 11.dp, alpha = alpha)
            }
        }
    }
}

/**
 * Stand-in for a `ChannelRow`: a 48dp avatar beside a name and a subscriber
 * count. Also the right shape for any other avatar-plus-two-lines row.
 */
@Composable
fun ChannelRowSkeleton(
    modifier: Modifier = Modifier,
    alpha: Float = rememberSkeletonAlpha()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sentupCard(elevation = 1, tinted = false)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Artist avatars stay round; everything else in the app squares off.
        SkeletonBox(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            alpha = alpha
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkeletonTextLine(width = 160.dp, height = 14.dp, alpha = alpha)
            SkeletonTextLine(width = 96.dp, height = 11.dp, alpha = alpha)
        }
    }
}

/**
 * Stand-in for a `PlaylistRow`: a 120dp-wide 16:9 thumbnail beside a title and
 * a count.
 */
@Composable
fun PlaylistRowSkeleton(
    modifier: Modifier = Modifier,
    alpha: Float = rememberSkeletonAlpha()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sentupCard(elevation = 1, tinted = false)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f),
            shape = SKELETON_ART_SHAPE,
            alpha = alpha
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkeletonTextLine(width = 150.dp, height = 14.dp, alpha = alpha)
            SkeletonTextLine(width = 80.dp, height = 11.dp, alpha = alpha)
        }
    }
}

/**
 * A run of identical placeholders, spaced like the list they stand in for.
 *
 * Takes the whole run rather than one row because these are emitted from a
 * single `LazyColumn` item: the placeholder count is fixed, so there is nothing
 * for lazy layout to save, and one item keeps the pulse hoisted once for the
 * whole group instead of per row.
 */
@Composable
fun SkeletonList(
    count: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    row: @Composable (alpha: Float) -> Unit
) {
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(count) { row(alpha) }
    }
}
