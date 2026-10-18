package com.sentup.android.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sentup.android.util.SentupHaptics
import com.sentup.android.util.rememberSentupHaptics

/**
 * Expressive haptics shared by every player style.
 *
 * Routes through [SentupHaptics], so everything here honours the user's haptics
 * setting along with the rest of the app.
 */
class PlayerHaptics(private val sentup: SentupHaptics) {

    /** A previous/next skip committed (button tap or swipe gesture). */
    fun skip() = sentup.confirm()

    /** Play/pause toggled; [nowPlaying] is the state being switched to. */
    fun playPause(nowPlaying: Boolean) = sentup.toggle(nowPlaying)

    /** A discrete action landed: liking a song, adding it somewhere. */
    fun confirm() = sentup.confirm()
}

@Composable
fun rememberPlayerHaptics(): PlayerHaptics {
    val sentup = rememberSentupHaptics()
    return remember(sentup) { PlayerHaptics(sentup) }
}
