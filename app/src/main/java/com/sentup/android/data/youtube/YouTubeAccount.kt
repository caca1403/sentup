package com.sentup.android.data.youtube

import com.sentup.android.data.NotificationItem
import com.sentup.android.data.NotificationTarget
import com.sentup.android.data.SessionManager
import com.sentup.android.util.KLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The signed-in account itself: who it is, and its notification inbox.
 * Both answer nothing signed out.
 */
internal class YouTubeAccount(
    private val musicApi: MusicApi,
    private val webApi: WebApi,
    private val sessionManager: SessionManager,
) {
    suspend fun fetchAccountInfo() = withContext(Dispatchers.IO) {
        // Captured before the call so the name, avatar and datasyncId it parses
        // land on the account that was asked, even if the user switches away
        // while the request is out.
        val session = sessionManager.captureSession() ?: return@withContext

        try {
            val jsonResponse = musicApi.fetchInternalApi("account/account_menu")

            if (jsonResponse.isEmpty()) {
                KLog.w(YOUTUBE_TAG, "fetchAccountInfo: empty response from account/account_menu")
                return@withContext
            }
            // Account payloads include identity details. Keep them out of the
            // release diagnostic ring buffer; success/failure is enough here.
            KLog.d(YOUTUBE_TAG, "fetchAccountInfo response received")
            
            var avatarUrl: String? = null
            var userName: String? = null
            
            try {
                val root = JSONObject(jsonResponse)
                
                // Navigate to the account section
                // Usually: actions -> openPopupAction -> popup -> multiPageMenuRenderer -> header -> activeAccountHeaderRenderer
                val actions = root.optJSONArray("actions")
                val popup = actions?.optJSONObject(0)
                    ?.optJSONObject("openPopupAction")
                    ?.optJSONObject("popup")
                    ?.optJSONObject("multiPageMenuRenderer")
                
                val header = popup?.optJSONObject("header")?.optJSONObject("activeAccountHeaderRenderer")
                
                if (header != null) {
                    // Extract Name
                    userName = getRunText(header.optJSONObject("accountName"))
                    
                    // Extract Avatar
                    val thumbnails = header.optJSONObject("avatar")?.optJSONArray("thumbnails")
                    if (thumbnails != null && thumbnails.length() > 0) {
                        avatarUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                    }
                }
                
                // Fallback: Check sections if header failed
                if (userName == null || avatarUrl == null) {
                    val sections = popup?.optJSONArray("sections")
                    if (sections != null) {
                        for (i in 0 until sections.length()) {
                            val item = sections.optJSONObject(i)
                                ?.optJSONObject("multiPageMenuSectionRenderer")
                                ?.optJSONArray("items")?.optJSONObject(0)
                                ?.optJSONObject("compactLinkRenderer")
                                
                            // Sometimes the first item is the account link
                            if (item != null) {
                                val thumb = item.optJSONObject("icon")?.optJSONArray("thumbnails")
                                if (avatarUrl == null && thumb != null) {
                                    avatarUrl = thumb.optJSONObject(thumb.length() - 1)?.optString("url")
                                }
                            }
                        }
                    }
                }
                
                // Fallback: Regex for avatar hosts if parsing failed
                // (yt3.ggpht.com and lh3.googleusercontent.com serve account avatars)
                if (avatarUrl == null) {
                    val avatarRegex = "\"url\"\\s*:\\s*\"(https://(?:yt3\\.ggpht\\.com|[a-z0-9]+\\.googleusercontent\\.com)/[^\"]+)\"".toRegex()
                    val match = avatarRegex.find(jsonResponse)
                    avatarUrl = match?.groupValues?.get(1)
                }

            } catch (jsonEx: Exception) {
                // Ignore
            }
            
            KLog.d(YOUTUBE_TAG, "fetchAccountInfo parsed name=$userName avatar=$avatarUrl")

            // Save avatar if found
            if (!avatarUrl.isNullOrEmpty()) {
                // Upgrade resolution
                val highResUrl = avatarUrl
                    .replace("=s88", "=s512")
                    .replace("=s48", "=s512")
                    .replace("=s96", "=s512")
                sessionManager.updateSessionIdentity(session, avatarUrl = highResUrl)
            }

            // Save user name if found
            if (!userName.isNullOrEmpty()) {
                sessionManager.updateSessionIdentity(session, name = userName)
            }

            // YouTube's own account identifier, which every authenticated
            // response carries for free (verified August 2026 against
            // account_menu, FEsubscriptions and FEwhat_to_watch: present on all
            // three, identical across them, alongside a `loggedOut` boolean).
            // Stored verbatim, trailing separators included - it is only ever
            // compared against another copy of itself.
            //
            // This is what lets the app recognise an account it already has:
            // signing back in repairs that profile instead of adding a
            // duplicate row, and a restored backup can tell that an account in
            // the file is the one already signed in here.
            runCatching {
                JSONObject(jsonResponse)
                    .optJSONObject("responseContext")
                    ?.optJSONObject("mainAppWebResponseContext")
                    ?.optString("datasyncId")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { sessionManager.updateSessionIdentity(session, datasyncId = it) }
            }
            
        } catch (e: Exception) {
            KLog.e(YOUTUBE_TAG, "Account identity refresh failed", e)
        }
    }

    /**
     * Every channel of the signed-in Google account: the account's own channel
     * and each brand channel beside it.
     *
     * Read from `account/accounts_list` - see [parseChannels] for the request
     * body and the shapes it answers with. Empty signed out, and empty on any
     * failure: to the caller, an account with one channel and a list that could
     * not be read look the same, which is why whoever shows this must offer the
     * account's own channel whether or not anything comes back.
     */
    suspend fun fetchChannels(): List<YouTubeChannel> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()
        try {
            val raw = webApi.postWatchApi(
                "account/accounts_list",
                JSONObject()
                    .put("context", webApi.webContext())
                    // What youtube.com sends when the account switcher opens:
                    // the whole list of channels, rather than a reduced one.
                    .put("requestType", "ACCOUNTS_LIST_REQUEST_TYPE_CHANNEL_SWITCHER")
                    .put("callCircumstance", "SWITCHING_USERS_FULL")
            ) ?: return@withContext emptyList()
            val channels = parseChannels(raw)
            // Counts only: the names and photos are account identity, and the
            // release ring buffer is exactly what a bug report carries.
            KLog.d(
                YOUTUBE_TAG,
                "accounts_list: ${channels.size} channel(s), ${channels.count { it.pageId != null }} with a page id",
            )
            if (channels.isEmpty()) {
                // A signed-in account with no readable rows is a shape change,
                // not an empty roster: every account has at least its own
                // channel. Log the renderer key *names* so the shape can be
                // relearned from a bug report; names are structural and carry
                // no account identity, so this is the whole extent of the body
                // that is ever logged.
                KLog.w(YOUTUBE_TAG, "accounts_list parsed no channels; response keys: " + responseKeyNames(raw))
            }
            channels
        } catch (e: Exception) {
            KLog.e(YOUTUBE_TAG, "fetchChannels failed", e)
            emptyList()
        }
    }

    /**
     * Distinct JSON key names down to [MAX_DEPTH] levels, comma-joined and
     * capped - the response's skeleton with every value left behind. The
     * channel rows live a few levels under the page renderer, so the walk goes
     * deep; names are structural and never account identity, which is what
     * makes this safe to log.
     */
    private fun responseKeyNames(body: String): String {
        val names = LinkedHashSet<String>()
        fun walk(node: Any?, depth: Int) {
            if (depth > 10 || names.size >= 400) return
            when (node) {
                is JSONObject -> {
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (!names.add(key)) continue
                        walk(node.opt(key), depth + 1)
                    }
                }
                is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), depth + 1)
            }
        }
        runCatching { walk(JSONObject(body), 0) }
        return names.joinToString(",").take(2400)
    }

    /**
     * The user's notification inbox (new uploads from subscribed channels,
     * replies, etc.). Requires login.
     *
     * The inbox is paged, and how far it goes depends on the account. [verified
     * September 2026] A page is about 20 items followed by a
     * `continuationItemRenderer` whose `getNotificationMenuEndpoint.ctoken` is
     * posted back to the same endpoint as `{"ctoken": ...}`; that account's
     * whole inbox was three pages. [verified October 2026] Another account's
     * was 11 items and no continuation at all. So up to
     * [NOTIFICATION_INBOX_MAX_PAGES] are followed, and a response without a
     * token is simply the end. [scar] Only the first page was ever read, so
     * the sheet stopped at 20 on an account with sixty. Whatever YouTube has
     * dropped altogether is what [NotificationHistoryStore] remembers.
     */
    suspend fun getNotifications(): List<NotificationItem> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()
        try {
            val raw = webApi.postWatchApi(
                "notification/get_notification_menu",
                JSONObject()
                    .put("context", webApi.webContext())
                    .put("notificationsMenuRequestType", "NOTIFICATIONS_MENU_REQUEST_TYPE_INBOX")
            ) ?: return@withContext emptyList()
            var root = JSONObject(raw)
            val renderers = mutableListOf<JSONObject>()
            var page = 1
            while (true) {
                findObjectsByKey(root, "notificationRenderer", renderers)
                if (page >= NOTIFICATION_INBOX_MAX_PAGES) break
                val endpoints = mutableListOf<JSONObject>()
                findObjectsByKey(root, "getNotificationMenuEndpoint", endpoints)
                val token = endpoints.firstNotNullOfOrNull { endpoint ->
                    endpoint.optString("ctoken").takeIf { it.isNotBlank() }
                } ?: break
                // A later page that fails keeps the pages already read.
                val next = webApi.postWatchApi(
                    "notification/get_notification_menu",
                    JSONObject().put("context", webApi.webContext()).put("ctoken", token)
                ) ?: break
                root = JSONObject(next)
                page++
            }
            renderers.distinctBy { it.optString("notificationId").ifBlank { it.toString() } }.mapNotNull { renderer ->
                val message = renderer.optJSONObject("shortMessage")?.optString("simpleText")
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                fun lastThumb(key: String): String? {
                    val thumbs = renderer.optJSONObject(key)?.optJSONArray("thumbnails") ?: return null
                    var url = thumbs.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))
                        ?.optString("url")?.takeIf { it.isNotBlank() }
                    if (url?.startsWith("//") == true) url = "https:$url"
                    return url
                }
                val navigation = renderer.optJSONObject("navigationEndpoint")
                fun endpointVideoId(key: String): String? =
                    navigation?.optJSONObject(key)?.optString("videoId")?.takeIf { it.isNotBlank() }
                val webUrl = navigation?.optJSONObject("commandMetadata")
                    ?.optJSONObject("webCommandMetadata")?.optString("url")
                    ?.takeIf { it.startsWith("/") }
                    ?.let { "https://www.youtube.com$it" }
                val postParams = navigation?.optJSONObject("browseEndpoint")
                    ?.takeIf { it.optString("browseId") == POST_DETAIL_BROWSE_ID }
                    ?.optString("params")?.takeIf { it.isNotBlank() }
                val target = endpointVideoId("watchEndpoint")?.let { NotificationTarget.Video(it) }
                    ?: endpointVideoId("reelWatchEndpoint")?.let { NotificationTarget.Short(it) }
                    ?: postParams?.let { NotificationTarget.Post(it, webUrl) }
                    ?: webUrl?.let { NotificationTarget.Link(it) }
                NotificationItem(
                    message = message,
                    sentTime = renderer.optJSONObject("sentTimeText")?.optString("simpleText").orEmpty(),
                    channelAvatarUrl = lastThumb("thumbnail"),
                    videoThumbnailUrl = lastThumb("videoThumbnail"),
                    target = target,
                    isRead = renderer.optBoolean("read", false),
                    id = renderer.optString("notificationId")
                )
            }
        } catch (e: Exception) {
            KLog.e(YOUTUBE_TAG, "getNotifications failed", e)
            emptyList()
        }
    }

    private companion object {
        /** Pages of the notification inbox read on one open: one request each. */
        private const val NOTIFICATION_INBOX_MAX_PAGES = 3
    }
}
