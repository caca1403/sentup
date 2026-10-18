package com.sentup.android.data.youtube

/**
 * How long to wait before repeating a request that came back transiently, or
 * null when the answer is not one a repeat can fix.
 *
 * Split out of [MusicApi] as a pure function because "did we retry, and did we
 * give up for the right reason" is the whole question a caller asks of it, and
 * it is the kind of answer that is much easier to be wrong about than to read.
 *
 * - **One repeat, not a loop.** A second failure will not be fixed by a third
 *   identical request, and the user is waiting on the answer.
 * - **500-504 and 429 are transient; 403 is not.** A 403 on this origin is
 *   "you may not read this", and repeating it only spends the budget faster.
 * - **A server-told wait is only honoured when it is brief.** `Retry-After: 60`
 *   says the answer will not change while the user waits; the rate-limit hold
 *   is where that belongs. A short one is YouTube shedding load for a moment,
 *   which is exactly what one repeat is for.
 */
internal fun transientRetryMs(code: Int, retryAfterHeader: String?, attempt: Int): Long? {
    if (attempt >= 1) return null
    if (code != 429 && code !in 500..504) return null
    val told = retryAfterHeader?.trim()?.toLongOrNull()?.times(1000L)
    return told?.takeIf { it in 0..MAX_TRANSIENT_WAIT_MS } ?: DEFAULT_TRANSIENT_WAIT_MS
}

/** What to wait for an unnamed transient: short enough to feel immediate. */
private const val DEFAULT_TRANSIENT_WAIT_MS = 1_200L

/** Ceiling on a server-told wait a user is made to sit through. */
private const val MAX_TRANSIENT_WAIT_MS = 8_000L