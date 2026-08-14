package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PingHistoryEntity
import com.example.data.SavedHostEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorySavedScreen(
    viewModel: PingViewModel,
    onSelectHostForPing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    val savedHosts by viewModel.savedHosts.collectAsState(initial = emptyList())
    val historyList by viewModel.historyList.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Author Attribution Card (with interactive toast)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable {
                    Toast.makeText(context, "AungKyawKhant(Lawksawk)", Toast.LENGTH_LONG).show()
                }
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PING KCYBER PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "AungKyawKhant(Lawksawk)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceCardElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("v1.0 • KCYBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberTextSecondary)
                }
            }
        }

        // Tab Selector (Saved Bookmarks vs History Logs)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberSurfaceCard,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Saved Hosts (${savedHosts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ping History (${historyList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        if (selectedTab == 0) {
            // Saved Hosts Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (savedHosts.isEmpty()) {
                    item {
                        Text(
                            text = "No saved hosts yet. Tap heart icon on main ping screen to bookmark hosts.",
                            color = CyberTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                items(savedHosts) { host ->
                    SavedHostItemCard(
                        host = host,
                        onSelect = { onSelectHostForPing(host.host) },
                        onToggleFav = { viewModel.toggleFavorite(host) },
                        onDelete = { viewModel.deleteSavedHost(host) }
                    )
                }
            }
        } else {
            // History Logs Tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DIAGNOSTIC TEST SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = CyberRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = CyberRed, fontSize = 12.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (historyList.isEmpty()) {
                    item {
                        Text(
                            text = "No test sessions recorded. Run ping tests to see logs here.",
                            color = CyberTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                items(historyList) { item ->
                    HistoryItemCard(
                        item = item,
                        onSelect = { onSelectHostForPing(item.targetHost) },
                        onDelete = { viewModel.deleteHistoryItem(item.id) },
                        onShare = {
                            val report = "Ping KCyber Report:\nTarget: ${item.targetHost} (${item.resolvedIp})\nProtocol: ${item.protocol}\nAvg Latency: ${"%.1f".format(item.avgLatencyMs)} ms (Min: ${"%.1f".format(item.minLatencyMs)} ms, Max: ${"%.1f".format(item.maxLatencyMs)} ms)\nPacket Loss: ${"%.1f".format(item.packetLossPercent)}%\nPackets: ${item.packetsReceived}/${item.packetsSent}\nBy AungKyawKhant(Lawksawk)"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ping KCyber Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History", color = CyberTextPrimary) },
            text = { Text("Are you sure you want to delete all past ping sessions?", color = CyberTextSecondary) },
            containerColor = CyberSurfaceCard,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = CyberTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SavedHostItemCard(
    host: SavedHostEntity,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onToggleFav, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (host.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (host.isFavorite) CyberPink else CyberTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = host.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberSurfaceCardElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = host.category, fontSize = 9.sp, color = CyberCyan)
                        }
                    }
                    Text(
                        text = host.host,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelect, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ping",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: PingHistoryEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (item.packetLossPercent > 0) CyberRed else CyberGreen)
                    )
                    Text(
                        text = item.targetHost,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberSurfaceCardElevated)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(text = item.protocol, fontSize = 9.sp, color = CyberCyan)
                    }
                }

                Text(text = dateStr, fontSize = 10.sp, color = CyberTextMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("AVG LATENCY", fontSize = 9.sp, color = CyberTextSecondary)
                        Text(
                            text = "${"%.1f".format(item.avgLatencyMs)} ms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                item.avgLatencyMs < 40 -> CyberGreen
                                item.avgLatencyMs < 100 -> CyberYellow
                                else -> CyberRed
                            }
                        )
                    }
                    Column {
                        Text("LOSS", fontSize = 9.sp, color = CyberTextSecondary)
                        Text(
                            text = "${"%.1f".format(item.packetLossPercent)}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (item.packetLossPercent > 0) CyberRed else CyberGreen
                        )
                    }
                    Column {
                        Text("PACKETS", fontSize = 9.sp, color = CyberTextSecondary)
                        Text(
                            text = "${item.packetsReceived}/${item.packetsSent}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
