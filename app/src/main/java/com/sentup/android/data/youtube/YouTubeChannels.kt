package com.sentup.android.data.youtube

import org.json.JSONArray
import org.json.JSONObject

/**
 * One channel of the signed-in Google account: the account's own channel, or
 * one of the brand channels ("marka hesabı") beside it.
 *
 * [pageId] is what the app needs to *be* that channel: it goes out as
 * `X-Goog-PageId` on every authenticated request (see
 * [brandChannelIdOf][com.sentup.android.data.brandChannelIdOf] for the same
 * value reached from the other direction). It is null for the account's own
 * channel, which is targeted by sending no page id at all - so a null here is
 * a real answer, not a parse failure.
 */
internal data class YouTubeChannel(
    val pageId: String?,
    val name: String,
    val handle: String?,
    val avatarUrl: String?,
    /** "Kişisel kanal" / "Marka hesabı", as YouTube words it under the name. */
    val byline: String?,
    /** True for the one channel YouTube says this session is currently on. */
    val isCurrent: Boolean,
)

/**
 * The channels in an `account/accounts_list` response.
 *
 * This is the request the account switcher on youtube.com makes when it opens
 * (`ACCOUNTS_LIST_REQUEST_TYPE_CHANNEL_SWITCHER`, `callCircumstance`
 * `SWITCHING_USERS_FULL`; the same body youtubei.js builds for
 * `getInfo(all = true)`), and the only endpoint that names the brand channels
 * at all - `account/account_menu` reports the *current* channel and nothing
 * else, which is why a brand channel used to be invisible in this app.
 *
 * Two response shapes are read, because the list has been sent both ways:
 * `accountItem` renderers (what the endpoint answered until late 2026, whatever
 * the wrapping, hence [findObjectsByKey]) and, as a fallback, the menu-link form
 * `compactLinkRenderer` that older responses used.
 *
 * A third wrapper arrived with the October 2026 account switcher: the list is
 * posted as `updateChannelSwitcherPageAction.page.channelSwitcherPageRenderer`,
 * whose `contents` carry the same fields under `accountItemRenderer`. [verified
 * October 2026 against the live accounts_list of a signed-in account: the old
 * `accountItem` key was gone and rows arrived as `accountItemRenderer` with the
 * same `accountName`/`accountPhoto`/`isSelected`/`channelHandle` fields, which
 * is why the shape is read by both names rather than by the new path alone.]
 * Items that cannot be named are dropped rather than shown as blanks.
 */
internal fun parseChannels(body: String): List<YouTubeChannel> {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()

    val items = mutableListOf<JSONObject>()
    findObjectsByKey(root, "accountItem", items)
    var modern = items.mapNotNull { accountItemChannel(it) }
    if (modern.isEmpty()) {
        items.clear()
        findObjectsByKey(root, "accountItemRenderer", items)
        modern = items.mapNotNull { accountItemChannel(it) }
    }
    if (modern.isNotEmpty()) return modern

    val links = mutableListOf<JSONObject>()
    findObjectsByKey(root, "compactLinkRenderer", links)
    return links.mapNotNull { compactLinkChannel(it) }
}

/**
 * The Google page id inside one account item, wherever it is nested.
 *
 * **Deliberately a scan rather than a field read.** youtubei.js - the closest
 * thing to a spec for this renderer - parses `accountName`, `accountPhoto`,
 * `isSelected`, `isDisabled`, `hasChannel`, `serviceEndpoint`, `accountByline`
 * and `channelHandle`, and no id field at all: the browser switches the channel
 * server-side through `serviceEndpoint` and then reloads a page whose ytcfg
 * carries the new `DELEGATED_SESSION_ID`. So the id has to be somewhere the
 * library does not read, and which key it sits under is not documented anywhere
 * - hence every plausible name, plus a scan of every string in the item for a
 * `...=123456789` form, since a switch URL would carry it as a query parameter.
 *
 * An item without one is not an error: the account's own channel has no page id
 * by definition.
 */
internal fun pageIdIn(item: JSONObject): String? {
    var found: String? = null

    fun fromString(value: String) {
        if (found != null) return
        found = PAGE_ID_IN_URL.find(value)?.groupValues?.get(1)
    }

    fun scan(node: Any?) {
        if (found != null) return
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext() && found == null) {
                    val key = keys.next()
                    if (key.lowercase() in PAGE_ID_KEYS) {
                        val value = node.optString(key).takeIf { it.isNotBlank() && it != "null" }
                        if (value != null) {
                            found = value
                            return
                        }
                    }
                    val value = node.opt(key)
                    if (value is JSONObject || value is JSONArray) scan(value) else if (value is String) fromString(value)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) scan(node.opt(i))
            is String -> fromString(node)
        }
    }

    scan(item)
    return found
}

private fun accountItemChannel(item: JSONObject): YouTubeChannel? {
    val name = getRunText(item.optJSONObject("accountName"))?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return YouTubeChannel(
        pageId = pageIdIn(item),
        name = name,
        handle = getRunText(item.optJSONObject("channelHandle"))?.trim()?.takeIf { it.isNotBlank() },
        avatarUrl = avatarOf(item.optJSONObject("accountPhoto")),
        byline = getRunText(item.optJSONObject("accountByline"))?.trim()?.takeIf { it.isNotBlank() },
        isCurrent = item.optBoolean("isSelected", false),
    )
}

/**
 * The older menu-link form. Kept narrow on purpose: this response also carries
 * ordinary menu links, and a link that is not a channel must not become one, so
 * only a `/channel/UC...` destination counts.
 */
private fun compactLinkChannel(link: JSONObject): YouTubeChannel? {
    val name = getRunText(link.optJSONObject("title"))?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val url = link.optJSONObject("navigationEndpoint")
        ?.optJSONObject("commandMetadata")
        ?.optJSONObject("webCommandMetadata")
        ?.optString("url")
        .orEmpty()
    val browseId = link.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
    if (!url.contains("/channel/") && !browseId.startsWith("UC")) return null
    return YouTubeChannel(
        pageId = pageIdIn(link),
        name = name,
        handle = null,
        avatarUrl = avatarOf(link.optJSONObject("icon")),
        byline = null,
        isCurrent = link.optBoolean("isSelected", false),
    )
}

/**
 * The largest thumbnail in an icon/photo object, at a size worth drawing.
 *
 * Google hands account photos out at the size the menu asks for (`=s88`, 88px),
 * which is soft next to a 40dp avatar on a dense screen; the `=sNNN` suffix is
 * a request parameter, so asking for a bigger one costs nothing.
 */
private fun avatarOf(holder: JSONObject?): String? {
    val thumbnails = holder?.optJSONArray("thumbnails") ?: return null
    var url = thumbnails.optJSONObject((thumbnails.length() - 1).coerceAtLeast(0))
        ?.optString("url")
        ?.takeIf { it.isNotBlank() }
        ?: return null
    if (url.startsWith("//")) url = "https:$url"
    return url.replace("=s88", "=s512").replace("=s48", "=s512").replace("=s96", "=s512")
}

/** Lowercase keys that would hold a Google page id, in the forms seen in Google payloads. */
private val PAGE_ID_KEYS = setOf(
    "pageid",
    "page_id",
    "accountid",
    "account_id",
    "delegatedsessionid",
    "delegated_session_id",
    "gaiaid",
    "obfuscatedgaiaid",
)

/** A page id carried in a URL or a tracking blob rather than in a field of its own. */
private val PAGE_ID_IN_URL = Regex("(?:pageId|page_id|accountId|delegatedSessionId)=([0-9]{6,})")
