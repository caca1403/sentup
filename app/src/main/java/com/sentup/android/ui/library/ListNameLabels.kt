package com.sentup.android.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sentup.android.R
import java.util.Locale

/**
 * The auto-generated names YouTube hands Sentüp for its own playlists.
 *
 * The library shows these titles exactly as YouTube returns them, so the user
 * sees English - sometimes machine-shaped English - next to the names they
 * wrote themselves. `Your Likes` and `Episodes for Later` are not playlists
 * anybody chose: they are furniture, and furniture should be named in the
 * language of the room it sits in.
 *
 * Deliberately small. Every pattern here matches one string YouTube itself
 * emits; anything else returns null and the caller keeps the raw name, because
 * the user's own names must never be rewritten.
 *
 * The `resolve` callback stays out of this file so the whole table (including
 * the Turkish-dotless-I fold below) compiles and tests without Android.
 */
enum class KnownListName {
    /** `Your Likes` - the likes list, id LM. */
    YourLikes,
    /** `Liked videos` - the likes list in video mode. */
    LikedVideos,
    /** `All Youtube Audio Library Songs [Order: ...]`. */
    AllAudioLibrarySongs,
    /** `Episodes for Later` and `Watch later` - the later list. */
    EpisodesForLater,
    /** `Presenting P!NK` - an artist spotlight page. The artist stays as-is. */
    Presenting,
    /** `Uploads from <artist>` - a channel's uploads shelf. */
    UploadsFrom,
    /** `Popular uploads` - YouTube's own uploads shelf. */
    PopularUploads,
    /** `Popular videos` - a browse shelf. */
    PopularVideos
}

internal data class KnownListNameMatch(
    val key: KnownListName,
    val argument: String? = null
)

/**
 * Lowercase with the locale out of the way, then unify the dotted/dotless I
 * pair so a Turkish user typing "ıslak" finds "Islak" and vice versa. Both
 * spellings fold to one letter because the app must match either one.
 */
internal fun foldForNameMatch(input: String): String = input
    .replace('ı', 'i')
    .replace('I', 'i')
    .replace('İ', 'i')
    .lowercase(Locale.ROOT)

/**
 * Trailing `[...]` on an auto-generated name is a sort order, not a title -
 * `All Youtube Audio Library Songs [Order: Artist]` - so it is stripped before
 * matching. Nothing else is touched.
 */
private val ORDER_SUFFIX = Regex("""\s*\[[^\]]*]\s*$""")

private val PRESENTING = Regex("""^presenting[:–—-]?\s+(.+)$""")
private val UPLOADS_FROM = Regex("""^uploads from (.+)$""")

/**
 * The [foldedGroup] captured from the folded title, mapped back to the slice
 * of [original] it came from. Folding changes length ("İ" and "ı" collapse),
 * so this matches on the folded *span* rather than on a string search: the
 * group index gives the offset, and the original is walked with the same fold
 * to find where that offset lands.
 */
private fun originalArgument(original: String, foldedGroup: String): String {
    // Everything up to the argument folds to a known-length prefix; walking
    // the original with the same replacements until its folded form reaches
    // the group start recovers the untouched tail.
    val foldedOriginal = foldForNameMatch(original)
    val start = foldedOriginal.indexOf(foldedGroup).takeIf { it >= 0 } ?: return foldedGroup
    var consumed = 0
    var index = 0
    while (index < original.length && consumed < start) {
        val foldedChar = foldForNameMatch(original[index].toString())
        consumed += foldedChar.length
        index++
    }
    return original.substring(index).trim()
}

internal fun matchKnownListName(raw: String): KnownListNameMatch? {
    if (raw.isBlank()) return null
    val base = ORDER_SUFFIX.replace(raw.trim(), "").trim()
    if (base.isEmpty()) return null
    val folded = foldForNameMatch(base)

    when (folded) {
        "your likes", "liked music" -> return KnownListNameMatch(KnownListName.YourLikes)
        "liked videos" -> return KnownListNameMatch(KnownListName.LikedVideos)
        "all youtube audio library songs", "all youtube audio library song" ->
            return KnownListNameMatch(KnownListName.AllAudioLibrarySongs)
        "episodes for later" -> return KnownListNameMatch(KnownListName.EpisodesForLater)
        "watch later" -> return KnownListNameMatch(KnownListName.EpisodesForLater)
        "popular uploads" -> return KnownListNameMatch(KnownListName.PopularUploads)
        "popular videos" -> return KnownListNameMatch(KnownListName.PopularVideos)
    }

    // Matched against the folded title to find the pattern, but the argument
    // is taken from the original so "Presenting P!NK" keeps its capital letters:
    // this string is shown to the user next to "Sunan:", so folding it would
    // print "Sunan: p!nk".
    PRESENTING.matchEntire(folded)?.let { match ->
        val artist = originalArgument(base, match.groupValues[1])
        if (artist.isNotEmpty()) {
            return KnownListNameMatch(KnownListName.Presenting, artist)
        }
    }

    UPLOADS_FROM.matchEntire(folded)?.let { match ->
        val name = originalArgument(base, match.groupValues[1])
        if (name.isNotEmpty()) {
            return KnownListNameMatch(KnownListName.UploadsFrom, name)
        }
    }
    return null
}

/**
 * [raw] as the user should read it. Only the patterns above are ever replaced;
 * anything else comes back untouched, which is what keeps a user's own playlist
 * named "My Mix" from being rewritten, and it is what makes the original name
 * still available to whoever needs the real title (rename, delete confirm).
 */
fun localizedListName(raw: String, resolve: (KnownListName, String?) -> String?): String {
    val match = matchKnownListName(raw) ?: return raw
    val localized = resolve(match.key, match.argument) ?: return raw
    return localized
}

/** [localizedListName] with the app's own strings as the resolver. */
@Composable
fun listNameLabel(raw: String): String {
    val match = matchKnownListName(raw) ?: return raw
    return when (match.key) {
        KnownListName.YourLikes -> stringResource(R.string.lk_your_likes)
        KnownListName.LikedVideos -> stringResource(R.string.lk_liked_videos)
        KnownListName.AllAudioLibrarySongs -> stringResource(R.string.lk_audio_library)
        KnownListName.EpisodesForLater -> stringResource(R.string.lk_episodes_later)
        KnownListName.PopularUploads -> stringResource(R.string.lk_popular_uploads)
        KnownListName.PopularVideos -> stringResource(R.string.lk_popular_videos)
        KnownListName.Presenting -> stringResource(R.string.lk_presenting, match.argument.orEmpty())
        KnownListName.UploadsFrom -> stringResource(R.string.lk_uploads_from, match.argument.orEmpty())
    }
}
