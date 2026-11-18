package com.sentup.android.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * VPN-resilient OkHttp-backed [HttpDataSource] replacing Media3's [androidx.media3.datasource.DefaultHttpDataSource]
 * (`java.net.HttpURLConnection`).
 *
 * Ensures audio, video, DASH, HLS, and offline cache streams use [VpnResilientNetwork]:
 * IPv4-first DNS + DoH fallback, proxy safety, NAT keep-alive pings, and instant socket eviction
 * whenever a VPN connects, switches server, or disconnects.
 */
@UnstableApi
private class VpnOkHttpDataSource(
    private val callFactory: Call.Factory,
) : BaseDataSource(/* isNetwork = */ true), HttpDataSource {

    private val requestProperties = HashMap<String, String>()
    private var currentDataSpec: DataSpec? = null
    private var response: Response? = null
    private var responseStream: InputStream? = null
    private var opened = false
    private var bytesToRead = 0L
    private var bytesRead = 0L
    private var lastResponseCode = -1
    private var lastResponseHeaders: Map<String, List<String>> = emptyMap()

    override fun setRequestProperty(name: String, value: String) {
        synchronized(requestProperties) {
            requestProperties[name] = value
        }
    }

    override fun clearRequestProperty(name: String) {
        synchronized(requestProperties) {
            requestProperties.remove(name)
        }
    }

    override fun clearAllRequestProperties() {
        synchronized(requestProperties) {
            requestProperties.clear()
        }
    }

    override fun getResponseCode(): Int = if (opened) lastResponseCode else -1

    override fun getResponseHeaders(): Map<String, List<String>> = lastResponseHeaders

    override fun getUri(): Uri? =
        response?.request?.url?.toString()?.let(Uri::parse) ?: currentDataSpec?.uri

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        bytesRead = 0L
        bytesToRead = 0L
        transferInitializing(dataSpec)

        val request = buildRequest(dataSpec)
        val executed: Response = try {
            (callFactory as? OkHttpClient)?.let { client ->
                VpnResilientNetwork.executeResilient(client, request)
            } ?: callFactory.newCall(request).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN,
            )
        }

        response = executed
        lastResponseCode = executed.code
        lastResponseHeaders = executed.headers.toMultimap()

        val body = executed.body
        responseStream = body?.byteStream()

        if (!executed.isSuccessful) {
            val errorBytes = runCatching { responseStream?.readBytes() ?: ByteArray(0) }
                .getOrDefault(ByteArray(0))
            closeQuietly()
            throw HttpDataSource.InvalidResponseCodeException(
                lastResponseCode,
                executed.message,
                null,
                lastResponseHeaders,
                dataSpec,
                errorBytes,
            )
        }

        // If server replied 200 instead of 206 despite Range header, skip to requested position.
        val bytesToSkip = if (lastResponseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0L
        val bodyLength = body?.contentLength()?.takeIf { it >= 0L } ?: C.LENGTH_UNSET.toLong()
        bytesToRead = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            bodyLength != C.LENGTH_UNSET.toLong() -> (bodyLength - bytesToSkip).coerceAtLeast(0L)
            else -> C.LENGTH_UNSET.toLong()
        }

        opened = true
        transferStarted(dataSpec)

        if (bytesToSkip > 0L) {
            try {
                skipInternal(bytesToSkip, dataSpec)
            } catch (e: IOException) {
                close()
                throw if (e is HttpDataSource.HttpDataSourceException) {
                    e
                } else {
                    HttpDataSource.HttpDataSourceException(
                        e,
                        dataSpec,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                        HttpDataSource.HttpDataSourceException.TYPE_OPEN,
                    )
                }
            }
        }

        return bytesToRead
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val stream = responseStream ?: return C.RESULT_END_OF_INPUT
        val readLen = if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val remaining = bytesToRead - bytesRead
            if (remaining <= 0L) return C.RESULT_END_OF_INPUT
            minOf(length.toLong(), remaining).toInt()
        } else {
            length
        }

        val count = try {
            stream.read(buffer, offset, readLen)
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                checkNotNull(currentDataSpec),
                HttpDataSource.HttpDataSourceException.TYPE_READ,
            )
        }
        if (count == -1) return C.RESULT_END_OF_INPUT
        bytesRead += count
        bytesTransferred(count)
        return count
    }

    override fun close() {
        currentDataSpec = null
        closeQuietly()
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun buildRequest(dataSpec: DataSpec): Request {
        val builder = Request.Builder().url(dataSpec.uri.toString())
        val headersSnapshot = synchronized(requestProperties) { HashMap(requestProperties) }
        headersSnapshot.forEach { (k, v) -> builder.header(k, v) }
        dataSpec.httpRequestHeaders.forEach { (k, v) -> builder.header(k, v) }

        val hasRangeHeader = headersSnapshot.keys.any { it.equals("Range", ignoreCase = true) } ||
            dataSpec.httpRequestHeaders.keys.any { it.equals("Range", ignoreCase = true) }
        if (!hasRangeHeader && (dataSpec.position != 0L || dataSpec.length != C.LENGTH_UNSET.toLong())) {
            val rangeValue = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                "bytes=${dataSpec.position}-"
            } else {
                "bytes=${dataSpec.position}-${dataSpec.position + dataSpec.length - 1}"
            }
            builder.header("Range", rangeValue)
        }

        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            builder.header("Accept-Encoding", "identity")
        }

        val method = dataSpec.httpMethodString
        val bodyBytes = dataSpec.httpBody
        val requestBody = when {
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET || dataSpec.httpMethod == DataSpec.HTTP_METHOD_HEAD -> null
            bodyBytes != null -> bodyBytes.toRequestBody(null)
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST -> ByteArray(0).toRequestBody("application/octet-stream".toMediaTypeOrNull())
            else -> null
        }
        builder.method(method, requestBody)
        return builder.build()
    }

    private fun skipInternal(bytesToSkip: Long, dataSpec: DataSpec) {
        if (bytesToSkip <= 0L) return
        val stream = responseStream ?: throw EOFException()
        val scratch = ByteArray(4096)
        var skipped = 0L
        while (skipped < bytesToSkip) {
            if (Thread.currentThread().isInterrupted) {
                throw HttpDataSource.HttpDataSourceException(
                    dataSpec,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN,
                )
            }
            val toRead = minOf(scratch.size.toLong(), bytesToSkip - skipped).toInt()
            val read = stream.read(scratch, 0, toRead)
            if (read == -1) throw EOFException()
            skipped += read
            bytesTransferred(read)
        }
    }

    private fun closeQuietly() {
        runCatching { responseStream?.close() }
        runCatching { response?.close() }
        responseStream = null
        response = null
    }
}

