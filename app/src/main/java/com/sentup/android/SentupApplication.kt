package com.sentup.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sentup.android.data.CrashReporter
import com.sentup.android.data.LocalVideoThumbnail
import com.sentup.android.data.LocalVideoThumbnailFetcher

class SentupApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Installed before anything else can crash, so the bug reporter has a
        // file to offer on the next launch. It wraps - never replaces - the
        // platform handler.
        CrashReporter.install(this)
        // Reconcile the persisted periodic job with the user's current opt-in.
        // The setting handler does the same immediately when the value changes.
        com.sentup.android.work.UploadCheckWorker.sync(this)
        com.sentup.android.work.ScheduledBackupWorker.sync(this)
        // Discord Rich Presence (no-ops until the user turns it on in Settings
        // AND the Discord app is installed and signed in).
        // Observes playback state only; it can never affect audio delivery,
        // and every failure stays contained so a missing Discord install
        // cannot reach startup.
        com.sentup.android.presence.DiscordPresenceManager.start(this)
    }

    /**
     * The one Coil loader for the whole process. AsyncImage and every direct
     * ImageRequest (artwork color extraction, notification artwork, downloads)
     * resolve to this instance through Context.imageLoader, so they share one
     * memory cache, one disk cache and one connection pool instead of each
     * call site building its own loader.
     *
     * The only component added is the device-video frame fetcher, which is
     * keyed on its own [LocalVideoThumbnail] model and so cannot affect any
     * other image: every existing request still resolves exactly as it did.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(LocalVideoThumbnailFetcher.Factory(this@SentupApplication))
                add(LocalVideoThumbnailFetcher.ThumbnailKeyer())
            }
            .build()
}
