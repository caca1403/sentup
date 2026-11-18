package com.sentup.android.data

import android.net.NetworkCapabilities
import android.net.Uri
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject

/**
 * Network helpers that keep Sentüp working reliably when a VPN is active.
 *
 * On Android, VPN tunnels (WireGuard, OpenVPN, WARP, ByeDPI, ProtonVPN, split-tunnel profiles) often:
 * 1. Omit [NetworkCapabilities.NET_CAPABILITY_VALIDATED] while still carrying live
 *    [NetworkCapabilities.NET_CAPABILITY_INTERNET] traffic, causing strict checks to fail.
 * 2. Route only IPv4 cleanly while leaving IPv6 DNS results unreachable, causing dual-stack calls to stall.
 * 3. Fail or block UDP/53 system DNS or leave zombie sockets across tunnel toggles/NAT expiries.
 * 4. Wrap unmetered Wi-Fi behind `TRANSPORT_VPN` without `NET_CAPABILITY_NOT_METERED`.
 */
object VpnResilientNetwork {

    private const val DNS_CACHE_TTL_MS = 60_000L
    private const val SYSTEM_DNS_TIMEOUT_MS = 1_500L
    private const val SHARED_POOL_KEEP_ALIVE_SEC = 45L
    private const val FORCE_DOH_TTL_MS = 120_000L

    private data class CachedDnsEntry(val addresses: List<InetAddress>, val resolvedAtMs: Long)

    private val epoch = AtomicLong(1L)
    private val dnsCache = ConcurrentHashMap<String, CachedDnsEntry>()
    private val forceDohHosts = ConcurrentHashMap<String, Long>()
    private val dnsExecutor = Executors.newCachedThreadPool { r ->
        Thread(r, "sentup-vpn-dns").apply { isDaemon = true }
    }

    private val globalConnectionPool = ConnectionPool(12, SHARED_POOL_KEEP_ALIVE_SEC, TimeUnit.SECONDS)
    private val fallbackConnectionPool = ConnectionPool(6, 25L, TimeUnit.SECONDS)
    private val http1FallbackClients = Collections.synchronizedMap(
        java.util.IdentityHashMap<OkHttpClient, OkHttpClient>()
    )
    private val http1NoProxyClients = Collections.synchronizedMap(
        java.util.IdentityHashMap<OkHttpClient, OkHttpClient>()
    )
    private val registeredPools: MutableSet<ConnectionPool> =
        Collections.newSetFromMap(ConcurrentHashMap<ConnectionPool, Boolean>())

    init {
        registeredPools += globalConnectionPool
        registeredPools += fallbackConnectionPool
    }