/**
 * HTTP data source for googlevideo media that downloads in bounded chunks over [VpnResilientNetwork].
 *
 * googlevideo paces open-ended requests (no Range end bound) to roughly the
 * media bitrate. Splitting progressive googlevideo URLs into [CHUNK_SIZE_BYTES] ranged requests
 * prevents throttling while keeping adaptive DASH/HLS manifests and segments unchunked.
 * Also sets the per-request User-Agent via [YouTubeRepository.uaForPlaybackUri].
 */
@UnstableApi
class ChunkedStreamDataSource private constructor(
    private val delegate: HttpDataSource,
    private val allowNetwork: () -> Boolean,
) : DataSource {

    class Factory(
        private val allowNetwork: () -> Boolean = { true },
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = ChunkedStreamDataSource(
            delegate = VpnOkHttpDataSource(streamingClient),
            allowNetwork = allowNetwork,
        )
    }

    companion object {
        private const val CHUNK_SIZE_BYTES = 10L * 1024 * 1024
        private const val UNSET = C.LENGTH_UNSET.toLong()
        private val CONTENT_RANGE_REGEX = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""")

        private val streamingClient: OkHttpClient by lazy {
            VpnResilientNetwork.sharedStreamingClient
        }

        private fun shouldChunk(uri: Uri): Boolean {
            if (uri.host?.endsWith(".googlevideo.com") != true) return false
            return !isUncacheablePlaybackUrl(uri.toString())
        }
    }

    private var currentSpec: DataSpec? = null
    private var chunked = false
    private var position = 0L
    private var requestedEnd = UNSET
    private var totalLength = UNSET
    private var chunkRemaining = 0L
    private var chunkProgressed = false

    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        if (!allowNetwork()) {
            throw IOException("Local only mode is on: network disabled")
        }
        delegate.setRequestProperty(
            "User-Agent",
            YouTubeRepository.uaForPlaybackUri(dataSpec.uri)
        )
        currentSpec = dataSpec
        position = dataSpec.position
        chunked = shouldChunk(dataSpec.uri)
        if (!chunked) {
            return delegate.open(dataSpec)
        }

        requestedEnd =
            if (dataSpec.length != UNSET) dataSpec.position + dataSpec.length else UNSET
        totalLength = UNSET
        openChunk()

        return when {
            dataSpec.length != UNSET ->
                if (totalLength != UNSET) {
                    minOf(dataSpec.length, totalLength - dataSpec.position)
                } else {
                    dataSpec.length
                }
            totalLength != UNSET -> totalLength - dataSpec.position
            else -> UNSET
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!chunked) return delegate.read(buffer, offset, length)
        if (length == 0) return 0

        if (chunkRemaining <= 0L) {
            val end = effectiveEnd()
            if (end != UNSET && position >= end) return C.RESULT_END_OF_INPUT
            delegate.close()
            openChunk()
            if (chunkRemaining <= 0L) return C.RESULT_END_OF_INPUT
        }

        val toRead = minOf(length.toLong(), chunkRemaining).toInt()
        val read = delegate.read(buffer, offset, toRead)
        if (read == C.RESULT_END_OF_INPUT) {
            if (!chunkProgressed) return C.RESULT_END_OF_INPUT
            chunkRemaining = 0
            return read(buffer, offset, length)
        }
        chunkProgressed = true
        position += read
        chunkRemaining -= read
        return read
    }

    override fun getUri(): Uri? = delegate.uri ?: currentSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

    override fun close() {
        currentSpec = null
        chunked = false
        chunkRemaining = 0
        delegate.close()
    }

    private fun effectiveEnd(): Long = when {
        requestedEnd != UNSET && totalLength != UNSET -> minOf(requestedEnd, totalLength)
        requestedEnd != UNSET -> requestedEnd
        else -> totalLength
    }

    private fun openChunk() {
        val spec = checkNotNull(currentSpec)
        val end = effectiveEnd()
        val maxLen = if (end != UNSET) end - position else Long.MAX_VALUE
        val len = minOf(CHUNK_SIZE_BYTES, maxLen)
        chunkProgressed = false
        if (len <= 0L) {
            chunkRemaining = 0
            return
        }

        val chunkSpec = spec.buildUpon().setPosition(position).setLength(len).build()
        try {
            delegate.open(chunkSpec)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            if (e.responseCode == 416) {
                chunkRemaining = 0
                return
            }
            throw e
        }

        val contentRange = delegate.responseHeaders["Content-Range"]?.firstOrNull()
        val match = contentRange?.let { CONTENT_RANGE_REGEX.find(it) }
        if (match != null) {
            val (_, rangeEnd, total) = match.destructured
            chunkRemaining = rangeEnd.toLong() - position + 1
            total.toLongOrNull()?.let { totalLength = it }
        } else {
            chunkRemaining = len
        }
    }
}
