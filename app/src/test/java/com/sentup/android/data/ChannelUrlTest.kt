package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The URL a channel leaves the app as.
 *
 * The whole point of an export is that somebody else can read it, so every case
 * here is a case that used to produce a file which looked fine and imported as
 * nothing: `…/channel/@name` is the one that actually shipped, and a
 * subscription list that silently loses its handle channels is worse than an
 * export that fails loudly.
 */
class ChannelUrlTest {

    @Test
    fun `a real channel id keeps the channel url`() {
        assertEquals(
            "https://www.youtube.com/channel/UC4QobU6STFB0P71PMvOGN5A",
            channelExportUrl("UC4QobU6STFB0P71PMvOGN5A"),
        )
    }

    @Test
    fun `a real channel id wins over a known handle`() {
        // An id never changes and a creator can change a handle, so the
        // stronger key goes out.
        assertEquals(
            "https://www.youtube.com/channel/UC4QobU6STFB0P71PMvOGN5A",
            channelExportUrl("UC4QobU6STFB0P71PMvOGN5A", "@sentup"),
        )
    }

    @Test
    fun `a handle id becomes the handle url`() {
        // The bug this function exists for: written as a channel id this is
        // `…/channel/@name`, which resolves to nothing anywhere.
        assertEquals("https://www.youtube.com/@somechannel", channelExportUrl("@somechannel"))
    }

    @Test
    fun `an id that is not a channel id uses the handle the profile knows`() {
        // A custom-URL channel, or an old `user/…` name pasted in as an id.
        assertEquals(
            "https://www.youtube.com/@somechannel",
            channelExportUrl("somechannel", "@somechannel"),
        )
        assertEquals(
            "https://www.youtube.com/@somechannel",
            channelExportUrl("somechannel", "somechannel"),
        )
    }

    @Test
    fun `an id with no shape at all is still exported rather than dropped`() {
        // Nothing here can improve the link, but a channel missing from an
        // export is a silent loss, and a wrong URL is a visible one.
        assertEquals("https://www.youtube.com/channel/abc123", channelExportUrl("abc123"))
    }

    @Test
    fun `whitespace never reaches the url`() {
        assertEquals(
            "https://www.youtube.com/channel/UC4QobU6STFB0P71PMvOGN5A",
            channelExportUrl("  UC4QobU6STFB0P71PMvOGN5A  "),
        )
        assertEquals(
            "https://www.youtube.com/@somechannel",
            channelExportUrl("@somechannel", "  @somechannel  "),
        )
    }

    @Test
    fun `a lone at sign is not a handle`() {
        // "@" alone would otherwise become `…/@`, which resolves to nothing.
        assertEquals("https://www.youtube.com/channel/@", channelExportUrl("@"))
    }

    @Test
    fun `a blank handle never becomes a bare at sign`() {
        assertEquals("https://www.youtube.com/channel/abc123", channelExportUrl("abc123", "@"))
        assertEquals("https://www.youtube.com/channel/abc123", channelExportUrl("abc123", "   "))
    }
}