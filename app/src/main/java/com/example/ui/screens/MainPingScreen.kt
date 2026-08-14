package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.network.PingConfig
import com.example.network.PingProtocol
import com.example.ui.PingViewModel
import com.example.ui.components.PingTerminalLog
import com.example.ui.components.PingWaveChart
import com.example.ui.components.TelemetryGrid
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun MainPingScreen(
    viewModel: PingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val config by viewModel.pingConfig.collectAsState()
    val session by viewModel.sessionState.collectAsState()
    val vibrate by viewModel.vibrateOnPacket.collectAsState()
    val vibrateLoss by viewModel.vibrateOnLoss.collectAsState()
    val showNotification by viewModel.showNotification.collectAsState()

    var showAdvancedSettings by remember { mutableStateOf(false) }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification.value = true
            Toast.makeText(context, "Notification permission enabled for background monitoring", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.showNotification.value = false
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val quickPresets = listOf(
        "8.8.8.8" to "Google DNS",
        "1.1.1.1" to "Cloudflare",
        "192.168.1.1" to "Gateway",
        "9.9.9.9" to "Quad9",
        "cloudflare.com" to "CF Edge",
        "steampowered.com" to "Steam",
        "google.com" to "Google"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Target Host Input Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TARGET HOST OR IP",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = config.host,
                        onValueChange = { viewModel.setHost(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. 8.8.8.8 or google.com", color = CyberTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                viewModel.startPing()
                            }
                        ),
                        trailingIcon = {
                            if (config.host.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setHost("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = CyberTextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            cursorColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Bookmark Save Button
                    IconButton(
                        onClick = {
                            if (config.host.isNotBlank()) {
                                viewModel.addSavedHost(config.host, config.host, "Custom")
                                Toast.makeText(context, "Saved ${config.host} to favorites", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceCardElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Save Host",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Protocol Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingProtocol.values().forEach { proto ->
                        val isSelected = config.protocol == proto
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateConfig(
                                    config.copy(
                                        protocol = proto,
                                        port = if (proto == PingProtocol.ICMP) 0 else if (proto == PingProtocol.TCP) 80 else 443
                                    )
                                )
                            },
                            label = {
                                Text(
                                    text = proto.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = CyberSurfaceCardElevated,
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

                // Quick Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPresets.forEach { (ip, label) ->
                        val isCurrent = config.host.equals(ip, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceCardElevated)
                                .border(1.dp, if (isCurrent) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setHost(ip)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isCurrent) CyberCyan else CyberTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Toggle Advanced Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedSettings = !showAdvancedSettings }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Advanced Parameters (Packets, Size, Interval, Haptics)",
                            fontSize = 12.sp,
                            color = CyberTextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Advanced Settings Panel
                AnimatedVisibility(visible = showAdvancedSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(com.example.ui.theme.CyberBg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Packet Count Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Packet Count:", fontSize = 12.sp, color = CyberTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(4, 10, 20, 0).forEach { cnt ->
                                    val isSelected = config.count == cnt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CyberCyan else CyberSurfaceCardElevated)
                                            .clickable { viewModel.updateConfig(config.copy(count = cnt)) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (cnt == 0) "∞" else "$cnt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) com.example.ui.theme.CyberBg else CyberTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Packet Size Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payload Size:", fontSize = 12.sp, color = CyberTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(32, 64, 128, 512).forEach { sz ->
                                    val isSelected = config.packetSize == sz
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CyberCyan else CyberSurfaceCardElevated)
                                            .clickable { viewModel.updateConfig(config.copy(packetSize = sz)) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${sz}B",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) com.example.ui.theme.CyberBg else CyberTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Interval Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Interval:", fontSize = 12.sp, color = CyberTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(500L to "0.5s", 1000L to "1.0s", 2000L to "2.0s").forEach { (ms, lbl) ->
                                    val isSelected = config.intervalMs == ms
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CyberCyan else CyberSurfaceCardElevated)
                                            .clickable { viewModel.updateConfig(config.copy(intervalMs = ms)) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = lbl,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) com.example.ui.theme.CyberBg else CyberTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Haptic Packet Loss Alert Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vibrate Pulse on Packet Loss", fontSize = 12.sp, color = CyberTextSecondary)
                            Switch(
                                checked = vibrateLoss,
                                onCheckedChange = { viewModel.vibrateOnLoss.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = com.example.ui.theme.CyberBg,
                                    checkedTrackColor = CyberCyan,
                                    uncheckedThumbColor = CyberTextSecondary,
                                    uncheckedTrackColor = CyberSurfaceCardElevated
                                )
                            )
                        }
                    }
                }
            }
        }

        // Quick Controls: Notification Bar & Vibration On/Off Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BACKGROUND & HAPTICS CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // 1. Notification Bar (Foreground & Background running)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showNotification) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showNotification) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Notification Bar",
                                tint = if (showNotification) CyberCyan else CyberTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Show in Notification Bar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Foreground & background live latency monitor",
                                fontSize = 11.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = showNotification,
                        onCheckedChange = { isEnabled ->
                            if (isEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (!hasPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.showNotification.value = true
                                    }
                                } else {
                                    viewModel.showNotification.value = true
                                }
                            } else {
                                viewModel.showNotification.value = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = com.example.ui.theme.CyberBg,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberTextSecondary,
                            uncheckedTrackColor = CyberSurfaceCardElevated
                        )
                    )
                }

                // 2. Vibration On / Off
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (vibrate) CyberGreen.copy(alpha = 0.15f) else CyberSurfaceCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Vibration",
                                tint = if (vibrate) CyberGreen else CyberTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Vibration",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (vibrate) CyberGreen.copy(alpha = 0.2f) else CyberBorder)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (vibrate) "ON" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (vibrate) CyberGreen else CyberTextMuted
                                    )
                                }
                            }
                            Text(
                                text = "Tactile haptic pulse on ping response",
                                fontSize = 11.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (vibrate) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberSurfaceCardElevated)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.testVibration()
                                        Toast.makeText(context, "Testing haptic tick...", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "TEST",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                        }

                        Switch(
                            checked = vibrate,
                            onCheckedChange = { viewModel.vibrateOnPacket.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = com.example.ui.theme.CyberBg,
                                checkedTrackColor = CyberGreen,
                                uncheckedThumbColor = CyberTextSecondary,
                                uncheckedTrackColor = CyberSurfaceCardElevated
                            )
                        )
                    }
                }
            }
        }

        // Live Wave Chart
        PingWaveChart(
            packets = session.packets,
            avgLatency = session.avgLatencyMs,
            isRunning = session.isRunning
        )

        // Telemetry Statistics Grid
        TelemetryGrid(
            minMs = session.minLatencyMs,
            avgMs = session.avgLatencyMs,
            maxMs = session.maxLatencyMs,
            jitterMs = session.jitterMs,
            sent = session.packetsSent,
            received = session.packetsReceived,
            lossPercent = session.packetLossPercent
        )

        // Start / Stop Action Button
        Button(
            onClick = {
                keyboardController?.hide()
                if (session.isRunning) {
                    viewModel.stopPing()
                } else {
                    // Check notification permission if notification is enabled and on Android 13+
                    if (showNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPerm) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    viewModel.startPing()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (session.isRunning) CyberRed else CyberCyan,
                contentColor = if (session.isRunning) CyberTextPrimary else com.example.ui.theme.CyberBg
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (session.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (session.isRunning) "STOP PING SESSION" else "START PING DIAGNOSTIC",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Live Terminal Console
        PingTerminalLog(logs = session.logs)
    }
}

