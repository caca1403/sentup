package com.sentup.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * The album-art picture: the artwork with the house cover language wrapped
 * around it.
 *
 * A cover is an object on the page, not a hole cut in it, and the two things
 * that make it read that way at any size are here rather than at the eight
 * call sites that used to remember one and forget the other: a hairline along
 * the edge (the same 1dp line [sentupCard] gives a card, kept faint so it
 * survives on a dark cover), and a placeholder that carries the same tonal
 * depth as the rest of the surfaces instead of a flat grey plate.
 *
 * Depth that belongs to a *card* - the [sentupCard] shadow - is deliberately
 * not baked in here: a 36dp queue thumbnail and a 160dp grid cover do not
 * float the same distance. Draw those through the card components
 * ([SentupMediaCard], the artwork slot of [SentupSongRow]), which add the
 * shadow; this adds the picture, the edge and the fallback.
 *
 * @param imageUrl The URL of the album art to load
 * @param contentDescription Content description for accessibility
 * @param modifier Modifier for the composable
 * @param size The size of the image (used for both width and height)
 * @param shape The shape to clip the image to
 * @param placeholderBackgroundColor Background color for the placeholder
 * @param placeholderIconColor Color for the music note icon
 */
@Composable
fun AlbumArtImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    placeholderBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    placeholderIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
) {
    AlbumArtPlate(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        shape = shape,
        placeholderBackgroundColor = placeholderBackgroundColor,
        placeholderIconColor = placeholderIconColor
    )
}

/**
 * A placeholder with a music note icon, used when album art fails to load.
 *
 * Filled with a shallow gradient off [backgroundColor] so an empty cover keeps
 * the tonal depth every other surface in the app has, instead of sitting on
 * the page as a flat plate.
 */
@Composable
fun MusicPlaceholder(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        backgroundColor,
                        lerp(backgroundColor, MaterialTheme.colorScheme.surfaceContainerLowest, 0.30f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * A variant that takes a nullable Uri for local album art as well.
 */
@Composable
fun AlbumArtImage(
    imageUrl: String?,
    localUri: android.net.Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    placeholderBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    placeholderIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
) {
    // Prefer local URI, fallback to remote URL
    AlbumArtPlate(
        model = localUri ?: imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        shape = shape,
        placeholderBackgroundColor = placeholderBackgroundColor,
        placeholderIconColor = placeholderIconColor
    )
}

/**
 * The one implementation behind both [AlbumArtImage] overloads: the picture,
 * clipped to [shape], with the hairline drawn *above* it (a border under the
 * image is covered by the image) and [MusicPlaceholder] rounding to the same
 * corners the picture does.
 */
@Composable
private fun AlbumArtPlate(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier,
    size: Dp,
    shape: Shape,
    placeholderBackgroundColor: Color,
    placeholderIconColor: Color
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(placeholderBackgroundColor)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .clip(shape),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                ) {
                    MusicPlaceholder(
                        backgroundColor = placeholderBackgroundColor,
                        iconColor = placeholderIconColor,
                        iconSize = size * 0.42f
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                ) {
                    MusicPlaceholder(
                        backgroundColor = placeholderBackgroundColor,
                        iconColor = placeholderIconColor,
                        iconSize = size * 0.42f
                    )
                }
            }
        )
        // The hairline sits on the last layer so the picture cannot paint
        // over it, the same edge the cards wear - faint enough to disappear
        // on a light cover, which is the point: it is there for dark ones.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                    shape = shape
                )
        )
    }
}
