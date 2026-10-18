package com.sentup.android.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The languages Sentüp actually speaks, and how the app is told to speak one.
 *
 * **Two, and both complete.** The APK used to ship translations for twenty-six
 * languages, all but one of them covering about a sixth of the strings - so the
 * picker offered Deutsch and Español, a tap switched the app's locale, and 85%
 * of the screen stayed English. A language button that lies is worse than no
 * button: it looks like the app is broken rather than untranslated. Those
 * partials are gone (the pristine copies are still in the upstream reference
 * tree), and `xml/locales_config.xml` lists exactly what is here, so Android's
 * own per-app language settings cannot promise more than the app keeps.
 *
 * Adding a language is a translation job to completion, then one line in
 * [options] and one in `locales_config.xml` - never the other way round.
 *
 * Each language is written in itself ("Türkçe", not "Turkish"), because the
 * person reading that row is looking for their own language, not for a word
 * describing it.
 */
data class AppLanguageOption(val tag: String, val label: String)

object AppLanguage {

    val options: List<AppLanguageOption> = listOf(
        AppLanguageOption("tr", "Türkçe"),
        AppLanguageOption("en", "English"),
    )

    const val DEFAULT_TAG = "tr"

    private const val PREFS_NAME = "sentup_language"
    private const val KEY_TAG = "language_tag"

    /**
     * The tag the app is speaking now, or the device's own language when nobody
     * has chosen.
     *
     * Read from the platform on 13+ because the *system* owns that setting
     * there: a person who switched Sentüp to Turkish from Android's language
     * settings must not find the app still claiming English.
     */
    fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java)
            manager.applicationLocales[0]?.toLanguageTag()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        } else {
            prefs(context).getString(KEY_TAG, null)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return systemTag(context)
    }

    /** Whether [tag] is one of the languages offered. */
    fun isSupported(tag: String?): Boolean =
        tag != null && options.any { it.tag.equals(tag.take(2), ignoreCase = true) }

    /**
     * Speak [tag], and make it stick.
     *
     * On 13+ the platform does the work and recreates the app for us. Below
     * that there is no per-app locale at all, so the choice is written here and
     * pushed onto the resources directly - `updateConfiguration` is deprecated,
     * and it is deprecated precisely because this is what it is for: there is
     * no replacement on those releases.
     */
    fun apply(context: Context, tag: String) {
        if (tag.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList(Locale.forLanguageTag(tag))
            return
        }
        prefs(context).edit().putString(KEY_TAG, tag).apply()
        applyToResources(context, tag)
        (context as? Activity)?.recreate()
    }

    /**
     * What the app should speak before anyone has chosen: the device's language
     * when it is one of ours, English otherwise. Guessing from the device is
     * what makes a Turkish phone open in Turkish; falling back to English rather
     * than to nothing is what makes an unsupported phone readable.
     */
    fun preferredTag(context: Context): String {
        val tag = systemTag(context)
        return if (isSupported(tag)) tag else DEFAULT_TAG
    }

    private fun systemTag(context: Context): String {
        val locales = context.resources.configuration.locales
        val first = if (locales.isEmpty) null else locales[0]
        return first?.toLanguageTag().orEmpty().ifBlank { DEFAULT_TAG }
    }

    private fun applyToResources(context: Context, tag: String) {
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}