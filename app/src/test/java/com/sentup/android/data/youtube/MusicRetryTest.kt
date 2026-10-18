package com.sentup.android.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When a music request is worth repeating, and when repeating it would only
 * make the wait longer.
 *
 * The answers here are all about not making things worse: every case that looks
 * like a "second attempt" in the code is a case where the second attempt is
 * skipped on purpose, and those are exactly the ones with no test unless
 * somebody writes them.
 */
class MusicRetryTest {

    @Test
    fun `a server error is repeated once`() {
        assertEquals(1_200L, transientRetryMs(503, null, attempt = 0))
        assertEquals(1_200L, transientRetryMs(500, null, attempt = 0))
        assertEquals(1_200L, transientRetryMs(502, null, attempt = 0))
        assertEquals(1_200L, transientRetryMs(504, null, attempt = 0))
    }

    @Test
    fun `a rate limit is repeated once`() {
        assertEquals(1_200L, transientRetryMs(429, null, attempt = 0))
    }

    @Test
    fun `the repeat is the last one`() {
        // Two identical failures are not a coincidence, and a user waiting on a
        // library page should not be made to sit through a third try.
        assertNull(transientRetryMs(503, null, attempt = 1))
        assertNull(transientRetryMs(429, "2", attempt = 1))
    }

    @Test
    fun `an answer no repeat can fix is not repeated`() {
        assertNull(transientRetryMs(403, null, attempt = 0))
        assertNull(transientRetryMs(404, null, attempt = 0))
        assertNull(transientRetryMs(400, null, attempt = 0))
    }

    @Test
    fun `a short server-told wait is obeyed`() {
        assertEquals(3_000L, transientRetryMs(429, "3", attempt = 0))
        assertEquals(8_000L, transientRetryMs(503, "8", attempt = 0))
    }

    @Test
    fun `a long server-told wait is left to the rate-limit hold`() {
        // 60 seconds is not something to make someone watch. The hold already
        // covers the window; the request that would be repeated here is a page
        // someone is waiting on, and it will not have changed by then.
        assertEquals(1_200L, transientRetryMs(429, "60", attempt = 0))
        assertEquals(1_200L, transientRetryMs(503, "3600", attempt = 0))
    }

    @Test
    fun `a header that is not a number falls back to the default wait`() {
        assertEquals(1_200L, transientRetryMs(429, "Wed, 21 Oct 2026 07:28:00 GMT", attempt = 0))
        assertEquals(1_200L, transientRetryMs(503, "", attempt = 0))
        assertEquals(1_200L, transientRetryMs(503, "   ", attempt = 0))
    }

    @Test
    fun `zero is an answer too`() {
        // Retry-After: 0 says try again now, and one repeat is what we do.
        assertEquals(0L, transientRetryMs(429, "0", attempt = 0))
    }
}