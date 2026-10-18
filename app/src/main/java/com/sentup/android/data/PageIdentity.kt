package com.sentup.android.data

/**
 * Which channel of a Google account a session is on.
 *
 * A Google account owns one *primary* channel and any number of *brand*
 * channels, and YouTube hands out the difference for free in `DATASYNC_ID` -
 * `responseContext.mainAppWebResponseContext.datasyncId`, the value [Profile]
 * stores. It is `"<brand page id>||<user session id>"` while the session is on
 * a brand channel and `"<user session id>||"` while it is on the primary one,
 * so the first half is only ever a channel id when the second half is not
 * empty.
 *
 * That first half is the id a browser sends as `X-Goog-PageId`, and sending it
 * is the whole difference between a request reading as the brand channel and
 * the same request reading as the primary one. Without it an app can be signed
 * into a brand channel and still never show a single thing from it.
 *
 * [Verified October 2026 against yt-dlp, the reference implementation of both
 * halves: `_parse_data_sync_id` splits the string exactly this way and
 * `_generate_cookie_auth_headers` sends the first part as `X-Goog-PageId`
 * beside `X-Goog-AuthUser`. YouTube's own client config calls the same value
 * `DELEGATED_SESSION_ID`.]
 */
internal fun brandChannelIdOf(datasyncId: String?): String? {
    val raw = datasyncId?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val separator = raw.indexOf("||")
    // No separator at all is not the shape YouTube writes; treat it as "this
    // says nothing about channels" rather than guessing at a prefix.
    if (separator < 0) return null
    // The primary-channel form puts the *user session id* first and leaves the
    // second half empty, so an empty tail means there is no brand channel here.
    if (raw.substring(separator + 2).isEmpty()) return null
    return raw.substring(0, separator).takeIf { it.isNotBlank() }
}

/**
 * The *account* half of a `DATASYNC_ID`: the part that is not a channel's page
 * id.
 *
 * A brand channel writes `"<page id>||<user session id>"` and the primary
 * channel writes `"<user session id>||"`, so the account half is the tail when
 * there is one and the head otherwise. It answers one question: is the login
 * that just arrived the same account as the one stored before it? That is the
 * difference between keeping a pinned channel across a re-login and dropping
 * one that belongs to an account the profile is no longer signed into.
 *
 * Both halves of the string are opaque; the split is the only structure
 * claimed here.
 */
internal fun accountIdOf(datasyncId: String?): String? {
    val raw = datasyncId?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val separator = raw.indexOf("||")
    if (separator < 0) return raw
    val tail = raw.substring(separator + 2).trim()
    if (tail.isNotEmpty()) return tail
    return raw.substring(0, separator).trim().takeIf { it.isNotBlank() }
}

/**
 * Which channel a profile is pointed at, from its two stored fields.
 *
 * One rule, in one place, because two call sites need the same answer: the
 * session built for the network layer ([ProfileManager.captureSession]) and the
 * one the picker reads back to know which row is ticked
 * ([SessionManager.activePageId]). They disagreed once - the picker's copy
 * ignored [channelPinned] and so could not show the account's own channel as
 * chosen while the session was on a brand channel - and the fix is to have only
 * one copy.
 *
 * A pin wins, including a pin on the account's own channel, which is a null
 * page id on purpose. Without a pin, the channel YouTube reports the session is
 * already on wins, so a profile nobody has pointed anywhere still reads the
 * account the way YouTube itself does.
 */
internal fun resolvePageId(pageId: String?, channelPinned: Boolean, datasyncId: String?): String? =
    if (channelPinned) pageId?.trim()?.takeIf { it.isNotBlank() } else brandChannelIdOf(datasyncId)
