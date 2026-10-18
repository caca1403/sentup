package com.sentup.android.data.youtube

import java.util.Locale

/**
 * The `hl` every YouTube request carries. It was a fixed `"en"`, so a Turkish
 * reader got "29K views • 4 hours ago" inside a Turkish app - the API renders
 * view counts, dates and relative times in this language, and the client never
 * asked. Anything the app does not ship as a language stays on `en`.
 */
internal fun apiHl(): String =
    when (Locale.getDefault().language) {
        "tr" -> "tr"
        else -> "en"
    }
