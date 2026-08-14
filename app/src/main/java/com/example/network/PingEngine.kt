package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

object PingEngine {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Executes real-time ping sessions based on config and emits packets sequentially.
     */
    fun startPing(config: PingConfig): Flow<PingPacket> = flow {
        var seq = 1
        val maxPackets = if (config.count <= 0) Int.MAX_VALUE else config.count

        while (currentCoroutineContext().isActive && seq <= maxPackets) {
            val packet = when (config.protocol) {
                PingProtocol.ICMP -> executeIcmpSingle(config.host, seq, config.packetSize, config.timeoutMs, config.ttl)
                PingProtocol.TCP -> executeTcpSingle(config.host, config.port, seq, config.timeoutMs)
                PingProtocol.HTTP -> executeHttpSingle(config.host, config.port, seq, config.timeoutMs)
            }
            emit(packet)

            if (seq < maxPackets) {
                delay(config.intervalMs)
            }
            seq++
        }
    }.flowOn(Dispatchers.IO)

    private fun executeIcmpSingle(host: String, seq: Int, packetSize: Int, timeoutMs: Int, ttl: Int): PingPacket {
        val startTime = System.currentTimeMillis()
        try {
            val countParam = "-c 1"
            val timeoutParam = "-W ${maxOf(1, timeoutMs / 1000)}"
            val sizeParam = "-s $packetSize"
            val ttlParam = "-t $ttl"
            val cmd = "/system/bin/ping $countParam $timeoutParam $sizeParam $ttlParam $host"
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            var latencyMs = -1f
            var bytes = packetSize + 8 // ICMP header
            var resTtl = ttl
            var success = false
            var message = ""

            while (reader.readLine().also { line = it } != null) {
                val l = line ?: ""
                message = l
                // Examples: "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms" or "time=14.2ms"
                if (l.contains("time=")) {
                    val timeSub = l.substringAfter("time=").substringBefore("ms").trim()
                    latencyMs = timeSub.toFloatOrNull() ?: -1f
                    if (latencyMs >= 0) {
                        success = true
                        if (l.contains("bytes from")) {
                            val bStr = l.substringBefore("bytes from").trim()
                            bytes = bStr.toIntOrNull() ?: bytes
                        }
                        if (l.contains("ttl=")) {
                            val ttlStr = l.substringAfter("ttl=").substringBefore(" ").trim()
                            resTtl = ttlStr.toIntOrNull() ?: resTtl
                        }
                    }
                }
            }
            process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)

            if (success && latencyMs >= 0) {
                return PingPacket(
                    seq = seq,
                    timeMs = latencyMs,
                    ttl = resTtl,
                    bytes = bytes,
                    status = PacketStatus.SUCCESS,
                    message = "Reply from $host: bytes=$bytes time=${"%.2f".format(latencyMs)}ms TTL=$resTtl"
                )
            }

            // If system binary failed or blocked, fallback to InetAddress.isReachable / TCP handshake check
            val startDirect = System.currentTimeMillis()
            val inet = InetAddress.getByName(host)
            val isReachable = inet.isReachable(timeoutMs)
            val directDuration = (System.currentTimeMillis() - startDirect).toFloat()

            if (isReachable) {
                return PingPacket(
                    seq = seq,
                    timeMs = maxOf(1f, directDuration),
                    ttl = ttl,
                    bytes = packetSize,
                    status = PacketStatus.SUCCESS,
                    message = "Host $host alive: latency ~${directDuration.toInt()}ms"
                )
            }

            return PingPacket(
                seq = seq,
                timeMs = 0f,
                status = PacketStatus.TIMEOUT,
                message = "Request timed out for icmp_seq $seq"
            )
        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime).toFloat()
            return PingPacket(
                seq = seq,
                timeMs = 0f,
                status = PacketStatus.ERROR,
                message = "Error: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun executeTcpSingle(host: String, port: Int, seq: Int, timeoutMs: Int): PingPacket {
        val targetPort = if (port <= 0) 80 else port
        val startTime = System.nanoTime()
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, targetPort), timeoutMs)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000f
            socket.close()
            PingPacket(
                seq = seq,
                timeMs = elapsedMs,
                bytes = 64,
                ttl = 64,
                status = PacketStatus.SUCCESS,
                message = "Connected to $host:$targetPort in ${"%.2f".format(elapsedMs)}ms"
            )
        } catch (e: Exception) {
            PingPacket(
                seq = seq,
                timeMs = 0f,
                status = PacketStatus.TIMEOUT,
                message = "TCP Connect to $host:$targetPort failed: ${e.message ?: "Timeout"}"
            )
        }
    }

    private fun executeHttpSingle(host: String, port: Int, seq: Int, timeoutMs: Int): PingPacket {
        val targetUrl = if (host.startsWith("http://") || host.startsWith("https://")) {
            host
        } else {
            if (port == 80) "http://$host" else "https://$host"
        }

        val startTime = System.nanoTime()
        return try {
            val request = Request.Builder()
                .url(targetUrl)
                .head()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val elapsedMs = (System.nanoTime() - startTime) / 1_000_000f
                PingPacket(
                    seq = seq,
                    timeMs = elapsedMs,
                    bytes = response.body?.contentLength()?.toInt() ?: 0,
                    status = PacketStatus.SUCCESS,
                    message = "HTTP ${response.code} from $targetUrl in ${"%.2f".format(elapsedMs)}ms (${response.protocol})"
                )
            }
        } catch (e: Exception) {
            PingPacket(
                seq = seq,
                timeMs = 0f,
                status = PacketStatus.ERROR,
                message = "HTTP ping failed: ${e.message ?: "Timeout"}"
            )
        }
    }

    /**
     * Resolves host IP address.
     */
    suspend fun resolveHostIp(host: String): String = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.removePrefix("https://").removePrefix("http://").substringBefore(":")
            val inet = InetAddress.getByName(cleanHost)
            inet.hostAddress ?: host
        } catch (e: Exception) {
            host
        }
    }

    /**
     * Scans standard network ports on a host.
     */
    suspend fun scanPorts(
        host: String,
        portsToScan: List<Pair<Int, String>>,
        timeoutMs: Int = 1200,
        onProgress: (PortScanResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        portsToScan.forEach { (port, service) ->
            val start = System.nanoTime()
            var isOpen = false
            var latency = 0f
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    isOpen = true
                    latency = (System.nanoTime() - start) / 1_000_000f
                }
            } catch (_: Exception) {
                isOpen = false
            }
            onProgress(PortScanResult(port, service, isOpen, latency))
        }
    }

    /**
     * Subnet LAN scanner to discover alive devices.
     */
    suspend fun scanSubnet(
        baseIpPrefix: String, // e.g. "192.168.1."
        currentDeviceIp: String,
        gatewayIp: String,
        onDeviceFound: (LanDevice) -> Unit
    ) = withContext(Dispatchers.IO) {
        for (i in 1..254) {
            val targetIp = "$baseIpPrefix$i"
            val start = System.currentTimeMillis()
            try {
                val inet = InetAddress.getByName(targetIp)
                val isReachable = inet.isReachable(350)
                val time = (System.currentTimeMillis() - start).toFloat()
                if (isReachable || targetIp == gatewayIp || targetIp == currentDeviceIp) {
                    val hostname = try { inet.canonicalHostName } catch (_: Exception) { targetIp }
                    onDeviceFound(
                        LanDevice(
                            ip = targetIp,
                            hostname = if (hostname != targetIp) hostname else "",
                            latencyMs = maxOf(1f, time),
                            isAlive = true,
                            isGateway = targetIp == gatewayIp,
                            isCurrentDevice = targetIp == currentDeviceIp
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Traceroute execution.
     */
    suspend fun runTraceroute(
        host: String,
        maxHops: Int = 20,
        timeoutMs: Int = 2000,
        onHopResult: (TracerouteHop) -> Unit
    ) = withContext(Dispatchers.IO) {
        for (ttl in 1..maxHops) {
            val start = System.currentTimeMillis()
            var hopIp = "*"
            var hopHost = ""
            var status = PacketStatus.TIMEOUT
            var latency = 0f

            try {
                val cmd = "/system/bin/ping -c 1 -t $ttl -W 2 $host"
                val process = Runtime.getRuntime().exec(cmd)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                var reachedDestination = false

                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: ""
                    if (l.contains("From") || l.contains("from")) {
                        // "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                        val parts = l.split(" ")
                        val fromIdx = parts.indexOfFirst { it.equals("from", ignoreCase = true) }
                        if (fromIdx != -1 && fromIdx + 1 < parts.size) {
                            hopIp = parts[fromIdx + 1].removeSuffix(":").removePrefix("(")
                                .removeSuffix(")")
                        }
                        status = PacketStatus.SUCCESS
                        latency = (System.currentTimeMillis() - start).toFloat()
                    }
                    if (l.contains("time=")) {
                        status = PacketStatus.SUCCESS
                        val timeStr = l.substringAfter("time=").substringBefore("ms").trim()
                        latency = timeStr.toFloatOrNull() ?: (System.currentTimeMillis() - start).toFloat()
                        reachedDestination = true
                    }
                }
                process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)

                if (hopIp != "*") {
                    try {
                        val inet = InetAddress.getByName(hopIp)
                        hopHost = inet.hostName
                    } catch (_: Exception) {}
                }

                val hop = TracerouteHop(
                    hopNumber = ttl,
                    ip = hopIp,
                    hostname = if (hopHost != hopIp) hopHost else "",
                    latencyMs = latency,
                    status = status
                )
                onHopResult(hop)

                if (reachedDestination || hopIp == host) {
                    break
                }
            } catch (e: Exception) {
                onHopResult(TracerouteHop(hopNumber = ttl, ip = "*", status = PacketStatus.TIMEOUT))
            }
        }
    }

    /**
     * DNS lookup for various record types.
     */
    suspend fun lookupDnsRecords(host: String): DnsLookupResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val cleanHost = host.removePrefix("https://").removePrefix("http://").substringBefore("/")
        val records = mutableListOf<DnsRecordItem>()
        try {
            val addresses = InetAddress.getAllByName(cleanHost)
            addresses.forEach { addr ->
                if (addr is java.net.Inet4Address) {
                    records.add(DnsRecordItem("A (IPv4)", addr.hostAddress ?: ""))
                } else if (addr is java.net.Inet6Address) {
                    records.add(DnsRecordItem("AAAA (IPv6)", addr.hostAddress ?: ""))
                }
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000f
            DnsLookupResult(
                host = cleanHost,
                queryTimeMs = elapsedMs,
                records = records,
                errorMessage = null
            )
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - start) / 1_000_000f
            DnsLookupResult(
                host = cleanHost,
                queryTimeMs = elapsedMs,
                records = emptyList(),
                errorMessage = e.localizedMessage ?: "Failed to resolve DNS records"
            )
        }
    }

    /**
     * Benchmark a single DNS/Host server for speed and packet loss.
     */
    suspend fun benchmarkServer(ip: String, count: Int = 3): Float = withContext(Dispatchers.IO) {
        var totalMs = 0f
        var successCount = 0
        for (i in 1..count) {
            val start = System.nanoTime()
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, 53), 1500)
                val ms = (System.nanoTime() - start) / 1_000_000f
                socket.close()
                totalMs += ms
                successCount++
            } catch (_: Exception) {
                // Fallback to ICMP
                val icmp = executeIcmpSingle(ip, i, 32, 1500, 64)
                if (icmp.status == PacketStatus.SUCCESS && icmp.timeMs > 0) {
                    totalMs += icmp.timeMs
                    successCount++
                }
            }
            delay(100)
        }
        if (successCount > 0) totalMs / successCount else -1f
    }
}
