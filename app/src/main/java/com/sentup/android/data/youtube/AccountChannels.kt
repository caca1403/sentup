package com.sentup.android.data.youtube

import android.content.Context
import com.sentup.android.data.Profile
import com.sentup.android.data.ProfileManager
import com.sentup.android.data.SessionManager
import com.sentup.android.data.brandChannelIdOf
import com.sentup.android.util.KLog
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * A request made while this element is on the coroutine context is read as the
 * account's channel with this page id, not the one the active profile pins.
 *
 * [ProfileManager.captureSession] consults [active] for exactly one thing: the
 * `X-Goog-PageId` the request goes out with (see
 * [com.sentup.android.data.youtube.authenticate]). The login - cookies,
 * generation, profile id - is unchanged, because a Google account's channels
 * are all behind the same session: the page id header *is* the whole difference
 * between a request reading as the personal channel and as a brand one.
 *
 * Carried as a [ThreadContextElement] so the value follows the coroutine across
 * dispatcher hops and is invisible to every other coroutine - a search or a
 * like fired while one multi-channel fetch is paging runs under the profile's
 * own channel, exactly as before.
 */
internal class ChannelView(
    val pageId: String?,
) : ThreadContextElement<ChannelView?> {

    override val key: CoroutineContext.Key<ChannelView> get() = Key

    override fun updateThreadContext(context: CoroutineContext): ChannelView? {
        val previous = onThread.get()
        onThread.set(this)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ChannelView?) {
        onThread.set(oldState)
    }

    companion object Key : CoroutineContext.Key<ChannelView> {
        private val onThread = ThreadLocal<ChannelView?>()

        /**
         * The channel requests launched from this thread are being read as, or
         * null when no multi-channel fetch is running on it.
         */
        fun active(): ChannelView? = onThread.get()
    }
}

/**
 * Which channels of one account a multi-channel fetch walks, in order: the
 * channel the app is currently read as (so the merged list still starts with
 * what the single-channel app showed), then the account's own channel - null,
 * the personal channel - unless that is already the active one, then each brand
 * channel. Duplicates drop, order is kept.
 */
internal fun channelViewIds(active: String?, brands: List<String>): List<String?> {
    val ids = LinkedHashSet<String?>()
    ids.add(active)
    ids.add(null)
    brands.forEach { ids.add(it) }
    return ids.toList()
}

/**
 * Identity keys of one item for the channel merge: its own id when it has one,
 * and a name+count key for the same list appearing on both channels under ids
 * that do not match. Names compare trimmed, whitespace-collapsed and case-folded,
 * and only when the row stated a count - a name alone is not an identity.
 */
internal fun channelItemKeys(id: String?, name: String?, countLabel: String?): List<String> {
    val keys = ArrayList<String>(2)
    id?.trim()?.takeIf { it.isNotBlank() }?.let { keys.add("id:$it") }
    val trimmedName = name?.trim()?.takeIf { it.isNotBlank() }
    val trimmedCount = countLabel?.trim()?.takeIf { it.isNotBlank() }
    if (trimmedName != null && trimmedCount != null) {
        keys.add("name:" + trimmedName.replace(CHANNEL_KEY_WHITESPACE, " ").lowercase() +
            "|$trimmedCount")
    }
    return keys
}

private val CHANNEL_KEY_WHITESPACE = Regex("\\s+")

/**
 * Merges pages read one channel at a time, first copy wins. An item is a repeat
 * when *any* of its keys was seen; keeping the first copy keeps the source label
 * (owner name, thumbnail) of the channel whose response arrived with it. Items
 * with no keys at all are kept - nothing here may silently drop a row it cannot
 * identify.
 */
internal fun <T> mergeChannelPages(pages: List<List<T>>, keys: (T) -> List<String>): List<T> {
    if (pages.size <= 1) return pages.firstOrNull().orEmpty()
    val seen = HashSet<String>()
    val merged = ArrayList<T>()
    for (page in pages) {
        for (item in page) {
            val itemKeys = keys(item)
            if (itemKeys.any { it in seen }) continue
            seen.addAll(itemKeys)
            merged.add(item)
        }
    }
    return merged
}

/**
 * The channels of the signed-in account, and the one place their roster comes
 * from: the active [Profile] (its pin and `datasyncId`) plus `accounts/accounts_list`,
 * which [com.sentup.android.data.youtube.YouTubeAccount.fetchChannels] already
 * reads for the picker. That makes the profile the single source of truth - a
 * fetch never asks the user which channel it means.
 *
 * [merge] runs the same account-scoped read as every channel of that account,
 * one after another, and returns them merged. With one channel there is nothing
 * to merge and the fetch runs exactly as it did before - no element, no header,
 * byte-identical. That is what lets playlists, likes and the subscriptions feed
 * show the personal and the brand account together without either one having to
 * be selected.
 */
internal class AccountChannels(
    context: Context,
    private val sessionManager: SessionManager,
    private val fetchChannels: suspend () -> List<YouTubeChannel>,
) {
    private val appContext = context.applicationContext
    private val profileManager = ProfileManager(appContext)

    /** The channel views of the active account, personal included. Signed out: empty. */
    suspend fun views(): List<ChannelView> {
        if (!sessionManager.isLoggedIn()) return emptyList()
        val profile = profileManager.active()
        return channelViewIds(profile.resolvedPageId, brandIds(profile)).map { ChannelView(it) }
    }

    /**
     * [fetch] once per channel of the account, sequentially, then merged.
     * [keys] says what makes an item the same item on two channels; null keys
     * simply do not participate in dedupe.
     */
    suspend fun <T> merge(fetch: suspend () -> List<T>, keys: (T) -> List<String>): List<T> {
        val channelViews = views()
        if (channelViews.size <= 1) return fetch()
        val pages = ArrayList<List<T>>(channelViews.size)
        for (view in channelViews) {
            pages.add(withContext(view) { fetch() })
        }
        val merged = mergeChannelPages(pages, keys)
        // Counts only: which channels an account owns is identity information,
        // and the count is what a bug report needs.
        KLog.d(
            YOUTUBE_TAG,
            "Account fetch read ${channelViews.size} channel(s), merged to ${merged.size} item(s)",
        )
        return merged
    }

    /**
     * The profile's brand page ids: the cached `accounts_list` roster, refreshed
     * at most every [BRANDS_TTL_MS] and keyed to the account behind the profile,
     * so re-signing into another account on the same profile never reuses the
     * old one's list. When the fetch fails the last known roster is kept, plus
     * whatever `datasyncId` reports the session is already on - the app may run
     * offline, and that half costs no request.
     */
    private suspend fun brandIds(profile: Profile): List<String> {
        val cacheKey = "${profile.id}|${profile.datasyncId.orEmpty()}"
        val cached = brandsCache[cacheKey]
        val now = System.currentTimeMillis()
        val reported = listOfNotNull(brandChannelIdOf(profile.datasyncId))
        if (cached != null && now - cached.fetchedAtMs < BRANDS_TTL_MS) {
            return cached.ids + reported
        }
        val fetched = runCatching {
            fetchChannels().mapNotNull { it.pageId }.distinct()
        }.getOrNull() ?: return cached?.ids.orEmpty() + reported
        brandsCache[cacheKey] = ChannelRoster(fetched, now)
        return fetched + reported
    }

    private data class ChannelRoster(val ids: List<String>, val fetchedAtMs: Long)

    private companion object {
        /** How long one account's brand roster is reused before it is re-read. */
        private const val BRANDS_TTL_MS = 10L * 60 * 1000L

        private val brandsCache = ConcurrentHashMap<String, ChannelRoster>()
    }
}
