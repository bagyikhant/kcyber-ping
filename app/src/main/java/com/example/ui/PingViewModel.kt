package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PingHistoryEntity
import com.example.data.SavedHostEntity
import com.example.network.DnsBenchmarkResult
import com.example.network.DnsLookupResult
import com.example.network.LanDevice
import com.example.network.NetworkHelper
import com.example.network.NetworkStatus
import com.example.network.PacketStatus
import com.example.network.PingConfig
import com.example.network.PingEngine
import com.example.network.PingPacket
import com.example.network.PingProtocol
import com.example.network.PingSessionState
import com.example.network.PortScanResult
import com.example.network.TracerouteHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class PingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.pingDao()

    // Main Ping State
    private val _pingConfig = MutableStateFlow(PingConfig())
    val pingConfig: StateFlow<PingConfig> = _pingConfig.asStateFlow()

    private val _sessionState = MutableStateFlow(PingSessionState())
    val sessionState: StateFlow<PingSessionState> = _sessionState.asStateFlow()

    private var pingJob: Job? = null

    // Network Status State
    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _isRefreshingNetwork = MutableStateFlow(false)
    val isRefreshingNetwork: StateFlow<Boolean> = _isRefreshingNetwork.asStateFlow()

    // Fast DNS Benchmark State
    private val _dnsBenchmarks = MutableStateFlow<List<DnsBenchmarkResult>>(emptyList())
    val dnsBenchmarks: StateFlow<List<DnsBenchmarkResult>> = _dnsBenchmarks.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    // Subnet LAN Scanner State
    private val _lanDevices = MutableStateFlow<List<LanDevice>>(emptyList())
    val lanDevices: StateFlow<List<LanDevice>> = _lanDevices.asStateFlow()

    private val _isScanningLan = MutableStateFlow(false)
    val isScanningLan: StateFlow<Boolean> = _isScanningLan.asStateFlow()

    // Port Scanner State
    private val _portScanResults = MutableStateFlow<List<PortScanResult>>(emptyList())
    val portScanResults: StateFlow<List<PortScanResult>> = _portScanResults.asStateFlow()

    private val _isScanningPorts = MutableStateFlow(false)
    val isScanningPorts: StateFlow<Boolean> = _isScanningPorts.asStateFlow()

    // Traceroute State
    private val _tracerouteHops = MutableStateFlow<List<TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteHop>> = _tracerouteHops.asStateFlow()

    private val _isTracing = MutableStateFlow(false)
    val isTracing: StateFlow<Boolean> = _isTracing.asStateFlow()

    // DNS Lookup State
    private val _dnsLookupResult = MutableStateFlow<DnsLookupResult?>(null)
    val dnsLookupResult: StateFlow<DnsLookupResult?> = _dnsLookupResult.asStateFlow()

    private val _isLookingUpDns = MutableStateFlow(false)
    val isLookingUpDns: StateFlow<Boolean> = _isLookingUpDns.asStateFlow()

    // Database Flows
    val historyList = dao.getAllHistory()
    val savedHosts = dao.getAllSavedHosts()

    // Settings
    val vibrateOnPacket = MutableStateFlow(true)

    init {
        initDefaultBenchmarkList()
        seedPresetHosts()
        refreshNetworkInfo()
    }

    private fun seedPresetHosts() {
        viewModelScope.launch(Dispatchers.IO) {
            val defaults = listOf(
                SavedHostEntity(host = "8.8.8.8", label = "Google DNS", category = "DNS", isFavorite = true),
                SavedHostEntity(host = "1.1.1.1", label = "Cloudflare Warp", category = "DNS", isFavorite = true),
                SavedHostEntity(host = "9.9.9.9", label = "Quad9 Security", category = "DNS", isFavorite = false),
                SavedHostEntity(host = "192.168.1.1", label = "Default Router Gateway", category = "Local", isFavorite = true),
                SavedHostEntity(host = "cloudflare.com", label = "Cloudflare Edge", category = "Cloud", isFavorite = false),
                SavedHostEntity(host = "google.com", label = "Google Search", category = "Web", isFavorite = false),
                SavedHostEntity(host = "steampowered.com", label = "Steam Gaming Edge", category = "Gaming", isFavorite = false)
            )
            dao.insertSavedHosts(defaults)
        }
    }

    private fun initDefaultBenchmarkList() {
        _dnsBenchmarks.value = listOf(
            DnsBenchmarkResult("cf", "Cloudflare DNS", "1.1.1.1", "Ultra-fast privacy-first DNS"),
            DnsBenchmarkResult("google", "Google Public DNS", "8.8.8.8", "Global high-resilience Anycast DNS"),
            DnsBenchmarkResult("quad9", "Quad9 Secure DNS", "9.9.9.9", "Threat-blocking secure DNS"),
            DnsBenchmarkResult("opendns", "OpenDNS (Cisco)", "208.67.222.222", "Enterprise-grade smart caching"),
            DnsBenchmarkResult("adguard", "AdGuard DNS", "94.140.14.14", "Ad-blocking and tracker filtering"),
            DnsBenchmarkResult("level3", "Level3 Lumen", "4.2.2.1", "Tier-1 backbone global transit"),
            DnsBenchmarkResult("steam", "Valve / Steam Node", "162.254.192.1", "Gaming network backbone"),
            DnsBenchmarkResult("aws", "AWS Global Accelerator", "13.248.118.1", "Amazon low-latency Anycast")
        )
    }

    fun updateConfig(config: PingConfig) {
        _pingConfig.value = config
    }

    fun setHost(host: String, protocol: PingProtocol = _pingConfig.value.protocol, port: Int = _pingConfig.value.port) {
        _pingConfig.update { it.copy(host = host.trim(), protocol = protocol, port = port) }
    }

    fun startPing() {
        if (_sessionState.value.isRunning) {
            stopPing()
            return
        }

        val config = _pingConfig.value
        val cleanHost = config.host.trim().ifEmpty { "8.8.8.8" }

        _sessionState.value = PingSessionState(
            isRunning = true,
            targetHost = cleanHost,
            resolvedIp = "Resolving...",
            protocol = config.protocol,
            packets = emptyList(),
            logs = listOf("Starting ${config.protocol.name} ping to $cleanHost...")
        )

        pingJob = viewModelScope.launch(Dispatchers.IO) {
            val resolved = PingEngine.resolveHostIp(cleanHost)
            _sessionState.update { it.copy(resolvedIp = resolved) }

            var sent = 0
            var received = 0
            var minMs = Float.MAX_VALUE
            var maxMs = 0f
            var sumMs = 0f
            val latencies = mutableListOf<Float>()
            val packetList = mutableListOf<PingPacket>()
            val logs = mutableListOf<String>()
            logs.add("PING $cleanHost ($resolved): ${config.packetSize} data bytes")

            PingEngine.startPing(config.copy(host = cleanHost))
                .catch { e ->
                    _sessionState.update { 
                        it.copy(
                            isRunning = false,
                            logs = it.logs + "Execution error: ${e.localizedMessage}"
                        ) 
                    }
                }
                .collect { packet ->
                    sent++
                    if (packet.status == PacketStatus.SUCCESS && packet.timeMs > 0f) {
                        received++
                        minMs = min(minMs, packet.timeMs)
                        maxMs = max(maxMs, packet.timeMs)
                        sumMs += packet.timeMs
                        latencies.add(packet.timeMs)
                        if (vibrateOnPacket.value) triggerHapticFeedback()
                    }

                    packetList.add(0, packet) // newest first
                    if (packetList.size > 200) packetList.removeLast()

                    val avg = if (received > 0) sumMs / received else 0f
                    val loss = if (sent > 0) ((sent - received).toFloat() / sent) * 100f else 0f

                    // Calculate jitter (mean absolute deviation)
                    val jitter = if (latencies.size > 1) {
                        val mean = avg
                        val variance = latencies.map { (it - mean).pow(2) }.average()
                        sqrt(variance).toFloat()
                    } else 0f

                    logs.add(packet.message)
                    if (logs.size > 150) logs.removeAt(0)

                    _sessionState.value = PingSessionState(
                        isRunning = true,
                        targetHost = cleanHost,
                        resolvedIp = resolved,
                        protocol = config.protocol,
                        packets = packetList.toList(),
                        packetsSent = sent,
                        packetsReceived = received,
                        packetLossPercent = loss,
                        minLatencyMs = if (minMs == Float.MAX_VALUE) 0f else minMs,
                        avgLatencyMs = avg,
                        maxLatencyMs = maxMs,
                        jitterMs = jitter,
                        currentLatencyMs = packet.timeMs,
                        logs = logs.toList()
                    )
                }

            // Session completed naturally
            finalizeSession()
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        finalizeSession()
    }

    private fun finalizeSession() {
        val current = _sessionState.value
        _sessionState.update { it.copy(isRunning = false) }

        if (current.packetsSent > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertHistory(
                    PingHistoryEntity(
                        targetHost = current.targetHost,
                        resolvedIp = current.resolvedIp,
                        protocol = current.protocol.name,
                        packetsSent = current.packetsSent,
                        packetsReceived = current.packetsReceived,
                        packetLossPercent = current.packetLossPercent,
                        minLatencyMs = current.minLatencyMs,
                        avgLatencyMs = current.avgLatencyMs,
                        maxLatencyMs = current.maxLatencyMs,
                        jitterMs = current.jitterMs
                    )
                )
            }
        }
    }

    fun refreshNetworkInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingNetwork.value = true
            val status = NetworkHelper.getNetworkStatus(getApplication())
            _networkStatus.value = status

            // Async fetch public IP & ISP
            val publicInfo = NetworkHelper.fetchPublicIpInfo()
            _networkStatus.update {
                it.copy(
                    publicIp = publicInfo.ip,
                    publicIsp = publicInfo.isp,
                    publicCountry = publicInfo.country,
                    publicCity = publicInfo.city
                )
            }
            _isRefreshingNetwork.value = false
        }
    }

    fun startDnsBenchmark() {
        if (_isBenchmarking.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isBenchmarking.value = true
            val currentList = _dnsBenchmarks.value.map { it.copy(isTesting = true, error = null) }
            _dnsBenchmarks.value = currentList

            val updated = mutableListOf<DnsBenchmarkResult>()
            currentList.forEach { item ->
                _dnsBenchmarks.update { list ->
                    list.map { if (it.id == item.id) it.copy(isTesting = true) else it }
                }
                val avgLatency = PingEngine.benchmarkServer(item.primaryIp, count = 3)
                val res = if (avgLatency > 0) {
                    item.copy(
                        latencyMs = avgLatency,
                        isTesting = false,
                        isSuccess = true
                    )
                } else {
                    item.copy(
                        latencyMs = -1f,
                        isTesting = false,
                        isSuccess = false,
                        error = "Timeout / Unreachable"
                    )
                }
                updated.add(res)
                _dnsBenchmarks.update { list ->
                    list.map { if (it.id == item.id) res else it }
                }
            }

            // Sort by lowest latency
            val sorted = _dnsBenchmarks.value.sortedWith(
                compareBy<DnsBenchmarkResult> { !it.isSuccess }
                    .thenBy { if (it.latencyMs > 0) it.latencyMs else Float.MAX_VALUE }
            )
            _dnsBenchmarks.value = sorted
            _isBenchmarking.value = false
        }
    }

    fun startLanScan() {
        if (_isScanningLan.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningLan.value = true
            _lanDevices.value = emptyList()

            val netStatus = _networkStatus.value
            val localIp = netStatus.localIpv4
            val prefix = if (localIp.contains(".")) {
                localIp.substringBeforeLast(".") + "."
            } else "192.168.1."

            val foundList = mutableListOf<LanDevice>()
            PingEngine.scanSubnet(
                baseIpPrefix = prefix,
                currentDeviceIp = localIp,
                gatewayIp = netStatus.gatewayIp
            ) { device ->
                foundList.add(device)
                _lanDevices.value = foundList.sortedBy { it.ip.substringAfterLast(".").toIntOrNull() ?: 0 }
            }
            _isScanningLan.value = false
        }
    }

    fun startPortScan(targetHost: String) {
        if (_isScanningPorts.value) return
        val host = targetHost.trim().ifEmpty { "192.168.1.1" }
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningPorts.value = true
            _portScanResults.value = emptyList()

            val standardPorts = listOf(
                21 to "FTP",
                22 to "SSH",
                23 to "Telnet",
                25 to "SMTP",
                53 to "DNS",
                80 to "HTTP",
                110 to "POP3",
                143 to "IMAP",
                443 to "HTTPS",
                445 to "SMB",
                993 to "IMAPS",
                995 to "POP3S",
                3306 to "MySQL",
                3389 to "RDP",
                5432 to "PostgreSQL",
                8080 to "HTTP-Alt",
                8443 to "HTTPS-Alt"
            )

            val results = mutableListOf<PortScanResult>()
            PingEngine.scanPorts(host, standardPorts) { res ->
                results.add(res)
                _portScanResults.value = results.sortedBy { it.port }
            }
            _isScanningPorts.value = false
        }
    }

    fun startTraceroute(targetHost: String) {
        if (_isTracing.value) return
        val host = targetHost.trim().ifEmpty { "8.8.8.8" }
        viewModelScope.launch(Dispatchers.IO) {
            _isTracing.value = true
            _tracerouteHops.value = emptyList()

            val hops = mutableListOf<TracerouteHop>()
            PingEngine.runTraceroute(host) { hop ->
                hops.add(hop)
                _tracerouteHops.value = hops.sortedBy { it.hopNumber }
            }
            _isTracing.value = false
        }
    }

    fun lookupDns(targetHost: String) {
        if (_isLookingUpDns.value) return
        val host = targetHost.trim().ifEmpty { "google.com" }
        viewModelScope.launch(Dispatchers.IO) {
            _isLookingUpDns.value = true
            val result = PingEngine.lookupDnsRecords(host)
            _dnsLookupResult.value = result
            _isLookingUpDns.value = false
        }
    }

    fun addSavedHost(host: String, label: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSavedHost(
                SavedHostEntity(
                    host = host.trim(),
                    label = label.trim().ifEmpty { host.trim() },
                    category = category,
                    isFavorite = true
                )
            )
        }
    }

    fun deleteSavedHost(host: SavedHostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSavedHost(host)
        }
    }

    fun toggleFavorite(host: SavedHostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSavedHost(host.copy(isFavorite = !host.isFavorite))
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllHistory()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHistoryById(id)
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) { }
    }
}
