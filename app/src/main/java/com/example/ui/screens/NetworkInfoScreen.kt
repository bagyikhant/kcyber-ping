package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun NetworkInfoScreen(
    viewModel: PingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.networkStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshingNetwork.collectAsState()

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Transport Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (status.isConnected) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f))
                            .border(1.dp, if (status.isConnected) CyberGreen else CyberRed, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status.isVpnActive) Icons.Default.VpnKey else if (status.connectionType.contains("Wi-Fi")) Icons.Default.Wifi else Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = if (status.isConnected) CyberGreen else CyberRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (status.isConnected) CyberGreen else CyberRed)
                            )
                            Text(
                                text = if (status.isConnected) "ACTIVE CONNECTION" else "DISCONNECTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isConnected) CyberGreen else CyberRed,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = status.connectionType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary
                        )
                        if (status.wifiSsid.isNotEmpty()) {
                            Text(
                                text = "SSID: ${status.wifiSsid}",
                                fontSize = 12.sp,
                                color = CyberCyan
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.refreshNetworkInfo() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceCardElevated)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyberCyan)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Link Speed & Signal Strength (if Wi-Fi)
        if (status.linkSpeedMbps > 0 || status.wifiRssiDbm != 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "LINK SPEED & TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Link Speed Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberSurfaceCardElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("LINK SPEED", fontSize = 10.sp, color = CyberTextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${status.linkSpeedMbps} Mbps",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberCyan
                                )
                            }
                        }

                        // Signal Level Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberSurfaceCardElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("SIGNAL (RSSI)", fontSize = 10.sp, color = CyberTextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (status.wifiRssiDbm != 0) "${status.wifiRssiDbm} dBm" else "--",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (status.wifiRssiDbm > -65) CyberGreen else CyberYellow
                                )
                            }
                        }

                        // Frequency Card
                        if (status.wifiFrequencyGhz > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberSurfaceCardElevated)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("BAND", fontSize = 10.sp, color = CyberTextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${"%.1f".format(status.wifiFrequencyGhz)} GHz",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberPink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Network IP Details Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LOCAL NETWORK IP CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                NetworkDataRow(
                    icon = Icons.Default.Router,
                    label = "Local IPv4",
                    value = status.localIpv4,
                    onCopy = { copyToClipboard("Local IPv4", status.localIpv4) }
                )

                NetworkDataRow(
                    icon = Icons.Default.Router,
                    label = "Gateway Router",
                    value = status.gatewayIp,
                    onCopy = { copyToClipboard("Gateway Router", status.gatewayIp) }
                )

                NetworkDataRow(
                    icon = Icons.Default.Dns,
                    label = "Subnet Mask",
                    value = status.subnetMask,
                    onCopy = { copyToClipboard("Subnet Mask", status.subnetMask) }
                )

                if (status.localIpv6.isNotEmpty()) {
                    NetworkDataRow(
                        icon = Icons.Default.Language,
                        label = "Local IPv6",
                        value = status.localIpv6,
                        onCopy = { copyToClipboard("Local IPv6", status.localIpv6) }
                    )
                }

                if (status.dnsServers.isNotEmpty()) {
                    NetworkDataRow(
                        icon = Icons.Default.Dns,
                        label = "DNS Servers",
                        value = status.dnsServers.joinToString(", "),
                        onCopy = { copyToClipboard("DNS Servers", status.dnsServers.joinToString(", ")) }
                    )
                }
            }
        }

        // Public IP & ISP Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PUBLIC WAN IP & ISP IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberElectric,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = CyberElectric,
                        modifier = Modifier.size(18.dp)
                    )
                }

                NetworkDataRow(
                    icon = Icons.Default.Public,
                    label = "Public IPv4",
                    value = status.publicIp,
                    valueColor = CyberElectric,
                    onCopy = { copyToClipboard("Public IP", status.publicIp) }
                )

                if (status.publicIsp.isNotEmpty()) {
                    NetworkDataRow(
                        icon = Icons.Default.Security,
                        label = "Internet Provider (ISP)",
                        value = status.publicIsp,
                        onCopy = { copyToClipboard("ISP", status.publicIsp) }
                    )
                }

                if (status.publicCountry.isNotEmpty() && status.publicCountry != "-") {
                    NetworkDataRow(
                        icon = Icons.Default.Language,
                        label = "Geo Location",
                        value = "${if (status.publicCity.isNotEmpty()) "${status.publicCity}, " else ""}${status.publicCountry}",
                        onCopy = { copyToClipboard("Geo Location", "${status.publicCity}, ${status.publicCountry}") }
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkDataRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = CyberTextPrimary,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceCardElevated)
            .clickable { onCopy() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = CyberTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = valueColor
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = CyberTextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
