package com.sentup.android.ui.video

import com.sentup.android.data.CommentItem
import com.sentup.android.data.VttCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoToolsEngineTest {
    @Test
    fun `abLoopNextSeekMs triggers only when position reaches B past A`() {
        assertNull(VideoToolsEngine.abLoopNextSeekMs(5_000L, null, 10_000L))
        assertNull(VideoToolsEngine.abLoopNextSeekMs(5_000L, 2_000L, 10_000L))
        assertEquals(2_000L, VideoToolsEngine.abLoopNextSeekMs(10_000L, 2_000L, 10_000L))
        assertNull(VideoToolsEngine.abLoopNextSeekMs(10_000L, 9_800L, 10_000L))
    }

    @Test
    fun `filterComments supports timestamps, questions, pinned, and search query`() {
        val comments = listOf(
            CommentItem(commentId = "1", text = "Harika video 03:45 kısmı efsane", author = "Ali", authorAvatarUrl = null, publishedTime = "1d", likeCount = "10", replyCount = "", isPinned = false, isHearted = false, isCreator = false, isVerified = false, repliesToken = null),
            CommentItem(commentId = "2", text = "Hangi mikrofonu kullanıyorsun?", author = "Veli", authorAvatarUrl = null, publishedTime = "2h", likeCount = "3", replyCount = "", isPinned = false, isHearted = false, isCreator = false, isVerified = false, repliesToken = null),
            CommentItem(commentId = "3", text = "Teşekkürler arkadaşlar", author = "Kanal", authorAvatarUrl = null, publishedTime = "3d", likeCount = "99", replyCount = "", isPinned = true, isHearted = true, isCreator = true, isVerified = true, repliesToken = null),
        )
        assertEquals(listOf("1"), VideoToolsEngine.filterComments(comments, "", CommentFilterMode.TIMESTAMPS).map { it.commentId })
        assertEquals(listOf("2"), VideoToolsEngine.filterComments(comments, "", CommentFilterMode.QUESTIONS).map { it.commentId })
        assertEquals(listOf("3"), VideoToolsEngine.filterComments(comments, "", CommentFilterMode.PINNED_OR_HEARTED).map { it.commentId })
        assertEquals(listOf("2"), VideoToolsEngine.filterComments(comments, "mikrofon", CommentFilterMode.ALL).map { it.commentId })
    }

    @Test
    fun `searchTranscriptCues filters cues case-insensitively`() {
        val cues = listOf(
            VttCue(1_000L, 3_000L, "Bugün sinir ağlarını inceliyoruz"),
            VttCue(3_500L, 6_000L, "Şimdi VPN ayarlarına geçelim"),
        )
        assertEquals(1, VideoToolsEngine.searchTranscriptCues(cues, "vpn").size)
        assertEquals(2, VideoToolsEngine.searchTranscriptCues(cues, "").size)
    }
}