    private val safeProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): MutableList<Proxy> {
            if (uri == null) return mutableListOf(Proxy.NO_PROXY)
            val resolved = runCatching { getDefault()?.select(uri) }.getOrNull()
            return if (resolved.isNullOrEmpty()) mutableListOf(Proxy.NO_PROXY) else resolved.toMutableList()
        }

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            runCatching { getDefault()?.connectFailed(uri, sa, ioe) }
        }
    }

    // Separate numeric-IP clients for DoH fallback (1.1.1.1 / 8.8.8.8 never recurse through vpnSafeDns).
    private val dohSystemProxyClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(safeProxySelector)
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .callTimeout(3000, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val dohDirectClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .callTimeout(3000, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Current connectivity epoch, incremented whenever the active network or VPN changes. */
    fun currentEpoch(): Long = epoch.get()

    /** Register a custom [ConnectionPool] so VPN toggles evict all sockets across every surface. */
    fun registerConnectionPool(pool: ConnectionPool) {
        registeredPools += pool
    }

    /**
     * Evicts all pooled HTTP sockets, clears the resolved DNS cache, and bumps [currentEpoch]
     * so cached pre-VPN stream URLs or half-open tunnel connections are never reused.
     */
    fun evictAllConnections() {
        epoch.incrementAndGet()
        dnsCache.clear()
        forceDohHosts.clear()
        registeredPools.forEach { pool -> runCatching { pool.evictAll() } }
        runCatching { dohSystemProxyClient.connectionPool.evictAll() }
        runCatching { dohDirectClient.connectionPool.evictAll() }
    }

    /**
     * Drops cached DNS entries for [hostname] and optionally forces DoH IPv4
     * resolution for a brief window (bypassing poisoned or stalled VPN/ISP DNS).
     */
    fun invalidateHostDns(hostname: String, preferDoh: Boolean = true) {
        if (hostname.isBlank()) return
        dnsCache.remove(hostname)
        if (preferDoh) {
            forceDohHosts[hostname] = System.currentTimeMillis() + FORCE_DOH_TTL_MS
        }
    }

    /**
     * Returns `true` when [capabilities] represents an internet-capable network,
     * treating active VPN transports ([NetworkCapabilities.TRANSPORT_VPN]) as usable
     * even when Android's captive-portal validator has not set
     * [NetworkCapabilities.NET_CAPABILITY_VALIDATED].
     */
    fun isNetworkUsable(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
    }

    /**
     * Returns `true` when [capabilities] is unmetered (or Wi-Fi wrapped inside a VPN).
     */
    fun isUnmeteredOrWifiVpn(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return true
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Orders [Inet4Address] entries ahead of IPv6 entries while preserving relative
     * order within each address family.
     */
    fun preferIpv4Addresses(addresses: List<InetAddress>): List<InetAddress> {
        if (addresses.size <= 1) return addresses
        val (ipv4, other) = addresses.partition { it is Inet4Address }
        return if (ipv4.isEmpty()) addresses else ipv4 + other
    }

    private fun isNumericIpLiteral(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty()) return false
        if (h.contains(':')) return true
        return h.all { it in '0'..'9' || it == '.' }
    }

    private fun systemLookupTimed(hostname: String): List<InetAddress>? {
        val future = dnsExecutor.submit<List<InetAddress>> {
            Dns.SYSTEM.lookup(hostname)
        }
        return try {
            future.get(SYSTEM_DNS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun isUsablePublicAddress(addr: InetAddress): Boolean =
        !addr.isLoopbackAddress && !addr.isAnyLocalAddress && !addr.isLinkLocalAddress && !addr.isMulticastAddress

    private fun fetchDohIpv4(hostname: String): List<InetAddress> {
        val encoded = Uri.encode(hostname)
        val endpoints = listOf(
            "https://1.1.1.1/dns-query?name=$encoded&type=A",
            "https://8.8.8.8/resolve?name=$encoded&type=A",
            "https://1.0.0.1/dns-query?name=$encoded&type=A",
            "https://8.8.4.4/resolve?name=$encoded&type=A",
        )
        for (client in listOf(dohSystemProxyClient, dohDirectClient)) {
            for (url in endpoints) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("Accept", "application/dns-json")
                        .get()
                        .build()
                    val parsed = client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use emptyList()
                        val body = resp.body?.string().orEmpty()
                        val answers = JSONObject(body).optJSONArray("Answer") ?: return@use emptyList()
                        buildList {
                            for (i in 0 until answers.length()) {
                                val entry = answers.optJSONObject(i) ?: continue
                                // DNS type 1 = IPv4 A record
                                if (entry.optInt("type") == 1) {
                                    val ip = entry.optString("data").trim()
                                    if (ip.isNotEmpty() && isNumericIpLiteral(ip)) {
                                        runCatching { InetAddress.getByName(ip) }.getOrNull()
                                            ?.takeIf(::isUsablePublicAddress)
                                            ?.let { add(it) }
                                    }
                                }
                            }
                        }
                    }
                    if (parsed.isNotEmpty()) return parsed
                } catch (_: Exception) {
                    // Try next DoH resolver or transport path.
                }
            }
        }
        return emptyList()
    }

    /**
     * [Dns] resolver that enforces bounded system lookup timeouts, sorts IPv4 ahead of IPv6,
     * caches lookups briefly, and automatically falls back to direct-IP DNS-over-HTTPS
     * (`1.1.1.1` / `8.8.8.8` / `1.0.0.1` / `8.8.4.4`) when a VPN tunnel drops UDP DNS,
     * returns poisoned addresses, or exposes only IPv6.
     */
    val ipv4PreferredDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (isNumericIpLiteral(hostname)) return Dns.SYSTEM.lookup(hostname)

            val now = System.currentTimeMillis()
            val forceDoh = (forceDohHosts[hostname] ?: 0L) > now
            if (!forceDoh) {
                dnsCache[hostname]?.let { cached ->
                    if (now - cached.resolvedAtMs in 0 until DNS_CACHE_TTL_MS && cached.addresses.isNotEmpty()) {
                        return cached.addresses
                    }
                }
            }

            if (forceDoh) {
                val dohFirst = fetchDohIpv4(hostname)
                if (dohFirst.isNotEmpty()) {
                    dnsCache[hostname] = CachedDnsEntry(dohFirst, now)
                    return dohFirst
                }
            }

            val sysAddresses = systemLookupTimed(hostname).orEmpty().filter(::isUsablePublicAddress)
            val hasIpv4 = sysAddresses.any { it is Inet4Address }
            val resolved = if (hasIpv4) {
                preferIpv4Addresses(sysAddresses)
            } else {
                val dohV4 = fetchDohIpv4(hostname)
                if (dohV4.isNotEmpty()) {
                    (dohV4 + sysAddresses).distinctBy { it.hostAddress }
                } else {
                    sysAddresses
                }
            }

            if (resolved.isEmpty()) {
                throw UnknownHostException("Unable to resolve host $hostname over system or DoH DNS")
            }
            dnsCache[hostname] = CachedDnsEntry(resolved, now)
            return resolved
        }
    }

    /**
     * Applies VPN-friendly DNS (IPv4-first + DoH fallback), fault-tolerant system proxy resolution,
     * shared evictable connection pooling, HTTP/2 + HTTP/1.1 negotiation,
     * and automatic connection-failure retries to [builder].
     */
    fun configureForVpn(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder
        .dns(ipv4PreferredDns)
        .proxySelector(safeProxySelector)
        .connectionPool(globalConnectionPool)
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)

    private fun http1FallbackClient(baseClient: OkHttpClient): OkHttpClient =
        synchronized(http1FallbackClients) {
            http1FallbackClients.getOrPut(baseClient) {
                baseClient.newBuilder()
                    .dns(ipv4PreferredDns)
                    .proxySelector(safeProxySelector)
                    .connectionPool(fallbackConnectionPool)
                    .protocols(listOf(Protocol.HTTP_1_1))
                    .retryOnConnectionFailure(true)
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            }
        }

    private fun http1NoProxyClient(baseClient: OkHttpClient): OkHttpClient =
        synchronized(http1NoProxyClients) {
            http1NoProxyClients.getOrPut(baseClient) {
                baseClient.newBuilder()
                    .dns(ipv4PreferredDns)
                    .proxy(Proxy.NO_PROXY)
                    .connectionPool(fallbackConnectionPool)
                    .protocols(listOf(Protocol.HTTP_1_1))
                    .retryOnConnectionFailure(true)
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            }
        }

    /**
     * Executes [request] with automatic VPN recovery: if the pooled HTTP/2 or DNS path throws an
     * [IOException] (socket reset, stall, handshake glitch, poisoned DNS), evicts stale pools,
     * invalidates host DNS toward DoH, and retries over HTTP/1.1 (then direct [Proxy.NO_PROXY] if
     * a system proxy blocked the path).
     */
    @Throws(IOException::class)
    fun executeResilient(baseClient: OkHttpClient, request: Request): okhttp3.Response {
        try {
            return baseClient.newCall(request).execute()
        } catch (firstError: IOException) {
            if (firstError.message?.contains("Local only mode", ignoreCase = true) == true) {
                throw firstError
            }
            if (request.body?.isOneShot() == true) {
                throw firstError
            }
            invalidateHostDns(request.url.host, preferDoh = true)
            runCatching { baseClient.connectionPool.evictAll() }
            runCatching { fallbackConnectionPool.evictAll() }
            try {
                return http1FallbackClient(baseClient).newCall(request).execute()
            } catch (secondError: IOException) {
                val hasCustomProxy = runCatching {
                    val proxies = safeProxySelector.select(request.url.toUri())
                    proxies.any { it.type() != Proxy.Type.DIRECT }
                }.getOrDefault(false)
                if (!hasCustomProxy) throw secondError
                return http1NoProxyClient(baseClient).newCall(request).execute()
            }
        }
    }

    /** Shared VPN-hardened OkHttp client for ExoPlayer/Media3 media chunk, DASH, and HLS transfers. */
    val sharedStreamingClient: OkHttpClient by lazy {
        configureForVpn(OkHttpClient.Builder())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** Shared VPN-hardened OkHttp client for Coil artwork/avatar/thumbnail loading. */
    val sharedImageClient: OkHttpClient by lazy {
        configureForVpn(OkHttpClient.Builder())
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
