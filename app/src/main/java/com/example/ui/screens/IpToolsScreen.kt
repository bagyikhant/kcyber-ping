package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.PacketStatus
import com.example.ui.PingViewModel
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberElectric
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow

enum class IpToolTab(val title: String) {
    LAN_SCAN("LAN Scanner"),
    PORT_SCAN("Port Scanner"),
    TRACEROUTE("Traceroute"),
    DNS_LOOKUP("DNS Lookup"),
    SUBNET_CALC("Subnet Calc")
}

@Composable
fun IpToolsScreen(
    viewModel: PingViewModel,
    onSelectHostForPing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(IpToolTab.LAN_SCAN) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tool Tabs Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IpToolTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { currentTab = tab },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        containerColor = CyberSurfaceCard,
                        labelColor = CyberTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CyberCyan else CyberBorder,
                        selectedBorderColor = CyberCyan
                    )
                )
            }
        }

        // Active Tool Content
        when (currentTab) {
            IpToolTab.LAN_SCAN -> LanScannerView(viewModel, onSelectHostForPing)
            IpToolTab.PORT_SCAN -> PortScannerView(viewModel)
            IpToolTab.TRACEROUTE -> TracerouteView(viewModel)
            IpToolTab.DNS_LOOKUP -> DnsLookupView(viewModel)
            IpToolTab.SUBNET_CALC -> SubnetCalculatorView()
        }
    }
}

