package com.sentup.android.ui.components

import androidx.compose.foundation.lazy.LazyListState

/**
 * How far down a list can be and still animate its way back to the top.
 *
 * Past this, animating is worse than jumping: `animateScrollToItem` travels the
 * whole distance, so on a feed a few hundred items deep it visibly flies through
 * everything in between and takes noticeably longer than the gesture that asked
 * for it. Below it the travel is short enough that the motion reads as the list
 * moving rather than as a delay, and it keeps the sense of having come back up.
 */
private const val ANIMATED_SCROLL_TO_TOP_MAX_ITEMS = 12

/**
 * Returns a list to the top, animating when that is quick and jumping when it
 * is not.
 *
 * The single place this decision is made, so every tab answers a re-tap the
 * same way.
 */
suspend fun LazyListState.scrollToTop() {
    if (firstVisibleItemIndex > ANIMATED_SCROLL_TO_TOP_MAX_ITEMS) {
        scrollToItem(0)
    } else {
        animateScrollToItem(0)
    }
}

/**
 * The grid twin of [scrollToTop]. A tab whose content is a lazy grid (Shorts)
 * answers a re-tap with the same animate-then-jump decision as the lazy lists,
 * so the bar behaves identically whichever container the page chose.
 */
suspend fun androidx.compose.foundation.lazy.grid.LazyGridState.scrollToTop() {
    if (firstVisibleItemIndex > ANIMATED_SCROLL_TO_TOP_MAX_ITEMS) {
        scrollToItem(0)
    } else {
        animateScrollToItem(0)
    }
}
