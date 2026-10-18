package com.sentup.android.data.youtube

import com.sentup.android.data.SessionManager
import com.sentup.android.data.YouTubeAuthUtils
import com.sentup.android.data.YouTubeRateLimit
import com.sentup.android.data.YouTubeSession
import com.sentup.android.util.KLog
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * InnerTube on music.youtube.com as the WEB_REMIX client.
 *
 * Four families of call, differing in what they need from the session:
 * [postMusicMetadata] and [browseMusic] read public pages and attach the
 * account only when there is one, [fetchInternalApi] and [fetchContinuation]
 * read the account's own library, and [postMusicApi] is an account write that
 * must be signed. The SAPISIDHASH is per origin, so a session signed for
 * www.youtube.com is refused here.
 *
 * **Every one of them goes out through [call].** They used to build their own
 * requests and had drifted apart: `fetchContinuation` identified no client at
 * all, `postMusicApi` sent neither a client nor a visitor id, and only
 * `fetchInternalApi` sent the client name. Nothing on this origin *requires*
 * those headers, which is exactly why they could be dropped without anything
 * breaking loudly - the API keeps answering while a request looks less like the
 * client it claims to be, and the degradation shows up weeks later as an empty
 * page. One builder is the only thing that makes "cannot forget to identify
 * itself" true.
 */
internal class MusicApi(
    private val http: YouTubeHttp,
    private val sessionManager: SessionManager,
) {
    /**
     * The client block callers that build their own bodies ask for.
     *
     * Deliberately without [com.sentup.android.data.youtube.YouTubeHttp.visitorIdentity]'s
     * visitor id: those callers are mostly writes, and the id is added by [call]
     * as the header it travels as everywhere else.
     */
    fun musicContext(): JSONObject =
        JSONObject().put(
            "client",
            JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", WEB_REMIX_VERSION)
                .put("hl", apiHl())
                .put("gl", http.contentRegion())
        )

    /** Read a page. Null when the request could not be made or came back empty. */
    fun postMusicMetadata(endpoint: String, payload: JSONObject): JSONObject? {
        val body = call(
            endpoint = endpoint,
            body = payload,
            session = sessionManager.captureSession(),
            retryTransient = true,
        ) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()?.takeUnless { it.has("error") }
    }

    /**
     * POST to InnerTube /browse with the WEB_REMIX client. Works anonymously;
     * cookies are attached when logged in so results are personalized.
     * Unlike [fetchInternalApi], this does NOT require a login.
     */
    fun browseMusic(browseId: String, params: String? = null): String? =
        call(
            endpoint = "browse",
            body = JSONObject().put("browseId", browseId).apply {
                if (params != null) put("params", params)
            },
            session = sessionManager.captureSession(),
            retryTransient = true,
        )

    /**
     * The account's own library by browse id (`FEmusic_liked_videos`,
     * `FEmusic_home`, …), or any other InnerTube endpoint given by path.
     *
     * An [endpoint] with no `/` in it is a browse id and is posted to /browse
     * as one; anything else is taken as the path itself. Empty string on
     * failure, which is what every caller here already handles as "nothing".
     */
    fun fetchInternalApi(endpoint: String): String {
        val browseId = endpoint.takeIf { !it.contains('/') }
        val body = if (browseId != null) JSONObject().put("browseId", browseId) else JSONObject()
        return call(
            endpoint = browseId?.let { "browse" } ?: endpoint,
            body = body,
            session = sessionManager.captureSession(),
            retryTransient = true,
        ).orEmpty()
    }

    /**
     * Fetch a continuation page using its continuation token.
     *
     * The account headers are conditional, the call is not. [Verified August
     * 2026: a public playlist's continuation chain walks to the end with no
     * cookies, no auth header and no visitorData.] Returning "" without a
     * session made every signed-out continuation look like a failed fetch,
     * which capped public playlists at their first page just as the parser
     * gap did for signed-in ones - the same symptom from a second cause.
     */
    fun fetchContinuation(continuationToken: String): String =
        call(
            endpoint = "browse",
            body = JSONObject().put("continuation", continuationToken),
            session = sessionManager.captureSession(),
            retryTransient = true,
        ).orEmpty()

    /**
     * POST to an InnerTube endpoint on music.youtube.com with cookies and a
     * music-origin SAPISIDHASH (the hash is per-origin — a www.youtube.com
     * hash is rejected here). Returns the raw body or null on failure.
     *
     * **Never repeated.** These are account writes - creating a playlist,
     * rating a song, editing a track - and a repeat is a second write for one
     * intent. The reads above get the retry; this one gets the truth about
     * what happened instead.
     */
    fun postMusicApi(endpoint: String, body: JSONObject): String? {
        val session = sessionManager.captureSession() ?: return null
        // Signing is not optional here: these are account writes, and an
        // unsigned one answers 200 having done nothing.
        if (YouTubeAuthUtils.getSapisid(session.cookies) == null) return null
        return call(
            endpoint = endpoint,
            body = body,
            session = session,
            retryTransient = false,
        )
    }

    /**
     * The one way a music request leaves this class.
     *
     * @param retryTransient whether a transient answer may be repeated once.
     *   Reads may: a 503 or a brief 429 is YouTube having a moment, and today
     *   it becomes an empty library page or a playlist that stops at its first
     *   page - the app looks broken rather than throttled. A long `Retry-After`
     *   is *not* repeated: [YouTubeRateLimit] already holds fan-out for that
     *   window, and burning the user's wait on one more request helps nobody.
     *
     * @return the response body, or null when the request failed or came back
     *   without one.
     */
    private fun call(
        endpoint: String,
        body: JSONObject,
        session: YouTubeSession?,
        retryTransient: Boolean,
    ): String? {
        val visitor = http.visitorIdentity.cachedOrNull()
        // Copied rather than mutated: several callers keep their payload around
        // and reuse it for a second attempt of their own, and a context stamped
        // into it once would ride along into that second body.
        val payload = JSONObject(body.toString())
        payload.optJSONObject("context")?.let { existing ->
            http.visitorIdentity.cachedOrNull()?.let { visitorId ->
                existing.optJSONObject("client")?.put("visitorData", visitorId)
            }
        } ?: payload.put("context", musicContext())

        var attempt = 0
        while (true) {
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", BROWSER_USER_AGENT)
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Origin", "https://music.youtube.com")
                // Client name 67 is WEB_REMIX, sent by every call on this origin:
                // a request that never identifies itself is the shape anti-abuse
                // looks for, and a partially-identified one is worse still.
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", WEB_REMIX_VERSION)
                .apply { visitor?.let { addHeader("X-Goog-Visitor-Id", it) } }
                .authenticate(session)

            try {
                http.okHttpClient.newCall(request.build()).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        if (text.isNotBlank()) {
                            http.noteSessionState(text, session)
                            return text
                        }
                        KLog.w(YOUTUBE_TAG, "Music $endpoint answered ${response.code} with no body")
                        return null
                    }

                    YouTubeRateLimit.note(response.code, "music $endpoint", response.header("Retry-After"))
                    val waitMs = if (retryTransient) {
                        transientRetryMs(response.code, response.header("Retry-After"), attempt)
                    } else {
                        null
                    }
                    KLog.w(YOUTUBE_TAG, "Music $endpoint HTTP ${response.code}: ${text.take(200)}")
                    if (waitMs == null) return null
                    attempt++
                    // Blocking is correct here: every caller of this class is
                    // already on Dispatchers.IO around a synchronous execute().
                    Thread.sleep(waitMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                KLog.e(YOUTUBE_TAG, "Music $endpoint request failed", e)
                return null
            }
        }
    }
}