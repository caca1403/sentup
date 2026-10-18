package com.sentup.android.data

/**
 * The YouTube URL a channel is written as when it leaves the app.
 *
 * This exists because the obvious line of code is wrong for a real share of
 * users. `https://www.youtube.com/channel/<id>` is correct only when `<id>`
 * really is a `UC…` id, and a channel reached by handle - which is how NewPipe
 * search hands rows over, and how a lot of people paste links - carries
 * `@name` there instead. That produces `…/channel/@name`, which YouTube 404s
 * and every importer in the exchange (NewPipe, PipePipe, Tubular) either drops
 * or stores as unresolvable: an export that looks complete and quietly is not.
 *
 * So the shape of the id decides the shape of the URL:
 *
 * - `@handle` → the handle URL, which YouTube resolves and everyone else
 *   understands
 * - `UC…` → the channel URL, because an id never changes and a handle does
 * - anything else → the handle when the profile knows one, since a handle is
 *   the only form of that id YouTube will still resolve. With no handle to
 *   fall back on it is written as a channel id, which is what the importer
 *   will try and what the app itself can still look up - a wrong link that
 *   someone can see beats a channel quietly missing from the file.
 */
internal fun channelExportUrl(channelId: String, handle: String? = null): String {
    val id = channelId.trim()
    val fallbackHandle = handle?.trim()?.trimStart('@')?.takeIf { it.isNotEmpty() }
    return when {
        id.startsWith("@") && id.length > 1 -> "https://www.youtube.com/$id"
        id.startsWith(UC_ID_PREFIX) -> "https://www.youtube.com/channel/$id"
        fallbackHandle != null -> "https://www.youtube.com/@$fallbackHandle"
        else -> "https://www.youtube.com/channel/$id"
    }
}

/** The only channel-id shape YouTube hands out today. */
private const val UC_ID_PREFIX = "UC"