@Composable
fun LanScannerView(
    viewModel: PingViewModel,
    onSelectHostForPing: (String) -> Unit
) {
    val devices by viewModel.lanDevices.collectAsState()
    val isScanning by viewModel.isScanningLan.collectAsState()
    val netStatus by viewModel.networkStatus.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LOCAL SUBNET DISCOVERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Scan network ${netStatus.localIpv4.substringBeforeLast(".")}.0/24 to find connected hosts, routers, and IP availability.",
                    fontSize = 12.sp,
                    color = CyberTextSecondary
                )
                Button(
                    onClick = { viewModel.startLanScan() },
                    enabled = !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = com.example.ui.theme.CyberBg
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = com.example.ui.theme.CyberBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCANNING LOCAL SUBNET...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN LOCAL NETWORK DEVICES", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = "FOUND HOSTS (${devices.size})",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (devices.isEmpty() && !isScanning) {
                item {
                    Text(
                        text = "No devices scanned yet. Tap Scan to discover active devices.",
                        color = CyberTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            items(devices) { device ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, if (device.isGateway) CyberGreen else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectHostForPing(device.ip) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberGreen)
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = device.ip,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextPrimary
                                    )
                                    if (device.isGateway) {
                                        Text(
                                            text = "GATEWAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CyberGreen
                                        )
                                    }
                                    if (device.isCurrentDevice) {
                                        Text(
                                            text = "THIS DEVICE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CyberCyan
                                        )
                                    }
                                }
                                if (device.hostname.isNotEmpty()) {
                                    Text(text = device.hostname, fontSize = 11.sp, color = CyberTextSecondary)
                                }
                            }
                        }
                        Text(
                            text = "${"%.1f".format(device.latencyMs)} ms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortScannerView(viewModel: PingViewModel) {
    var targetHost by remember { mutableStateOf("192.168.1.1") }
    val results by viewModel.portScanResults.collectAsState()
    val isScanning by viewModel.isScanningPorts.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET HOST FOR PORT AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 192.168.1.1 or example.com", color = CyberTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.startPortScan(targetHost)
                    },
                    enabled = !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = com.example.ui.theme.CyberBg
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = com.example.ui.theme.CyberBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCANNING COMMON PORTS...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START PORT SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { port ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, if (port.isOpen) CyberGreen else CyberBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (port.isOpen) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (port.isOpen) CyberGreen else CyberTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Port ${port.port} (${port.serviceName})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (port.isOpen) CyberTextPrimary else CyberTextSecondary
                                )
                            }
                        }
                        Text(
                            text = if (port.isOpen) "OPEN (${"%.1f".format(port.latencyMs)}ms)" else "CLOSED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (port.isOpen) CyberGreen else CyberTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TracerouteView(viewModel: PingViewModel) {
    var targetHost by remember { mutableStateOf("8.8.8.8") }
    val hops by viewModel.tracerouteHops.collectAsState()
    val isTracing by viewModel.isTracing.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET DESTINATION TRACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 8.8.8.8 or 1.1.1.1", color = CyberTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.startTraceroute(targetHost)
                    },
                    enabled = !isTracing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = com.example.ui.theme.CyberBg
                    )
                ) {
                    if (isTracing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = com.example.ui.theme.CyberBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRACING ROUTE HOPS...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Polyline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXECUTE TRACEROUTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hops) { hop ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${hop.hopNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Column {
                                Text(
                                    text = hop.ip,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary
                                )
                                if (hop.hostname.isNotEmpty()) {
                                    Text(text = hop.hostname, fontSize = 11.sp, color = CyberTextSecondary)
                                }
                            }
                        }
                        Text(
                            text = if (hop.status == PacketStatus.SUCCESS && hop.latencyMs > 0) "${"%.1f".format(hop.latencyMs)} ms" else "*",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (hop.status == PacketStatus.SUCCESS) CyberGreen else CyberTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DnsLookupView(viewModel: PingViewModel) {
    var targetHost by remember { mutableStateOf("google.com") }
    val lookupResult by viewModel.dnsLookupResult.collectAsState()
    val isLookingUp by viewModel.isLookingUpDns.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DNS RECORDS QUERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. google.com or cloudflare.com", color = CyberTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.lookupDns(targetHost)
                    },
                    enabled = !isLookingUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = com.example.ui.theme.CyberBg
                    )
                ) {
                    if (isLookingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = com.example.ui.theme.CyberBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESOLVING DNS RECORDS...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("QUERY DNS RECORDS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        lookupResult?.let { res ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RESOLVED FOR ${res.host}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            text = "Query time: ${"%.1f".format(res.queryTimeMs)} ms",
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )
                    }

                    if (res.records.isEmpty() && res.errorMessage != null) {
                        Text(text = res.errorMessage, color = CyberRed, fontSize = 12.sp)
                    } else {
                        res.records.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurfaceCardElevated)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item.type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                                Text(
                                    text = item.value,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubnetCalculatorView() {
    var ipInput by remember { mutableStateOf("192.168.1.100") }
    var cidrInput by remember { mutableStateOf(24) }

    fun calculateSubnet(): SubnetDetails {
        val cidr = cidrInput.coerceIn(8, 30)
        val hostBits = 32 - cidr
        val totalHosts = (1L shl hostBits)
        val usableHosts = if (totalHosts > 2) totalHosts - 2 else 0

        val maskLong = if (cidr == 0) 0L else (-1L shl hostBits) and 0xFFFFFFFFL
        val maskStr = "${(maskLong shr 24) and 0xFF}.${(maskLong shr 16) and 0xFF}.${(maskLong shr 8) and 0xFF}.${maskLong and 0xFF}"

        val ipParts = try {
            ipInput.split(".").map { it.toLong() }
        } catch (_: Exception) {
            listOf(192L, 168L, 1L, 1L)
        }

        val ipLong = if (ipParts.size == 4) {
            (ipParts[0] shl 24) or (ipParts[1] shl 16) or (ipParts[2] shl 8) or ipParts[3]
        } else 0L

        val netLong = ipLong and maskLong
        val netStr = "${(netLong shr 24) and 0xFF}.${(netLong shr 16) and 0xFF}.${(netLong shr 8) and 0xFF}.${netLong and 0xFF}"

        val bcastLong = netLong or (maskLong.inv() and 0xFFFFFFFFL)
        val bcastStr = "${(bcastLong shr 24) and 0xFF}.${(bcastLong shr 16) and 0xFF}.${(bcastLong shr 8) and 0xFF}.${bcastLong and 0xFF}"

        val firstHostLong = netLong + 1
        val firstHostStr = "${(firstHostLong shr 24) and 0xFF}.${(firstHostLong shr 16) and 0xFF}.${(firstHostLong shr 8) and 0xFF}.${firstHostLong and 0xFF}"

        val lastHostLong = bcastLong - 1
        val lastHostStr = "${(lastHostLong shr 24) and 0xFF}.${(lastHostLong shr 16) and 0xFF}.${(lastHostLong shr 8) and 0xFF}.${lastHostLong and 0xFF}"

        return SubnetDetails(
            networkAddress = "$netStr/$cidr",
            netmask = maskStr,
            broadcast = bcastStr,
            usableRange = "$firstHostStr - $lastHostStr",
            usableHosts = usableHosts
        )
    }

    val details = calculateSubnet()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "IP / CIDR SUBNET CALCULATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("IP Address", color = CyberTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CIDR Prefix: /$cidrInput", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8, 16, 24, 28, 30).forEach { prefix ->
                            val isSel = cidrInput == prefix
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) CyberCyan else CyberSurfaceCardElevated)
                                    .clickable { cidrInput = prefix }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "/$prefix",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) com.example.ui.theme.CyberBg else CyberTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CALCULATED SUBNET METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                SubnetRow("Network Address", details.networkAddress)
                SubnetRow("Subnet Netmask", details.netmask)
                SubnetRow("Usable Host Range", details.usableRange)
                SubnetRow("Broadcast Address", details.broadcast)
                SubnetRow("Total Usable Hosts", "${details.usableHosts}")
            }
        }
    }
}

@Composable
fun SubnetRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceCardElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = CyberTextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberTextPrimary
        )
    }
}

data class SubnetDetails(
    val networkAddress: String,
    val netmask: String,
    val broadcast: String,
    val usableRange: String,
    val usableHosts: Long
)
